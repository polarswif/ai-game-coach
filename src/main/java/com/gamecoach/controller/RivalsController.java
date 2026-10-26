package com.gamecoach.controller;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.MatchAnalyzer;
import com.gamecoach.analysis.PlayerAnalysis;
import com.gamecoach.datasource.MatchHistorySource;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.RoundScore;
import com.gamecoach.parsing.BadUploadException;
import com.gamecoach.parsing.HeroDoubt;
import com.gamecoach.parsing.ParsedScoreboard;
import com.gamecoach.parsing.ScoreboardParser;
import com.gamecoach.parsing.ScoreboardValidator;
import com.gamecoach.parsing.Screenshot;
import com.gamecoach.parsing.UploadedMatchStore;
import com.gamecoach.service.CoachingService;
import com.gamecoach.service.CoachingUnavailableException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marvel Rivals endpoints: screenshot upload and review, match picker, scoreboard, analysis, and
 * coaching feedback. Uploaded matches and the match source share one set of ids, so every endpoint
 * works for both. Unknown matches or players throw NoSuchElementException, which ApiExceptionHandler
 * turns into a 404.
 */
@RestController
@RequestMapping("/api/rivals")
public class RivalsController {

    private final MatchHistorySource matches;
    private final MatchAnalyzer analyzer;
    private final CoachingService coach;
    private final UploadedMatchStore uploads;
    private final ScoreboardValidator validator;
    private final Optional<ScoreboardParser> parser;
    private final HeroRoleCatalog heroes;

    private static final Pattern SCORE = Pattern.compile("^\\s*(\\d{1,2})\\s*[-:]\\s*(\\d{1,2})\\s*$");

    /**
     * The parser is Optional because it only exists with gamecoach.coach.provider=claude. Spring
     * passes Optional.empty() when there's no such bean, instead of failing to start.
     */
    public RivalsController(MatchHistorySource matches, MatchAnalyzer analyzer, CoachingService coach,
                            UploadedMatchStore uploads, ScoreboardValidator validator,
                            Optional<ScoreboardParser> parser, HeroRoleCatalog heroes) {
        this.matches = matches;
        this.analyzer = analyzer;
        this.coach = coach;
        this.uploads = uploads;
        this.validator = validator;
        this.parser = parser;
        this.heroes = heroes;
    }

    /**
     * Reads an end-of-match scoreboard screenshot (multipart form field "screenshot"). The optional
     * "score" field is the round score from the match history screen, your team's rounds first,
     * e.g. "3-2". The optional "hero" field is the hero you played: hero portraits are small and the
     * reader can mistake one for another, and your own hero is what coaching depends on most.
     * Returns the match as read plus warnings to review; its matchId works with every other endpoint.
     */
    @PostMapping("/matches")
    public ResponseEntity<ParsedScoreboard> upload(@RequestParam("screenshot") MultipartFile screenshot,
                                                   @RequestParam(name = "score", required = false) String score,
                                                   @RequestParam(name = "hero", required = false) String hero)
            throws IOException {
        ScoreboardParser reader = parser.orElseThrow(() -> new CoachingUnavailableException(
                "Reading screenshots needs the Claude API: set gamecoach.coach.provider=claude.", null));
        RoundScore rounds = parseScore(score);
        String yourHero = officialHero(hero);           // checked before the paid call, so a typo costs nothing
        Screenshot image = new Screenshot(screenshot.getBytes(), screenshot.getContentType());
        ParsedScoreboard parsed = reader.parse(image, uploads.newId(), rounds);
        if (yourHero != null) {
            parsed = withUploaderHero(parsed, yourHero);
        }
        uploads.save(parsed.match());
        return ResponseEntity.status(HttpStatus.CREATED).body(parsed);
    }

    /**
     * The review step: replaces an uploaded match with the user's corrected version and checks it
     * again. Only uploaded matches can be corrected. The id in the path wins over the body's.
     */
    @PutMapping("/matches/{matchId}")
    public ParsedScoreboard correct(@PathVariable("matchId") String matchId, @RequestBody MatchDetails corrected) {
        if (!uploads.contains(matchId)) {
            throw new java.util.NoSuchElementException("No uploaded match with id " + matchId);
        }
        MatchDetails match = new MatchDetails(matchId, corrected.mode(), corrected.map(), corrected.duration(),
                corrected.result(), corrected.replayId(),
                corrected.bans() == null ? List.of() : corrected.bans(),
                corrected.players() == null ? List.of() : corrected.players(),
                corrected.score());
        uploads.save(match);
        return new ParsedScoreboard(match, validator.check(match));
    }

    /** The match picker. */
    @GetMapping("/players/{username}/matches")
    public List<MatchSummary> recentMatches(@PathVariable("username") String username) {
        return matches.getRecentMatches(username);
    }

    /** Full scoreboard for one match. */
    @GetMapping("/matches/{matchId}")
    public MatchDetails matchDetails(@PathVariable("matchId") String matchId) {
        return match(matchId);
    }

    /**
     * The computed comparisons for the player named in ?username= within that match.
     * Leave ?username= out to analyze the uploader (the highlighted row).
     */
    @GetMapping("/matches/{matchId}/analysis")
    public PlayerAnalysis analysis(@PathVariable("matchId") String matchId,
                                   @RequestParam(name = "username", required = false) String username) {
        return analyzer.analyze(match(matchId), username);
    }

    /** Coaching feedback for the player named in ?username=, or the uploader when it's left out. */
    @PostMapping("/matches/{matchId}/feedback")
    public Feedback feedback(@PathVariable("matchId") String matchId,
                             @RequestParam(name = "username", required = false) String username) {
        MatchDetails match = match(matchId);
        return coach.coach(match, analyzer.analyze(match, username));
    }

    /** An uploaded match if there is one with this id, otherwise the match source's. */
    private MatchDetails match(String matchId) {
        return uploads.find(matchId).orElseGet(() -> matches.getMatchDetails(matchId));
    }

    /** The typed hero in the table's spelling; null when absent; 400 when it isn't a known hero. */
    private String officialHero(String hero) {
        if (hero == null || hero.isBlank()) {
            return null;
        }
        return heroes.officialName(hero).orElseThrow(() -> new BadUploadException(
                "\"" + hero.strip() + "\" isn't a Marvel Rivals hero I know. Use the name as the game shows it, e.g. Daredevil."));
    }

    /**
     * Puts the hero you said you played on the highlighted row. The reader's own doubts about that
     * row's hero are dropped (you've answered them), a note says what changed, and the match is
     * checked again so a hero that doesn't fit the row's role icon is still flagged.
     */
    ParsedScoreboard withUploaderHero(ParsedScoreboard parsed, String hero) {
        MatchDetails match = parsed.match();
        PlayerLine you = match.players().stream().filter(PlayerLine::uploader).findFirst().orElse(null);
        if (you == null) {
            List<String> warnings = new java.util.ArrayList<>(parsed.warnings());
            warnings.add("No highlighted row was found, so the hero you gave (" + hero + ") wasn't applied.");
            return new ParsedScoreboard(match, warnings, parsed.heroDoubts());
        }
        List<PlayerLine> players = match.players().stream()
                .map(p -> p == you ? p.withLastHero(hero) : p)
                .toList();
        MatchDetails fixed = match.withPlayers(players);

        java.util.LinkedHashSet<String> warnings = new java.util.LinkedHashSet<>();
        for (String w : parsed.warnings()) {
            boolean aboutYourHero = you.name() != null && w.contains(you.name())
                    && w.toLowerCase(java.util.Locale.ROOT).contains("hero");
            if (!aboutYourHero) {
                warnings.add(w);
            }
        }
        if (!HeroRoleCatalog.sameHero(you.lastHero(), hero)) {
            warnings.add("Your hero set to " + hero + " as you said (the screenshot was read as "
                    + (you.lastHero() == null ? "no hero" : you.lastHero()) + ").");
        }
        warnings.addAll(validator.check(fixed));
        return new ParsedScoreboard(fixed, List.copyOf(warnings),
                HeroDoubt.without(parsed.heroDoubts(), match.players().indexOf(you)));
    }

    /** "3-2" or "3:2" → RoundScore(3, 2); null when absent. */
    static RoundScore parseScore(String score) {
        if (score == null || score.isBlank()) {
            return null;
        }
        Matcher m = SCORE.matcher(score);
        if (!m.matches()) {
            throw new BadUploadException("The score should look like 3-2 (your team's rounds first), not \"" + score + "\".");
        }
        return new RoundScore(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
    }
}

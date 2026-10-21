package com.gamecoach.controller;

import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.analysis.OverwatchMatchAnalyzer;
import com.gamecoach.analysis.OverwatchPlayerAnalysis;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchPlayerLine;
import com.gamecoach.parsing.BadUploadException;
import com.gamecoach.parsing.OverwatchMatchReader;
import com.gamecoach.parsing.OverwatchMatchStore;
import com.gamecoach.parsing.OverwatchMatchValidator;
import com.gamecoach.parsing.OverwatchScreens;
import com.gamecoach.parsing.ParsedOverwatchMatch;
import com.gamecoach.parsing.Screenshot;
import com.gamecoach.service.CoachingUnavailableException;
import com.gamecoach.service.OverwatchCoachingService;

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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Overwatch match coaching from the end-of-match report screenshots, the same flow as Rivals:
 * upload, review (and fix with PUT), then analysis and feedback. Reading and coaching need the
 * Claude API (gamecoach.coach.provider=claude); without it they answer 503 with what to set.
 */
@RestController
@RequestMapping("/api/overwatch/matches")
public class OverwatchMatchController {

    private final Optional<OverwatchMatchReader> reader;
    private final Optional<OverwatchCoachingService> coach;
    private final OverwatchMatchStore store;
    private final OverwatchMatchValidator validator;
    private final OverwatchMatchAnalyzer analyzer;
    private final OverwatchHeroCatalog heroes;

    /** Reader and coach are Optional: they only exist with the Claude provider. */
    public OverwatchMatchController(Optional<OverwatchMatchReader> reader, Optional<OverwatchCoachingService> coach,
                                    OverwatchMatchStore store, OverwatchMatchValidator validator,
                                    OverwatchMatchAnalyzer analyzer, OverwatchHeroCatalog heroes) {
        this.reader = reader;
        this.coach = coach;
        this.store = store;
        this.validator = validator;
        this.analyzer = analyzer;
        this.heroes = heroes;
    }

    /**
     * Reads the match report. Form fields: "teams" (the Teams tab, required), "summary" (the
     * Summary tab, recommended: it has the result, score, map, mode and length), "personal" (the
     * Personal tab, once per hero, optional) and "hero" (the hero you ended on, optional; only
     * needed if the reader gets your portrait wrong). Returns the match and warnings to review.
     */
    @PostMapping
    public ResponseEntity<ParsedOverwatchMatch> upload(
            @RequestParam("teams") MultipartFile teams,
            @RequestParam(name = "summary", required = false) MultipartFile summary,
            @RequestParam(name = "personal", required = false) List<MultipartFile> personal,
            @RequestParam(name = "hero", required = false) String hero) throws IOException {
        OverwatchMatchReader matchReader = reader.orElseThrow(() -> new CoachingUnavailableException(
                "Reading screenshots needs the Claude API: set gamecoach.coach.provider=claude.", null));
        String yourHero = officialHero(hero);          // checked before the paid call, so a typo costs nothing
        List<Screenshot> personalShots = new ArrayList<>();
        if (personal != null) {
            for (MultipartFile file : personal) {
                if (file != null && !file.isEmpty()) {
                    personalShots.add(image(file));
                }
            }
        }
        OverwatchScreens screens = new OverwatchScreens(image(teams),
                summary == null || summary.isEmpty() ? null : image(summary), personalShots);
        ParsedOverwatchMatch parsed = matchReader.read(screens, store.newId());
        if (yourHero != null) {
            parsed = withUploaderHero(parsed, yourHero);
        }
        store.save(parsed.match());
        return ResponseEntity.status(HttpStatus.CREATED).body(parsed);
    }

    /** The review step: replaces the match with the corrected version and checks it again. */
    @PutMapping("/{matchId}")
    public ParsedOverwatchMatch correct(@PathVariable("matchId") String matchId, @RequestBody OverwatchMatch corrected) {
        match(matchId);   // 404 when unknown
        OverwatchMatch fixed = corrected.withId(matchId);
        store.save(fixed);
        return new ParsedOverwatchMatch(fixed, validator.check(fixed));
    }

    @GetMapping("/{matchId}")
    public OverwatchMatch get(@PathVariable("matchId") String matchId) {
        return match(matchId);
    }

    /** The computed comparisons for ?username=, or your own row when it's left out. */
    @GetMapping("/{matchId}/analysis")
    public OverwatchPlayerAnalysis analysis(@PathVariable("matchId") String matchId,
                                            @RequestParam(name = "username", required = false) String username) {
        return analyzer.analyze(match(matchId), username);
    }

    /** Coaching for ?username=, or your own row when it's left out. */
    @PostMapping("/{matchId}/feedback")
    public Feedback feedback(@PathVariable("matchId") String matchId,
                             @RequestParam(name = "username", required = false) String username) {
        OverwatchCoachingService overwatchCoach = coach.orElseThrow(() -> new CoachingUnavailableException(
                "Overwatch coaching needs the Claude API: set gamecoach.coach.provider=claude.", null));
        OverwatchMatch match = match(matchId);
        return overwatchCoach.coach(match, analyzer.analyze(match, username));
    }

    private OverwatchMatch match(String matchId) {
        return store.find(matchId).orElseThrow(() -> new NoSuchElementException(
                "No Overwatch match with id " + matchId + ". Uploaded matches are kept until the app restarts."));
    }

    private static Screenshot image(MultipartFile file) throws IOException {
        return new Screenshot(file.getBytes(), file.getContentType());
    }

    private String officialHero(String hero) {
        if (hero == null || hero.isBlank()) {
            return null;
        }
        return heroes.officialName(hero).orElseThrow(() -> new BadUploadException(
                "\"" + hero.strip() + "\" isn't an Overwatch hero I know. Use the name as the game shows it, e.g. Ramattra."));
    }

    /**
     * Puts the hero you said you ended on onto your row, drops the reader's doubts about that row's
     * hero, notes the change, and checks the match again.
     */
    ParsedOverwatchMatch withUploaderHero(ParsedOverwatchMatch parsed, String hero) {
        OverwatchMatch match = parsed.match();
        OverwatchPlayerLine you = match.players().stream().filter(OverwatchPlayerLine::uploader).findFirst().orElse(null);
        if (you == null) {
            List<String> warnings = new ArrayList<>(parsed.warnings());
            warnings.add("Your row wasn't identified, so the hero you gave (" + hero + ") wasn't applied.");
            return new ParsedOverwatchMatch(match, warnings);
        }
        OverwatchMatch fixed = match.withPlayers(match.players().stream()
                .map(p -> p == you ? p.withHero(hero) : p).toList());
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        for (String w : parsed.warnings()) {
            boolean aboutYourHero = (you.name() != null && w.contains(you.name()) || w.startsWith("Your row's portrait"))
                    && w.toLowerCase(Locale.ROOT).contains("portrait");
            if (!aboutYourHero && !validatorWarning(w, match)) {
                warnings.add(w);
            }
        }
        if (!OverwatchHeroCatalog.sameHero(you.hero(), hero)) {
            warnings.add("Your hero set to " + hero + " as you said (the screenshot was read as "
                    + (you.hero() == null ? "no hero" : you.hero()) + ").");
        }
        warnings.addAll(validator.check(fixed));
        return new ParsedOverwatchMatch(fixed, List.copyOf(warnings));
    }

    /** The validator's warnings are recomputed after the change, so the old ones are dropped. */
    private boolean validatorWarning(String warning, OverwatchMatch before) {
        return validator.check(before).contains(warning);
    }
}

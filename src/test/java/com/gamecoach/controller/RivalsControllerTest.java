package com.gamecoach.controller;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.MatchAnalyzer;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.RoundScore;
import com.gamecoach.parsing.BadUploadException;
import com.gamecoach.parsing.ParsedScoreboard;
import com.gamecoach.parsing.ScoreboardParser;
import com.gamecoach.parsing.ScoreboardValidator;
import com.gamecoach.parsing.UploadedMatchStore;
import com.gamecoach.service.CoachingUnavailableException;
import com.gamecoach.service.FakeCoachingService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Calls the controller directly with the fake parts, the same ones Spring wires in by default. */
class RivalsControllerTest {

    private static final String PLAYER29_MATCH = "10378920354";
    private static final String DEADPOOL_MATCH = "5518188_1770352206_1245014_11001_11";

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final MatchAnalyzer analyzer = new MatchAnalyzer(new HeroRoleCatalog(), new TeamUpCatalog());
    private final ScoreboardValidator validator = new ScoreboardValidator(new HeroRoleCatalog(), new TeamUpCatalog());
    private final UploadedMatchStore uploads = new UploadedMatchStore();

    /** A stand-in reader: "reads" every screenshot as the Player29 match, with the given id and score. */
    private final ScoreboardParser fakeReader = (screenshot, matchId, score) -> new ParsedScoreboard(
            new MatchDetails(matchId, "Convoy", "Tokyo 2099: Spider-Islands", source.getMatchDetails(PLAYER29_MATCH).duration(),
                    source.getMatchDetails(PLAYER29_MATCH).result(), null, List.of(),
                    source.getMatchDetails(PLAYER29_MATCH).players(), score),
            List.of("Row 2: name hidden."));

    private final RivalsController controller = new RivalsController(source, analyzer, new FakeCoachingService(),
            uploads, validator, Optional.of(fakeReader));

    private static MockMultipartFile png() {
        return new MockMultipartFile("screenshot", "scoreboard.png", "image/png", new byte[] {1, 2, 3});
    }

    @Test
    void anUploadedMatchWorksWithEveryEndpoint() throws Exception {
        ResponseEntity<ParsedScoreboard> response = controller.upload(png(), "3-2");
        assertEquals(201, response.getStatusCode().value());
        ParsedScoreboard parsed = response.getBody();
        String id = parsed.match().matchId();
        assertTrue(id.startsWith("upload-"));
        assertEquals(new RoundScore(3, 2), parsed.match().score());
        assertEquals(List.of("Row 2: name hidden."), parsed.warnings());

        assertEquals(parsed.match(), controller.matchDetails(id));
        assertEquals("Player29", controller.analysis(id, null).player());
        assertTrue(controller.feedback(id, null).strengths().get(0).contains("33 assists"));
    }

    @Test
    void theReviewStepReplacesTheMatchAndChecksItAgain() throws Exception {
        MatchDetails read = controller.upload(png(), null).getBody().match();
        List<PlayerLine> fixed = new ArrayList<>(read.players());
        fixed.set(0, fixed.get(0).withAccuracy(330.0));                 // a typo the review should flag
        ParsedScoreboard corrected = controller.correct(read.matchId(), read.withPlayers(fixed));
        assertEquals(330.0, controller.matchDetails(read.matchId()).players().get(0).accuracyPercent());
        assertTrue(corrected.warnings().stream().anyMatch(w -> w.contains("outside 0-100")));

        assertThrows(NoSuchElementException.class, () -> controller.correct(PLAYER29_MATCH, read));   // not an upload
    }

    @Test
    void theScoreMustLookLikeAScore() {
        assertEquals(new RoundScore(2, 1), RivalsController.parseScore(" 2:1 "));
        assertEquals(null, RivalsController.parseScore(""));
        assertThrows(BadUploadException.class, () -> RivalsController.parseScore("we won"));
    }

    @Test
    void uploadsNeedTheClaudeProvider() {
        RivalsController withoutReader = new RivalsController(source, analyzer, new FakeCoachingService(),
                uploads, validator, Optional.empty());
        CoachingUnavailableException e = assertThrows(CoachingUnavailableException.class,
                () -> withoutReader.upload(png(), null));
        assertTrue(e.getMessage().contains("gamecoach.coach.provider=claude"));
    }

    @Test
    void pickerListsAllMatches() {
        assertEquals(3, controller.recentMatches("anyone").size());
    }

    @Test
    void noUsernameMeansTheUploader() {
        assertEquals("Player29", controller.analysis(PLAYER29_MATCH, null).player());
        assertEquals("Player08", controller.analysis(DEADPOOL_MATCH, null).player());
    }

    @Test
    void feedbackUsesTheUploadersRealNumbers() {
        Feedback feedback = controller.feedback(PLAYER29_MATCH, null);
        assertTrue(feedback.strengths().get(0).contains("33 assists"));
        assertTrue(feedback.weaknesses().get(0).contains("20.0%"));
    }

    @Test
    void feedbackForANamedPlayer() {
        assertTrue(controller.feedback(PLAYER29_MATCH, "player44").strengths().get(0).contains("0 assists"));
    }

    @Test
    void feedbackSkipsAccuracyWhenThereIsNone() {
        MatchDetails match = source.getMatchDetails(PLAYER29_MATCH);
        List<PlayerLine> players = new ArrayList<>(match.players());
        players.set(4, players.get(4).withAccuracy(null));
        MatchDetails noAccuracy = match.withPlayers(players);
        Feedback feedback = new FakeCoachingService().coach(noAccuracy, analyzer.analyze(noAccuracy, "Player29"));
        assertTrue(feedback.weaknesses().stream().noneMatch(w -> w.contains("Accuracy")));
    }

    @Test
    void unknownMatchOrPlayerThrows() {
        assertThrows(NoSuchElementException.class, () -> controller.matchDetails("no-such-match"));
        assertThrows(NoSuchElementException.class, () -> controller.feedback(PLAYER29_MATCH, "someone-else"));
    }

    @Test
    void exceptionHandlerReturns404WithTheReason() {
        ResponseEntity<ProblemDetail> response =
                new ApiExceptionHandler().notFound(new NoSuchElementException("someone-else is not in match x"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("someone-else is not in match x", response.getBody().getDetail());
    }
}

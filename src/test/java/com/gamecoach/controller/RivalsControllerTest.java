package com.gamecoach.controller;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.MatchAnalyzer;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.service.FakeCoachingService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Calls the controller directly with the fake parts, the same ones Spring wires in by default. */
class RivalsControllerTest {

    private static final String PLAYER29_MATCH = "10378920354";
    private static final String DEADPOOL_MATCH = "5518188_1770352206_1245014_11001_11";

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final MatchAnalyzer analyzer = new MatchAnalyzer(new HeroRoleCatalog(), new TeamUpCatalog());
    private final RivalsController controller = new RivalsController(source, analyzer, new FakeCoachingService());

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

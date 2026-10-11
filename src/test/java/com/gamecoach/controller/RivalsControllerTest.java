package com.gamecoach.controller;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.MatchAnalyzer;
import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.Feedback;
import com.gamecoach.service.FakeCoachingService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Calls the controller directly with the fake parts, the same ones Spring wires in by default. */
class RivalsControllerTest {

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final RivalsController controller = new RivalsController(
            source, new MatchAnalyzer(new HeroRoleCatalog()), new FakeCoachingService());
    private final String matchId = source.getRecentMatches("anyone").get(0).matchId();

    @Test
    void feedbackUsesThePlayersRealNumbers() {
        Feedback feedback = controller.feedback(matchId, "PlayerOne");
        assertTrue(feedback.strengths().get(0).contains("30 assists"));
        assertTrue(feedback.weaknesses().get(0).contains("38.6%"));
    }

    @Test
    void feedbackSkipsAccuracyWhenThereIsNone() {
        Feedback feedback = controller.feedback(matchId, "player39");
        assertTrue(feedback.weaknesses().stream().noneMatch(w -> w.contains("Accuracy")));
    }

    @Test
    void unknownMatchOrPlayerThrows() {
        assertThrows(NoSuchElementException.class, () -> controller.matchDetails("no-such-match"));
        assertThrows(NoSuchElementException.class, () -> controller.feedback(matchId, "someone-else"));
    }

    @Test
    void exceptionHandlerReturns404WithTheReason() {
        ResponseEntity<ProblemDetail> response =
                new ApiExceptionHandler().notFound(new NoSuchElementException("someone-else is not in match x"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("someone-else is not in match x", response.getBody().getDetail());
    }
}

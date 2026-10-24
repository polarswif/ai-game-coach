package com.gamecoach.controller;

import com.gamecoach.OverwatchMatchFixtures;
import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.analysis.OverwatchMatchAnalyzer;
import com.gamecoach.analysis.OverwatchPlayerAnalysis;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.parsing.BadUploadException;
import com.gamecoach.parsing.OverwatchMatchReader;
import com.gamecoach.parsing.InMemoryOverwatchMatchStore;
import com.gamecoach.parsing.OverwatchMatchStore;
import com.gamecoach.parsing.OverwatchMatchValidator;
import com.gamecoach.parsing.OverwatchScreens;
import com.gamecoach.parsing.ParsedOverwatchMatch;
import com.gamecoach.service.CoachingUnavailableException;
import com.gamecoach.service.OverwatchCoachingService;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Calls the controller directly with a stand-in reader and coach. */
class OverwatchMatchControllerTest {

    private final OverwatchHeroCatalog heroes = new OverwatchHeroCatalog();
    private final OverwatchMatchValidator validator = new OverwatchMatchValidator(heroes);
    private final OverwatchMatchStore store = new InMemoryOverwatchMatchStore();
    private final List<OverwatchScreens> readScreens = new ArrayList<>();

    /** "Reads" every upload as the Colosseo match, with a portrait doubt about your row. */
    private final OverwatchMatchReader fakeReader = (screens, matchId) -> {
        readScreens.add(screens);
        return new ParsedOverwatchMatch(OverwatchMatchFixtures.colosseo().withId(matchId),
                List.of("Row 1 (PLAYERONE): portrait unclear, could be Ramattra or Orisa."));
    };
    private final OverwatchCoachingService fakeCoach = (match, analysis) ->
            new Feedback(List.of("Good " + analysis.hero()), List.of("Watch deaths"), List.of("Try X"), "Context");

    private OverwatchMatchController controller(boolean withClaude) {
        return new OverwatchMatchController(withClaude ? Optional.of(fakeReader) : Optional.empty(),
                withClaude ? Optional.of(fakeCoach) : Optional.empty(), store, validator,
                new OverwatchMatchAnalyzer(heroes), heroes);
    }

    private static MockMultipartFile png(String field) {
        return new MockMultipartFile(field, field + ".png", "image/png", new byte[] {1, 2, 3});
    }

    @Test
    void anUploadWorksWithEveryEndpoint() throws Exception {
        OverwatchMatchController c = controller(true);
        ParsedOverwatchMatch parsed = c.upload(png("teams"), png("summary"), List.of(png("personal")), null).getBody();
        String id = parsed.match().matchId();
        assertTrue(id.startsWith("ow-"));
        assertEquals(1, readScreens.get(0).personal().size());
        assertEquals("Colosseo", c.get(id).map());
        OverwatchPlayerAnalysis a = c.analysis(id, null);
        assertEquals("PLAYERONE", a.player());
        assertEquals("Good Ramattra", c.feedback(id, null).strengths().get(0));
        assertEquals("Good Roadhog", c.feedback(id, "PLAYERSEVEN").strengths().get(0));
    }

    @Test
    void theHeroYouTypeReplacesYourPortraitAndItsDoubts() throws Exception {
        ParsedOverwatchMatch parsed = controller(true).upload(png("teams"), png("summary"), null, "orisa").getBody();
        assertEquals("Orisa", parsed.match().players().get(0).hero());
        assertFalse(parsed.warnings().contains("Row 1 (PLAYERONE): portrait unclear, could be Ramattra or Orisa."));
        assertTrue(parsed.warnings().contains("Your hero set to Orisa as you said (the screenshot was read as Ramattra)."));
        // Orisa isn't on the Summary's hero list, so the check still tells you something doesn't add up.
        assertTrue(parsed.warnings().stream().anyMatch(w -> w.startsWith("Your row's portrait reads Orisa")));
    }

    @Test
    void anUnknownHeroIsRejectedBeforeThePaidRead() {
        assertThrows(BadUploadException.class, () -> controller(true).upload(png("teams"), null, null, "Daredevil"));
        assertTrue(readScreens.isEmpty());
    }

    @Test
    void aCorrectionIsSavedAndCheckedAgain() throws Exception {
        OverwatchMatchController c = controller(true);
        String id = c.upload(png("teams"), null, null, null).getBody().match().matchId();
        OverwatchMatch fixed = OverwatchMatchFixtures.teamsOnly();      // a different id in the body
        ParsedOverwatchMatch result = c.correct(id, fixed);
        assertEquals(id, result.match().matchId());                      // the path's id wins
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Add the Summary screenshot")));
        assertThrows(NoSuchElementException.class, () -> c.correct("ow-missing", fixed));
    }

    @Test
    void withoutClaudeReadingAndCoachingSayWhatToSet() {
        OverwatchMatchController c = controller(false);
        assertThrows(CoachingUnavailableException.class, () -> c.upload(png("teams"), null, null, null));
        store.save(OverwatchMatchFixtures.colosseo());
        assertEquals("PLAYERONE", c.analysis("ow-test1234", null).player());   // analysis needs no AI
        assertThrows(CoachingUnavailableException.class, () -> c.feedback("ow-test1234", null));
    }
}

package com.gamecoach.persistence;

import com.gamecoach.OverwatchMatchFixtures;
import com.gamecoach.controller.MatchHistoryController;
import com.gamecoach.controller.MatchHistoryController.MatchListItem;
import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.parsing.OverwatchMatchStore;
import com.gamecoach.parsing.UploadedMatchStore;
import com.gamecoach.service.CoachingArchive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The database layer against a real database: H2 in memory, with the same tables Hibernate creates
 * in PostgreSQL. Each test uses its own match ids, since they share one database.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:persistence-test;DB_CLOSE_DELAY=-1")
class PersistenceTest {

    @Autowired
    private UploadedMatchStore rivals;

    @Autowired
    private OverwatchMatchStore overwatch;

    @Autowired
    private CoachingArchive archive;

    @Autowired
    private MatchHistoryController history;

    @Autowired
    private SavedCoachingRepository coachings;

    @Autowired
    private MatchJson json;

    private static MatchDetails rivalsMatch(String id) {
        MatchDetails m = new FakeMatchHistorySource().getMatchDetails("10378920354");
        return new MatchDetails(id, m.mode(), m.map(), m.duration(), m.result(), m.replayId(), m.bans(), m.players(), m.score());
    }

    private static final Feedback FEEDBACK = new Feedback(List.of("Strong"), List.of("Weak"), List.of("Try"), "Close match.");

    @Test
    void theAppUsesTheDatabaseStores() {
        assertInstanceOf(DbUploadedMatchStore.class, rivals);
        assertInstanceOf(DbOverwatchMatchStore.class, overwatch);
        assertInstanceOf(DbCoachingArchive.class, archive);
    }

    @Test
    void aMatchComesBackExactlyAsSaved() {
        MatchDetails r = rivalsMatch("upload-roundtrip");
        rivals.save(r);
        assertEquals(r, rivals.find("upload-roundtrip").orElseThrow());

        OverwatchMatch o = OverwatchMatchFixtures.colosseo().withId("ow-roundtrip");
        overwatch.save(o);
        assertEquals(o, overwatch.find("ow-roundtrip").orElseThrow());

        // Each store only finds its own game's matches.
        assertTrue(rivals.find("ow-roundtrip").isEmpty());
        assertTrue(overwatch.find("upload-roundtrip").isEmpty());
    }

    @Test
    void savingAgainReplacesTheMatch() {
        OverwatchMatch first = OverwatchMatchFixtures.colosseo().withId("ow-replace");
        overwatch.save(first);
        OverwatchMatch corrected = first.withPlayers(first.players().stream()
                .map(p -> "SHYLIAR".equals(p.name()) ? p.withHero("Zenyatta") : p).toList());
        overwatch.save(corrected);
        assertEquals(corrected, overwatch.find("ow-replace").orElseThrow());
    }

    @Test
    void coachingIsSavedPerMatchVersionAndPlayer() {
        OverwatchMatch match = OverwatchMatchFixtures.colosseo().withId("ow-coaching");
        overwatch.save(match);
        archive.save("ow-coaching", match, "name:playerone", FEEDBACK);

        assertEquals(FEEDBACK, archive.find("ow-coaching", match, "name:playerone").orElseThrow());
        assertTrue(archive.find("ow-coaching", match, "name:playerseven").isEmpty());          // another player
        OverwatchMatch corrected = match.withPlayers(match.players().subList(0, 9));
        assertTrue(archive.find("ow-coaching", corrected, "name:playerone").isEmpty());       // another version
        archive.save("ow-coaching", match, "name:playerone", FEEDBACK);                        // no duplicate row
        assertEquals(1, coachings.countByMatch_MatchId("ow-coaching"));
    }

    @Test
    void coachingForAMatchThatIsntSavedStaysInMemory() {
        MatchDetails sample = new FakeMatchHistorySource().getMatchDetails("10378920354");
        archive.save(sample.matchId(), sample, "name:player29", FEEDBACK);
        assertEquals(FEEDBACK, archive.find(sample.matchId(), sample, "name:player29").orElseThrow());
    }

    @Test
    void theFingerprintIsStableAndChangesWithTheMatch() {
        OverwatchMatch match = OverwatchMatchFixtures.colosseo();
        assertEquals(json.fingerprint(match), json.fingerprint(OverwatchMatchFixtures.colosseo()));
        assertNotEquals(json.fingerprint(match), json.fingerprint(match.withId("ow-other")));
        assertEquals(64, json.fingerprint(match).length());
    }

    @Test
    void historyListsNewestFirstAndDeleteRemovesTheCoachingToo() throws InterruptedException {
        rivals.save(rivalsMatch("upload-history-old"));
        Thread.sleep(20);
        OverwatchMatch newer = OverwatchMatchFixtures.colosseo().withId("ow-history-new");
        overwatch.save(newer);
        archive.save("ow-history-new", newer, "name:playerone", FEEDBACK);

        List<String> ids = history.list(null).stream().map(MatchListItem::matchId).toList();
        assertTrue(ids.indexOf("ow-history-new") < ids.indexOf("upload-history-old"));
        MatchListItem item = history.list("overwatch").stream()
                .filter(i -> i.matchId().equals("ow-history-new")).findFirst().orElseThrow();
        assertEquals("Colosseo", item.map());
        assertEquals("Ramattra", item.yourHero());
        assertEquals("1-0", item.score());
        assertEquals(330L, item.durationSeconds());
        assertTrue(history.list("rivals").stream().noneMatch(i -> i.game().equals("overwatch")));

        assertEquals(204, history.delete("ow-history-new").getStatusCode().value());
        assertTrue(overwatch.find("ow-history-new").isEmpty());
        assertEquals(0, coachings.countByMatch_MatchId("ow-history-new"));
        assertThrows(NoSuchElementException.class, () -> history.delete("ow-history-new"));
    }
}

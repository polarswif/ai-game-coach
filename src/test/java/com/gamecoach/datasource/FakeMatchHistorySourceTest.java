package com.gamecoach.datasource;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Sanity checks on the hand-typed screenshot data, to catch typos. */
class FakeMatchHistorySourceTest {

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final HeroRoleCatalog roles = new HeroRoleCatalog();

    private List<MatchDetails> allMatches() {
        return source.getRecentMatches("anyone").stream()
                .map(summary -> source.getMatchDetails(summary.matchId()))
                .toList();
    }

    @Test
    void everyMatchHasSixPlayersPerTeam() {
        for (MatchDetails match : allMatches()) {
            assertEquals(12, match.players().size(), match.matchId());
            assertEquals(6, match.players().stream().filter(p -> p.team().equals("A")).count(), match.matchId());
            assertEquals(6, match.players().stream().filter(p -> p.team().equals("B")).count(), match.matchId());
        }
    }

    @Test
    void everyMatchHasOneUploaderOneMvpAndOneSvp() {
        for (MatchDetails match : allMatches()) {
            assertEquals(1, match.players().stream().filter(PlayerLine::uploader).count(), match.matchId());
            assertEquals(1, match.players().stream().filter(PlayerLine::mvp).count(), match.matchId());
            assertEquals(1, match.players().stream().filter(PlayerLine::svp).count(), match.matchId());
        }
    }

    /**
     * The same check the screenshot reader will run: a hero's role in the table must match the
     * role icon on its row. A mismatch means the portrait (or icon) was misread.
     */
    @Test
    void everyRowsIconMatchesItsHerosRole() {
        for (MatchDetails match : allMatches()) {
            for (PlayerLine p : match.players()) {
                assertNotEquals(Role.UNKNOWN, p.role(), p.lastHero());
                assertFalse(p.lastHero() == null || p.lastHero().isBlank());
                Role fromTable = roles.roleOf(p.lastHero());
                if (fromTable != Role.UNKNOWN) {   // plain "Deadpool" isn't in the table; his icon decides
                    assertEquals(fromTable, p.role(), p.lastHero() + " in match " + match.matchId());
                }
            }
        }
    }

    @Test
    void pickerRowsMatchTheUploadersScoreboardRow() {
        for (MatchSummary summary : source.getRecentMatches("anyone")) {
            PlayerLine me = source.getMatchDetails(summary.matchId()).players().stream()
                    .filter(PlayerLine::uploader).findFirst().orElseThrow();
            assertEquals(me.lastHero(), summary.hero());
            assertEquals(me.kills(), summary.kills());
            assertEquals(me.deaths(), summary.deaths());
            assertEquals(me.assists(), summary.assists());
        }
    }

    @Test
    void unknownMatchThrows() {
        assertThrows(NoSuchElementException.class, () -> source.getMatchDetails("no-such-match"));
    }
}

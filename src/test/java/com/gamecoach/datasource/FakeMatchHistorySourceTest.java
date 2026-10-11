package com.gamecoach.datasource;

import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FakeMatchHistorySourceTest {

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final String matchId = source.getRecentMatches("anyone").get(0).matchId();

    @Test
    void matchHasSixPlayersPerTeam() {
        MatchDetails match = source.getMatchDetails(matchId);
        assertEquals(12, match.players().size());
        assertEquals(6, match.players().stream().filter(p -> p.team().equals("A")).count());
        assertEquals(6, match.players().stream().filter(p -> p.team().equals("B")).count());
    }

    /** Catches typos in the hand-entered data: per-hero K/D/A must add up to each player's totals. */
    @Test
    void perHeroStatsAddUpToPlayerTotals() {
        for (PlayerLine player : source.getMatchDetails(matchId).players()) {
            assertFalse(player.heroes().isEmpty(), player.name() + " has no heroes");
            assertEquals(player.kills(), player.heroes().stream().mapToInt(HeroStats::kills).sum(), player.name() + " kills");
            assertEquals(player.deaths(), player.heroes().stream().mapToInt(HeroStats::deaths).sum(), player.name() + " deaths");
            assertEquals(player.assists(), player.heroes().stream().mapToInt(HeroStats::assists).sum(), player.name() + " assists");
        }
    }

    @Test
    void unknownMatchThrows() {
        assertThrows(NoSuchElementException.class, () -> source.getMatchDetails("no-such-match"));
    }
}

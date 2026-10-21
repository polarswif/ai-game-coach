package com.gamecoach.datasource.overfast;

import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.datasource.CareerStatsSource.PlayerMatch;
import com.gamecoach.datasource.CareerStatsUnavailableException;
import com.gamecoach.datasource.overfast.OverFastApi.Competitive;
import com.gamecoach.datasource.overfast.OverFastApi.PlatformRanks;
import com.gamecoach.datasource.overfast.OverFastApi.PlayerShort;
import com.gamecoach.datasource.overfast.OverFastApi.PlayerSummary;
import com.gamecoach.datasource.overfast.OverFastApi.Rank;
import com.gamecoach.datasource.overfast.OverFastApi.SearchResult;
import com.gamecoach.datasource.overfast.OverFastApi.Stats;
import com.gamecoach.datasource.overfast.OverFastApi.StatsSummary;
import com.gamecoach.datasource.overfast.OverFastApi.Values;
import com.gamecoach.model.CareerProfile;
import com.gamecoach.model.CareerProfile.HeroCareer;
import com.gamecoach.model.OverwatchMode;
import com.gamecoach.model.OverwatchRole;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OverFast replies → our models, and OverFast errors → messages, with a canned client (no HTTP). */
class OverFastCareerSourceTest {

    private static Stats stats(int games, int won, long seconds) {
        return new Stats(games, won, games - won, seconds, 100.0 * won / games, 2.5,
                new Values(100.0, 50.0, 40.0, 50000.0, 1000.0), new Values(10.0, 5.0, 4.0, 5000.0, 100.0));
    }

    /** A client that returns fixed replies, remembers what it was asked, or throws a set error. */
    private static class CannedClient implements OverFastClient {
        PlayerSummary summary;
        StatsSummary stats;
        OverFastException error;
        String askedId;
        String askedMode;
        String askedPlatform;

        @Override
        public PlayerSummary summary(String playerId) {
            askedId = playerId;
            if (error != null) {
                throw error;
            }
            return summary;
        }

        @Override
        public StatsSummary statsSummary(String playerId, String gamemode, String platform) {
            askedMode = gamemode;
            askedPlatform = platform;
            return stats;
        }

        @Override
        public SearchResult search(String name) {
            if (error != null) {
                throw error;
            }
            return new SearchResult(1, List.of(new PlayerShort("Swift-1234", "Swift", "Grandmaster", "https://x/a.png", 1_760_000_000L, true)));
        }
    }

    private final CannedClient client = new CannedClient();
    private final OverFastCareerSource source = new OverFastCareerSource(client, new OverwatchHeroCatalog());

    private void cannedPlayer() {
        Map<String, Stats> heroes = new LinkedHashMap<>();
        heroes.put("ana", stats(10, 6, 3600));
        heroes.put("soldier-76", stats(20, 9, 7200));
        heroes.put("brand-new-hero", stats(2, 1, 600));
        client.summary = new PlayerSummary("Swift", new Competitive(
                new PlatformRanks(22, null, null, null, null),                                   // no PC ranks
                new PlatformRanks(22, null, null, new Rank("Gold", 3), new Rank("silver", 1))));
        client.stats = new StatsSummary(stats(32, 16, 11400),
                Map.of("support", stats(10, 6, 3600), "damage", stats(20, 9, 7200), "oddity", stats(1, 1, 60)),
                heroes);
    }

    @Test
    void aCareerIsMappedWithHeroNamesRolesAndOrder() {
        cannedPlayer();
        CareerProfile p = source.getCareerProfile("Swift#1234", OverwatchMode.QUICKPLAY, null);
        assertEquals("Swift-1234", client.askedId);                                    // # became -
        assertEquals("quickplay", client.askedMode);
        assertNull(client.askedPlatform);
        assertEquals("Swift", p.name());
        assertEquals(List.of("soldier-76", "ana", "brand-new-hero"), p.heroes().stream().map(HeroCareer::heroKey).toList());
        HeroCareer soldier = p.heroes().get(0);
        assertEquals("Soldier: 76", soldier.hero());
        assertEquals(OverwatchRole.DAMAGE, soldier.role());
        assertEquals(Duration.ofHours(2), soldier.stats().timePlayed());
        assertEquals(45.0, soldier.stats().winRatePercent());
        assertNull(p.heroes().get(2).role());                                          // newer than the table
        assertEquals(2, p.roles().size());                                             // "oddity" isn't a role
        assertEquals(5000.0, p.overall().per10().damage());
    }

    @Test
    void ranksComeFromThePlatformThatHasThem() {
        cannedPlayer();
        CareerProfile p = source.getCareerProfile("Swift-1234", OverwatchMode.COMPETITIVE, null);
        assertEquals(22, p.ranks().season());
        assertEquals("Gold 3", p.ranks().byRole().get(OverwatchRole.SUPPORT).toString());
        assertEquals("Silver 1", p.ranks().openQueue().toString());
        assertNull(source.getCareerProfile("Swift-1234", OverwatchMode.COMPETITIVE, "pc").ranks());
    }

    @Test
    void noStatsMeansPrivateOrNeverPlayed() {
        cannedPlayer();
        client.stats = new StatsSummary(null, null, null);
        NoSuchElementException e = assertThrows(NoSuchElementException.class,
                () -> source.getCareerProfile("Swift-1234", OverwatchMode.COMPETITIVE, "pc"));
        assertTrue(e.getMessage().contains("no competitive stats on pc"));
        assertTrue(e.getMessage().contains("private"));
        assertTrue(e.getMessage().contains("try mode=quickplay"));
    }

    @Test
    void anUnknownPlayerIsA404WithWhenToRetry() {
        client.error = new OverFastException(404, "Player not found", 600, null);
        NoSuchElementException e = assertThrows(NoSuchElementException.class,
                () -> source.getCareerProfile("swift-1234", OverwatchMode.QUICKPLAY, null));
        assertTrue(e.getMessage().contains("No Overwatch profile found for swift-1234"));
        assertTrue(e.getMessage().contains("use the playerId"));
        assertTrue(e.getMessage().contains("10 minutes"));
    }

    @Test
    void rateLimitsAndOutagesAreUnavailable() {
        client.error = new OverFastException(429, "Too many requests", 30, null);
        assertTrue(assertThrows(CareerStatsUnavailableException.class,
                () -> source.getCareerProfile("Swift-1234", OverwatchMode.QUICKPLAY, null)).getMessage().contains("30 seconds"));
        client.error = new OverFastException(504, "Blizzard timed out", null, null);
        assertTrue(assertThrows(CareerStatsUnavailableException.class,
                () -> source.searchPlayers("Swift")).getMessage().contains("HTTP 504"));
        client.error = new OverFastException(0, "Couldn't reach OverFast API: timeout", null, null);
        assertEquals("Couldn't reach OverFast API: timeout", assertThrows(CareerStatsUnavailableException.class,
                () -> source.searchPlayers("Swift")).getMessage());
    }

    @Test
    void searchResultsAreMapped() {
        assertEquals(List.of(new PlayerMatch("Swift-1234", "Swift", "Grandmaster", "https://x/a.png",
                java.time.Instant.ofEpochSecond(1_760_000_000L), true)), source.searchPlayers(" Swift "));
    }

    @Test
    void aBlankBattleTagIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> OverFastCareerSource.playerId(" "));
    }
}

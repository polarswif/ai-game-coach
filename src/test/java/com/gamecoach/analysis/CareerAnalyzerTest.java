package com.gamecoach.analysis;

import com.gamecoach.analysis.CareerAnalysis.Comparison;
import com.gamecoach.analysis.CareerAnalysis.HeroSummary;
import com.gamecoach.analysis.CareerAnalysis.PercentDiffs;
import com.gamecoach.model.CareerProfile;
import com.gamecoach.model.CareerProfile.CareerStats;
import com.gamecoach.model.CareerProfile.HeroCareer;
import com.gamecoach.model.OverwatchMode;
import com.gamecoach.model.OverwatchRole;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.gamecoach.OverwatchFixtures.stats;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerAnalyzerTest {

    private final CareerAnalyzer analyzer = new CareerAnalyzer();

    // Support: 10 hours. Ana is 4 of them, Mercy half an hour, the rest other supports.
    private final CareerStats support = stats(60, 33, Duration.ofHours(10), 3.0, 1200, 1500, 600, 300000, 600000);
    private final CareerStats ana = stats(20, 8, Duration.ofHours(4), 2.0, 400, 700, 360, 100000, 280000);
    private final CareerStats mercy = stats(3, 2, Duration.ofMinutes(30), 4.0, 10, 90, 9, 2000, 30000);
    // Tank: Reinhardt is the only tank, so there's nothing to compare him with.
    private final CareerStats tank = stats(12, 6, Duration.ofHours(2), 2.5, 300, 100, 160, 120000, 0);
    private final CareerStats overall = stats(72, 39, Duration.ofHours(12), 2.9, 1500, 1600, 760, 420000, 600000);

    private CareerAnalysis analysis() {
        Map<OverwatchRole, CareerStats> roles = new EnumMap<>(OverwatchRole.class);
        roles.put(OverwatchRole.SUPPORT, support);
        roles.put(OverwatchRole.TANK, tank);
        CareerProfile profile = new CareerProfile("Swift-1234", "Swift", OverwatchMode.QUICKPLAY, null, null, overall,
                roles, List.of(
                        new HeroCareer("mercy", "Mercy", OverwatchRole.SUPPORT, mercy),
                        new HeroCareer("reinhardt", "Reinhardt", OverwatchRole.TANK, tank),
                        new HeroCareer("ana", "Ana", OverwatchRole.SUPPORT, ana)));
        return analyzer.analyze(profile);
    }

    private HeroSummary hero(String key) {
        return analysis().heroes().stream().filter(h -> h.heroKey().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void rolesAndHeroesAreOrderedByTimeWithTheirShare() {
        CareerAnalysis a = analysis();
        assertEquals(OverwatchRole.SUPPORT, a.mainRole());
        assertEquals(83.3, a.roles().get(0).timeSharePercent());
        assertEquals(List.of("ana", "reinhardt", "mercy"), a.heroes().stream().map(HeroSummary::heroKey).toList());
        assertEquals(33.3, hero("ana").timeSharePercent());
        assertEquals(100.0, a.overall().timeSharePercent());
    }

    @Test
    void aHeroIsComparedWithTheOtherHeroesInItsRole() {
        Comparison vs = hero("ana").vsOtherRoleHeroes();
        // The other supports: 6 hours, 40 games, 25 won.
        assertEquals(Duration.ofHours(6), vs.otherTimePlayed());
        assertEquals(40, vs.otherGamesPlayed());
        assertEquals(62.5, vs.otherWinRatePercent());
        assertEquals(-22.5, vs.winRateDiffPoints());                     // Ana 40% vs 62.5%
        assertEquals(6.67, vs.otherPer10().deaths());                     // 240 deaths over 36 ten-minute blocks
        assertEquals(124.9, vs.percentDiffs().deaths());                  // Ana dies 15 per 10 min: much more
        assertEquals(31.3, vs.percentDiffs().healing());                  // and heals more
    }

    @Test
    void tinyBaselinesAreNotCompared() {
        // Other heroes: 2 hours with 1 assist (0.08 per 10) and 100 healing (8.3 per 10).
        CareerStats role = stats(20, 10, Duration.ofHours(3), 2.0, 300, 21, 100, 300_000, 10_100);
        CareerStats hero = stats(10, 5, Duration.ofHours(1), 2.0, 100, 20, 40, 100_000, 10_000);
        PercentDiffs d = CareerAnalyzer.compare(hero, role).percentDiffs();
        assertNull(d.assists());                                          // would have been about +4000%
        assertNull(d.healing());                                          // a damage player's stray healing
        assertEquals(33.4, d.deaths());                                   // 6.67 vs 5.0 per 10 still compared
    }

    @Test
    void zeroAssistsMeansNotTrackedNotWorse() {
        CareerStats role = stats(20, 10, Duration.ofHours(3), 2.0, 300, 60, 100, 300_000, 0);
        CareerStats hero = stats(10, 5, Duration.ofHours(1), 2.0, 100, 0, 40, 100_000, 0);
        assertNull(CareerAnalyzer.compare(hero, role).percentDiffs().assists());
    }

    @Test
    void littlePlayTimeIsASmallSample() {
        assertTrue(hero("mercy").smallSample());                          // 30 minutes, 3 games
        assertFalse(hero("ana").smallSample());
    }

    @Test
    void noComparisonWithoutAnHourOnOtherHeroes() {
        assertNull(hero("reinhardt").vsOtherRoleHeroes());                // he's the only tank played
    }

    @Test
    void unknownRoleHeroesAreKeptWithoutAComparison() {
        CareerProfile profile = new CareerProfile("Swift-1234", "Swift", OverwatchMode.QUICKPLAY, null, null, overall,
                Map.of(), List.of(new HeroCareer("new-hero", "new-hero", null, ana)));
        HeroSummary h = analyzer.analyze(profile).heroes().get(0);
        assertNull(h.role());
        assertNull(h.vsOtherRoleHeroes());
        assertNull(analyzer.analyze(profile).mainRole());
    }
}

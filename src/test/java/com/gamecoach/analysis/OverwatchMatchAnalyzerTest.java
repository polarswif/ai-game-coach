package com.gamecoach.analysis;

import com.gamecoach.OverwatchMatchFixtures;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchResult;
import com.gamecoach.model.OverwatchRole;
import com.gamecoach.model.RoundScore;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverwatchMatchAnalyzerTest {

    private final OverwatchMatchAnalyzer analyzer = new OverwatchMatchAnalyzer(new OverwatchHeroCatalog());

    @Test
    void theUploaderIsAnalyzedWhenNoNameIsGiven() {
        OverwatchPlayerAnalysis a = analyzer.analyze(OverwatchMatchFixtures.colosseo(), null);
        assertEquals("PLAYERONE", a.player());
        assertEquals(OverwatchRole.TANK, a.role());
        assertEquals("Ramattra", a.hero());
        assertEquals("WIN", a.outcome());
        assertEquals("1-2-2", a.teamComposition());
        assertEquals("1-2-2", a.enemyComposition());
        assertEquals(21.0, a.eliminationsPerDeath());
        assertEquals(7, a.heroStats().get(0).stats().size());       // the Personal tiles come along
    }

    @Test
    void statsArePer10WithShareRanksAndTheSameRoleEnemy() {
        OverwatchPlayerAnalysis a = analyzer.analyze(OverwatchMatchFixtures.colosseo(), null);
        OverwatchStatComparison elims = a.stats().get(OverwatchStat.ELIMINATIONS);
        assertEquals(38.2, elims.per10());                          // 21 in 5:30 (the game says 38.25 over 5:29 played)
        assertEquals(32.8, elims.teamSharePercent());               // 21 of 64
        assertEquals(1, elims.lobbyRank());
        assertEquals(3, elims.sameRoleEnemyBest());                 // the enemy tank had 3

        OverwatchStatComparison deaths = a.stats().get(OverwatchStat.DEATHS);
        assertEquals(1, deaths.teamRank());                         // tied for fewest
        assertEquals(6, deaths.sameRoleEnemyBest());

        assertEquals(4337, a.stats().get(OverwatchStat.DAMAGE).sameRoleEnemyBest());
        assertEquals(3829, a.stats().get(OverwatchStat.MITIGATION).sameRoleEnemyBest());
    }

    @Test
    void pushUsesDeathsForClosenessBecauseItsScoreIsntRounds() {
        MatchCloseness c = analyzer.analyze(OverwatchMatchFixtures.colosseo(), null).closeness();
        assertFalse(c.fromRounds());
        assertEquals(7, c.teamDeaths());
        assertEquals(25, c.enemyDeaths());
        assertEquals(Closeness.ONE_SIDED, c.level());
        assertTrue(c.teamAheadInFights());
    }

    @Test
    void otherModesUseTheFinalScore() {
        OverwatchMatch m = OverwatchMatchFixtures.colosseo();
        OverwatchMatch control = new OverwatchMatch(m.matchId(), "Control", "Ilios", m.duration(), OverwatchResult.VICTORY,
                new RoundScore(2, 1), null, m.players(), m.yourHeroes(), m.heroStats());
        MatchCloseness c = analyzer.analyze(control, null).closeness();
        assertTrue(c.fromRounds());
        assertEquals(Closeness.CLOSE, c.level());
    }

    @Test
    void anotherPlayerFromTheEnemyTeamSeesTheMatchFromTheirSide() {
        OverwatchPlayerAnalysis a = analyzer.analyze(OverwatchMatchFixtures.colosseo(), "playerseven");
        assertEquals("LOSS", a.outcome());
        assertEquals(21, a.stats().get(OverwatchStat.ELIMINATIONS).sameRoleEnemyBest());
        assertTrue(a.heroStats().isEmpty());                         // Personal tiles are the uploader's only
        assertEquals("Roadhog", a.heroesPlayed().get(0).hero());
    }

    @Test
    void withoutTheSummaryThereAreNoRatesOrOutcome() {
        OverwatchPlayerAnalysis a = analyzer.analyze(OverwatchMatchFixtures.teamsOnly(), null);
        assertNull(a.outcome());
        assertNull(a.stats().get(OverwatchStat.ELIMINATIONS).per10());
    }

    @Test
    void anUnknownPlayerIsNotFound() {
        assertThrows(NoSuchElementException.class, () -> analyzer.analyze(OverwatchMatchFixtures.colosseo(), "Nobody"));
    }
}

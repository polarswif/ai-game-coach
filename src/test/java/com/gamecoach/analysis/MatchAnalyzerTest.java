package com.gamecoach.analysis;

import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Expected values are worked out by hand from the Hell's Heaven scoreboard in FakeMatchHistorySource. */
class MatchAnalyzerTest {

    private final MatchAnalyzer analyzer = new MatchAnalyzer(new HeroRoleCatalog());
    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final MatchDetails match =
            source.getMatchDetails(source.getRecentMatches("anyone").get(0).matchId());

    @Test
    void basicsForTheCoachedPlayer() {
        PlayerAnalysis a = analyzer.analyze(match, "PlayerOne");
        assertEquals("A", a.team());
        assertTrue(a.won());                  // team A won 2-1
        assertEquals(6, a.teamSize());
        assertEquals(12, a.lobbySize());
        assertEquals(6.0, a.kda());           // (24 + 30) / 9, matches tracker.gg's 6.00
    }

    @Test
    void healingShareAndRanks() {
        StatComparison healing = analyzer.analyze(match, "PlayerOne").stats().get(Stat.HEALING);
        assertEquals(29180, healing.value());
        assertEquals(46.4, healing.teamSharePercent());   // 29180 of the team's 62926
        assertEquals(2, healing.teamRank());              // behind -Player02- (33746)
        assertEquals(3, healing.lobbyRank());             // also behind Player31 (36367)
    }

    @Test
    void damagePerMinuteAndTeamRank() {
        StatComparison damage = analyzer.analyze(match, "PlayerOne").stats().get(Stat.DAMAGE);
        assertEquals(765.5, damage.perMinute());          // 11737 over 15m20s
        assertEquals(5, damage.teamRank());               // only -Player02- dealt less
    }

    @Test
    void fewerDeathsRanksHigher() {
        StatComparison deaths = analyzer.analyze(match, "PlayerOne").stats().get(Stat.DEATHS);
        assertEquals(3, deaths.teamRank());               // player41 (6) and Player19 (8) died less
        assertEquals(4, deaths.lobbyRank());              // plus player 43 (8)
    }

    @Test
    void tiedValuesShareARank() {
        StatComparison assists = analyzer.analyze(match, "PlayerOne").stats().get(Stat.ASSISTS);
        assertEquals(1, assists.teamRank());              // tied with -Player02- at 30
    }

    @Test
    void accuracyRankSkipsPlayersWithNoAccuracy() {
        assertEquals(Integer.valueOf(7), analyzer.analyze(match, "PlayerOne").accuracyLobbyRank());
        PlayerAnalysis player39 = analyzer.analyze(match, "player39");
        assertNull(player39.accuracyPercent());
        assertNull(player39.accuracyLobbyRank());
        assertFalse(player39.won());          // team B lost
    }

    @Test
    void heroesGetRolesAndUnlistedHeroesAreUnknown() {
        HeroPlayed mantis = analyzer.analyze(match, "PlayerOne").heroes().get(0);
        assertEquals("Mantis", mantis.hero());
        assertEquals(Role.STRATEGIST, mantis.role());
        assertEquals(14, mantis.kills());

        PlayerAnalysis player17 = analyzer.analyze(match, "Player17");
        assertEquals(7, player17.heroes().size());
        assertEquals(Role.VANGUARD, player17.heroes().get(1).role());  // The Hood
    }

    @Test
    void playerLookupIgnoresCase() {
        assertEquals("PlayerOne", analyzer.analyze(match, "playerone").player());
    }

    @Test
    void unknownPlayerThrows() {
        assertThrows(NoSuchElementException.class, () -> analyzer.analyze(match, "someone-else"));
    }

    // ---- Deadpool: the label from the role table, plus a stats-based second opinion ----

    /** The fake match with PlayerOne's heroes replaced. Their healing is 2nd on their team. */
    private MatchDetails withSwiftHeroes(List<HeroStats> heroes) {
        List<PlayerLine> players = match.players().stream()
                .map(p -> !p.name().equals("PlayerOne") ? p : new PlayerLine(
                        p.name(), heroes, p.team(), p.rank(), p.rankScore(), p.kills(), p.deaths(),
                        p.assists(), p.damage(), p.blocked(), p.healing(), p.accuracyPercent()))
                .toList();
        return new MatchDetails(match.matchId(), match.mode(), match.map(), match.duration(),
                match.teamAScore(), match.teamBScore(), players);
    }

    @Test
    void unlabeledDeadpoolGetsTheRoleHisStatsSuggest() {
        MatchDetails m = withSwiftHeroes(List.of(new HeroStats("Deadpool", 24, 9, 30)));
        HeroPlayed deadpool = analyzer.analyze(m, "PlayerOne").heroes().get(0);
        assertEquals(Role.STRATEGIST, deadpool.statsSuggestedRole());   // 2nd in team healing
        assertEquals(Role.STRATEGIST, deadpool.role());                 // no label, so the guess is used
    }

    @Test
    void labeledDeadpoolKeepsTheLabelAndShowsADisagreement() {
        MatchDetails m = withSwiftHeroes(List.of(new HeroStats("Deadpool (Vanguard)", 24, 9, 30)));
        HeroPlayed deadpool = analyzer.analyze(m, "PlayerOne").heroes().get(0);
        assertEquals(Role.VANGUARD, deadpool.role());                   // the label wins
        assertEquals(Role.STRATEGIST, deadpool.statsSuggestedRole());   // but the stats disagree
    }

    @Test
    void noGuessWhenDeadpoolSharedTheMatchWithOtherHeroes() {
        MatchDetails m = withSwiftHeroes(List.of(
                new HeroStats("Deadpool", 10, 2, 11), new HeroStats("Mantis", 14, 7, 19)));
        HeroPlayed deadpool = analyzer.analyze(m, "PlayerOne").heroes().get(0);
        assertNull(deadpool.statsSuggestedRole());   // totals mix both heroes, so no guess
        assertEquals(Role.UNKNOWN, deadpool.role());
    }

    @Test
    void otherHeroesNeverGetAStatsGuess() {
        HeroPlayed mantis = analyzer.analyze(match, "PlayerOne").heroes().get(0);
        assertNull(mantis.statsSuggestedRole());
    }

    @Test
    void guessRulesInOrder() {
        assertEquals(Role.STRATEGIST, MatchAnalyzer.guessRoleFromStats(ranks(5000, 1, 0, 6)));
        assertEquals(Role.VANGUARD, MatchAnalyzer.guessRoleFromStats(ranks(0, 6, 20000, 1)));
        assertEquals(Role.DUELIST, MatchAnalyzer.guessRoleFromStats(ranks(0, 6, 0, 6)));
        assertEquals(Role.DUELIST, MatchAnalyzer.guessRoleFromStats(ranks(800, 3, 900, 4)));
    }

    /** Builds just the healing and blocked comparisons the guess reads. */
    private static Map<Stat, StatComparison> ranks(int healing, int healRank, int blocked, int blockRank) {
        return Map.of(
                Stat.HEALING, new StatComparison(healing, 0, 0, healRank, healRank),
                Stat.BLOCKED, new StatComparison(blocked, 0, 0, blockRank, blockRank));
    }
}

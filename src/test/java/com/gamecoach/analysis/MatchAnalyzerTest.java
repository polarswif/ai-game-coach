package com.gamecoach.analysis;

import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Expected values are worked out by hand from the two in-game scoreboards in FakeMatchHistorySource. */
class MatchAnalyzerTest {

    private final MatchAnalyzer analyzer = new MatchAnalyzer(new HeroRoleCatalog());
    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final MatchDetails player29Match = source.getMatchDetails("10378920354");
    private final MatchDetails deadpoolMatch = source.getMatchDetails("5518188_1770352206_1245014_11001_11");

    /** A copy of the match with one row replaced. */
    private static MatchDetails replaceRow(MatchDetails match, int index, PlayerLine row) {
        List<PlayerLine> players = new ArrayList<>(match.players());
        players.set(index, row);
        return match.withPlayers(players);
    }

    // ---- The coached player and basic facts ----

    @Test
    void noNameMeansTheUploadersHighlightedRow() {
        PlayerAnalysis a = analyzer.analyze(player29Match, (String) null);
        assertEquals("Player29", a.player());
        assertEquals("A", a.team());
        assertEquals(Role.STRATEGIST, a.role());   // from the row's role icon
        assertTrue(a.won());                        // VICTORY banner, and Player29 is on team A
        assertEquals(6, a.teamSize());
        assertEquals(12, a.lobbySize());
        assertEquals(5.71, a.kda());                // (7 + 33) / 7
    }

    @Test
    void lookupByNameIgnoresCaseAndSkipsHiddenNames() {
        // Row 2 of this match has a null name; looking someone up must not trip over it.
        assertEquals("Player29", analyzer.analyze(player29Match, "player29").player());
        assertTrue(analyzer.analyze(player29Match, "player44").mvp());
    }

    @Test
    void teamBLostWhenTeamAWon() {
        PlayerAnalysis asfas = analyzer.analyze(player29Match, "player32");
        assertFalse(asfas.won());
        assertTrue(asfas.svp());
    }

    @Test
    void unknownPlayerOrMissingUploaderThrows() {
        assertThrows(NoSuchElementException.class, () -> analyzer.analyze(player29Match, "someone-else"));
        PlayerLine player29 = player29Match.players().get(4);
        PlayerLine notUploader = new PlayerLine(player29.name(), player29.team(), player29.role(), player29.lastHero(),
                player29.heroes(), player29.kills(), player29.deaths(), player29.assists(), player29.finalHits(),
                player29.damage(), player29.blocked(), player29.healing(), player29.accuracyPercent(),
                false, false, false, null, null);
        MatchDetails noUploader = replaceRow(player29Match, 4, notUploader);
        assertThrows(NoSuchElementException.class, () -> analyzer.analyze(noUploader, (String) null));
    }

    // ---- Stat comparisons ----

    @Test
    void healingShareAndRanks() {
        StatComparison healing = analyzer.analyze(player29Match, "Player29").stats().get(Stat.HEALING);
        assertEquals(31613, healing.value());
        assertEquals(43.6, healing.teamSharePercent());   // 31613 of the team's 72499
        assertEquals(2, healing.teamRank());              // behind player34 (40886)
        assertEquals(2, healing.lobbyRank());             // nobody on team B healed more than 31613
    }

    @Test
    void damagePerMinuteAndRanks() {
        StatComparison damage = analyzer.analyze(player29Match, "Player29").stats().get(Stat.DAMAGE);
        assertEquals(106.6, damage.perMinute());          // 1718 over 16m07s
        assertEquals(6, damage.teamRank());
        assertEquals(12, damage.lobbyRank());
    }

    @Test
    void fewerDeathsRanksHigher() {
        StatComparison deaths = analyzer.analyze(player29Match, "Player29").stats().get(Stat.DEATHS);
        assertEquals(1, deaths.teamRank());               // 7 deaths, the fewest on the team
        assertEquals(1, deaths.lobbyRank());              // and in the lobby (next best: 8)
    }

    @Test
    void finalHitsTieSharesARank() {
        StatComparison finalHits = analyzer.analyze(player29Match, "Player29").stats().get(Stat.FINAL_HITS);
        assertEquals(1, finalHits.value());
        assertEquals(6, finalHits.teamRank());
        assertEquals(11, finalHits.lobbyRank());          // tied with Player30 (1); 10 players had more
        assertEquals(11, analyzer.analyze(player29Match, "Player30").stats().get(Stat.FINAL_HITS).lobbyRank());
    }

    @Test
    void accuracyRankSkipsPlayersWithNoAccuracy() {
        assertEquals(Integer.valueOf(12), analyzer.analyze(player29Match, "Player29").accuracyLobbyRank());
        MatchDetails hidden = replaceRow(player29Match, 4, player29Match.players().get(4).withAccuracy(null));
        PlayerAnalysis a = analyzer.analyze(hidden, "Player29");
        assertNull(a.accuracyPercent());
        assertNull(a.accuracyLobbyRank());
    }

    // ---- Heroes ----

    @Test
    void withoutAHoverListTheRowsPortraitIsTheOnlyHero() {
        List<HeroPlayed> heroes = analyzer.analyze(player29Match, "Player29").heroes();
        assertEquals(1, heroes.size());
        assertEquals("Rocket Raccoon", heroes.get(0).hero());
        assertEquals(Role.STRATEGIST, heroes.get(0).role());
        assertTrue(heroes.get(0).lastPlayed());
        assertNull(heroes.get(0).kills());               // no per-hero numbers on the in-game scoreboard
    }

    @Test
    void hoverListMarksWhichHeroWasLast() {
        PlayerLine withHover = player29Match.players().get(4).withHeroes(List.of(
                HeroStats.nameOnly("Jeff the Land Shark"), HeroStats.nameOnly("Rocket Raccoon")));
        List<HeroPlayed> heroes = analyzer.analyze(replaceRow(player29Match, 4, withHover), "Player29").heroes();
        assertEquals(2, heroes.size());
        assertFalse(heroes.get(0).lastPlayed());         // Jeff, played earlier
        assertEquals(Role.STRATEGIST, heroes.get(0).role());
        assertTrue(heroes.get(1).lastPlayed());          // Rocket, the portrait on the row
    }

    @Test
    void perHeroNumbersPassThroughWhenKnown() {
        PlayerLine withNumbers = player29Match.players().get(4)
                .withHeroes(List.of(new HeroStats("Rocket Raccoon", 7, 7, 33)));
        HeroPlayed rocket = analyzer.analyze(replaceRow(player29Match, 4, withNumbers), "Player29").heroes().get(0);
        assertEquals(Integer.valueOf(7), rocket.kills());
        assertEquals(Integer.valueOf(33), rocket.assists());
    }

    // ---- Deadpool: the role icon decides; the stats guess is a last resort ----

    @Test
    void deadpoolGetsHisRoleFromTheRowIcon() {
        PlayerLine deadpoolRow = deadpoolMatch.players().get(5);   // name blacked out, so analyze the row
        PlayerAnalysis a = analyzer.analyze(deadpoolMatch, deadpoolRow);
        assertNull(a.player());
        assertTrue(a.mvp());
        assertEquals(Role.STRATEGIST, a.heroes().get(0).role());
    }

    @Test
    void statsGuessIsOnlyUsedWhenTheIconIsUnreadable_andCanBeWrong() {
        PlayerLine noIcon = deadpoolMatch.players().get(5).withRole(Role.UNKNOWN);
        MatchDetails m = replaceRow(deadpoolMatch, 5, noIcon);
        // His healing is 3rd on his team and his blocked damage 4th, so the guess says Duelist,
        // even though the icon showed Strategist. That's why the icon is checked first.
        assertEquals(Role.DUELIST, analyzer.analyze(m, noIcon).heroes().get(0).role());
    }

    @Test
    void labeledDeadpoolUsesTheRoleTable() {
        PlayerLine labeled = deadpoolMatch.players().get(5)
                .withHeroes(List.of(HeroStats.nameOnly("Deadpool (Vanguard)")));
        assertEquals(Role.VANGUARD, analyzer.analyze(deadpoolMatch, labeled).heroes().get(0).role());
    }

    @Test
    void noGuessWhenTheIconIsUnreadableAndDeadpoolSharedTheRow() {
        PlayerLine mixed = deadpoolMatch.players().get(5).withRole(Role.UNKNOWN).withHeroes(List.of(
                HeroStats.nameOnly("Luna Snow"), HeroStats.nameOnly("Deadpool")));
        List<HeroPlayed> heroes = analyzer.analyze(deadpoolMatch, mixed).heroes();
        assertEquals(Role.STRATEGIST, heroes.get(0).role());   // Luna Snow, from the table
        assertEquals(Role.UNKNOWN, heroes.get(1).role());      // totals mix both heroes, so no guess
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

package com.gamecoach.analysis;

import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.EquippedTeamUp;
import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.Medal;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.PortraitStyle;
import com.gamecoach.model.ProficiencyBadge;
import com.gamecoach.model.ProficiencyShown;
import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Expected values are worked out by hand from the three in-game scoreboards in FakeMatchHistorySource. */
class MatchAnalyzerTest {

    private final MatchAnalyzer analyzer = new MatchAnalyzer(new HeroRoleCatalog(), new TeamUpCatalog());
    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final MatchDetails player29Match = source.getMatchDetails("10378920354");
    private final MatchDetails deadpoolMatch = source.getMatchDetails("5518188_1770352206_1245014_11001_11");
    private final MatchDetails teamUpMatch = source.getMatchDetails("player10-convoy-2823");

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
                player29.heroes(), player29.teamUp(), player29.proficiency(), player29.kills(), player29.deaths(), player29.assists(), player29.finalHits(),
                player29.damage(), player29.blocked(), player29.healing(), player29.accuracyPercent(),
                false, false, false, List.of(), null, null, null);
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
    void guessComparesWithTeammatesOfEachRoleInsteadOfAssuming222() {
        // A 1-2-3 team where a Vanguard row out-heals a teammate Strategist. That happens when the
        // Vanguard played a Strategist earlier in the match: a row's totals cover every hero they used.
        // Deadpool is only 3rd in team healing, but he out-heals one real Strategist, so: Strategist.
        PlayerLine deadpool = line(Role.UNKNOWN, 5000, 9000);
        List<PlayerLine> team = List.of(deadpool,
                line(Role.VANGUARD, 40000, 13000), line(Role.DUELIST, 8000, 0), line(Role.DUELIST, 9000, 0),
                line(Role.STRATEGIST, 7000, 20000), line(Role.STRATEGIST, 6000, 6000));
        assertEquals(Role.STRATEGIST, MatchAnalyzer.guessRoleFromStats(deadpool, team));
    }

    @Test
    void guessWithNoTeammateStrategistsNeedsTheMostHealing() {
        PlayerLine deadpool = line(Role.UNKNOWN, 5000, 5000);
        List<PlayerLine> team = List.of(deadpool,
                line(Role.VANGUARD, 40000, 2000), line(Role.VANGUARD, 30000, 0), line(Role.DUELIST, 8000, 0),
                line(Role.DUELIST, 9000, 0), line(Role.DUELIST, 7000, 1500));
        assertEquals(Role.STRATEGIST, MatchAnalyzer.guessRoleFromStats(deadpool, team));
    }

    @Test
    void guessFallsThroughToVanguardThenDuelist() {
        PlayerLine tank = line(Role.UNKNOWN, 35000, 0);
        PlayerLine dps = line(Role.UNKNOWN, 9000, 0);
        List<PlayerLine> mates = List.of(line(Role.VANGUARD, 30000, 0), line(Role.DUELIST, 8000, 0),
                line(Role.DUELIST, 9000, 0), line(Role.STRATEGIST, 7000, 20000), line(Role.STRATEGIST, 6000, 18000));
        List<PlayerLine> withTank = new ArrayList<>(mates);
        withTank.add(tank);
        List<PlayerLine> withDps = new ArrayList<>(mates);
        withDps.add(dps);
        assertEquals(Role.VANGUARD, MatchAnalyzer.guessRoleFromStats(tank, withTank));
        assertEquals(Role.DUELIST, MatchAnalyzer.guessRoleFromStats(dps, withDps));
    }

    // ---- Team-ups (Season 9 rework): the box beside the portrait ----

    @Test
    void equippedTeamUpIsLookedUpFromHeroAndPartner() {
        TeamUpAnalysis t = analyzer.analyze(teamUpMatch, "Player20").teamUp();
        assertEquals("Frozen Haven", t.name());          // Cloak & Dagger + Luna Snow
        assertEquals("Luna Snow", t.partner());
        assertTrue(t.enhancedOnScoreboard());            // full-color box
        assertTrue(t.partnerOnTeam());                   // Player 14 played Luna Snow
        assertNull(t.betterOption());
    }

    @Test
    void baseOnlyTeamUpWhenThePartnerIsMissing() {
        TeamUpAnalysis t = analyzer.analyze(teamUpMatch, "Player 21").teamUp();
        assertEquals("Abyssal Flames", t.name());        // Venom + Phoenix; no Phoenix on the team
        assertFalse(t.enhancedOnScoreboard());
        assertFalse(t.partnerOnTeam());
        assertNull(t.betterOption());                    // his other partner (Blade) isn't there either
    }

    @Test
    void betterOptionWhenTheOtherTeamUpsPartnerIsOnTheTeam() {
        // Devil Dinosaur equipped Primal Punishment (The Punisher, not on his team), but
        // Jeff the Land Shark was on his team, which would have enhanced Surf & Turf.
        TeamUpAnalysis t = analyzer.analyze(teamUpMatch, "Player_16").teamUp();
        assertEquals("Primal Punishment", t.name());
        assertEquals("Surf & Turf (Jeff the Land Shark)", t.betterOption());
    }

    @Test
    void listsTeammatesTeamUpsThePlayersHeroEnhances() {
        assertEquals(List.of("Frozen Haven (Player20)"),
                analyzer.analyze(teamUpMatch, "Player 14").teammateTeamUpsYouEnhance());   // Luna Snow
        assertEquals(List.of("Atlas Bond (Player 14)"),
                analyzer.analyze(teamUpMatch, "player42").teammateTeamUpsYouEnhance());      // White Fox
        assertEquals(List.of(), analyzer.analyze(teamUpMatch, (String) null).teammateTeamUpsYouEnhance());
    }

    @Test
    void noTeamUpBoxMeansNoTeamUpAnalysis() {
        assertNull(analyzer.analyze(player29Match, (String) null).teamUp());   // older scoreboard layout
    }

    @Test
    void unknownPartnerKeepsTheBoxButHasNoName() {
        PlayerLine misread = teamUpMatch.players().get(0).withTeamUp(new EquippedTeamUp("Not A Hero", false));
        TeamUpAnalysis t = analyzer.analyze(teamUpMatch, misread).teamUp();
        assertNull(t.name());
        assertEquals("Not A Hero", t.partner());
    }

    // ---- Team compositions, read from the role icons ----

    @Test
    void compositionsComeFromTheRoleIcons() {
        PlayerAnalysis player29 = analyzer.analyze(player29Match, (String) null);
        assertEquals("2-2-2", player29.teamComposition());
        assertEquals("1-2-3", player29.enemyComposition());
        PlayerAnalysis player08 = analyzer.analyze(deadpoolMatch, (String) null);
        assertEquals("2-1-3", player08.teamComposition());    // the Strategist Deadpool counts as a Strategist
        assertEquals("3-1-2", player08.enemyComposition());
    }

    @Test
    void unreadableIconsShowUpInTheComposition() {
        MatchDetails m = replaceRow(deadpoolMatch, 5, deadpoolMatch.players().get(5).withRole(Role.UNKNOWN));
        assertEquals("2-1-2 (+1 unknown)", analyzer.analyze(m, (String) null).teamComposition());
    }

    // ---- Medals ----

    @Test
    void medalsAndMultiKillsCarryIntoTheAnalysis() {
        PlayerAnalysis player44 = analyzer.analyze(player29Match, "player44");
        assertEquals(List.of(Medal.MOST_KOS), player44.medals());
        assertEquals(Integer.valueOf(5), player44.multiKill());   // a Penta
        PlayerAnalysis dizzle = analyzer.analyze(player29Match, "Plàyer12");
        assertTrue(dizzle.medals().isEmpty());
        assertNull(dizzle.multiKill());
    }

    @Test
    void anUnreadableBadgeStillProvesCount() {
        PlayerAnalysis player08 = analyzer.analyze(deadpoolMatch, (String) null);
        assertEquals(25, player08.proficiency().minimumLevel());
        assertEquals("Count", player08.proficiency().minimumRank());
        PlayerLine invisibleWoman = deadpoolMatch.players().get(4);
        assertEquals(50, analyzer.analyze(deadpoolMatch, invisibleWoman).proficiency().minimumLevel());  // animated Lord
    }

    // ---- Proficiency: Lord portraits and Proficiency Badges ----

    /** The Player29 row (Rocket Raccoon) with these cosmetics. */
    private ProficiencyAnalysis proficiencyWith(PortraitStyle portrait, ProficiencyBadge badge) {
        PlayerLine row = player29Match.players().get(4).withProficiency(new ProficiencyShown(portrait, badge));
        return analyzer.analyze(replaceRow(player29Match, 4, row), (String) null).proficiency();
    }

    @Test
    void nothingShownMeansNoProficiencyClaim() {
        assertNull(analyzer.analyze(player29Match, (String) null).proficiency());   // not read
        assertNull(proficiencyWith(PortraitStyle.DEFAULT, null));                  // nothing equipped
    }

    @Test
    void lordPortraitAloneProvesLevel20() {
        ProficiencyAnalysis p = proficiencyWith(PortraitStyle.LORD, null);
        assertEquals("Rocket Raccoon", p.hero());
        assertEquals(20, p.minimumLevel());
        assertEquals("Lord", p.minimumRank());
    }

    @Test
    void theHigherOfPortraitAndBadgeWins() {
        ProficiencyAnalysis warrior = proficiencyWith(PortraitStyle.LORD, ProficiencyBadge.WARRIOR);
        assertEquals(35, warrior.minimumLevel());
        assertEquals("Warrior", warrior.minimumRank());

        ProficiencyAnalysis animated = proficiencyWith(PortraitStyle.ANIMATED_LORD, ProficiencyBadge.COUNT);
        assertEquals(50, animated.minimumLevel());       // animated Lord (50) beats a Count badge (25)
        assertEquals("Champion", animated.minimumRank());

        assertEquals(55, proficiencyWith(PortraitStyle.ANIMATED_LORD, ProficiencyBadge.CHAMPION).minimumLevel());
    }

    @Test
    void aBadgeWithTheDefaultPortraitStillCounts() {
        ProficiencyAnalysis p = proficiencyWith(PortraitStyle.DEFAULT, ProficiencyBadge.GUARDIAN);
        assertEquals(45, p.minimumLevel());
        assertEquals(PortraitStyle.DEFAULT, p.portrait());
    }

    /** A bare row for the guess tests: only the role icon, blocked and healing matter. */
    private static PlayerLine line(Role role, int blocked, int healing) {
        return new PlayerLine(null, "A", role, "Someone", List.of(), null, null, 0, 0, 0, 0, 0, blocked, healing,
                null, false, false, false, List.of(), null, null, null);
    }
}

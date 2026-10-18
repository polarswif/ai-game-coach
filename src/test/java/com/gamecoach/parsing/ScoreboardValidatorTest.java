package com.gamecoach.parsing;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.EquippedTeamUp;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.model.Medal;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardValidatorTest {

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final ScoreboardValidator validator = new ScoreboardValidator(new HeroRoleCatalog(), new TeamUpCatalog());
    private final MatchDetails player29 = source.getMatchDetails("10378920354");

    /** The match with one row changed. */
    private static MatchDetails change(MatchDetails match, int row, UnaryOperator<PlayerLine> edit) {
        List<PlayerLine> players = new ArrayList<>(match.players());
        players.set(row, edit.apply(players.get(row)));
        return match.withPlayers(players);
    }

    private static PlayerLine withHero(PlayerLine p, String hero, int damage, int finalHits, int kills) {
        return new PlayerLine(p.name(), p.team(), p.role(), hero, p.heroes(), p.teamUp(), p.proficiency(), kills,
                p.deaths(), p.assists(), finalHits, damage, p.blocked(), p.healing(), p.accuracyPercent(), p.uploader(),
                p.mvp(), p.svp(), p.medals(), p.multiKill(), p.rank(), p.rankScore());
    }

    private void assertWarns(MatchDetails match, String fragment) {
        List<String> warnings = validator.check(match);
        assertTrue(warnings.stream().anyMatch(w -> w.contains(fragment)), "expected \"" + fragment + "\" in " + warnings);
    }

    @Test
    void theRealMatchesPassEveryCheck() {
        for (MatchSummary summary : source.getRecentMatches("anyone")) {
            assertEquals(List.of(), validator.check(source.getMatchDetails(summary.matchId())), summary.matchId());
        }
    }

    @Test
    void aMissingRowIsNoticed() {
        assertWarns(player29.withPlayers(player29.players().subList(0, 11)), "Expected 6 players per team");
    }

    @Test
    void anUnknownHeroOrAWrongRoleIconIsNoticed() {
        assertWarns(change(player29, 0, p -> withHero(p, "Hulkk", p.damage(), p.finalHits(), p.kills())), "isn't in the hero table");
        assertWarns(change(player29, 0, p -> p.withRole(Role.STRATEGIST)), "the role icon reads STRATEGIST but Hulk is a VANGUARD");
    }

    @Test
    void aMedalMustGoToTheLobbyLeader() {
        // Row 1 (Hulk) didn't do the most damage in the lobby.
        assertWarns(change(player29, 0, p -> p.withMedals(List.of(Medal.MOST_DAMAGE), null)), "has the most-damage medal");
    }

    @Test
    void impossibleNumbersAreNoticed() {
        assertWarns(change(player29, 0, p -> withHero(p, p.lastHero(), p.damage(), 40, p.kills())), "final hits is more than");
        assertWarns(change(player29, 0, p -> p.withMedals(p.medals(), 9)), "multi-kill of 9");
        assertWarns(change(player29, 0, p -> p.withAccuracy(330.0)), "outside 0-100");
    }

    @Test
    void finalHitsFarFromTheEnemysDeathsPointAtAMisreadDigit() {
        // 7 final hits misread as 70: team A's final hits jump far above team B's deaths.
        assertWarns(change(player29, 0, p -> withHero(p, p.lastHero(), p.damage(), 70, 79)), "final hits");
    }

    @Test
    void anMvpOnTheLosingTeamIsNoticed() {
        int enemyRow = 6;   // the bottom half; Player29's team won
        MatchDetails match = change(player29, enemyRow, p -> new PlayerLine(p.name(), p.team(), p.role(), p.lastHero(),
                p.heroes(), p.teamUp(), p.proficiency(), p.kills(), p.deaths(), p.assists(), p.finalHits(), p.damage(),
                p.blocked(), p.healing(), p.accuracyPercent(), p.uploader(), true, false, p.medals(), p.multiKill(),
                p.rank(), p.rankScore()));
        assertWarns(match, "marked MVP but is on the losing team");
    }

    @Test
    void aTeamUpThatDoesntExistIsNoticed() {
        assertWarns(change(player29, 0, p -> p.withTeamUp(new EquippedTeamUp("Squirrel Girl", false))), "Hulk has no team-up with Squirrel Girl");
    }

    @Test
    void noHighlightedRowIsNoticed() {
        List<PlayerLine> players = new ArrayList<>();
        for (PlayerLine p : player29.players()) {
            players.add(new PlayerLine(p.name(), p.team(), p.role(), p.lastHero(), p.heroes(), p.teamUp(), p.proficiency(),
                    p.kills(), p.deaths(), p.assists(), p.finalHits(), p.damage(), p.blocked(), p.healing(),
                    p.accuracyPercent(), false, p.mvp(), p.svp(), p.medals(), p.multiKill(), p.rank(), p.rankScore()));
        }
        assertWarns(player29.withPlayers(players), "No highlighted row");
    }
}

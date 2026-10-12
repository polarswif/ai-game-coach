package com.gamecoach.datasource;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.model.Medal;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sanity checks on the hand-typed screenshot data, to catch typos. */
class FakeMatchHistorySourceTest {

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final HeroRoleCatalog roles = new HeroRoleCatalog();
    private final TeamUpCatalog teamUps = new TeamUpCatalog();

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

    /**
     * Another check the screenshot reader will run: a team-up box is in full color exactly when its
     * partner is on the same team, and every (hero, partner) pair exists in the team-up table.
     */
    @Test
    void everyTeamUpBoxAgreesWithTheTeamAndTheTable() {
        for (MatchDetails match : allMatches()) {
            for (PlayerLine p : match.players()) {
                if (p.teamUp() == null) {
                    continue;
                }
                String partner = p.teamUp().partner();
                assertTrue(teamUps.find(p.lastHero(), partner).isPresent(), p.lastHero() + " + " + partner);
                boolean partnerOnTeam = match.players().stream()
                        .anyMatch(o -> o != p && o.team().equals(p.team()) && HeroRoleCatalog.sameHero(o.lastHero(), partner));
                assertEquals(partnerOnTeam, p.teamUp().enhanced(), p.lastHero() + " + " + partner);
            }
        }
    }

    /**
     * Another reader check: each lobby medal is awarded exactly once per match, to the player with the
     * lobby's highest number for that stat (either team). Multi-kill numbers run from 3 to 6.
     */
    @Test
    void eachMedalGoesToTheLobbyLeaderForItsStat() {
        Map<Medal, ToIntFunction<PlayerLine>> statOf = Map.of(
                Medal.MOST_KOS, PlayerLine::kills,
                Medal.MOST_DAMAGE, PlayerLine::damage,
                Medal.MOST_BLOCKED, PlayerLine::blocked,
                Medal.MOST_HEALING, PlayerLine::healing,
                Medal.MOST_ASSISTS, PlayerLine::assists);
        for (MatchDetails match : allMatches()) {
            for (Medal medal : Medal.values()) {
                List<PlayerLine> holders = match.players().stream().filter(p -> p.medals().contains(medal)).toList();
                assertEquals(1, holders.size(), medal + " in match " + match.matchId());
                ToIntFunction<PlayerLine> stat = statOf.get(medal);
                int best = match.players().stream().mapToInt(stat).max().getAsInt();
                assertEquals(best, stat.applyAsInt(holders.get(0)), medal + " in match " + match.matchId());
            }
            for (PlayerLine p : match.players()) {
                if (p.multiKill() != null) {
                    assertTrue(p.multiKill() >= 3 && p.multiKill() <= 6, p.lastHero() + " " + p.multiKill());
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

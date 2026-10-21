package com.gamecoach;

import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchMatch.HeroStat;
import com.gamecoach.model.OverwatchMatch.HeroStatsPanel;
import com.gamecoach.model.OverwatchMatch.HeroTime;
import com.gamecoach.model.OverwatchPlayerLine;
import com.gamecoach.model.OverwatchResult;
import com.gamecoach.model.OverwatchRole;
import com.gamecoach.model.RoundScore;

import java.time.Duration;
import java.util.List;

/**
 * A real match from the user's screenshots (Push on Colosseo, 5:30, a 1-0 win on Ramattra), as the
 * reader should transcribe it. Portraits other than the uploader's are stand-ins: the tests only
 * need valid heroes in the right roles.
 */
public final class OverwatchMatchFixtures {

    private OverwatchMatchFixtures() {
    }

    public static final OverwatchPlayerLine PLAYERONE = row("PLAYERONE", null, "A", OverwatchRole.TANK, "Ramattra", 21, 0, 1, 7013, 105, 4405, true);

    public static List<OverwatchPlayerLine> players() {
        return List.of(
                PLAYERONE,
                row("PLAYERFOUR", "Haunted Shadow", "A", OverwatchRole.DAMAGE, "Sojourn", 10, 2, 2, 2491, 50, 506, false),
                row("REAPER", "Assassin", "A", OverwatchRole.DAMAGE, "Tracer", 13, 3, 2, 5104, 0, 0, false),
                row("PLAYERTHREE", "Philosopher", "A", OverwatchRole.SUPPORT, "Zenyatta", 15, 9, 1, 5109, 2805, 0, false),
                row("PLAYER2026", null, "A", OverwatchRole.SUPPORT, "Baptiste", 5, 15, 1, 2105, 5723, 0, false),
                row("PLAYERSEVEN", "Magitechnician", "B", OverwatchRole.TANK, "Roadhog", 3, 0, 6, 4337, 1123, 3829, false),
                row("PLAYERTWENTYTHREE", null, "B", OverwatchRole.DAMAGE, "Ashe", 5, 2, 5, 3518, 0, 0, false),
                row("PLAYERTWENTYEIGHT", "Vendetta's Vanguard", "B", OverwatchRole.DAMAGE, "Mei", 3, 1, 6, 3045, 347, 2322, false),
                row("PLAYEREIGHTEEN", "Philosopher", "B", OverwatchRole.SUPPORT, "Moira", 4, 3, 4, 1570, 5399, 344, false),
                row("PLAYERTWENTYFIVE", null, "B", OverwatchRole.SUPPORT, "Ana", 4, 4, 4, 1985, 4365, 909, false));
    }

    public static List<HeroStatsPanel> ramattraStats() {
        return List.of(new HeroStatsPanel("Ramattra", List.of(
                new HeroStat("Weapon accuracy", "30%", "30%"),
                new HeroStat("Ravenous Vortex kills", "4", "7.29"),
                new HeroStat("Objective contest time", "01:28", "02:40"),
                new HeroStat("Pummel accuracy", "38%", "38%"),
                new HeroStat("Annihilation efficiency", "230", "230"),
                new HeroStat("Players protected", "0", "0.00"),
                new HeroStat("Pummel kills", "10", "18.21"))));
    }

    public static OverwatchMatch colosseo() {
        return new OverwatchMatch("ow-test1234", "Push", "Colosseo", Duration.ofMinutes(5).plusSeconds(30),
                OverwatchResult.VICTORY, new RoundScore(1, 0), "10/21/26 - 18:31", players(),
                List.of(new HeroTime("Ramattra", 100.0, Duration.ofMinutes(5).plusSeconds(29))), ramattraStats());
    }

    /** The same match as read from the Teams screen alone: no result, length or heroes. */
    public static OverwatchMatch teamsOnly() {
        return new OverwatchMatch("ow-test5678", null, null, null, null, null, null, players(), List.of(), List.of());
    }

    public static OverwatchPlayerLine row(String name, String title, String team, OverwatchRole role, String hero,
                                          int e, int a, int d, int dmg, int heal, int mit, boolean uploader) {
        return new OverwatchPlayerLine(name, title, team, role, hero, e, a, d, dmg, heal, mit, uploader);
    }
}

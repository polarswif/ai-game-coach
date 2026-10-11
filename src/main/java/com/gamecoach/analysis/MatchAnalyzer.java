package com.gamecoach.analysis;

import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchResult;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Computes the comparisons the coach needs: KDA, per-minute stats, team share, ranks, and hero roles. */
@Service
public class MatchAnalyzer {

    private final HeroRoleCatalog roles;

    public MatchAnalyzer(HeroRoleCatalog roles) {
        this.roles = roles;
    }

    /**
     * Analyzes the player with this name (case is ignored), or the uploader's highlighted row
     * when the name is null or blank. Throws NoSuchElementException if there is no such row.
     */
    public PlayerAnalysis analyze(MatchDetails match, String playerName) {
        return analyze(match, findPlayer(match, playerName));
    }

    /** Analyzes one row of the match. Useful when the row's name is hidden. */
    public PlayerAnalysis analyze(MatchDetails match, PlayerLine player) {
        List<PlayerLine> lobby = match.players();
        List<PlayerLine> team = lobby.stream().filter(p -> p.team().equals(player.team())).toList();
        double minutes = Math.max(match.duration().toSeconds() / 60.0, 1.0 / 60);

        Map<Stat, StatComparison> stats = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) {
            int value = stat.read(player);
            int teamTotal = team.stream().mapToInt(stat::read).sum();
            stats.put(stat, new StatComparison(
                    value,
                    round(value / minutes, 1),
                    teamTotal == 0 ? 0.0 : round(100.0 * value / teamTotal, 1),
                    rank(player, team, stat),
                    rank(player, lobby, stat)));
        }

        // No "Heroes Used" list captured: the row's portrait is the only hero we know about.
        List<HeroStats> used = player.heroes().isEmpty()
                ? List.of(HeroStats.nameOnly(player.lastHero()))
                : player.heroes();
        List<HeroPlayed> heroes = new ArrayList<>();
        for (HeroStats hero : used) {
            boolean last = HeroRoleCatalog.sameHero(hero.hero(), player.lastHero());
            Role role = roleOf(hero.hero(), last, player, used.size(), stats);
            heroes.add(new HeroPlayed(hero.hero(), role, last, hero.kills(), hero.deaths(), hero.assists()));
        }

        return new PlayerAnalysis(
                player.name(),
                player.team(),
                player.role(),
                won(match, player.team()),
                player.mvp(),
                player.svp(),
                team.size(),
                lobby.size(),
                round((player.kills() + player.assists()) / (double) Math.max(player.deaths(), 1), 2),
                heroes,
                stats,
                player.accuracyPercent(),
                accuracyRank(player, lobby));
    }

    /**
     * Works out one hero's role, trying the most reliable source first:
     * 1. The role table. Most heroes have one fixed role, and "Deadpool (Strategist)" is listed too.
     * 2. The row's role icon, but only for the last hero played, since that's the hero it belongs to.
     *    This covers a plain "Deadpool" and heroes newer than the table.
     * 3. For a Deadpool row with no readable icon, a guess from the stats. It's last because it can
     *    be wrong: a damage-heavy Strategist Deadpool looks like a Duelist on paper.
     */
    private Role roleOf(String hero, boolean last, PlayerLine player, int heroCount,
                        Map<Stat, StatComparison> stats) {
        Role fromTable = roles.roleOf(hero);
        if (fromTable != Role.UNKNOWN) {
            return fromTable;
        }
        if (last && player.role() != null && player.role() != Role.UNKNOWN) {
            return player.role();
        }
        if (HeroRoleCatalog.isDeadpool(hero) && heroCount == 1) {
            return guessRoleFromStats(stats);   // only safe when the row's totals are all Deadpool's
        }
        return Role.UNKNOWN;
    }

    private static PlayerLine findPlayer(MatchDetails match, String name) {
        boolean wantUploader = name == null || name.isBlank();
        for (PlayerLine line : match.players()) {
            if (wantUploader ? line.uploader() : name.equalsIgnoreCase(line.name())) {
                return line;
            }
        }
        throw new NoSuchElementException(wantUploader
                ? "Match " + match.matchId() + " has no highlighted uploader row"
                : name + " is not in match " + match.matchId());
    }

    /** 1 + the number of players who did strictly better, so ties share a rank. */
    private static int rank(PlayerLine player, List<PlayerLine> group, Stat stat) {
        int mine = stat.read(player);
        int better = 0;
        for (PlayerLine other : group) {
            int theirs = stat.read(other);
            if (stat.higherIsBetter() ? theirs > mine : theirs < mine) {
                better++;
            }
        }
        return better + 1;
    }

    /**
     * Last-resort guess at a Deadpool player's role from how their stats rank on their own team.
     * Teams usually run 2 Vanguards, 2 Duelists and 2 Strategists, so a top-2 healer on the team
     * was probably a Strategist, and a top-2 blocker probably a Vanguard. Otherwise: Duelist.
     */
    static Role guessRoleFromStats(Map<Stat, StatComparison> stats) {
        StatComparison healing = stats.get(Stat.HEALING);
        if (healing.value() > 0 && healing.teamRank() <= 2) {
            return Role.STRATEGIST;
        }
        StatComparison blocked = stats.get(Stat.BLOCKED);
        if (blocked.value() > 0 && blocked.teamRank() <= 2) {
            return Role.VANGUARD;
        }
        return Role.DUELIST;
    }

    private static Integer accuracyRank(PlayerLine player, List<PlayerLine> lobby) {
        Double mine = player.accuracyPercent();
        if (mine == null) {
            return null;
        }
        int better = 0;
        for (PlayerLine other : lobby) {
            if (other.accuracyPercent() != null && other.accuracyPercent() > mine) {
                better++;
            }
        }
        return better + 1;
    }

    /** The banner is from team A's point of view, so team B won when team A lost. */
    private static boolean won(MatchDetails match, String team) {
        boolean teamAWon = match.result() == MatchResult.VICTORY;
        return "A".equals(team) == teamAWon;
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}

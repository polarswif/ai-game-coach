package com.gamecoach.analysis;

import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Computes the comparisons the coach needs: KDA, per-minute stats, team share, and ranks. */
@Service
public class MatchAnalyzer {

    private final HeroRoleCatalog roles;

    public MatchAnalyzer(HeroRoleCatalog roles) {
        this.roles = roles;
    }

    /** Throws NoSuchElementException if no player in the match has that name (case is ignored). */
    public PlayerAnalysis analyze(MatchDetails match, String playerName) {
        PlayerLine player = findPlayer(match, playerName);
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

        List<HeroPlayed> heroes = new ArrayList<>();
        for (HeroStats hero : player.heroes()) {
            Role role = roles.roleOf(hero.hero());
            Role suggested = null;
            if (HeroRoleCatalog.isDeadpool(hero.hero()) && player.heroes().size() == 1) {
                suggested = guessRoleFromStats(stats);
                if (role == Role.UNKNOWN) {
                    role = suggested;   // no label on the scoreboard, so the guess is the best we have
                }
            }
            heroes.add(new HeroPlayed(hero.hero(), role, suggested,
                    hero.kills(), hero.deaths(), hero.assists()));
        }

        return new PlayerAnalysis(
                player.name(),
                player.team(),
                won(match, player.team()),
                team.size(),
                lobby.size(),
                round((player.kills() + player.assists()) / (double) Math.max(player.deaths(), 1), 2),
                heroes,
                stats,
                player.accuracyPercent(),
                accuracyRank(player, lobby));
    }

    private static PlayerLine findPlayer(MatchDetails match, String name) {
        for (PlayerLine line : match.players()) {
            if (line.name().equalsIgnoreCase(name)) {
                return line;
            }
        }
        throw new NoSuchElementException(name + " is not in match " + match.matchId());
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
     * Guesses which role a Deadpool player was in from how their stats rank on their own team.
     * Teams usually run 2 Vanguards, 2 Duelists and 2 Strategists, so a top-2 healer on the team
     * was most likely a Strategist, and a top-2 blocker most likely a Vanguard. Healing is checked
     * first because only Strategists heal in large amounts. Otherwise: Duelist.
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

    private static boolean won(MatchDetails match, String team) {
        return "A".equals(team) ? match.teamAScore() > match.teamBScore()
                                : match.teamBScore() > match.teamAScore();
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}

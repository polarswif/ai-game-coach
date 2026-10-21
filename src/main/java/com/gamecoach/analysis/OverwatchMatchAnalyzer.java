package com.gamecoach.analysis;

import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchMatch.HeroTime;
import com.gamecoach.model.OverwatchPlayerLine;
import com.gamecoach.model.OverwatchResult;
import com.gamecoach.model.OverwatchRole;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.ToIntFunction;

/**
 * The Overwatch counterpart of MatchAnalyzer: per-10 rates, team share, ranks, the matching enemy
 * in the same role, team compositions and how close the match was.
 */
@Service
public class OverwatchMatchAnalyzer {

    private final OverwatchHeroCatalog heroes;

    public OverwatchMatchAnalyzer(OverwatchHeroCatalog heroes) {
        this.heroes = heroes;
    }

    /** The player with this name (case ignored), or the uploader's row when the name is blank. */
    public OverwatchPlayerAnalysis analyze(OverwatchMatch match, String playerName) {
        return analyzeRow(match, findPlayer(match, playerName));
    }

    /** Analyzes one row; useful when the row's name is hidden. */
    public OverwatchPlayerAnalysis analyzeRow(OverwatchMatch match, OverwatchPlayerLine player) {
        List<OverwatchPlayerLine> lobby = match.players();
        List<OverwatchPlayerLine> team = lobby.stream().filter(p -> p.team().equals(player.team())).toList();
        List<OverwatchPlayerLine> enemies = lobby.stream().filter(p -> !p.team().equals(player.team())).toList();
        OverwatchRole role = roleOf(player);
        List<OverwatchPlayerLine> enemySameRole = enemies.stream().filter(p -> roleOf(p) == role && role != null).toList();
        Double tens = match.duration() == null || match.duration().isZero() ? null : match.duration().toSeconds() / 600.0;

        Map<OverwatchStat, OverwatchStatComparison> stats = new EnumMap<>(OverwatchStat.class);
        for (OverwatchStat stat : OverwatchStat.values()) {
            int value = stat.read(player);
            int teamTotal = team.stream().mapToInt(stat::read).sum();
            Integer enemyBest = enemySameRole.isEmpty() ? null : enemySameRole.stream().mapToInt(stat::read)
                    .reduce(stat.higherIsBetter() ? Math::max : Math::min).getAsInt();
            stats.put(stat, new OverwatchStatComparison(
                    value,
                    tens == null ? null : round(value / tens, 1),
                    teamTotal == 0 ? 0.0 : round(100.0 * value / teamTotal, 1),
                    rank(player, team, stat),
                    rank(player, lobby, stat),
                    enemyBest));
        }

        Boolean won = won(match, player.team());
        String outcome = match.result() == null ? null
                : match.result() == OverwatchResult.DRAW ? "DRAW" : won ? "WIN" : "LOSS";
        List<HeroTime> heroesPlayed = player.uploader() && !match.yourHeroes().isEmpty()
                ? match.yourHeroes() : List.of(new HeroTime(player.hero(), null, null));

        return new OverwatchPlayerAnalysis(
                player.name(),
                player.team(),
                role,
                player.hero(),
                outcome,
                team.size(),
                lobby.size(),
                composition(team),
                composition(enemies),
                heroesPlayed,
                round(player.eliminations() / (double) Math.max(player.deaths(), 1), 2),
                stats,
                player.uploader() ? match.heroStats() : List.of(),
                closeness(match, team, enemies, won != null && won));
    }

    /** The role icon when it was read, otherwise the hero's role from the table. */
    private OverwatchRole roleOf(OverwatchPlayerLine p) {
        return p.role() != null ? p.role() : heroes.roleOfName(p.hero());
    }

    /**
     * Closeness from the final score when it's meaningful, else from deaths, as in Rivals. Push is
     * excluded: its final score is just who pushed further (1 vs 0), not a count of rounds.
     */
    static MatchCloseness closeness(OverwatchMatch match, List<OverwatchPlayerLine> team,
                                    List<OverwatchPlayerLine> enemies, boolean won) {
        int teamDeaths = team.stream().mapToInt(OverwatchPlayerLine::deaths).sum();
        int enemyDeaths = enemies.stream().mapToInt(OverwatchPlayerLine::deaths).sum();
        double ratio = round(Math.max(teamDeaths, enemyDeaths) / (double) Math.max(Math.min(teamDeaths, enemyDeaths), 1), 2);
        boolean scoreUsable = match.score() != null && match.mode() != null
                && !match.mode().strip().equalsIgnoreCase("push")
                && match.score().teamA() + match.score().teamB() > 0;
        Integer ours = null;
        Integer theirs = null;
        Closeness level;
        if (scoreUsable) {
            boolean teamA = "A".equals(team.isEmpty() ? "A" : team.get(0).team());
            ours = teamA ? match.score().teamA() : match.score().teamB();
            theirs = teamA ? match.score().teamB() : match.score().teamA();
            level = MatchAnalyzer.fromRounds(ours, theirs);
        } else {
            level = ratio <= MatchAnalyzer.CLOSE_RATIO ? Closeness.CLOSE
                    : ratio >= MatchAnalyzer.ONE_SIDED_RATIO ? Closeness.ONE_SIDED
                    : Closeness.CLEAR_EDGE;
        }
        boolean ahead = teamDeaths == enemyDeaths ? won : teamDeaths < enemyDeaths;
        return new MatchCloseness(level, ours, theirs, ahead, teamDeaths, enemyDeaths, ratio,
                share(team, enemies, OverwatchPlayerLine::damage), share(team, enemies, OverwatchPlayerLine::healing));
    }

    /** Tanks-damage-supports, e.g. "1-2-2", with unreadable icons as "+N unknown". */
    String composition(List<OverwatchPlayerLine> team) {
        long t = team.stream().filter(p -> roleOf(p) == OverwatchRole.TANK).count();
        long d = team.stream().filter(p -> roleOf(p) == OverwatchRole.DAMAGE).count();
        long s = team.stream().filter(p -> roleOf(p) == OverwatchRole.SUPPORT).count();
        long unknown = team.size() - t - d - s;
        return t + "-" + d + "-" + s + (unknown > 0 ? " (+" + unknown + " unknown)" : "");
    }

    /** Null when the result is unknown. The result is from team A's side. */
    private static Boolean won(OverwatchMatch match, String team) {
        if (match.result() == null || match.result() == OverwatchResult.DRAW) {
            return match.result() == null ? null : false;
        }
        return "A".equals(team) == (match.result() == OverwatchResult.VICTORY);
    }

    private static OverwatchPlayerLine findPlayer(OverwatchMatch match, String name) {
        boolean wantUploader = name == null || name.isBlank();
        for (OverwatchPlayerLine line : match.players()) {
            if (wantUploader ? line.uploader() : name.strip().equalsIgnoreCase(line.name())) {
                return line;
            }
        }
        throw new NoSuchElementException(wantUploader
                ? "Match " + match.matchId() + " has no row marked as yours. Mark it, or add ?username="
                : name + " is not in match " + match.matchId());
    }

    private static int rank(OverwatchPlayerLine player, List<OverwatchPlayerLine> group, OverwatchStat stat) {
        int mine = stat.read(player);
        int better = 0;
        for (OverwatchPlayerLine other : group) {
            int theirs = stat.read(other);
            if (stat.higherIsBetter() ? theirs > mine : theirs < mine) {
                better++;
            }
        }
        return better + 1;
    }

    private static double share(List<OverwatchPlayerLine> team, List<OverwatchPlayerLine> enemies,
                                ToIntFunction<OverwatchPlayerLine> stat) {
        int ours = team.stream().mapToInt(stat).sum();
        int total = ours + enemies.stream().mapToInt(stat).sum();
        return total == 0 ? 50.0 : round(100.0 * ours / total, 1);
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}

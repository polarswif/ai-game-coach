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
import java.util.function.ToIntFunction;

/** Computes the comparisons the coach needs: KDA, per-minute stats, team share, ranks, and hero roles. */
@Service
public class MatchAnalyzer {

    private final HeroRoleCatalog roles;
    private final TeamUpCatalog teamUps;

    public MatchAnalyzer(HeroRoleCatalog roles, TeamUpCatalog teamUps) {
        this.roles = roles;
        this.teamUps = teamUps;
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
        List<PlayerLine> enemies = lobby.stream().filter(p -> !p.team().equals(player.team())).toList();
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
            Role role = roleOf(hero.hero(), last, player, used.size(), team);
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
                composition(team),
                composition(enemies),
                round((player.kills() + player.assists()) / (double) Math.max(player.deaths(), 1), 2),
                heroes,
                teamUp(player, team),
                teammateTeamUpsYouEnhance(player, team),
                stats,
                player.accuracyPercent(),
                accuracyRank(player, lobby));
    }

    /**
     * Works out one hero's role, trying the most reliable source first:
     * 1. The role table. Most heroes have one fixed role, and "Deadpool (Strategist)" is listed too.
     * 2. The row's role icon, but only for the last hero played, since that's the hero it belongs to.
     *    This covers a plain "Deadpool" and heroes newer than the table.
     * 3. For a Deadpool row with no readable icon, a guess from the stats compared with his
     *    teammates. It's last because it can be wrong (see guessRoleFromStats).
     */
    private Role roleOf(String hero, boolean last, PlayerLine player, int heroCount, List<PlayerLine> team) {
        Role fromTable = roles.roleOf(hero);
        if (fromTable != Role.UNKNOWN) {
            return fromTable;
        }
        if (last && player.role() != null && player.role() != Role.UNKNOWN) {
            return player.role();
        }
        if (HeroRoleCatalog.isDeadpool(hero) && heroCount == 1) {
            return guessRoleFromStats(player, team);   // only safe when the row's totals are all Deadpool's
        }
        return Role.UNKNOWN;
    }

    /**
     * Looks up the equipped team-up from (last hero, partner in the box), checks whether a teammate
     * really played the partner, and spots a better choice: the hero's other team-up when its
     * partner is on the team but the equipped one isn't enhanced.
     */
    private TeamUpAnalysis teamUp(PlayerLine player, List<PlayerLine> team) {
        if (player.teamUp() == null) {
            return null;
        }
        String partner = player.teamUp().partner();
        TeamUp equipped = teamUps.find(player.lastHero(), partner).orElse(null);
        boolean partnerOnTeam = onTeam(partner, player, team);

        String betterOption = null;
        if (!partnerOnTeam) {
            for (TeamUp other : teamUps.forHero(player.lastHero())) {
                if (!HeroRoleCatalog.sameHero(other.partner(), partner) && onTeam(other.partner(), player, team)) {
                    betterOption = other.name() + " (" + other.partner() + ")";
                }
            }
        }
        return new TeamUpAnalysis(
                equipped == null ? null : equipped.name(),
                partner,
                player.teamUp().enhanced(),
                partnerOnTeam,
                equipped == null ? null : equipped.baseEffect(),
                equipped == null ? null : equipped.enhancedEffect(),
                betterOption);
    }

    /** Teammates' equipped team-ups whose partner is this player's hero, as "Team-up (player)". */
    private List<String> teammateTeamUpsYouEnhance(PlayerLine player, List<PlayerLine> team) {
        List<String> result = new ArrayList<>();
        for (PlayerLine mate : team) {
            if (mate == player || mate.teamUp() == null
                    || !HeroRoleCatalog.sameHero(mate.teamUp().partner(), player.lastHero())) {
                continue;
            }
            teamUps.find(mate.lastHero(), mate.teamUp().partner()).ifPresent(t ->
                    result.add(t.name() + " (" + (mate.name() == null ? mate.lastHero() : mate.name()) + ")"));
        }
        return result;
    }

    /** True when a teammate (not the player) played this hero, as their last hero or in their hero list. */
    private static boolean onTeam(String hero, PlayerLine player, List<PlayerLine> team) {
        for (PlayerLine mate : team) {
            if (mate == player) {
                continue;
            }
            if (HeroRoleCatalog.sameHero(mate.lastHero(), hero)
                    || mate.heroes().stream().anyMatch(h -> HeroRoleCatalog.sameHero(h.hero(), hero))) {
                return true;
            }
        }
        return false;
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
     * Last-resort guess at a Deadpool player's role, used only when his row's icon couldn't be read.
     * Team compositions vary (2-2-2, 1-3-2, 1-2-3, 2-1-3...), so instead of assuming two of each role,
     * it compares him with the teammates whose icons show each role:
     * - Strategist if he healed at least as much as the lowest-healing teammate Strategist
     *   (or, with no teammate Strategists, more than every teammate).
     * - Otherwise Vanguard, by the same test on damage blocked against teammate Vanguards.
     * - Otherwise Duelist.
     * It can still be wrong: a damage-heavy Strategist Deadpool heals less than other Strategists.
     */
    static Role guessRoleFromStats(PlayerLine player, List<PlayerLine> team) {
        List<PlayerLine> mates = team.stream().filter(p -> p != player).toList();
        if (player.healing() > 0 && keepsUpWith(player.healing(), mates, Role.STRATEGIST, PlayerLine::healing)) {
            return Role.STRATEGIST;
        }
        if (player.blocked() > 0 && keepsUpWith(player.blocked(), mates, Role.VANGUARD, PlayerLine::blocked)) {
            return Role.VANGUARD;
        }
        return Role.DUELIST;
    }

    private static boolean keepsUpWith(int value, List<PlayerLine> mates, Role role, ToIntFunction<PlayerLine> stat) {
        List<PlayerLine> sameRole = mates.stream().filter(p -> p.role() == role).toList();
        if (sameRole.isEmpty()) {
            return mates.stream().allMatch(p -> value > stat.applyAsInt(p));
        }
        return value >= sameRole.stream().mapToInt(stat).min().getAsInt();
    }

    /**
     * A team's composition from its role icons, written Vanguards-Duelists-Strategists, e.g. "1-2-3".
     * Rows whose icon couldn't be read are added as "+N unknown".
     */
    static String composition(List<PlayerLine> team) {
        long v = team.stream().filter(p -> p.role() == Role.VANGUARD).count();
        long d = team.stream().filter(p -> p.role() == Role.DUELIST).count();
        long s = team.stream().filter(p -> p.role() == Role.STRATEGIST).count();
        long unknown = team.size() - v - d - s;
        return v + "-" + d + "-" + s + (unknown > 0 ? " (+" + unknown + " unknown)" : "");
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

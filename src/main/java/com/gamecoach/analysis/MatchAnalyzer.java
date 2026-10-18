package com.gamecoach.analysis;

import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchResult;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.PortraitStyle;
import com.gamecoach.model.ProficiencyShown;
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
                proficiency(player),
                teammateTeamUpsYouEnhance(player, team),
                player.medals(),
                player.multiKill(),
                stats,
                player.accuracyPercent(),
                accuracyRank(player, lobby),
                closeness(team, enemies, won(match, player.team()), roundsFor(match, player.team())));
    }

    /** Deaths within this ratio count as a close match. */
    static final double CLOSE_RATIO = 1.3;
    /** Deaths at or above this ratio count as one-sided. */
    static final double ONE_SIDED_RATIO = 2.0;

    /**
     * How close the match was, from the death totals. Deaths are the cleanest measure: one team's
     * final hits always equal the other team's deaths, and they don't depend on hero choice the way
     * damage and healing do. Damage and healing shares are passed along as extra context.
     */
    static MatchCloseness closeness(List<PlayerLine> team, List<PlayerLine> enemies, boolean won, int[] rounds) {
        int teamDeaths = team.stream().mapToInt(PlayerLine::deaths).sum();
        int enemyDeaths = enemies.stream().mapToInt(PlayerLine::deaths).sum();
        double ratio = round(Math.max(teamDeaths, enemyDeaths) / (double) Math.max(Math.min(teamDeaths, enemyDeaths), 1), 2);
        Closeness level = rounds != null ? fromRounds(rounds[0], rounds[1])
                : ratio <= CLOSE_RATIO ? Closeness.CLOSE
                : ratio >= ONE_SIDED_RATIO ? Closeness.ONE_SIDED
                : Closeness.CLEAR_EDGE;
        boolean ahead = teamDeaths == enemyDeaths ? won : teamDeaths < enemyDeaths;
        return new MatchCloseness(level,
                rounds == null ? null : rounds[0], rounds == null ? null : rounds[1],
                ahead, teamDeaths, enemyDeaths, ratio,
                share(team, enemies, PlayerLine::damage), share(team, enemies, PlayerLine::healing));
    }

    /**
     * The round score is the most direct sign of closeness, and the same rule works in every mode:
     * a sweep (the loser won no rounds, e.g. 2-0 or 3-0) is one-sided, a one-round margin
     * (2-1, 3-2) is close, and anything else (3-1) is a clear edge.
     */
    static Closeness fromRounds(int won, int lost) {
        if (Math.min(won, lost) == 0) {
            return Closeness.ONE_SIDED;
        }
        return Math.abs(won - lost) <= 1 ? Closeness.CLOSE : Closeness.CLEAR_EDGE;
    }

    /** The score from this team's side, as {rounds won, rounds lost}; null when unknown. */
    private static int[] roundsFor(MatchDetails match, String team) {
        if (match.score() == null) {
            return null;
        }
        boolean teamA = "A".equals(team);
        return teamA ? new int[] {match.score().teamA(), match.score().teamB()}
                : new int[] {match.score().teamB(), match.score().teamA()};
    }

    private static double share(List<PlayerLine> team, List<PlayerLine> enemies, ToIntFunction<PlayerLine> stat) {
        int ours = team.stream().mapToInt(stat).sum();
        int total = ours + enemies.stream().mapToInt(stat).sum();
        return total == 0 ? 50.0 : round(100.0 * ours / total, 1);
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
                betterOption);
    }

    /**
     * The minimum proficiency the row proves on its last hero: the higher of the portrait's and the
     * badge's levels. Null when nothing is shown, because players can leave both unequipped.
     */
    static ProficiencyAnalysis proficiency(PlayerLine player) {
        ProficiencyShown shown = player.proficiency();
        if (shown == null) {
            return null;
        }
        PortraitStyle portrait = shown.portrait() == null ? PortraitStyle.DEFAULT : shown.portrait();
        int level = portrait.minimumLevel();
        String rank = portrait.rankName();
        if (shown.badge() != null && shown.badge().minimumLevel() > level) {
            level = shown.badge().minimumLevel();
            rank = shown.badge().rankName();
        }
        if (rank == null) {
            return null;   // default portrait and no badge: nothing proven
        }
        return new ProficiencyAnalysis(player.lastHero(), portrait, shown.badge(), level, rank);
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

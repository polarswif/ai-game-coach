package com.gamecoach.datasource.overfast;

import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.datasource.CareerStatsSource;
import com.gamecoach.datasource.CareerStatsUnavailableException;
import com.gamecoach.datasource.overfast.OverFastApi.PlatformRanks;
import com.gamecoach.datasource.overfast.OverFastApi.PlayerSummary;
import com.gamecoach.datasource.overfast.OverFastApi.SearchResult;
import com.gamecoach.datasource.overfast.OverFastApi.Stats;
import com.gamecoach.datasource.overfast.OverFastApi.StatsSummary;
import com.gamecoach.datasource.overfast.OverFastApi.Values;
import com.gamecoach.model.CareerProfile;
import com.gamecoach.model.CareerProfile.CareerStats;
import com.gamecoach.model.CareerProfile.CompetitiveRanks;
import com.gamecoach.model.CareerProfile.HeroCareer;
import com.gamecoach.model.CareerProfile.Rank;
import com.gamecoach.model.CareerProfile.StatValues;
import com.gamecoach.model.OverwatchMode;
import com.gamecoach.model.OverwatchRole;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Overwatch careers from OverFast API: two calls per player (summary for ranks, stats summary for
 * the numbers), turned into our CareerProfile. OverFast errors become messages that tell the user
 * what to do: fix the BattleTag, make the profile public, or wait.
 */
@Component
public class OverFastCareerSource implements CareerStatsSource {

    private final OverFastClient client;
    private final OverwatchHeroCatalog heroes;

    public OverFastCareerSource(OverFastClient client, OverwatchHeroCatalog heroes) {
        this.client = client;
        this.heroes = heroes;
    }

    @Override
    public CareerProfile getCareerProfile(String battleTag, OverwatchMode mode, String platform) {
        String id = playerId(battleTag);
        try {
            PlayerSummary summary = client.summary(id);
            StatsSummary stats = client.statsSummary(id, mode.key(), platform);
            if (stats == null || stats.general() == null) {
                throw new NoSuchElementException(noStatsMessage(id, mode, platform));
            }
            String name = summary != null && summary.username() != null ? summary.username() : id;
            return new CareerProfile(id, name, mode, platform, ranks(summary, platform), toStats(stats.general()),
                    roles(stats.roles()), heroCareers(stats.heroes()));
        } catch (OverFastException e) {
            throw translate(e, id);
        }
    }

    @Override
    public List<PlayerMatch> searchPlayers(String name) {
        try {
            SearchResult result = client.search(name.strip());
            if (result == null || result.results() == null) {
                return List.of();
            }
            return result.results().stream()
                    .map(p -> new PlayerMatch(p.playerId(), p.name(), p.isPublic()))
                    .toList();
        } catch (OverFastException e) {
            throw translate(e, name);
        }
    }

    /** "Name#1234" → "Name-1234". OverFast uses a dash because "#" can't appear in a URL path. */
    static String playerId(String battleTag) {
        if (battleTag == null || battleTag.isBlank()) {
            throw new IllegalArgumentException("A BattleTag is required, like Name#1234.");
        }
        return battleTag.strip().replace('#', '-');
    }

    static String noStatsMessage(String id, OverwatchMode mode, String platform) {
        String modeName = mode == OverwatchMode.QUICKPLAY ? "quick play" : "competitive";
        return id + " has no " + modeName + " stats" + (platform == null ? "" : " on " + platform) + ". Either "
                + "the career profile is private (set Career Profile Visibility to Public in Overwatch's "
                + "Social options; Blizzard can take a while to update) or they haven't played "
                + modeName + (mode == OverwatchMode.COMPETITIVE ? "; try mode=quickplay." : ".");
    }

    /** Turns an OverFast failure into what the controller layer maps to 404 or 503. */
    static RuntimeException translate(OverFastException e, String id) {
        Integer wait = e.retryAfterSeconds();
        return switch (e.status()) {
            case 404 -> new NoSuchElementException("No Overwatch profile found for " + id + ". Check the "
                    + "spelling, the capital letters and the number after the #."
                    + (wait == null ? "" : " OverFast will look it up again in about " + minutes(wait) + "."));
            case 429 -> new CareerStatsUnavailableException("OverFast API is limiting requests right now; try again "
                    + (wait == null ? "in a few seconds." : "in " + wait + " seconds."), e);
            case 0 -> new CareerStatsUnavailableException(e.getMessage(), e);
            default -> new CareerStatsUnavailableException("OverFast API returned an error (HTTP " + e.status()
                    + "): " + e.getMessage() + ". Blizzard's profile pages may be slow or down; try again later.", e);
        };
    }

    private static String minutes(int seconds) {
        int m = Math.max(1, Math.round(seconds / 60f));
        return m + (m == 1 ? " minute" : " minutes");
    }

    /**
     * Ranks for the requested platform; with no platform, PC if it has any rank, otherwise console.
     * Null when there are none at all.
     */
    static CompetitiveRanks ranks(PlayerSummary summary, String platform) {
        if (summary == null || summary.competitive() == null) {
            return null;
        }
        PlatformRanks pc = summary.competitive().pc();
        PlatformRanks console = summary.competitive().console();
        PlatformRanks chosen;
        if ("console".equals(platform)) {
            chosen = console;
        } else if ("pc".equals(platform)) {
            chosen = pc;
        } else {
            chosen = hasAnyRank(pc) ? pc : console;
        }
        if (!hasAnyRank(chosen)) {
            return null;
        }
        Map<OverwatchRole, Rank> byRole = new EnumMap<>(OverwatchRole.class);
        putRank(byRole, OverwatchRole.TANK, chosen.tank());
        putRank(byRole, OverwatchRole.DAMAGE, chosen.damage());
        putRank(byRole, OverwatchRole.SUPPORT, chosen.support());
        return new CompetitiveRanks(chosen.season(), byRole, rank(chosen.open()));
    }

    private static boolean hasAnyRank(PlatformRanks r) {
        return r != null && (rank(r.tank()) != null || rank(r.damage()) != null
                || rank(r.support()) != null || rank(r.open()) != null);
    }

    private static void putRank(Map<OverwatchRole, Rank> map, OverwatchRole role, OverFastApi.Rank r) {
        Rank rank = rank(r);
        if (rank != null) {
            map.put(role, rank);
        }
    }

    private static Rank rank(OverFastApi.Rank r) {
        if (r == null || r.division() == null || r.division().isBlank() || r.tier() == null) {
            return null;
        }
        return new Rank(r.division().strip().toLowerCase(Locale.ROOT), r.tier());
    }

    private static Map<OverwatchRole, CareerStats> roles(Map<String, Stats> roles) {
        Map<OverwatchRole, CareerStats> result = new EnumMap<>(OverwatchRole.class);
        if (roles != null) {
            roles.forEach((key, stats) -> {
                OverwatchRole role = OverwatchRole.fromKey(key);
                if (role != null && stats != null) {
                    result.put(role, toStats(stats));
                }
            });
        }
        return result;
    }

    /** One entry per hero with stats, most played first. */
    private List<HeroCareer> heroCareers(Map<String, Stats> byHero) {
        List<HeroCareer> list = new ArrayList<>();
        if (byHero != null) {
            byHero.forEach((key, stats) -> {
                if (stats != null) {
                    list.add(new HeroCareer(key, heroes.nameOf(key), heroes.roleOf(key), toStats(stats)));
                }
            });
        }
        list.sort(Comparator.comparing((HeroCareer h) -> h.stats().timePlayed()).reversed()
                .thenComparing(HeroCareer::heroKey));
        return list;
    }

    static CareerStats toStats(Stats s) {
        return new CareerStats(orZero(s.gamesPlayed()), orZero(s.gamesWon()), orZero(s.gamesLost()),
                Duration.ofSeconds(s.timePlayed() == null ? 0 : s.timePlayed()),
                orZero(s.winrate()), orZero(s.kda()), values(s.total()), values(s.average()));
    }

    private static StatValues values(Values v) {
        if (v == null) {
            return new StatValues(0, 0, 0, 0, 0);
        }
        return new StatValues(orZero(v.eliminations()), orZero(v.assists()), orZero(v.deaths()),
                orZero(v.damage()), orZero(v.healing()));
    }

    private static int orZero(Integer n) {
        return n == null ? 0 : n;
    }

    private static double orZero(Double n) {
        return n == null ? 0 : n;
    }
}

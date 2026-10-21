package com.gamecoach.analysis;

import com.gamecoach.analysis.CareerAnalysis.Comparison;
import com.gamecoach.analysis.CareerAnalysis.HeroSummary;
import com.gamecoach.analysis.CareerAnalysis.RoleSummary;
import com.gamecoach.model.CareerProfile;
import com.gamecoach.model.CareerProfile.CareerStats;
import com.gamecoach.model.CareerProfile.HeroCareer;
import com.gamecoach.model.CareerProfile.StatValues;
import com.gamecoach.model.OverwatchRole;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Turns an Overwatch CareerProfile into a CareerAnalysis. Pure arithmetic, no I/O.
 *
 * The key step is "this hero versus your other heroes in the same role". OverFast gives totals for
 * each role and each hero, so subtracting the hero's totals from the role's leaves exactly the
 * other heroes combined; dividing by their time gives fair per-10-minute rates.
 */
@Component
public class CareerAnalyzer {

    /** Under this much play time a hero is a small sample. */
    static final Duration MIN_TIME = Duration.ofHours(1);
    /** Under this many games a hero is a small sample. */
    static final int MIN_GAMES = 5;

    public CareerAnalysis analyze(CareerProfile profile) {
        Duration total = profile.overall().timePlayed();

        List<RoleSummary> roles = new ArrayList<>();
        profile.roles().forEach((role, stats) -> roles.add(summary(role, stats, total)));
        roles.sort(Comparator.comparing(RoleSummary::timePlayed).reversed());
        OverwatchRole mainRole = roles.isEmpty() || roles.get(0).timePlayed().isZero() ? null : roles.get(0).role();

        List<HeroSummary> heroes = profile.heroes().stream()
                .map(h -> heroSummary(h, profile.roles(), total))
                .sorted(Comparator.comparing(HeroSummary::timePlayed).reversed())
                .toList();

        return new CareerAnalysis(profile.playerId(), profile.name(), profile.mode(), profile.platform(),
                profile.ranks(), summary(null, profile.overall(), total), mainRole, roles, heroes);
    }

    private static RoleSummary summary(OverwatchRole role, CareerStats s, Duration total) {
        return new RoleSummary(role, s.timePlayed(), share(s.timePlayed(), total), s.gamesPlayed(),
                s.winRatePercent(), s.kda(), s.per10());
    }

    private static HeroSummary heroSummary(HeroCareer h, Map<OverwatchRole, CareerStats> roles, Duration total) {
        CareerStats s = h.stats();
        boolean small = s.timePlayed().compareTo(MIN_TIME) < 0 || s.gamesPlayed() < MIN_GAMES;
        CareerStats roleStats = h.role() == null ? null : roles.get(h.role());
        return new HeroSummary(h.heroKey(), h.hero(), h.role(), s.timePlayed(), share(s.timePlayed(), total),
                s.gamesPlayed(), s.winRatePercent(), s.kda(), s.per10(), small,
                roleStats == null ? null : compare(s, roleStats));
    }

    /** The hero against the rest of its role: role totals minus the hero's own. */
    static Comparison compare(CareerStats hero, CareerStats role) {
        Duration otherTime = role.timePlayed().minus(hero.timePlayed());
        if (otherTime.compareTo(MIN_TIME) < 0) {
            return null;
        }
        int otherGames = role.gamesPlayed() - hero.gamesPlayed();
        int otherWon = role.gamesWon() - hero.gamesWon();
        double otherWinRate = otherGames <= 0 ? 0 : round1(100.0 * otherWon / otherGames);
        StatValues otherTotals = minus(role.totals(), hero.totals());
        StatValues otherPer10 = per10(otherTotals, otherTime);
        StatValues diffs = new StatValues(
                percentDiff(hero.per10().eliminations(), otherPer10.eliminations()),
                percentDiff(hero.per10().assists(), otherPer10.assists()),
                percentDiff(hero.per10().deaths(), otherPer10.deaths()),
                percentDiff(hero.per10().damage(), otherPer10.damage()),
                percentDiff(hero.per10().healing(), otherPer10.healing()));
        return new Comparison(otherTime, otherGames, otherWinRate, otherPer10,
                round1(hero.winRatePercent() - otherWinRate), diffs);
    }

    private static StatValues minus(StatValues a, StatValues b) {
        return new StatValues(Math.max(0, a.eliminations() - b.eliminations()), Math.max(0, a.assists() - b.assists()),
                Math.max(0, a.deaths() - b.deaths()), Math.max(0, a.damage() - b.damage()),
                Math.max(0, a.healing() - b.healing()));
    }

    /** Totals over some play time → per 10 minutes, rounded to 2 decimals. */
    static StatValues per10(StatValues totals, Duration time) {
        double tens = time.toSeconds() / 600.0;
        if (tens <= 0) {
            return new StatValues(0, 0, 0, 0, 0);
        }
        return new StatValues(round2(totals.eliminations() / tens), round2(totals.assists() / tens),
                round2(totals.deaths() / tens), round2(totals.damage() / tens), round2(totals.healing() / tens));
    }

    private static double percentDiff(double value, double other) {
        return other == 0 ? 0 : round1(100.0 * (value - other) / other);
    }

    private static double share(Duration part, Duration total) {
        return total.isZero() ? 0 : round1(100.0 * part.toSeconds() / total.toSeconds());
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}

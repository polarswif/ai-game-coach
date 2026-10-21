package com.gamecoach;

import com.gamecoach.model.CareerProfile.CareerStats;
import com.gamecoach.model.CareerProfile.StatValues;

import java.time.Duration;

/** Small builders for Overwatch test data, shared by several test classes. */
public final class OverwatchFixtures {

    private OverwatchFixtures() {
    }

    /**
     * Career stats from totals; the per-10 values are worked out from the totals and the time,
     * the same way Blizzard does, so tests stay consistent.
     */
    public static CareerStats stats(int games, int won, Duration time, double kda,
                                    double elims, double assists, double deaths, double damage, double healing) {
        double tens = time.toSeconds() / 600.0;
        StatValues totals = new StatValues(elims, assists, deaths, damage, healing);
        StatValues per10 = new StatValues(r2(elims / tens), r2(assists / tens), r2(deaths / tens),
                r2(damage / tens), r2(healing / tens));
        double winRate = games == 0 ? 0 : Math.round(1000.0 * won / games) / 10.0;
        return new CareerStats(games, won, games - won, time, winRate, kda, totals, per10);
    }

    private static double r2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}

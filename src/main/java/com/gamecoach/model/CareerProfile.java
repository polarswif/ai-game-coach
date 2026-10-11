package com.gamecoach.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Overwatch career data: no individual matches, only aggregates. */
public record CareerProfile(
        String battleTag,
        Map<String, String> ranksByRole,
        List<HeroStats> heroes) {

    public record HeroStats(
            String hero,
            String role,
            int gamesPlayed,
            double winRatePercent,
            double kda,
            Duration timePlayed) {
    }
}

package com.gamecoach.analysis;

import com.gamecoach.model.CareerProfile.CompetitiveRanks;
import com.gamecoach.model.CareerProfile.StatValues;
import com.gamecoach.model.OverwatchMode;
import com.gamecoach.model.OverwatchRole;

import java.time.Duration;
import java.util.List;

/**
 * The computed picture of an Overwatch career: where the time goes, and how each hero compares with
 * the same player's other heroes in that role. With no individual matches and no other players,
 * the player's own other heroes are the fairest benchmark: same person, same rank, same habits.
 *
 * @param mainRole the role with the most time played; null if no role has any
 * @param roles    most played first
 * @param heroes   most played first
 */
public record CareerAnalysis(
        String playerId,
        String name,
        OverwatchMode mode,
        String platform,
        CompetitiveRanks ranks,
        RoleSummary overall,
        OverwatchRole mainRole,
        List<RoleSummary> roles,
        List<HeroSummary> heroes) {

    /**
     * Totals for the account (role null) or one role.
     *
     * @param timeSharePercent share of the player's total time in this mode
     */
    public record RoleSummary(
            OverwatchRole role,
            Duration timePlayed,
            double timeSharePercent,
            int gamesPlayed,
            double winRatePercent,
            double kda,
            StatValues per10) {
    }

    /**
     * One hero.
     *
     * @param role             null for a hero newer than the hero table
     * @param smallSample      under an hour or under 5 games: too little to judge, like one match in Rivals
     * @param vsOtherRoleHeroes the player's other heroes in this role combined; null when they have
     *                         less than an hour on them, so there's nothing fair to compare with
     */
    public record HeroSummary(
            String heroKey,
            String hero,
            OverwatchRole role,
            Duration timePlayed,
            double timeSharePercent,
            int gamesPlayed,
            double winRatePercent,
            double kda,
            StatValues per10,
            boolean smallSample,
            Comparison vsOtherRoleHeroes) {
    }

    /**
     * The player's other heroes in the same role, combined, and the hero's differences from them.
     * A positive deathsPer10Diff means this hero dies more; positive damage/healing/eliminations
     * diffs mean this hero does more.
     *
     * @param percentDiffs per-10 differences as a percent of the other heroes' value (0 when theirs is 0)
     */
    public record Comparison(
            Duration otherTimePlayed,
            int otherGamesPlayed,
            double otherWinRatePercent,
            StatValues otherPer10,
            double winRateDiffPoints,
            StatValues percentDiffs) {
    }
}

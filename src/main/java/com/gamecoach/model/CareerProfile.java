package com.gamecoach.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * An Overwatch player's career, from their public Blizzard profile. Blizzard publishes no
 * individual matches, only totals, so everything here is a sum or an average over many games.
 *
 * @param playerId BattleTag in the form OverFast uses, with "#" replaced by "-" (e.g. "TeKrop-2217")
 * @param name     the display name
 * @param mode     quick play or competitive: the stats below cover only that mode
 * @param platform "pc", "console", or null when both are combined
 * @param ranks    competitive ranks from the last season played; null when the player has none
 *                 (never played competitive, or a private profile). Shown for both modes.
 * @param overall  all heroes combined
 * @param roles    all heroes of each role combined; a role the player never played is missing
 * @param heroes   one entry per hero played, most played first
 */
public record CareerProfile(
        String playerId,
        String name,
        OverwatchMode mode,
        String platform,
        CompetitiveRanks ranks,
        CareerStats overall,
        Map<OverwatchRole, CareerStats> roles,
        List<HeroCareer> heroes) {

    /** One hero's career, with the hero's display name and role from the Overwatch hero table. */
    public record HeroCareer(String heroKey, String hero, OverwatchRole role, CareerStats stats) {
    }

    /**
     * Career numbers for a hero, a role or the whole account.
     *
     * @param winRatePercent 0 to 100
     * @param kda            (eliminations + assists) / deaths, as Blizzard computes it
     * @param totals         sums over all games; used to combine and subtract groups of heroes exactly
     * @param per10          averages per 10 minutes, which make heroes with different play time comparable
     */
    public record CareerStats(
            int gamesPlayed,
            int gamesWon,
            int gamesLost,
            Duration timePlayed,
            double winRatePercent,
            double kda,
            StatValues totals,
            StatValues per10) {
    }

    /** The five numbers Blizzard tracks for every hero. */
    public record StatValues(double eliminations, double assists, double deaths, double damage, double healing) {
    }

    /**
     * Competitive ranks in one season. A rank is missing for a role the player hasn't placed in.
     *
     * @param season    the season number; null when Blizzard didn't say
     * @param byRole    role queue ranks
     * @param openQueue the open queue rank, or null
     */
    public record CompetitiveRanks(Integer season, Map<OverwatchRole, Rank> byRole, Rank openQueue) {
    }

    /** A division and tier, such as Gold 3. Tier 1 is the highest within a division. */
    public record Rank(String division, int tier) {
        @Override
        public String toString() {
            return division.isEmpty() ? "Unranked"
                    : Character.toUpperCase(division.charAt(0)) + division.substring(1) + " " + tier;
        }
    }
}

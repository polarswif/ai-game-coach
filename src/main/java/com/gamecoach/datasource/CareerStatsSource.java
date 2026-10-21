package com.gamecoach.datasource;

import com.gamecoach.model.CareerProfile;
import com.gamecoach.model.OverwatchMode;

import java.time.Instant;
import java.util.List;

/**
 * For games that only publish career totals (Overwatch). Implementations wrap one provider; today
 * that's OverFast API.
 *
 * Failures are exceptions rather than an empty result, because the reason matters to the user:
 * an unknown BattleTag, a private profile and a provider outage each need a different answer.
 * Unknown or private players throw {@link java.util.NoSuchElementException} (404); an unreachable
 * or rate-limited provider throws {@link CareerStatsUnavailableException} (503).
 */
public interface CareerStatsSource {

    /**
     * A player's career for one mode.
     *
     * @param battleTag "Name#1234" or "Name-1234"; letter case matters
     * @param platform  "pc" or "console", or null to combine both
     */
    CareerProfile getCareerProfile(String battleTag, OverwatchMode mode, String platform);

    /** Players whose name or BattleTag matches, to find the exact BattleTag to ask for. */
    List<PlayerMatch> searchPlayers(String name);

    /**
     * One search result.
     *
     * Blizzard no longer shows the number after the # in search, so several players can share a
     * name; title, avatar and last update are what tell them apart.
     *
     * @param playerId      what to put in the profile and analysis URLs
     * @param title         the in-game title under the name; null when none
     * @param avatar        URL of the player icon; null when unknown
     * @param lastUpdated   when Blizzard last updated the profile; null when unknown
     * @param publicProfile null when the provider doesn't know yet
     */
    record PlayerMatch(String playerId, String name, String title, String avatar, Instant lastUpdated,
                       Boolean publicProfile) {
    }
}

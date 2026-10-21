package com.gamecoach.datasource;

import com.gamecoach.model.CareerProfile;
import com.gamecoach.model.OverwatchMode;

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
     * @param publicProfile null when the provider doesn't know yet
     */
    record PlayerMatch(String playerId, String name, Boolean publicProfile) {
    }
}

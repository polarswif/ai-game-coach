package com.gamecoach.datasource.overfast;

import com.gamecoach.datasource.overfast.OverFastApi.PlayerSummary;
import com.gamecoach.datasource.overfast.OverFastApi.SearchResult;
import com.gamecoach.datasource.overfast.OverFastApi.StatsSummary;

/**
 * The OverFast calls this app makes. An interface so tests can hand OverFastCareerSource canned
 * replies without any HTTP; RestClientOverFastClient is the real one. Every method throws
 * OverFastException on failure.
 */
public interface OverFastClient {

    /** @param playerId "Name-1234" (with a dash), as OverFast expects */
    PlayerSummary summary(String playerId);

    /**
     * @param gamemode "quickplay" or "competitive"
     * @param platform "pc", "console", or null to combine both
     */
    StatsSummary statsSummary(String playerId, String gamemode, String platform);

    SearchResult search(String name);
}

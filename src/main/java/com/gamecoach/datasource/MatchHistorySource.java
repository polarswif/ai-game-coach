package com.gamecoach.datasource;

import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;

import java.util.List;

/**
 * For games that expose per-match data (Marvel Rivals).
 * Implementations wrap one provider, e.g. a community stats API.
 */
public interface MatchHistorySource {

    /** Most recent matches for a player, newest first (up to 25). Empty if the player is not found. */
    List<MatchSummary> getRecentMatches(String username);

    /** Full scoreboard for one match. Throws NoSuchElementException if the match is unknown. */
    MatchDetails getMatchDetails(String matchId);
}

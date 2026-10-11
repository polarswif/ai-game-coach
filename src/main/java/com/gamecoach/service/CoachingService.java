package com.gamecoach.service;

import com.gamecoach.analysis.PlayerAnalysis;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;

/**
 * Turns a match and one player's analysis into coaching feedback.
 * Today: a fake implementation. Build step 3 adds one that calls the Claude API.
 */
public interface CoachingService {

    /**
     * @param match    the full scoreboard, for context
     * @param analysis the computed comparisons for the player being coached
     */
    Feedback coach(MatchDetails match, PlayerAnalysis analysis);
}

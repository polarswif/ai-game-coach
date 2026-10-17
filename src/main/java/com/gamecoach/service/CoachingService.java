package com.gamecoach.service;

import com.gamecoach.analysis.PlayerAnalysis;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;

/**
 * Turns a match and one player's analysis into coaching feedback.
 * FakeCoachingService gives canned feedback; ClaudeCoachingService asks Claude. The
 * gamecoach.coach.provider setting picks which one Spring creates.
 */
public interface CoachingService {

    /**
     * @param match    the full scoreboard, for context
     * @param analysis the computed comparisons for the player being coached
     */
    Feedback coach(MatchDetails match, PlayerAnalysis analysis);
}

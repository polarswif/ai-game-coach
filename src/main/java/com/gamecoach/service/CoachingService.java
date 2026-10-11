package com.gamecoach.service;

import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;

/**
 * Turns a match scoreboard into coaching feedback for one player.
 * Today: a fake implementation. Later: one that calls the Claude API.
 */
public interface CoachingService {

    /** @param player the row on the scoreboard that belongs to the person being coached */
    Feedback coach(MatchDetails match, PlayerLine player);
}

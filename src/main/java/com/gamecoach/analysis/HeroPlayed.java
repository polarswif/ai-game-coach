package com.gamecoach.analysis;

import com.gamecoach.model.Role;

/**
 * A hero the player used, with its role worked out by MatchAnalyzer.
 *
 * @param lastPlayed true for the hero shown on the scoreboard row (the last one used)
 * @param kills      null unless per-hero numbers were captured (same for deaths and assists)
 */
public record HeroPlayed(
        String hero,
        Role role,
        boolean lastPlayed,
        Integer kills,
        Integer deaths,
        Integer assists) {
}

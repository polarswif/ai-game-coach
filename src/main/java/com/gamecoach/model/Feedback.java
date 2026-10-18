package com.gamecoach.model;

import java.util.List;

/**
 * Coaching result for one player in one match. Lists can be any length.
 *
 * @param matchContext one or two sentences on how close the match was and how much weight the
 *                     coaching deserves (a stomp says less about the player than a hard-fought match);
 *                     may be null
 */
public record Feedback(
        List<String> strengths,
        List<String> weaknesses,
        List<String> advice,
        String matchContext) {
}

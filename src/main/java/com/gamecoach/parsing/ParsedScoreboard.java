package com.gamecoach.parsing;

import com.gamecoach.model.MatchDetails;

import java.util.List;

/**
 * A scoreboard read from a screenshot, plus everything that looked wrong or uncertain.
 * The user reviews the match (and fixes rows with PUT) before asking for coaching, so warnings
 * point at exactly what to check instead of hiding a misread.
 *
 * @param match    the match as read, with a generated matchId used by the other endpoints
 * @param warnings things to check: the reader's own doubts and failed consistency checks; empty when all is well
 */
public record ParsedScoreboard(MatchDetails match, List<String> warnings) {
}

package com.gamecoach.parsing;

import com.gamecoach.model.MatchDetails;

import java.util.List;

/**
 * A scoreboard read from a screenshot, plus everything that looked wrong or uncertain.
 * The user reviews the match (and fixes rows with PUT) before asking for coaching, so warnings
 * point at exactly what to check instead of hiding a misread.
 *
 * @param match      the match as read, with a generated matchId used by the other endpoints
 * @param warnings   things to check: the reader's own doubts and failed consistency checks; empty when all is well
 * @param heroDoubts rows whose portrait the reader wasn't sure of, with likely alternatives; empty when none
 */
public record ParsedScoreboard(MatchDetails match, List<String> warnings, List<HeroDoubt> heroDoubts) {

    public ParsedScoreboard {
        heroDoubts = heroDoubts == null ? List.of() : List.copyOf(heroDoubts);
    }

    /** No hero doubts: used after the player's own corrections, which are trusted. */
    public ParsedScoreboard(MatchDetails match, List<String> warnings) {
        this(match, warnings, List.of());
    }
}

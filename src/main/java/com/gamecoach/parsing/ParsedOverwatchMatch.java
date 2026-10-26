package com.gamecoach.parsing;

import com.gamecoach.model.OverwatchMatch;

import java.util.List;

/**
 * An Overwatch match read from screenshots, plus everything to check before coaching.
 *
 * @param warnings   the reader's own doubts and failed consistency checks; empty when all is well
 * @param heroDoubts rows whose portrait the reader wasn't sure of, with likely alternatives; empty when none
 */
public record ParsedOverwatchMatch(OverwatchMatch match, List<String> warnings, List<HeroDoubt> heroDoubts) {

    public ParsedOverwatchMatch {
        heroDoubts = heroDoubts == null ? List.of() : List.copyOf(heroDoubts);
    }

    /** No hero doubts: used after the player's own corrections, which are trusted. */
    public ParsedOverwatchMatch(OverwatchMatch match, List<String> warnings) {
        this(match, warnings, List.of());
    }
}

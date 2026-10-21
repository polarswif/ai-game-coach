package com.gamecoach.parsing;

import com.gamecoach.model.OverwatchMatch;

import java.util.List;

/**
 * An Overwatch match read from screenshots, plus everything to check before coaching.
 *
 * @param warnings the reader's own doubts and failed consistency checks; empty when all is well
 */
public record ParsedOverwatchMatch(OverwatchMatch match, List<String> warnings) {
}

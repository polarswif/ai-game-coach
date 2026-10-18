package com.gamecoach.parsing;

import com.gamecoach.model.RoundScore;

/** Reads an end-of-match scoreboard screenshot into match data. */
public interface ScoreboardParser {

    /**
     * @param screenshot the uploaded image
     * @param matchId    the id to give the match
     * @param score      the round score typed in by the uploader (the scoreboard doesn't show it); may be null
     * @throws ScoreboardUnreadableException when the image isn't a readable scoreboard
     */
    ParsedScoreboard parse(Screenshot screenshot, String matchId, RoundScore score);
}

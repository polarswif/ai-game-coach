package com.gamecoach.parsing;

/** Reads Overwatch match report screenshots into match data. */
public interface OverwatchMatchReader {

    /**
     * @throws ScoreboardUnreadableException when no scoreboard rows can be read
     */
    ParsedOverwatchMatch read(OverwatchScreens screens, String matchId);
}

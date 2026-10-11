package com.gamecoach.model;

/**
 * Marvel Rivals hero roles, shown as an icon on each scoreboard row.
 * UNKNOWN is the safe answer when the role can't be known: the icon couldn't be read and
 * the hero isn't in the role table (newer than the table, or a misread name).
 */
public enum Role {
    VANGUARD,
    DUELIST,
    STRATEGIST,
    UNKNOWN
}

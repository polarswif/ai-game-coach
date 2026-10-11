package com.gamecoach.model;

/**
 * Marvel Rivals hero roles. UNKNOWN is the safe answer when the role can't be known:
 * Deadpool (he switches roles each match), a hero newer than the role table,
 * or a hero name the screenshot reader got wrong.
 */
public enum Role {
    VANGUARD,
    DUELIST,
    STRATEGIST,
    UNKNOWN
}

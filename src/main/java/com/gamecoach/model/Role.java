package com.gamecoach.model;

/**
 * Marvel Rivals hero roles. UNKNOWN is the safe answer when the role can't be known:
 * a hero newer than the role table, a hero name the screenshot reader got wrong,
 * or an unlabeled "Deadpool" whose player also played other heroes.
 */
public enum Role {
    VANGUARD,
    DUELIST,
    STRATEGIST,
    UNKNOWN
}

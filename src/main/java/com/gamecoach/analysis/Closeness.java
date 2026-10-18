package com.gamecoach.analysis;

/**
 * How close a match was, judged from how many times each team died. A team's final hits roughly
 * equal the other team's deaths, so the death counts show who won the fights.
 */
public enum Closeness {
    /** Deaths within 30% of each other: a hard-fought match, the most useful kind to coach. */
    CLOSE,
    /** One team died noticeably more, but the match was still contested. */
    CLEAR_EDGE,
    /** One team died at least twice as often: likely a stomp, so the stats say less about the player. */
    ONE_SIDED
}

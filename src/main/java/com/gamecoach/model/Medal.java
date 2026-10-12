package com.gamecoach.model;

/**
 * The per-match medals in the scoreboard's "Medals" column. Each one goes to the single best player
 * in the whole lobby (either team) for one stat, so the screenshot reader can check them against the
 * numbers. The column can also show a red number, the player's best multi-kill (3 = Triple,
 * 4 = Quad, 5 = Penta, 6 = Hexa: that many KOs in quick succession); that's PlayerLine.multiKill.
 * Proficiency Badges are NOT in this column; they sit on the bottom-left corner of the portrait.
 */
public enum Medal {
    /** Crossed swords: most KOs. */
    MOST_KOS,
    /** Flame: most damage. */
    MOST_DAMAGE,
    /** Shield: most damage blocked. */
    MOST_BLOCKED,
    /** Green plus: most healing. */
    MOST_HEALING,
    /** Fist: most assists. */
    MOST_ASSISTS
}

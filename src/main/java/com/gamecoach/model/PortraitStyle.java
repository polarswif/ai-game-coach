package com.gamecoach.model;

/**
 * Which version of the hero portrait a row shows. Lord and animated Lord portraits are proficiency
 * rewards, so they set a minimum level on that hero. Players can equip the default portrait at any
 * level, so DEFAULT doesn't mean a low level.
 */
public enum PortraitStyle {
    /** The normal portrait. Says nothing about proficiency. */
    DEFAULT(0, null),
    /** The Lord portrait (alternate art), unlocked at Lord, level 20. */
    LORD(20, "Lord"),
    /** The animated Lord portrait, unlocked at Champion, level 50. It has a moving effect on top. */
    ANIMATED_LORD(50, "Champion");

    private final int minimumLevel;
    private final String rankName;

    PortraitStyle(int minimumLevel, String rankName) {
        this.minimumLevel = minimumLevel;
        this.rankName = rankName;
    }

    public int minimumLevel() {
        return minimumLevel;
    }

    /** Null for DEFAULT. */
    public String rankName() {
        return rankName;
    }
}

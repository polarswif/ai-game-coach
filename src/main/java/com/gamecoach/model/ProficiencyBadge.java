package com.gamecoach.model;

/**
 * The Proficiency Badge a player can show next to their row on the scoreboard. Badges are earned on
 * one hero's proficiency track (Season 6 and later), so a badge means the player has at least that
 * level on the hero they're playing. Players choose whether to equip one, so no badge tells us nothing.
 * The badge is the small emblem on the bottom-left corner of the portrait (not the "Medals" column).
 * Each tier is a crest with a star in the middle, told apart by color and wings:
 * Count is cyan/blue with no wings, Warrior lavender with small wings, Guardian bright pink with
 * large wings, Champion red and gold with flames.
 */
public enum ProficiencyBadge {
    COUNT(25, "Count"),
    WARRIOR(35, "Warrior"),
    GUARDIAN(45, "Guardian"),
    CHAMPION(55, "Champion"),
    /**
     * A badge is there but its tier can't be told apart (the emblems are tiny, and a blurry Warrior and
     * Guardian look alike). Any badge proves at least Count, so it counts as level 25. When unsure
     * between two tiers, the reader should record the lower one, which is still true.
     */
    UNREADABLE(25, "Count");

    private final int minimumLevel;
    private final String rankName;

    ProficiencyBadge(int minimumLevel, String rankName) {
        this.minimumLevel = minimumLevel;
        this.rankName = rankName;
    }

    /** The proficiency level the badge is awarded at. */
    public int minimumLevel() {
        return minimumLevel;
    }

    /** The proficiency rank that level belongs to. */
    public String rankName() {
        return rankName;
    }
}

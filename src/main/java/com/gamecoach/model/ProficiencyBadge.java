package com.gamecoach.model;

/**
 * The Proficiency Badge a player can show next to their row on the scoreboard. Badges are earned on
 * one hero's proficiency track (Season 6 and later), so a badge means the player has at least that
 * level on the hero they're playing. Players choose whether to equip one, so no badge tells us nothing.
 */
public enum ProficiencyBadge {
    COUNT(25, "Count"),
    WARRIOR(35, "Warrior"),
    GUARDIAN(45, "Guardian"),
    CHAMPION(55, "Champion");

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

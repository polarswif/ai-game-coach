package com.gamecoach.model;

import java.util.Locale;

/**
 * Overwatch's three roles. Separate from {@link Role}, which is Marvel Rivals' Vanguard, Duelist and
 * Strategist: the games use different names, and keeping two enums stops one game's role from
 * being used for the other by mistake.
 */
public enum OverwatchRole {
    TANK, DAMAGE, SUPPORT;

    /** "tank", "Damage", " SUPPORT " → the role; null for anything else. */
    public static OverwatchRole fromKey(String key) {
        if (key == null) {
            return null;
        }
        return switch (key.strip().toLowerCase(Locale.ROOT)) {
            case "tank" -> TANK;
            case "damage" -> DAMAGE;
            case "support" -> SUPPORT;
            default -> null;
        };
    }
}

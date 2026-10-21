package com.gamecoach.model;

import java.util.Locale;

/** Which games the career stats cover. Blizzard keeps quick play and competitive separate. */
public enum OverwatchMode {
    QUICKPLAY, COMPETITIVE;

    /** The value OverFast expects in its "gamemode" parameter. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** "quickplay", "qp", "competitive", "comp" (any case) → the mode; null for anything else. */
    public static OverwatchMode fromKey(String key) {
        if (key == null) {
            return null;
        }
        return switch (key.strip().toLowerCase(Locale.ROOT)) {
            case "quickplay", "qp", "quick play" -> QUICKPLAY;
            case "competitive", "comp", "ranked" -> COMPETITIVE;
            default -> null;
        };
    }
}

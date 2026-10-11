package com.gamecoach.model;

import java.util.List;

/** One player's row on a match scoreboard. */
public record PlayerLine(
        String name,
        List<String> heroes,   // every hero shown on the scoreboard; players can swap mid-match
        String team,           // "A" or "B"
        String rank,           // e.g. "G2"
        int rankScore,         // e.g. 4654
        int kills,
        int deaths,
        int assists,
        int damage,
        int blocked,
        int healing,
        double accuracyPercent) {
}

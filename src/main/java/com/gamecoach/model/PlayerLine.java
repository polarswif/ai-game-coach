package com.gamecoach.model;

import java.util.List;

/** One player's row on a match scoreboard. */
public record PlayerLine(
        String name,
        List<HeroStats> heroes,   // every hero played, in scoreboard order; players can swap mid-match
        String team,              // "A" or "B"
        String rank,              // e.g. "G2"
        int rankScore,            // e.g. 4654
        int kills,
        int deaths,
        int assists,
        int damage,
        int blocked,
        int healing,
        Double accuracyPercent) { // null when the scoreboard shows no accuracy ("—")
}

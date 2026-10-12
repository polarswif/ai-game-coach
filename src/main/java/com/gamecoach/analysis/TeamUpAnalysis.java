package com.gamecoach.analysis;

/**
 * The player's equipped team-up, worked out from the scoreboard box and the team-up table.
 *
 * @param name               the team-up's name; null if (hero, partner) isn't in the table
 *                           (a misread portrait, or a team-up newer than the table)
 * @param enhancedOnScoreboard true when the box was in full color
 * @param partnerOnTeam      true when a teammate played the partner hero (worked out from the rows).
 *                           Should agree with enhancedOnScoreboard; if not, something was misread.
 * @param betterOption       the hero's other team-up when its partner IS on the team but the equipped
 *                           one isn't enhanced, e.g. "Surf & Turf (Jeff the Land Shark)"; otherwise null
 */
public record TeamUpAnalysis(
        String name,
        String partner,
        boolean enhancedOnScoreboard,
        boolean partnerOnTeam,
        String baseEffect,
        String enhancedEffect,
        String betterOption) {
}

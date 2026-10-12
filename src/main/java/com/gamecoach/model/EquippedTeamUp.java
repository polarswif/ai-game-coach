package com.gamecoach.model;

/**
 * The team-up box next to a player's portrait on the in-game scoreboard (Season 9 and later).
 * The box shows the equipped team-up's partner hero and its symbol. When the partner is on the
 * player's team the box is in full color (enhanced); otherwise it's shaded (base effect only).
 * The team-up's name isn't printed: MatchAnalyzer looks it up from the hero and the partner.
 *
 * @param partner  the hero pictured in the box
 * @param enhanced true when the box is in full color
 */
public record EquippedTeamUp(String partner, boolean enhanced) {
}

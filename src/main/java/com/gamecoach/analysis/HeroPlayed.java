package com.gamecoach.analysis;

import com.gamecoach.model.Role;

/**
 * A hero the player used, with its role filled in from the role table.
 *
 * @param role               the role from the table, e.g. "Deadpool (Strategist)" is STRATEGIST.
 *                           For a plain "Deadpool" with no label this falls back to the stats guess,
 *                           or UNKNOWN if there is no guess.
 * @param statsSuggestedRole Deadpool only: the role his stats point to, as a second opinion.
 *                           Null for every other hero, and for Deadpool when the player also
 *                           played other heroes (the healing and blocked totals then mix heroes).
 *                           If it differs from {@code role}, the label and the stats disagree.
 */
public record HeroPlayed(
        String hero,
        Role role,
        Role statsSuggestedRole,
        int kills,
        int deaths,
        int assists) {
}

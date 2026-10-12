package com.gamecoach.model;

/**
 * The proficiency cosmetics visible on one scoreboard row. They belong to the row's last hero.
 * The purple (Colonel, level 30) and gold (Elite, level 40) avatar frames aren't shown on the
 * scoreboard, so they aren't recorded.
 *
 * @param portrait the portrait style; DEFAULT when it's the normal art
 * @param badge    the equipped Proficiency Badge; null when none is shown (not equipped, or none earned)
 */
public record ProficiencyShown(PortraitStyle portrait, ProficiencyBadge badge) {
}

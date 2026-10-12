package com.gamecoach.analysis;

import com.gamecoach.model.PortraitStyle;
import com.gamecoach.model.ProficiencyBadge;

/**
 * What the row's Lord portrait and Proficiency Badge prove about the player's experience on their
 * last hero. It's a lower bound only: both are optional to equip, so the real level may be higher.
 *
 * @param hero         the hero it applies to (the row's last hero)
 * @param portrait     the portrait style shown
 * @param badge        the badge shown; null when none
 * @param minimumLevel the highest level either one proves, e.g. 55 for a Champion badge
 * @param minimumRank  the rank that level belongs to, e.g. "Champion"
 */
public record ProficiencyAnalysis(
        String hero,
        PortraitStyle portrait,
        ProficiencyBadge badge,
        int minimumLevel,
        String minimumRank) {
}

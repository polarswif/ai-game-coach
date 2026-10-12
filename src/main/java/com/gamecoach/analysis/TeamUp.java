package com.gamecoach.analysis;

/**
 * One row of the team-up table: a hero's team-up and the partner hero who enhances it.
 *
 * @param baseEffect     what it does on its own (always active once equipped)
 * @param enhancedEffect what it adds when the partner is on the same team
 */
public record TeamUp(
        String hero,
        String name,
        String partner,
        String baseEffect,
        String enhancedEffect) {
}

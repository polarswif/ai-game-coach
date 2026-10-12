package com.gamecoach.analysis;

/**
 * One row of the team-up table: a hero's team-up and the partner hero who enhances it.
 * What the team-up does is deliberately not stored; balance patches change it every season.
 */
public record TeamUp(
        String hero,
        String name,
        String partner) {
}

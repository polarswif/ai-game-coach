package com.gamecoach.model;

/**
 * One hero a player used during a match.
 * The in-game "Heroes Used" list shows only portraits, so the K/D/A are null unless they came
 * from somewhere with per-hero numbers (a tracker.gg screenshot, or the in-game per-hero detail).
 * They are Integer rather than int because int can't be null.
 */
public record HeroStats(
        String hero,
        Integer kills,
        Integer deaths,
        Integer assists) {

    /** A hero known only by name, with no per-hero numbers. */
    public static HeroStats nameOnly(String hero) {
        return new HeroStats(hero, null, null, null);
    }
}

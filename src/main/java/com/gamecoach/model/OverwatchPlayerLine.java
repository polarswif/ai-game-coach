package com.gamecoach.model;

/**
 * One row of the Overwatch Teams screen (the end-of-match scoreboard).
 *
 * @param name     the player name; null when hidden or unreadable
 * @param title    the small line under some names ("Philosopher"); a cosmetic title, never a hero
 * @param team     "A" for the uploader's team (top, blue), "B" for the enemy team (bottom, red)
 * @param role     from the row's role icon; null when it couldn't be read
 * @param hero     the hero in the portrait: the hero the player was on when the match ended
 * @param damage   hero damage done (the DMG column)
 * @param mitigation damage mitigated (the MIT column): blocked, absorbed or reduced
 * @param uploader true for the uploader's own row
 */
public record OverwatchPlayerLine(
        String name,
        String title,
        String team,
        OverwatchRole role,
        String hero,
        int eliminations,
        int assists,
        int deaths,
        int damage,
        int healing,
        int mitigation,
        boolean uploader) {

    public OverwatchPlayerLine withHero(String newHero) {
        return new OverwatchPlayerLine(name, title, team, role, newHero, eliminations, assists, deaths, damage,
                healing, mitigation, uploader);
    }
}

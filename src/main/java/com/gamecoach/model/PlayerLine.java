package com.gamecoach.model;

import java.util.List;

/**
 * One player's row on the in-game end-of-match scoreboard.
 *
 * @param name            null when the name is hidden (blacked out, or covered by a popup)
 * @param team            "A" for the uploader's team (the top half), "B" for the opponents
 * @param role            from the role icon at the left of the row
 * @param lastHero        the portrait on the row, which is the last hero the player used
 * @param heroes          every hero used, from the "Heroes Used" hover or a tracker screenshot;
 *                        empty when neither was captured
 * @param accuracyPercent null when the scoreboard shows no accuracy ("—"); 0% is a real 0.0
 * @param uploader        true for the highlighted row: the person who took the screenshot
 * @param rank            null unless a tracker screenshot supplied it (the game doesn't show ranks)
 * @param rankScore       null unless a tracker screenshot supplied it
 */
public record PlayerLine(
        String name,
        String team,
        Role role,
        String lastHero,
        List<HeroStats> heroes,
        int kills,
        int deaths,
        int assists,
        int finalHits,
        int damage,
        int blocked,
        int healing,
        Double accuracyPercent,
        boolean uploader,
        boolean mvp,
        boolean svp,
        String rank,
        Integer rankScore) {

    // Records can't be changed after they're built, so these return a copy with one field
    // replaced. The review step (correcting a misread row) will use them too.

    public PlayerLine withRole(Role newRole) {
        return new PlayerLine(name, team, newRole, lastHero, heroes, kills, deaths, assists, finalHits,
                damage, blocked, healing, accuracyPercent, uploader, mvp, svp, rank, rankScore);
    }

    public PlayerLine withHeroes(List<HeroStats> newHeroes) {
        return new PlayerLine(name, team, role, lastHero, newHeroes, kills, deaths, assists, finalHits,
                damage, blocked, healing, accuracyPercent, uploader, mvp, svp, rank, rankScore);
    }

    public PlayerLine withAccuracy(Double newAccuracy) {
        return new PlayerLine(name, team, role, lastHero, heroes, kills, deaths, assists, finalHits,
                damage, blocked, healing, newAccuracy, uploader, mvp, svp, rank, rankScore);
    }
}

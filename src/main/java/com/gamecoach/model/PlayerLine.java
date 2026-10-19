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
 * @param teamUp          the team-up box beside the portrait; null when the screenshot has none
 *                        (older scoreboards) or it couldn't be read
 * @param proficiency     Lord portrait and Proficiency Badge, for the last hero; null when not read.
 *                        Both are optional to equip, so a plain row doesn't mean low proficiency
 * @param accuracyPercent null when the scoreboard shows no accuracy ("—"); 0% is a real 0.0
 * @param uploader        true for the highlighted row: the person who took the screenshot
 * @param medals          the per-match medals in the "Medals" column; empty when none
 * @param multiKill        the red number in the "Medals" column: best multi-kill (3 to 6 KOs in quick succession); null when none
 * @param rank            null unless a tracker screenshot supplied it (the game doesn't show ranks)
 * @param rankScore       null unless a tracker screenshot supplied it
 */
public record PlayerLine(
        String name,
        String team,
        Role role,
        String lastHero,
        List<HeroStats> heroes,
        EquippedTeamUp teamUp,
        ProficiencyShown proficiency,
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
        List<Medal> medals,
        Integer multiKill,
        String rank,
        Integer rankScore) {

    // Records can't be changed after they're built, so these return a copy with one field
    // replaced. The review step (correcting a misread row) will use them too.

    public PlayerLine withRole(Role newRole) {
        return new PlayerLine(name, team, newRole, lastHero, heroes, teamUp, proficiency, kills, deaths, assists, finalHits,
                damage, blocked, healing, accuracyPercent, uploader, mvp, svp, medals, multiKill, rank, rankScore);
    }

    public PlayerLine withLastHero(String newLastHero) {
        return new PlayerLine(name, team, role, newLastHero, heroes, teamUp, proficiency, kills, deaths, assists, finalHits,
                damage, blocked, healing, accuracyPercent, uploader, mvp, svp, medals, multiKill, rank, rankScore);
    }

    public PlayerLine withHeroes(List<HeroStats> newHeroes) {
        return new PlayerLine(name, team, role, lastHero, newHeroes, teamUp, proficiency, kills, deaths, assists, finalHits,
                damage, blocked, healing, accuracyPercent, uploader, mvp, svp, medals, multiKill, rank, rankScore);
    }

    public PlayerLine withTeamUp(EquippedTeamUp newTeamUp) {
        return new PlayerLine(name, team, role, lastHero, heroes, newTeamUp, proficiency, kills, deaths, assists, finalHits,
                damage, blocked, healing, accuracyPercent, uploader, mvp, svp, medals, multiKill, rank, rankScore);
    }

    public PlayerLine withProficiency(ProficiencyShown newProficiency) {
        return new PlayerLine(name, team, role, lastHero, heroes, teamUp, newProficiency, kills, deaths, assists, finalHits,
                damage, blocked, healing, accuracyPercent, uploader, mvp, svp, medals, multiKill, rank, rankScore);
    }

    public PlayerLine withMedals(List<Medal> newMedals, Integer newMultiKill) {
        return new PlayerLine(name, team, role, lastHero, heroes, teamUp, proficiency, kills, deaths, assists, finalHits,
                damage, blocked, healing, accuracyPercent, uploader, mvp, svp, newMedals, newMultiKill, rank, rankScore);
    }

    public PlayerLine withAccuracy(Double newAccuracy) {
        return new PlayerLine(name, team, role, lastHero, heroes, teamUp, proficiency, kills, deaths, assists, finalHits,
                damage, blocked, healing, newAccuracy, uploader, mvp, svp, medals, multiKill, rank, rankScore);
    }
}

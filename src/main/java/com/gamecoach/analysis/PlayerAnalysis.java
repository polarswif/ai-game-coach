package com.gamecoach.analysis;

import com.gamecoach.model.Medal;
import com.gamecoach.model.Role;

import java.util.List;
import java.util.Map;

/**
 * Numbers computed in Java about one player in one match, so the coaching
 * model reads finished comparisons instead of doing arithmetic itself.
 *
 * @param player            the player's name; null when it was hidden on the scoreboard
 * @param role              from the row's role icon (the role of their last hero)
 * @param teamComposition  the player's team from its role icons, Vanguards-Duelists-Strategists, e.g. "2-2-2"
 * @param enemyComposition the other team, same format, e.g. "1-2-3"
 * @param kda               (kills + assists) / deaths, with deaths counted as at least 1
 * @param teamUp            the player's equipped team-up; null when the scoreboard had no team-up box
 * @param proficiency       what the Lord portrait and Proficiency Badge prove about experience on the
 *                          last hero; null when neither is shown (which says nothing about experience)
 * @param teammateTeamUpsYouEnhance teammates' equipped team-ups that this player's hero enhances,
 *                          e.g. "Frozen Haven (Player20)"
 * @param medals            per-match medals won (most KOs, damage, blocked, healing or assists in the lobby)
 * @param multiKill          best multi-kill shown on the scoreboard (5 = a Penta); null when none
 * @param accuracyPercent   null when the scoreboard shows no accuracy
 * @param accuracyLobbyRank rank among players with an accuracy value; null when accuracyPercent is null
 */
public record PlayerAnalysis(
        String player,
        String team,
        Role role,
        boolean won,
        boolean mvp,
        boolean svp,
        int teamSize,
        int lobbySize,
        String teamComposition,
        String enemyComposition,
        double kda,
        List<HeroPlayed> heroes,
        TeamUpAnalysis teamUp,
        ProficiencyAnalysis proficiency,
        List<String> teammateTeamUpsYouEnhance,
        List<Medal> medals,
        Integer multiKill,
        Map<Stat, StatComparison> stats,
        Double accuracyPercent,
        Integer accuracyLobbyRank) {
}

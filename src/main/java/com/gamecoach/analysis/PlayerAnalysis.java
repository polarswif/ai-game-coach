package com.gamecoach.analysis;

import java.util.List;
import java.util.Map;

/**
 * Numbers computed in Java about one player in one match, so the coaching
 * model reads finished comparisons instead of doing arithmetic itself.
 *
 * @param kda               (kills + assists) / deaths, with deaths counted as at least 1
 * @param accuracyPercent   null when the scoreboard shows no accuracy
 * @param accuracyLobbyRank rank among players with an accuracy value; null when accuracyPercent is null
 */
public record PlayerAnalysis(
        String player,
        String team,
        boolean won,
        int teamSize,
        int lobbySize,
        double kda,
        List<HeroPlayed> heroes,
        Map<Stat, StatComparison> stats,
        Double accuracyPercent,
        Integer accuracyLobbyRank) {
}

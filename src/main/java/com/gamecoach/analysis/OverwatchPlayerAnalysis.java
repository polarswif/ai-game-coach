package com.gamecoach.analysis;

import com.gamecoach.model.OverwatchMatch.HeroStatsPanel;
import com.gamecoach.model.OverwatchMatch.HeroTime;
import com.gamecoach.model.OverwatchRole;

import java.util.List;
import java.util.Map;

/**
 * Numbers computed in Java about one player in one Overwatch match, so the coach reads finished
 * comparisons instead of doing arithmetic.
 *
 * @param player            null when the name was hidden
 * @param role              the row's role icon, or the hero's role from the table when the icon wasn't read
 * @param outcome           "WIN", "LOSS" or "DRAW" for this player's team; null when the result is unknown
 * @param teamComposition   tanks-damage-supports from role icons, e.g. "1-2-2"
 * @param heroesPlayed      the uploader's heroes from the Summary; for anyone else, just the portrait's hero
 * @param eliminationsPerDeath eliminations / deaths, deaths counted as at least 1
 * @param heroStats         Personal-screen tiles; only for the uploader
 * @param closeness         how close the match was, from the final score (outside Push) or the death totals
 */
public record OverwatchPlayerAnalysis(
        String player,
        String team,
        OverwatchRole role,
        String hero,
        String outcome,
        int teamSize,
        int lobbySize,
        String teamComposition,
        String enemyComposition,
        List<HeroTime> heroesPlayed,
        double eliminationsPerDeath,
        Map<OverwatchStat, OverwatchStatComparison> stats,
        List<HeroStatsPanel> heroStats,
        MatchCloseness closeness) {
}

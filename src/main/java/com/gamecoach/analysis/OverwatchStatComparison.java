package com.gamecoach.analysis;

/**
 * How one of a player's stats compares with the rest of the match. Ranks start at 1 for the best
 * value; ties share a rank. Overwatch players think in "per 10 minutes", so that's the rate here.
 *
 * @param per10            value per 10 minutes of game length; null when the length is unknown
 * @param teamSharePercent the player's share of their team's total, 0 to 100
 * @param teamRank         rank within the player's own team
 * @param lobbyRank        rank among all players in the match
 * @param sameRoleEnemyBest the best value among enemies in the same role (for deaths, the fewest); null when none
 */
public record OverwatchStatComparison(
        int value,
        Double per10,
        double teamSharePercent,
        int teamRank,
        int lobbyRank,
        Integer sameRoleEnemyBest) {
}

package com.gamecoach.analysis;

/**
 * How one of a player's stats compares to everyone else in the match.
 * Ranks start at 1 for the best value; ties share a rank.
 *
 * @param perMinute        value per minute of match duration
 * @param teamSharePercent the player's share of their team's total, 0 to 100
 * @param teamRank         rank among the player's own team
 * @param lobbyRank        rank among all players in the match
 */
public record StatComparison(
        int value,
        double perMinute,
        double teamSharePercent,
        int teamRank,
        int lobbyRank) {
}

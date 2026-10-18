package com.gamecoach.model;

/**
 * Rounds won by each team, from the match history screen (for example VICTORY 3-2).
 * Domination is best of 3 (2-0, 2-1); Convoy and Convergence can run longer (3-1, 3-2).
 *
 * @param teamA rounds won by team A, the uploader's team
 * @param teamB rounds won by team B
 */
public record RoundScore(int teamA, int teamB) {

    public RoundScore {
        if (teamA < 0 || teamB < 0) {
            throw new IllegalArgumentException("Round counts can't be negative: " + teamA + "-" + teamB);
        }
    }
}

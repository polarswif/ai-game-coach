package com.gamecoach.model;

import java.time.Duration;
import java.util.List;

/**
 * Full in-game scoreboard for a single match: every player, both teams.
 *
 * @param duration match length, from the top-right corner
 * @param result   VICTORY or DEFEAT for team A, the uploader's team
 * @param replayId null when the screenshot doesn't show one
 * @param bans     banned heroes, if any are shown; may be empty
 * @param score    rounds won by each team, from the match history screen; null when unknown
 */
public record MatchDetails(
        String matchId,
        String mode,
        String map,
        Duration duration,
        MatchResult result,
        String replayId,
        List<String> bans,
        List<PlayerLine> players,
        RoundScore score) {

    public MatchDetails withPlayers(List<PlayerLine> newPlayers) {
        return new MatchDetails(matchId, mode, map, duration, result, replayId, bans, newPlayers, score);
    }

    public MatchDetails withScore(RoundScore newScore) {
        return new MatchDetails(matchId, mode, map, duration, result, replayId, bans, players, newScore);
    }
}

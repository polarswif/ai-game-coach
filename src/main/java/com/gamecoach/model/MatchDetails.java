package com.gamecoach.model;

import java.time.Duration;
import java.util.List;

/** Full scoreboard for a single match: every player, both teams. */
public record MatchDetails(
        String matchId,
        String mode,
        String map,
        Duration duration,
        int teamAScore,
        int teamBScore,
        List<PlayerLine> players) {
}

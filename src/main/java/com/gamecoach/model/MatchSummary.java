package com.gamecoach.model;

import java.time.LocalDate;

/** One row in the "last 25 matches" picker. */
public record MatchSummary(
        String matchId,
        LocalDate playedOn,
        String mode,
        String map,
        String hero,
        int kills,
        int deaths,
        int assists,
        boolean won) {
}

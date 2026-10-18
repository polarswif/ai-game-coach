package com.gamecoach.analysis;

/**
 * How close the match was, from the coached player's side.
 *
 * Why it matters: in a stomp, most numbers reflect the mismatch between the teams more than the
 * player's own play (a losing player dies more, a winning player farms bigger totals). Hard-fought
 * matches are the ones where coaching is most reliable.
 *
 * @param level               CLOSE, CLEAR_EDGE or ONE_SIDED; from the round score when known, else from deaths
 * @param roundsWon           rounds won by the coached player's team; null when the score is unknown
 * @param roundsLost          rounds won by the other team; null when the score is unknown
 * @param teamAheadInFights   true when the coached player's team died less (won more fights).
 *                            This isn't always the team that won: objective play can decide a match.
 * @param teamDeaths          total deaths of the coached player's team
 * @param enemyDeaths         total deaths of the other team
 * @param deathRatio          more deaths divided by fewer deaths (1.0 = even); the fewer side counts as at least 1
 * @param damageSharePercent  the coached team's share of all damage in the match
 * @param healingSharePercent the coached team's share of all healing in the match
 */
public record MatchCloseness(
        Closeness level,
        Integer roundsWon,
        Integer roundsLost,
        boolean teamAheadInFights,
        int teamDeaths,
        int enemyDeaths,
        double deathRatio,
        double damageSharePercent,
        double healingSharePercent) {

    /** True when the verdict came from the round score rather than the death totals. */
    public boolean fromRounds() {
        return roundsWon != null;
    }
}

package com.gamecoach.model;

import java.time.Duration;
import java.util.List;

/**
 * An Overwatch match read from the end-of-match report screens. The Teams screen gives the rows;
 * the Summary screen adds the result, final score, map, mode, length and the uploader's heroes;
 * the Personal screen adds hero-specific stats. Fields from a screen that wasn't uploaded are null
 * or empty.
 *
 * @param mode       "Push", "Control", "Escort"...; null when unknown
 * @param duration   game length; null when unknown (no Summary screen)
 * @param result     null when unknown (no Summary screen)
 * @param score      the final score, uploader's team first; its meaning depends on the mode (rounds
 *                   in Control, points in Escort, a single win in Push); null when unknown
 * @param playedAt   the date and time as the Summary screen prints it, e.g. "10/21/26 - 18:31"
 * @param yourHeroes the uploader's heroes with their share of play time, from Summary or Personal
 * @param heroStats  one panel per hero from the Personal screen
 */
public record OverwatchMatch(
        String matchId,
        String mode,
        String map,
        Duration duration,
        OverwatchResult result,
        RoundScore score,
        String playedAt,
        List<OverwatchPlayerLine> players,
        List<HeroTime> yourHeroes,
        List<HeroStatsPanel> heroStats) {

    public OverwatchMatch {
        players = players == null ? List.of() : List.copyOf(players);
        yourHeroes = yourHeroes == null ? List.of() : List.copyOf(yourHeroes);
        heroStats = heroStats == null ? List.of() : List.copyOf(heroStats);
    }

    public OverwatchMatch withPlayers(List<OverwatchPlayerLine> newPlayers) {
        return new OverwatchMatch(matchId, mode, map, duration, result, score, playedAt, newPlayers, yourHeroes, heroStats);
    }

    public OverwatchMatch withId(String newId) {
        return new OverwatchMatch(newId, mode, map, duration, result, score, playedAt, players, yourHeroes, heroStats);
    }

    /**
     * A hero the uploader played.
     *
     * @param percentPlayed share of the uploader's match time, 0 to 100; null when not shown
     * @param playTime      null when not shown
     */
    public record HeroTime(String hero, Double percentPlayed, Duration playTime) {
    }

    /** The Personal screen's tiles for one hero. */
    public record HeroStatsPanel(String hero, List<HeroStat> stats) {

        public HeroStatsPanel {
            stats = stats == null ? List.of() : List.copyOf(stats);
        }
    }

    /**
     * One tile, kept as the screen shows it, since tiles mix counts ("4"), percents ("30%") and
     * times ("01:28").
     *
     * @param label e.g. "Weapon accuracy", "Ravenous Vortex kills"
     * @param per10 the tile's "AVG PER 10 MIN" line. It is this match's rate, not a career average.
     */
    public record HeroStat(String label, String value, String per10) {
    }
}

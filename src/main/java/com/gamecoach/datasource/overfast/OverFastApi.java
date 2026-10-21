package com.gamecoach.datasource.overfast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * The parts of OverFast's JSON this app reads, as records. Jackson fills them from the reply;
 * @JsonProperty maps OverFast's snake_case names, and ignoreUnknown skips every field we don't
 * use (avatars, icons, endorsements), so new fields on their side can't break us.
 *
 * Numbers that can be missing are wrapper types (Integer, Double) so "missing" stays null
 * instead of silently becoming 0.
 */
public final class OverFastApi {

    private OverFastApi() {
    }

    /** GET /players?name=... */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchResult(Integer total, List<PlayerShort> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerShort(
            @JsonProperty("player_id") String playerId,
            String name,
            @JsonProperty("is_public") Boolean isPublic) {
    }

    /** GET /players/{id}/summary. competitive is null for a private profile or no competitive play. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerSummary(String username, Competitive competitive) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Competitive(PlatformRanks pc, PlatformRanks console) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlatformRanks(Integer season, Rank tank, Rank damage, Rank support, Rank open) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rank(String division, Integer tier) {
    }

    /**
     * GET /players/{id}/stats/summary. Every part is null for a private profile or a mode the player
     * never played. roles is keyed "tank"/"damage"/"support"; heroes by hero key such as "ana".
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatsSummary(Stats general, Map<String, Stats> roles, Map<String, Stats> heroes) {
    }

    /** time_played is in seconds; winrate is a percent (46.66 means 46.66%). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stats(
            @JsonProperty("games_played") Integer gamesPlayed,
            @JsonProperty("games_won") Integer gamesWon,
            @JsonProperty("games_lost") Integer gamesLost,
            @JsonProperty("time_played") Long timePlayed,
            Double winrate,
            Double kda,
            Values total,
            Values average) {
    }

    /** "average" means per 10 minutes. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Values(Double eliminations, Double assists, Double deaths, Double damage, Double healing) {
    }
}

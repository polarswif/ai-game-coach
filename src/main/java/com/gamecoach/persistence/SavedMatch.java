package com.gamecoach.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One uploaded match, as a row of the saved_match table.
 *
 * An @Entity is a class Hibernate maps to a table: each field is a column, each object a row.
 * The full match (every player, every stat) is stored as JSON text in match_json, because its shape
 * is deep and differs per game. The columns beside it copy the few facts the match history page
 * lists and sorts by, so that page never has to read the JSON.
 */
@Entity
@Table(name = "saved_match", indexes = @Index(name = "idx_saved_match_uploaded_at", columnList = "uploaded_at"))
public class SavedMatch {

    /** The match id the app already uses ("upload-1a2b3c4d", "ow-1a2b3c4d"), so it's the primary key. */
    @Id
    @Column(name = "match_id", length = 40)
    private String matchId;

    /** "rivals" or "overwatch". */
    @Column(nullable = false, length = 20)
    private String game;

    @Column(name = "map_name", length = 120)
    private String mapName;

    @Column(name = "game_mode", length = 60)
    private String gameMode;

    /** VICTORY, DEFEAT or DRAW; null when unknown. */
    @Column(name = "match_result", length = 20)
    private String matchResult;

    /** "3-1", your team first; null when unknown. */
    @Column(length = 20)
    private String score;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "your_hero", length = 60)
    private String yourHero;

    @Column(name = "your_name", length = 60)
    private String yourName;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** The whole match record as JSON. */
    @Column(name = "match_json", nullable = false, length = 1_000_000)
    private String matchJson;

    /** JPA needs a constructor with no arguments to build objects from rows. */
    protected SavedMatch() {
    }

    public SavedMatch(String matchId, String game, Instant uploadedAt) {
        this.matchId = matchId;
        this.game = game;
        this.uploadedAt = uploadedAt;
        this.updatedAt = uploadedAt;
    }

    /** Replaces everything but the id, game and upload time (used when a match is corrected). */
    void update(String mapName, String gameMode, String matchResult, String score, Long durationSeconds,
                String yourHero, String yourName, String matchJson, Instant now) {
        this.mapName = mapName;
        this.gameMode = gameMode;
        this.matchResult = matchResult;
        this.score = score;
        this.durationSeconds = durationSeconds;
        this.yourHero = yourHero;
        this.yourName = yourName;
        this.matchJson = matchJson;
        this.updatedAt = now;
    }

    public String getMatchId() { return matchId; }
    public String getGame() { return game; }
    public String getMapName() { return mapName; }
    public String getGameMode() { return gameMode; }
    public String getMatchResult() { return matchResult; }
    public String getScore() { return score; }
    public Long getDurationSeconds() { return durationSeconds; }
    public String getYourHero() { return yourHero; }
    public String getYourName() { return yourName; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getMatchJson() { return matchJson; }
}

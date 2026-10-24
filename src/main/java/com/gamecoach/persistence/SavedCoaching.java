package com.gamecoach.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * One coaching result, as a row of the coaching table. Each row belongs to one saved match (a
 * foreign key), so one match can have many coachings: one per player coached, and a new one when
 * the match is corrected (the fingerprint changes).
 */
@Entity
@Table(name = "coaching", uniqueConstraints = @UniqueConstraint(
        name = "uq_coaching_match_player_version", columnNames = {"match_id", "player_key", "match_fingerprint"}))
public class SavedCoaching {

    /** A number the database hands out itself (1, 2, 3...): IDENTITY means "auto-increment". */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The match this coaching is about. In the table this is the match_id column, a foreign key to
     * saved_match. ON DELETE CASCADE: deleting a match deletes its coaching rows with it.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SavedMatch match;

    /** Who was coached: "name:playerone", or "row:A:Ramattra" when the name was hidden. */
    @Column(name = "player_key", nullable = false, length = 80)
    private String playerKey;

    /** A SHA-256 of the match as coached, so a corrected match gets fresh coaching. */
    @Column(name = "match_fingerprint", nullable = false, length = 64)
    private String matchFingerprint;

    /** The Feedback record (strengths, weaknesses, advice, matchContext) as JSON. */
    @Column(name = "feedback_json", nullable = false, length = 100_000)
    private String feedbackJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavedCoaching() {
    }

    public SavedCoaching(SavedMatch match, String playerKey, String matchFingerprint, String feedbackJson, Instant createdAt) {
        this.match = match;
        this.playerKey = playerKey;
        this.matchFingerprint = matchFingerprint;
        this.feedbackJson = feedbackJson;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getPlayerKey() { return playerKey; }
    public String getMatchFingerprint() { return matchFingerprint; }
    public String getFeedbackJson() { return feedbackJson; }
    public Instant getCreatedAt() { return createdAt; }
}

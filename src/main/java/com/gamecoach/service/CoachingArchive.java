package com.gamecoach.service;

import com.gamecoach.model.Feedback;

import java.util.Optional;

/**
 * Coaching already written, so the same player in the same version of a match is never paid for
 * twice, not even after a restart. The app saves to the database (persistence.DbCoachingArchive);
 * tests use inMemory().
 *
 * @see #find
 */
public interface CoachingArchive {

    /**
     * @param match     the match as coached; a corrected match counts as a different version
     * @param playerKey who was coached, e.g. "name:playerone"
     */
    Optional<Feedback> find(String matchId, Object match, String playerKey);

    void save(String matchId, Object match, String playerKey, Feedback feedback);

    /** A map-based archive, lost on restart. */
    static CoachingArchive inMemory() {
        return new InMemoryCoachingArchive();
    }
}

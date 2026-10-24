package com.gamecoach.parsing;

import com.gamecoach.model.MatchDetails;

import java.util.Optional;
import java.util.UUID;

/**
 * Where uploaded Marvel Rivals matches are kept. The app uses the database version
 * (persistence.DbUploadedMatchStore); tests use InMemoryUploadedMatchStore, so they need no database.
 */
public interface UploadedMatchStore {

    /** A fresh id like "upload-1a2b3c4d", so uploaded matches can't collide with other sources' ids. */
    default String newId() {
        return "upload-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Saves a new match, or replaces the one with the same id (a correction). */
    void save(MatchDetails match);

    Optional<MatchDetails> find(String matchId);

    default boolean contains(String matchId) {
        return find(matchId).isPresent();
    }
}

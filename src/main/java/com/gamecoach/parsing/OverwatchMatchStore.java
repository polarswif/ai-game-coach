package com.gamecoach.parsing;

import com.gamecoach.model.OverwatchMatch;

import java.util.Optional;
import java.util.UUID;

/**
 * Where uploaded Overwatch matches are kept. The app uses persistence.DbOverwatchMatchStore; tests
 * use InMemoryOverwatchMatchStore.
 */
public interface OverwatchMatchStore {

    /** A fresh id like "ow-1a2b3c4d". */
    default String newId() {
        return "ow-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Saves a new match, or replaces the one with the same id (a correction). */
    void save(OverwatchMatch match);

    Optional<OverwatchMatch> find(String matchId);
}

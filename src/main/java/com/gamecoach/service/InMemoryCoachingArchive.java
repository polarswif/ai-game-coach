package com.gamecoach.service;

import com.gamecoach.model.Feedback;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * CoachingArchive in an LRU map (the newest 500). Within one run, a record's hashCode() is a fine
 * version stamp; it changes between runs, which doesn't matter for a map that's gone by then.
 */
class InMemoryCoachingArchive implements CoachingArchive {

    private static final int CAPACITY = 500;

    private final Map<String, Feedback> entries = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Feedback> eldest) {
                    return size() > CAPACITY;
                }
            });

    @Override
    public Optional<Feedback> find(String matchId, Object match, String playerKey) {
        return Optional.ofNullable(entries.get(key(matchId, match, playerKey)));
    }

    @Override
    public void save(String matchId, Object match, String playerKey, Feedback feedback) {
        entries.put(key(matchId, match, playerKey), feedback);
    }

    private static String key(String matchId, Object match, String playerKey) {
        return matchId + "#" + Integer.toHexString(match.hashCode()) + "|" + playerKey;
    }
}

package com.gamecoach.parsing;

import com.gamecoach.model.OverwatchMatch;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Overwatch matches read from screenshots, kept in memory until the database step replaces this
 * (like UploadedMatchStore for Rivals). Lost on restart; holds the most recent 200.
 */
@Component
public class OverwatchMatchStore {

    private static final int CAPACITY = 200;

    private final Map<String, OverwatchMatch> matches = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, OverwatchMatch> eldest) {
                    return size() > CAPACITY;
                }
            });

    /** A fresh id like "ow-1a2b3c4d". */
    public String newId() {
        return "ow-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void save(OverwatchMatch match) {
        matches.put(match.matchId(), match);
    }

    public Optional<OverwatchMatch> find(String matchId) {
        return Optional.ofNullable(matches.get(matchId));
    }
}

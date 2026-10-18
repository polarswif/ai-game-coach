package com.gamecoach.parsing;

import com.gamecoach.model.MatchDetails;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Matches read from uploaded screenshots, kept in memory until the database step replaces this.
 * Everything is lost when the app restarts. Holds the most recent 200, dropping the oldest.
 */
@Component
public class UploadedMatchStore {

    private static final int CAPACITY = 200;

    private final Map<String, MatchDetails> matches = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, MatchDetails> eldest) {
                    return size() > CAPACITY;
                }
            });

    /** A fresh id like "upload-1a2b3c4d", so uploaded matches can't collide with other sources' ids. */
    public String newId() {
        return "upload-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void save(MatchDetails match) {
        matches.put(match.matchId(), match);
    }

    public Optional<MatchDetails> find(String matchId) {
        return Optional.ofNullable(matches.get(matchId));
    }

    public boolean contains(String matchId) {
        return matches.containsKey(matchId);
    }
}

package com.gamecoach.parsing;

import com.gamecoach.model.MatchDetails;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** UploadedMatchStore in a map: lost on restart, holds the newest 200. For tests. */
public class InMemoryUploadedMatchStore implements UploadedMatchStore {

    private static final int CAPACITY = 200;

    private final Map<String, MatchDetails> matches = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, MatchDetails> eldest) {
                    return size() > CAPACITY;
                }
            });

    @Override
    public void save(MatchDetails match) {
        matches.put(match.matchId(), match);
    }

    @Override
    public Optional<MatchDetails> find(String matchId) {
        return Optional.ofNullable(matches.get(matchId));
    }
}

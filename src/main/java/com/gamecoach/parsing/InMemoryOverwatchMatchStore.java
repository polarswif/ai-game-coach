package com.gamecoach.parsing;

import com.gamecoach.model.OverwatchMatch;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** OverwatchMatchStore in a map: lost on restart, holds the newest 200. For tests. */
public class InMemoryOverwatchMatchStore implements OverwatchMatchStore {

    private static final int CAPACITY = 200;

    private final Map<String, OverwatchMatch> matches = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, OverwatchMatch> eldest) {
                    return size() > CAPACITY;
                }
            });

    @Override
    public void save(OverwatchMatch match) {
        matches.put(match.matchId(), match);
    }

    @Override
    public Optional<OverwatchMatch> find(String matchId) {
        return Optional.ofNullable(matches.get(matchId));
    }
}

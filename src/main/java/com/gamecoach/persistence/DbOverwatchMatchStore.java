package com.gamecoach.persistence;

import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchPlayerLine;
import com.gamecoach.parsing.OverwatchMatchStore;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Uploaded Overwatch matches, saved in the database. */
@Component
public class DbOverwatchMatchStore implements OverwatchMatchStore {

    static final String GAME = "overwatch";

    private final SavedMatchRepository repository;
    private final MatchJson json;

    public DbOverwatchMatchStore(SavedMatchRepository repository, MatchJson json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    @Transactional
    public void save(OverwatchMatch match) {
        Instant now = Instant.now();
        SavedMatch row = repository.findById(match.matchId()).orElseGet(() -> new SavedMatch(match.matchId(), GAME, now));
        OverwatchPlayerLine you = match.players().stream().filter(OverwatchPlayerLine::uploader).findFirst().orElse(null);
        row.update(match.map(), match.mode(),
                match.result() == null ? null : match.result().name(),
                match.score() == null ? null : match.score().teamA() + "-" + match.score().teamB(),
                match.duration() == null ? null : match.duration().toSeconds(),
                you == null ? null : you.hero(),
                you == null ? null : you.name(),
                json.write(match), now);
        repository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OverwatchMatch> find(String matchId) {
        return repository.findById(matchId)
                .filter(row -> GAME.equals(row.getGame()))
                .map(row -> json.read(row.getMatchJson(), OverwatchMatch.class));
    }
}

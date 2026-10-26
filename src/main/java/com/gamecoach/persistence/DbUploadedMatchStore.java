package com.gamecoach.persistence;

import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.parsing.UploadedMatchStore;

import com.gamecoach.security.CurrentUser;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Uploaded Marvel Rivals matches, saved in the database. */
@Component
public class DbUploadedMatchStore implements UploadedMatchStore {

    static final String GAME = "rivals";

    private final SavedMatchRepository repository;
    private final MatchJson json;

    public DbUploadedMatchStore(SavedMatchRepository repository, MatchJson json) {
        this.repository = repository;
        this.json = json;
    }

    /**
     * @Transactional: the read and the write happen in one database transaction, so both succeed or
     * neither does.
     */
    @Override
    @Transactional
    public void save(MatchDetails match) {
        Instant now = Instant.now();
        SavedMatch row = repository.findById(match.matchId())
                .orElseGet(() -> new SavedMatch(match.matchId(), GAME, now, CurrentUser.username().orElse(null)));
        if (!CurrentUser.canSee(row.getOwner())) {
            throw new java.util.NoSuchElementException("No match with id " + match.matchId());
        }
        PlayerLine you = match.players().stream().filter(PlayerLine::uploader).findFirst().orElse(null);
        row.update(match.map(), match.mode(),
                match.result() == null ? null : match.result().name(),
                match.score() == null ? null : match.score().teamA() + "-" + match.score().teamB(),
                match.duration() == null ? null : match.duration().toSeconds(),
                you == null ? null : you.lastHero(),
                you == null ? null : you.name(),
                json.write(match), now);
        repository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchDetails> find(String matchId) {
        return repository.findById(matchId)
                .filter(row -> GAME.equals(row.getGame()))
                .filter(row -> CurrentUser.canSee(row.getOwner()))     // someone else's match stays hidden
                .map(row -> json.read(row.getMatchJson(), MatchDetails.class));
    }
}

package com.gamecoach.persistence;

import com.gamecoach.model.Feedback;
import com.gamecoach.service.CoachingArchive;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Coaching saved in the database, next to its match. Coaching for a match that isn't saved (the
 * built-in sample matches) has no row to belong to, so it stays in memory instead.
 */
@Component
public class DbCoachingArchive implements CoachingArchive {

    private final SavedMatchRepository matches;
    private final SavedCoachingRepository coachings;
    private final MatchJson json;
    private final CoachingArchive memory = CoachingArchive.inMemory();

    public DbCoachingArchive(SavedMatchRepository matches, SavedCoachingRepository coachings, MatchJson json) {
        this.matches = matches;
        this.coachings = coachings;
        this.json = json;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Feedback> find(String matchId, Object match, String playerKey) {
        if (!matches.existsById(matchId)) {
            return memory.find(matchId, match, playerKey);
        }
        return coachings.findFirstByMatch_MatchIdAndPlayerKeyAndMatchFingerprint(matchId, playerKey, json.fingerprint(match))
                .map(row -> json.read(row.getFeedbackJson(), Feedback.class));
    }

    @Override
    @Transactional
    public void save(String matchId, Object match, String playerKey, Feedback feedback) {
        Optional<SavedMatch> saved = matches.findById(matchId);
        if (saved.isEmpty()) {
            memory.save(matchId, match, playerKey, feedback);
            return;
        }
        String fingerprint = json.fingerprint(match);
        if (coachings.findFirstByMatch_MatchIdAndPlayerKeyAndMatchFingerprint(matchId, playerKey, fingerprint).isEmpty()) {
            coachings.save(new SavedCoaching(saved.get(), playerKey, fingerprint, json.write(feedback), Instant.now()));
        }
    }
}

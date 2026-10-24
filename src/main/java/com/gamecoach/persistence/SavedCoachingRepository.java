package com.gamecoach.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Database access for coaching rows. "Match_MatchId" follows the match field to its matchId. */
public interface SavedCoachingRepository extends JpaRepository<SavedCoaching, Long> {

    Optional<SavedCoaching> findFirstByMatch_MatchIdAndPlayerKeyAndMatchFingerprint(
            String matchId, String playerKey, String matchFingerprint);

    long countByMatch_MatchId(String matchId);
}

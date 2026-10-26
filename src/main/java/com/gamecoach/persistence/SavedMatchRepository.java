package com.gamecoach.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Database access for saved matches. Spring Data writes the implementation at startup: JpaRepository
 * brings save, findById, existsById, deleteById and more, and each method below is turned into a
 * query from its name ("find by game, order by uploadedAt descending").
 */
public interface SavedMatchRepository extends JpaRepository<SavedMatch, String> {

    List<SavedMatch> findAllByOrderByUploadedAtDesc();

    List<SavedMatch> findByGameOrderByUploadedAtDesc(String game);

    List<SavedMatch> findByOwnerOrderByUploadedAtDesc(String owner);

    List<SavedMatch> findByOwnerAndGameOrderByUploadedAtDesc(String owner, String game);

    /** Uploads by one account since a moment: the daily upload cap. */
    long countByOwnerAndUploadedAtAfter(String owner, java.time.Instant since);
}

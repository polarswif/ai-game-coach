package com.gamecoach.controller;

import com.gamecoach.persistence.SavedMatch;
import com.gamecoach.persistence.SavedMatchRepository;

import com.gamecoach.security.CurrentUser;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * The saved matches of both games, newest first, for the website's "My matches" page. Reads only the
 * summary columns, never the full match JSON, so the list stays fast however big matches get.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchHistoryController {

    /** One line of the list. durationSeconds and score may be null when the screenshot didn't show them. */
    public record MatchListItem(String matchId, String game, String map, String mode, String result, String score,
                                Long durationSeconds, String yourHero, String yourName, Instant uploadedAt) {
    }

    private final SavedMatchRepository matches;

    public MatchHistoryController(SavedMatchRepository matches) {
        this.matches = matches;
    }

    /** ?game=rivals or ?game=overwatch narrows the list; leave it out for both. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<MatchListItem> list(@RequestParam(name = "game", required = false) String game) {
        String g = game == null || game.isBlank() ? null : game.strip().toLowerCase(Locale.ROOT);
        String me = CurrentUser.username().orElse(null);
        List<SavedMatch> rows;
        if (me == null) {   // no accounts in play (tests): everything
            rows = g == null ? matches.findAllByOrderByUploadedAtDesc() : matches.findByGameOrderByUploadedAtDesc(g);
        } else {            // only your own uploads
            rows = g == null ? matches.findByOwnerOrderByUploadedAtDesc(me) : matches.findByOwnerAndGameOrderByUploadedAtDesc(me, g);
        }
        return rows.stream().map(MatchHistoryController::item).toList();
    }

    /** Deletes a match and, through the foreign key's ON DELETE CASCADE, its coaching. */
    @DeleteMapping("/{matchId}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable("matchId") String matchId) {
        SavedMatch row = matches.findById(matchId).orElse(null);
        if (row == null || !CurrentUser.canSee(row.getOwner())) {
            throw new NoSuchElementException("No saved match with id " + matchId);
        }
        matches.deleteById(matchId);
        return ResponseEntity.noContent().build();
    }

    private static MatchListItem item(SavedMatch m) {
        return new MatchListItem(m.getMatchId(), m.getGame(), m.getMapName(), m.getGameMode(), m.getMatchResult(),
                m.getScore(), m.getDurationSeconds(), m.getYourHero(), m.getYourName(), m.getUploadedAt());
    }
}

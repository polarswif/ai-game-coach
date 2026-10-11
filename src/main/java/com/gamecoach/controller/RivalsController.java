package com.gamecoach.controller;

import com.gamecoach.datasource.MatchHistorySource;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.service.CoachingService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

/** Marvel Rivals endpoints: match picker, scoreboard, and coaching feedback. */
@RestController
@RequestMapping("/api/rivals")
public class RivalsController {

    private final MatchHistorySource matches;
    private final CoachingService coach;

    public RivalsController(MatchHistorySource matches, CoachingService coach) {
        this.matches = matches;
        this.coach = coach;
    }

    /** The "last 25 matches" picker. */
    @GetMapping("/players/{username}/matches")
    public List<MatchSummary> recentMatches(@PathVariable("username") String username) {
        return matches.getRecentMatches(username);
    }

    /** Full scoreboard for one match. */
    @GetMapping("/matches/{matchId}")
    public MatchDetails matchDetails(@PathVariable("matchId") String matchId) {
        return loadMatch(matchId);
    }

    /** Coaching feedback for the player named in ?username= within that match. */
    @PostMapping("/matches/{matchId}/feedback")
    public Feedback feedback(@PathVariable("matchId") String matchId,
                             @RequestParam("username") String username) {
        MatchDetails match = loadMatch(matchId);
        PlayerLine player = findPlayer(match, username);
        return coach.coach(match, player);
    }

    private MatchDetails loadMatch(String matchId) {
        try {
            return matches.getMatchDetails(matchId);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private PlayerLine findPlayer(MatchDetails match, String username) {
        for (PlayerLine line : match.players()) {
            if (line.name().equalsIgnoreCase(username)) {
                return line;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, username + " is not in match " + match.matchId());
    }
}

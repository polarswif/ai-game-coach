package com.gamecoach.controller;

import com.gamecoach.analysis.MatchAnalyzer;
import com.gamecoach.analysis.PlayerAnalysis;
import com.gamecoach.datasource.MatchHistorySource;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.service.CoachingService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Marvel Rivals endpoints: match picker, scoreboard, analysis, and coaching feedback.
 * Unknown matches or players throw NoSuchElementException, which ApiExceptionHandler turns into a 404.
 */
@RestController
@RequestMapping("/api/rivals")
public class RivalsController {

    private final MatchHistorySource matches;
    private final MatchAnalyzer analyzer;
    private final CoachingService coach;

    public RivalsController(MatchHistorySource matches, MatchAnalyzer analyzer, CoachingService coach) {
        this.matches = matches;
        this.analyzer = analyzer;
        this.coach = coach;
    }

    /** The match picker. */
    @GetMapping("/players/{username}/matches")
    public List<MatchSummary> recentMatches(@PathVariable("username") String username) {
        return matches.getRecentMatches(username);
    }

    /** Full scoreboard for one match. */
    @GetMapping("/matches/{matchId}")
    public MatchDetails matchDetails(@PathVariable("matchId") String matchId) {
        return matches.getMatchDetails(matchId);
    }

    /** The computed comparisons for the player named in ?username= within that match. */
    @GetMapping("/matches/{matchId}/analysis")
    public PlayerAnalysis analysis(@PathVariable("matchId") String matchId,
                                   @RequestParam("username") String username) {
        return analyzer.analyze(matches.getMatchDetails(matchId), username);
    }

    /** Coaching feedback for the player named in ?username= within that match. */
    @PostMapping("/matches/{matchId}/feedback")
    public Feedback feedback(@PathVariable("matchId") String matchId,
                             @RequestParam("username") String username) {
        MatchDetails match = matches.getMatchDetails(matchId);
        return coach.coach(match, analyzer.analyze(match, username));
    }
}

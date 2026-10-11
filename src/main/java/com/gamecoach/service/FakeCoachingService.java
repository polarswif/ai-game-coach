package com.gamecoach.service;

import com.gamecoach.analysis.PlayerAnalysis;
import com.gamecoach.analysis.Stat;
import com.gamecoach.analysis.StatComparison;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Canned feedback for development, filled in with real numbers from the analysis.
 * Active when application.properties has gamecoach.coach.provider=fake.
 */
@Service
@ConditionalOnProperty(name = "gamecoach.coach.provider", havingValue = "fake")
public class FakeCoachingService implements CoachingService {

    @Override
    public Feedback coach(MatchDetails match, PlayerAnalysis analysis) {
        StatComparison assists = analysis.stats().get(Stat.ASSISTS);
        StatComparison deaths = analysis.stats().get(Stat.DEATHS);

        List<String> weaknesses = new ArrayList<>();
        if (analysis.accuracyPercent() != null) {
            weaknesses.add(String.format("[fake] Accuracy was %.1f%%, which is worth reviewing.",
                    analysis.accuracyPercent()));
        }
        weaknesses.add("[fake] You died " + deaths.value() + " times on " + match.map()
                + " (rank " + deaths.teamRank() + " of " + analysis.teamSize() + " on your team).");

        return new Feedback(
                List.of("[fake] You recorded " + assists.value() + " assists, rank " + assists.teamRank()
                        + " of " + analysis.teamSize() + " on your team."),
                weaknesses,
                List.of("[fake] Next match, focus on staying alive between fights."));
    }
}

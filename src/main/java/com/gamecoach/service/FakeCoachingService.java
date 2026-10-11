package com.gamecoach.service;

import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.PlayerLine;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/** Canned feedback for development. Active with the "dev" Spring profile. */
@Service
@Profile("dev")
public class FakeCoachingService implements CoachingService {

    @Override
    public Feedback coach(MatchDetails match, PlayerLine player) {
        return new Feedback(
                List.of(
                        "[fake] You recorded " + player.assists() + " assists, so you were involved in a lot of fights."),
                List.of(
                        "[fake] Accuracy was " + String.format("%.1f", player.accuracyPercent()) + "%, which is worth reviewing.",
                        "[fake] You died " + player.deaths() + " times on " + match.map() + "."),
                List.of(
                        "[fake] Next match, focus on staying alive between fights."));
    }
}

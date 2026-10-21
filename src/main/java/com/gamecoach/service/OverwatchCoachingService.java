package com.gamecoach.service;

import com.gamecoach.analysis.OverwatchPlayerAnalysis;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.OverwatchMatch;

/** Writes coaching for one player in one Overwatch match. */
public interface OverwatchCoachingService {

    /**
     * @throws CoachingUnavailableException when the coach can't answer right now
     */
    Feedback coach(OverwatchMatch match, OverwatchPlayerAnalysis analysis);
}

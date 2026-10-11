package com.gamecoach.model;

import java.util.List;

/** Coaching result for one player in one match. Lists can be any length. */
public record Feedback(
        List<String> strengths,
        List<String> weaknesses,
        List<String> advice) {
}

package com.gamecoach.service.claude;

import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardToolInputTest {

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();

    @Test
    void everyRealMatchSurvivesTheRoundTrip() {
        for (MatchSummary summary : source.getRecentMatches("anyone")) {
            MatchDetails original = source.getMatchDetails(summary.matchId());
            List<String> warnings = new ArrayList<>();
            MatchDetails read = ScoreboardToolInput.toMatch(ScoreboardInputs.of(original), original.matchId(), warnings);
            assertEquals(original, read, summary.matchId());
            assertEquals(List.of(), warnings, summary.matchId());
        }
    }

    @Test
    void sloppyValuesAreReadOrFlaggedInsteadOfFailing() {
        MatchDetails match = source.getMatchDetails("10378920354");
        Map<String, Object> input = ScoreboardInputs.of(match);
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) ((List<Object>) input.get("players")).get(0);
        row.put("damage", "11,418");             // a number with a comma
        row.put("accuracyPercent", "33%");
        row.put("role", "vanguard");             // lower case
        row.remove("healing");                   // missing
        row.put("medals", List.of("Most KOs", "shiny"));
        input.put("duration", "16m 7s");

        List<String> warnings = new ArrayList<>();
        PlayerLine p = ScoreboardToolInput.toMatch(input, "x", warnings).players().get(0);
        assertEquals(11418, p.damage());
        assertEquals(33.0, p.accuracyPercent());
        assertEquals(Role.VANGUARD, p.role());
        assertEquals(0, p.healing());
        assertEquals(1, p.medals().size());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("healing wasn't read")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("unknown medal \"shiny\"")));
    }

    @Test
    void durationsInSeveralFormats() {
        assertEquals(Duration.ofMinutes(16).plusSeconds(7), ScoreboardToolInput.duration("16:07"));
        assertEquals(Duration.ofMinutes(62).plusSeconds(30), ScoreboardToolInput.duration("1:02:30"));
        assertEquals(Duration.ofMinutes(13).plusSeconds(22), ScoreboardToolInput.duration("13 m 22 s"));
        assertNull(ScoreboardToolInput.duration("soon"));
        assertNull(ScoreboardToolInput.duration(null));
    }

    @Test
    void anUnreadableLengthBecomesZeroWithAWarning() {
        Map<String, Object> input = ScoreboardInputs.of(source.getMatchDetails("10378920354"));
        input.put("duration", "??");
        List<String> warnings = new ArrayList<>();
        assertEquals(Duration.ZERO, ScoreboardToolInput.toMatch(input, "x", warnings).duration());
        assertTrue(warnings.get(0).contains("match length"));
    }
}

package com.gamecoach.service.claude;

import com.gamecoach.OverwatchMatchFixtures;
import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.analysis.OverwatchHeroNotesCatalog;
import com.gamecoach.analysis.OverwatchMatchAnalyzer;
import com.gamecoach.analysis.OverwatchPlayerAnalysis;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.service.claude.ClaudeApi.ContentBlock;
import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;
import com.gamecoach.service.claude.ClaudeApi.Usage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeOverwatchCoachTest {

    private static class FakeClient implements ClaudeClient {
        final List<MessagesRequest> requests = new ArrayList<>();

        @Override
        public MessagesResponse send(MessagesRequest request) {
            requests.add(request);
            return new MessagesResponse("msg", "claude-sonnet-5-5", List.of(new ContentBlock("tool_use", null,
                    CoachingPrompt.TOOL_NAME, Map.of(
                            "strengths", List.of("21 eliminations, the most in the lobby."),
                            "weaknesses", List.of("Watch your Pummel accuracy."),
                            "advice", List.of("Track Vortex kills next game."),
                            "matchContext", "A one-sided win."))), "tool_use", new Usage(3000, 600));
        }
    }

    private final FakeClient client = new FakeClient();
    private final ClaudeOverwatchCoach coach = new ClaudeOverwatchCoach(client,
            new ClaudeProperties("test-key", null, null, null, null), new OverwatchHeroNotesCatalog());
    private final OverwatchMatch match = OverwatchMatchFixtures.colosseo();
    private final OverwatchPlayerAnalysis analysis =
            new OverwatchMatchAnalyzer(new OverwatchHeroCatalog()).analyze(match, null);

    @Test
    void theMessageHasTheMatchTheHeroStatsAndTheScoreboard() {
        String text = coach.buildRequest(match, analysis).messages().get(0).text();
        assertTrue(text.contains("Mode: Push | Map: Colosseo | Length: 5:30 | Result for the coached player's team: WIN"));
        assertTrue(text.contains("Final score (coached player's team first): 1 vs 0"));
        assertTrue(text.contains("in Push it only says who pushed further"));
        assertTrue(text.contains("the final score isn't a round count in this mode"));
        assertTrue(text.contains("ONE-SIDED"));
        assertTrue(text.contains("Heroes played: Ramattra (100%, 5:29)"));
        assertTrue(text.contains("- Ramattra: Pummel kills 10 (per 10 min this match: 18.21)"));
        assertTrue(text.contains("Eliminations | 21 | 38.2 | 32.8% | 1 | 1 | 3"));
        assertTrue(text.contains("PLAYERONE [COACHED PLAYER] | Tank | Ramattra | 21/0/1 | Dmg 7013 | Heal 105 | Mit 4405"));
        assertTrue(text.contains("PLAYERSEVEN | Tank | Roadhog"));
    }

    @Test
    void theCoachedHerosNotesAreIncluded() {
        String text = coach.buildRequest(match, analysis).messages().get(0).text();
        assertTrue(text.contains("HERO NOTES (researched; how the hero plays now)"));
        assertTrue(text.contains("- Ramattra: Pummel's punches pierce enemies and barriers"));
        assertTrue(!text.contains("- Roadhog:"));                         // only the coached player's heroes
    }

    @Test
    void itAnswersThroughTheSharedCoachingToolAndCaches() {
        Feedback first = coach.coach(match, analysis);
        Feedback second = coach.coach(match, analysis);
        assertEquals("A one-sided win.", first.matchContext());
        assertEquals(first, second);
        assertEquals(1, client.requests.size());
        assertEquals(CoachingPrompt.TOOL_NAME, client.requests.get(0).tools().get(0).name());
        assertTrue(client.requests.get(0).system().contains("expert Overwatch coach"));
    }
}

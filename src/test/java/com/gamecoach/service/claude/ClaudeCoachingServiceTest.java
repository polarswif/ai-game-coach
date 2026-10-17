package com.gamecoach.service.claude;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.MatchAnalyzer;
import com.gamecoach.analysis.PlayerAnalysis;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.service.CoachingUnavailableException;
import com.gamecoach.service.claude.ClaudeApi.ContentBlock;
import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;
import com.gamecoach.service.claude.ClaudeApi.Usage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Claude coach with a fake ClaudeClient that records each request and returns a canned
 * reply. No network, no key, no cost.
 */
class ClaudeCoachingServiceTest {

    private static final Feedback CANNED = new Feedback(
            List.of("You led the lobby in assists with 33."),
            List.of("Your 20% accuracy was the lowest on your team."),
            List.of("Stay closer to your Vanguards between fights."));

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final MatchAnalyzer analyzer = new MatchAnalyzer(new HeroRoleCatalog(), new TeamUpCatalog());
    private final MatchDetails player29Match = source.getMatchDetails("10378920354");
    private final MatchDetails deadpoolMatch = source.getMatchDetails("5518188_1770352206_1245014_11001_11");
    private final MatchDetails teamUpMatch = source.getMatchDetails("player10-convoy-2823");
    private final ClaudeProperties properties = new ClaudeProperties("test-key", null, null, null, null);

    /** A fake client: remembers every request and answers with whatever reply the test set. */
    private static class FakeClient implements ClaudeClient {
        final List<MessagesRequest> requests = new ArrayList<>();
        MessagesResponse reply = toolReply(CANNED, "tool_use");
        RuntimeException error;

        @Override
        public MessagesResponse send(MessagesRequest request) {
            requests.add(request);
            if (error != null) {
                throw error;
            }
            return reply;
        }
    }

    private static MessagesResponse toolReply(Feedback input, String stopReason) {
        return new MessagesResponse("msg_test", "claude-sonnet-5-5",
                List.of(new ContentBlock("tool_use", null, CoachingPrompt.TOOL_NAME, input)),
                stopReason, new Usage(1500, 300));
    }

    private final FakeClient client = new FakeClient();
    private final ClaudeCoachingService coach = new ClaudeCoachingService(client, properties);

    private String promptFor(MatchDetails match, PlayerAnalysis analysis) {
        return coach.buildRequest(match, analysis).messages().get(0).content();
    }

    // ---- The request ----

    @Test
    void requestUsesTheSettingsAndForcesTheTool() {
        MessagesRequest request = coach.buildRequest(player29Match, analyzer.analyze(player29Match, (String) null));
        assertEquals(ClaudeProperties.DEFAULT_MODEL, request.model());
        assertEquals(2000, request.maxTokens());
        assertEquals("tool", request.toolChoice().type());
        assertEquals(CoachingPrompt.TOOL_NAME, request.toolChoice().name());
        assertEquals(CoachingPrompt.TOOL_NAME, request.tools().get(0).name());
        assertEquals("user", request.messages().get(0).role());
        assertTrue(request.system().contains("Ranks: 1 is best"));
    }

    @Test
    void promptCarriesTheAnalysisAndTheScoreboard() {
        String prompt = promptFor(player29Match, analyzer.analyze(player29Match, (String) null));
        assertTrue(prompt.contains("Name: Player29 | Role: Strategist"));
        assertTrue(prompt.contains("Rocket Raccoon (Strategist, last played)"));
        assertTrue(prompt.contains("Result for the coached player's team: WIN"));
        assertTrue(prompt.contains("Length: 16:07"));
        assertTrue(prompt.contains("Team composition (Vanguards-Duelists-Strategists): 2-2-2"));
        assertTrue(prompt.contains("Medals: most assists in the lobby"));
        assertTrue(prompt.contains("Assists | 33 |"));
        assertTrue(prompt.contains("Player29 [COACHED PLAYER] | Strategist | Rocket Raccoon | 7/7/33"));
        assertTrue(prompt.contains("Accuracy: 20%"));
        assertTrue(prompt.contains("Proficiency: nothing shown (says nothing about experience)"));
        assertTrue(prompt.contains("Team-up: not shown on this scoreboard"));
    }

    @Test
    void promptIncludesTheBetterTeamUpOption() {
        String prompt = promptFor(teamUpMatch, analyzer.analyze(teamUpMatch, "Player_16"));
        assertTrue(prompt.contains("Primal Punishment with The Punisher (not enhanced"));
        assertTrue(prompt.contains("Better option this match: Surf & Turf (Jeff the Land Shark)"));
        assertTrue(prompt.contains("Best multi-kill: 3 KOs in quick succession"));
        assertTrue(prompt.contains("Result for the coached player's team: WIN"));   // team B won that match
    }

    @Test
    void promptStatesProficiencyAsAMinimum() {
        String prompt = promptFor(deadpoolMatch, analyzer.analyze(deadpoolMatch, (String) null));
        assertTrue(prompt.contains("at least Warrior (level 35) on Cloak & Dagger; a minimum only"));
    }

    @Test
    void aHiddenNameRowIsStillMarkedAsTheCoachedPlayer() {
        PlayerAnalysis deadpool = analyzer.analyze(deadpoolMatch, deadpoolMatch.players().get(5));
        String prompt = promptFor(deadpoolMatch, deadpool);
        assertTrue(prompt.contains("Name: (hidden) | Role: Strategist | MVP"));
        assertTrue(prompt.contains("(hidden) [COACHED PLAYER] | Strategist | Deadpool"));
        assertEquals(1, prompt.split("\\[COACHED PLAYER]", -1).length - 1);   // exactly one row marked
    }

    // ---- The reply ----

    @Test
    void theToolCallBecomesFeedback() {
        assertEquals(CANNED, coach.coach(player29Match, analyzer.analyze(player29Match, (String) null)));
    }

    @Test
    void askingTwiceForTheSamePlayerCallsTheApiOnce() {
        coach.coach(player29Match, analyzer.analyze(player29Match, (String) null));
        coach.coach(player29Match, analyzer.analyze(player29Match, "player29"));          // same player, any case
        assertEquals(1, client.requests.size());
        coach.coach(player29Match, analyzer.analyze(player29Match, "player34"));            // a different player
        assertEquals(2, client.requests.size());
    }

    @Test
    void aCutOffReplyIsReported() {
        client.reply = toolReply(CANNED, "max_tokens");
        CoachingUnavailableException e = assertThrows(CoachingUnavailableException.class,
                () -> coach.coach(player29Match, analyzer.analyze(player29Match, (String) null)));
        assertTrue(e.getMessage().contains("max-tokens"));
    }

    @Test
    void aReplyWithoutUsableCoachingIsRejectedAndNotCached() {
        client.reply = new MessagesResponse("msg_test", "claude-sonnet-5-5",
                List.of(new ContentBlock("text", "Here is some prose instead.", null, null)), "end_turn", null);
        PlayerAnalysis analysis = analyzer.analyze(player29Match, (String) null);
        assertThrows(CoachingUnavailableException.class, () -> coach.coach(player29Match, analysis));

        client.reply = toolReply(new Feedback(List.of("x"), List.of(), List.of("y")), "tool_use");  // empty weaknesses
        assertThrows(CoachingUnavailableException.class, () -> coach.coach(player29Match, analysis));

        client.reply = toolReply(CANNED, "tool_use");
        assertEquals(CANNED, coach.coach(player29Match, analysis));   // failures weren't cached
    }

    @Test
    void apiErrorsBecomeClearMessagesWithoutTheKey() {
        client.error = new ClaudeApiException(401, "authentication_error", "invalid x-api-key", null);
        CoachingUnavailableException e = assertThrows(CoachingUnavailableException.class,
                () -> coach.coach(player29Match, analyzer.analyze(player29Match, (String) null)));
        assertTrue(e.getMessage().contains("ANTHROPIC_API_KEY"));
        assertFalse(e.getMessage().contains("test-key"));

        client.error = new ClaudeApiException(0, "network_error", "timed out", null);
        assertTrue(assertThrows(CoachingUnavailableException.class,
                () -> coach.coach(player29Match, analyzer.analyze(player29Match, "player34")))
                .getMessage().contains("Couldn't reach"));
    }

    // ---- Settings ----

    @Test
    void settingsHaveDefaultsAndNeverPrintTheKey() {
        ClaudeProperties p = new ClaudeProperties("sk-ant-secret", "", null, null, null);
        assertEquals(ClaudeProperties.DEFAULT_MODEL, p.model());
        assertEquals("https://api.anthropic.com", p.baseUrl());
        assertEquals(60, p.timeout().toSeconds());
        assertFalse(p.toString().contains("sk-ant-secret"));
        assertFalse(new ClaudeProperties("  ", null, null, null, null).hasApiKey());
    }
}

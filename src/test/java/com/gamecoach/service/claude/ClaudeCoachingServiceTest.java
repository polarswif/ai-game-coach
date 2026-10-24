package com.gamecoach.service.claude;

import com.gamecoach.service.CoachingArchive;
import com.gamecoach.analysis.HeroNotesCatalog;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            List.of("Stay closer to your Vanguards between fights."),
            "A close match, so this coaching carries full weight.");

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

    private static MessagesResponse toolReply(Feedback f, String stopReason) {
        // The tool input arrives as a Map, the way Jackson parses a JSON object.
        Map<String, Object> input = new HashMap<>();
        input.put("strengths", f.strengths());
        input.put("weaknesses", f.weaknesses());
        input.put("advice", f.advice());
        input.put("matchContext", f.matchContext());
        return new MessagesResponse("msg_test", "claude-sonnet-5-5",
                List.of(new ContentBlock("tool_use", null, CoachingPrompt.TOOL_NAME, input)),
                stopReason, new Usage(1500, 300));
    }

    private final FakeClient client = new FakeClient();
    private final ClaudeCoachingService coach = new ClaudeCoachingService(client, properties, new HeroNotesCatalog(), CoachingArchive.inMemory());

    private String promptFor(MatchDetails match, PlayerAnalysis analysis) {
        return coach.buildRequest(match, analysis).messages().get(0).text();
    }

    // ---- The request ----

    @Test
    void requestUsesTheSettingsAndOffersTheTool() {
        MessagesRequest request = coach.buildRequest(player29Match, analyzer.analyze(player29Match, (String) null));
        assertEquals(ClaudeProperties.DEFAULT_MODEL, request.model());
        assertEquals(4000, request.maxTokens());
        assertEquals("auto", request.toolChoice().type());      // newer models reject a forced tool
        assertEquals(null, request.toolChoice().name());
        assertEquals(CoachingPrompt.TOOL_NAME, request.tools().get(0).name());
        assertEquals("user", request.messages().get(0).role());
        assertTrue(request.system().contains("Ranks: 1 is best"));
        assertTrue(request.system().contains("The match result is context, never a strength or weakness"));
        assertTrue(request.system().contains("Each stat or point goes in only one place"));
        assertTrue(request.system().contains("A mid-pack or bottom-half number is never a strength"));
        assertTrue(request.system().contains("Never base a weakness on a gap with a player on a different hero"));
    }

    @Test
    void promptSaysHowCloseTheMatchWas() {
        assertTrue(promptFor(player29Match, analyzer.analyze(player29Match, (String) null))
                .contains("Verdict: CLOSE (hard-fought)"));
        assertTrue(promptFor(deadpoolMatch, analyzer.analyze(deadpoolMatch, deadpoolMatch.players().get(5)))
                .contains("Verdict: ONE-SIDED in the coached player's team's favor (a likely stomp win)"));
        String loss = promptFor(teamUpMatch, analyzer.analyze(teamUpMatch, "Player10<3"));
        assertTrue(loss.contains("Verdict: CLEAR EDGE to the enemy team in fights"));
        assertTrue(loss.contains("Deaths: coached player's team 118, enemy team 72 (ratio 1.64"));
        assertTrue(loss.contains("Based on: death totals (the round score wasn't recorded)"));
    }

    @Test
    void aKnownScoreDecidesTheVerdict() {
        MatchDetails swept = teamUpMatch.withScore(new com.gamecoach.model.RoundScore(0, 2));
        String prompt = promptFor(swept, analyzer.analyze(swept, "Player10<3"));
        assertTrue(prompt.contains("Verdict: ONE-SIDED: a sweep loss"));
        assertTrue(prompt.contains("Based on: the round score, 0-2 for the coached player's team"));
    }

    @Test
    void theToolAsksForMatchContext() {
        assertTrue(CoachingPrompt.tool().inputSchema().toString().contains("matchContext"));
        assertTrue(CoachingPrompt.systemPrompt().contains("ONE-SIDED loss"));
    }

    @Test
    void promptIncludesNotesForTheHeroesPlayed() {
        String prompt = promptFor(player29Match, analyzer.analyze(player29Match, (String) null));
        assertTrue(prompt.contains("HERO NOTES (confirmed by players)"));
        assertTrue(prompt.contains("- Rocket Raccoon: His gun is much more effective at short range"));
    }

    @Test
    void promptLeavesOutTheNotesSectionWhenThereAreNone() {
        // Every real hero has notes now, so use a catalog that only knows Magneto.
        ClaudeCoachingService noNotes = new ClaudeCoachingService(client, properties,
                HeroNotesCatalog.fromText("Magneto|A shield tank."), CoachingArchive.inMemory());
        PlayerAnalysis rocket = analyzer.analyze(player29Match, (String) null);
        assertFalse(noNotes.buildRequest(player29Match, rocket).messages().get(0).text().contains("HERO NOTES"));
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
    void aRoughLossMayHaveNoStrengths() {
        Feedback honest = new Feedback(List.of(), List.of("Six deaths, most on your team."), List.of("Play closer to your tank."), "A one-sided loss.");
        client.reply = toolReply(honest, "tool_use");
        assertEquals(honest, coach.coach(player29Match, analyzer.analyze(player29Match, (String) null)));
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

        client.reply = toolReply(new Feedback(List.of("x"), List.of("w"), List.of(), null), "tool_use");  // no advice
        assertThrows(CoachingUnavailableException.class, () -> coach.coach(player29Match, analysis));
        client.reply = toolReply(new Feedback(List.of(), List.of(), List.of("y"), null), "tool_use");     // nothing to say
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

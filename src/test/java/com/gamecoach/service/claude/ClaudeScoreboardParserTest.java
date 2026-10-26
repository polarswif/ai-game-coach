package com.gamecoach.service.claude;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.RoundScore;
import com.gamecoach.parsing.ParsedScoreboard;
import com.gamecoach.parsing.ScoreboardUnreadableException;
import com.gamecoach.parsing.ScoreboardValidator;
import com.gamecoach.parsing.Screenshot;
import com.gamecoach.service.CoachingUnavailableException;
import com.gamecoach.service.claude.ClaudeApi.ContentBlock;
import com.gamecoach.service.claude.ClaudeApi.ImagePart;
import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;
import com.gamecoach.service.claude.ClaudeApi.TextPart;
import com.gamecoach.service.claude.ClaudeApi.Usage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The screenshot reader with a fake ClaudeClient: no network, no key, no cost. */
class ClaudeScoreboardParserTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    private final MatchDetails player29 = new FakeMatchHistorySource().getMatchDetails("10378920354");
    private final HeroRoleCatalog roles = new HeroRoleCatalog();

    private static class FakeClient implements ClaudeClient {
        final List<MessagesRequest> requests = new ArrayList<>();
        MessagesResponse reply;
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

    private final FakeClient client = new FakeClient();
    private final ClaudeScoreboardParser parser = new ClaudeScoreboardParser(client,
            new ClaudeProperties("test-key", null, null, null, null), roles,
            new ScoreboardValidator(roles, new TeamUpCatalog()), new HeroPortraitSheet(), new TeamUpCatalog());

    private static MessagesResponse reply(Map<String, Object> input) {
        return new MessagesResponse("msg", "claude-sonnet-5-5",
                List.of(new ContentBlock("tool_use", null, ScoreboardPrompt.TOOL_NAME, input)), "tool_use", new Usage(3000, 2500));
    }

    @Test
    void sendsTheImageFirstThenTheInstructions() {
        MessagesRequest request = parser.buildRequest(new Screenshot(PNG, "image/png"));
        List<?> parts = (List<?>) request.messages().get(0).content();
        ImagePart image = assertInstanceOf(ImagePart.class, parts.get(0));
        assertEquals("image/png", image.source().mediaType());
        assertEquals(Base64.getEncoder().encodeToString(PNG), image.source().data());
        assertEquals(ScoreboardPrompt.SHEET_LABEL, ((TextPart) parts.get(1)).text());   // the portrait chart
        assertInstanceOf(ImagePart.class, parts.get(2));
        TextPart text = assertInstanceOf(TextPart.class, parts.get(parts.size() - 1));
        assertTrue(text.text().contains("Rocket Raccoon"));                  // the exact hero names
        assertEquals(ScoreboardPrompt.TOOL_NAME, request.tools().get(0).name());
        assertEquals("auto", request.toolChoice().type());
        assertEquals(ClaudeScoreboardParser.MAX_TOKENS, request.maxTokens());
    }

    @Test
    void aCleanReadComesBackWithNoWarnings() {
        client.reply = reply(ScoreboardInputs.of(player29));
        ParsedScoreboard parsed = parser.parse(new Screenshot(PNG, "image/png"), "upload-1", null);
        assertEquals(player29.players(), parsed.match().players());
        assertEquals("upload-1", parsed.match().matchId());
        assertEquals(List.of(), parsed.warnings());
    }

    @Test
    void theTypedScoreIsAttachedAndChecked() {
        client.reply = reply(ScoreboardInputs.of(player29));            // Player29's team won
        ParsedScoreboard parsed = parser.parse(new Screenshot(PNG, "image/png"), "upload-2", new RoundScore(1, 2));
        assertEquals(new RoundScore(1, 2), parsed.match().score());
        assertTrue(parsed.warnings().stream().anyMatch(w -> w.contains("doesn't match the VICTORY banner")));
    }

    @Test
    void theModelsDoubtsAndFailedChecksAreBothReported() {
        Map<String, Object> input = ScoreboardInputs.of(player29);
        input.put("warnings", List.of("Row 4: the name is partly covered."));
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) ((List<Object>) input.get("players")).get(0);
        row.put("hero", "Hulkk");                                   // misread portrait
        client.reply = reply(input);

        List<String> warnings = parser.parse(new Screenshot(PNG, "image/png"), "u", null).warnings();
        assertEquals("Row 4: the name is partly covered.", warnings.get(0));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("\"Hulkk\" isn't in the hero table")));
    }

    @Test
    void notAScoreboardIsA422() {
        client.reply = reply(Map.of("players", List.of(), "warnings", List.of("The image shows a cat.")));
        ScoreboardUnreadableException e = assertThrows(ScoreboardUnreadableException.class,
                () -> parser.parse(new Screenshot(PNG, "image/png"), "u", null));
        assertTrue(e.getMessage().contains("cat"));

        client.reply = new MessagesResponse("msg", "m", List.of(new ContentBlock("text", "Sorry", null, null)), "end_turn", null);
        assertThrows(ScoreboardUnreadableException.class, () -> parser.parse(new Screenshot(PNG, "image/png"), "u", null));
    }

    @Test
    void apiFailuresUseTheSameMessagesAsTheCoach() {
        client.error = new ClaudeApiException(401, "authentication_error", "invalid x-api-key", null);
        CoachingUnavailableException e = assertThrows(CoachingUnavailableException.class,
                () -> parser.parse(new Screenshot(PNG, "image/png"), "u", null));
        assertTrue(e.getMessage().contains("ANTHROPIC_API_KEY"));
    }

    @Test
    void unsureRowsComeBackAsDoubtsWithValidAlternatives() {
        Map<String, Object> input = ScoreboardInputs.of(player29);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) input.get("players");
        String hero = roles.officialName((String) rows.get(2).get("hero")).orElseThrow();
        List<String> others = roles.heroNames().stream().map(n -> roles.officialName(n).orElseThrow())
                .distinct().filter(n -> !n.equals(hero)).limit(2).toList();
        rows.get(2).put("portraitLooks", "blond hair, red jacket");
        rows.get(2).put("heroSure", false);
        rows.get(2).put("heroAlternatives", List.of(hero, "Not A Hero", others.get(0).toLowerCase(), others.get(1)));
        rows.get(5).put("heroSure", true);
        client.reply = reply(input);

        ParsedScoreboard parsed = parser.parse(new Screenshot(PNG, "image/png"), "u", null);
        assertEquals(1, parsed.heroDoubts().size());
        var doubt = parsed.heroDoubts().get(0);
        assertEquals(2, doubt.index());
        assertEquals(hero, doubt.hero());
        assertEquals(others, doubt.alternatives());      // own hero and unknown names dropped, spelling fixed
        assertEquals("blond hair, red jacket", doubt.seen());
        assertEquals(player29.players(), parsed.match().players());   // the match itself is unchanged
    }

    @Test
    void aRealScreenshotAlsoGetsAZoomedPortraitStrip() throws Exception {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(2000, 1125,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", png);

        MessagesRequest request = parser.buildRequest(new Screenshot(png.toByteArray(), "image/png"));
        List<?> parts = (List<?>) request.messages().get(0).content();
        assertEquals(6, parts.size());                   // screenshot, zoom label + zoom, sheet label + sheet, instructions
        assertEquals(ScoreboardPrompt.ZOOM_LABEL, ((TextPart) parts.get(1)).text());
        ImagePart zoom = assertInstanceOf(ImagePart.class, parts.get(2));
        assertEquals("image/jpeg", zoom.source().mediaType());
        java.awt.image.BufferedImage strip = javax.imageio.ImageIO.read(
                new java.io.ByteArrayInputStream(Base64.getDecoder().decode(zoom.source().data())));
        assertTrue((long) strip.getWidth() * strip.getHeight() <= PortraitZoom.MAX_PIXELS);
        assertTrue(strip.getHeight() > 1125);                            // enlarged, not shrunk
        assertTrue(Math.abs((double) strip.getWidth() / strip.getHeight() - 720.0 / 1125) < 0.01);
        assertEquals(ScoreboardPrompt.SHEET_LABEL, ((TextPart) parts.get(3)).text());
        assertTrue(((TextPart) parts.get(5)).text().contains("HERO NAMES"));
    }

    @Test
    void withTheSheetOnlyUnsureRowsAreDescribed() {
        @SuppressWarnings("unchecked")
        Map<String, Object> players = (Map<String, Object>) ScoreboardPrompt.tool().inputSchema().get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) ((Map<String, Object>) players.get("players")).get("items");
        @SuppressWarnings("unchecked")
        List<String> keys = List.copyOf(((Map<String, Object>) row.get("properties")).keySet());
        assertEquals(keys.indexOf("hero") + 1, keys.indexOf("heroSure"));
        assertTrue(keys.indexOf("portraitLooks") > keys.indexOf("heroSure"));
        List<?> required = (List<?>) row.get("required");
        assertTrue(required.contains("heroSure"));
        assertTrue(!required.contains("portraitLooks"));
    }

    @Test
    void theInstructionsListEachHerosTeamUpPartners() {
        MessagesRequest request = parser.buildRequest(new Screenshot(PNG, "image/png"));
        List<?> parts = (List<?>) request.messages().get(0).content();
        String text = ((TextPart) parts.get(parts.size() - 1)).text();
        assertTrue(text.contains("TEAM-UP PARTNERS"));
        String strange = text.lines().filter(l -> l.startsWith("Doctor Strange: ")).findFirst().orElseThrow();
        assertEquals("Doctor Strange: Hulk, Invisible Woman", strange);
        assertTrue(text.lines().filter(l -> l.startsWith("Deadpool: ")).count() == 1);
    }

    @Test
    void aCostumeLabelFromTheSheetBecomesTheHerosName() {
        Map<String, Object> input = ScoreboardInputs.of(player29);
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) ((List<Object>) input.get("players")).get(7);
        row.put("hero", "Loki (Lady Loki costume)");
        row.put("role", "STRATEGIST");
        client.reply = reply(input);
        ParsedScoreboard parsed = parser.parse(new Screenshot(PNG, "image/png"), "u", null);
        assertEquals("Loki", parsed.match().players().get(7).lastHero());
        assertTrue(parsed.warnings().stream().noneMatch(w -> w.contains("isn't in the hero table")));
    }
}

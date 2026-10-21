package com.gamecoach.service.claude;

import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchResult;
import com.gamecoach.model.OverwatchRole;
import com.gamecoach.parsing.OverwatchMatchValidator;
import com.gamecoach.parsing.OverwatchScreens;
import com.gamecoach.parsing.ParsedOverwatchMatch;
import com.gamecoach.parsing.ScoreboardUnreadableException;
import com.gamecoach.parsing.Screenshot;
import com.gamecoach.service.claude.ClaudeApi.ContentBlock;
import com.gamecoach.service.claude.ClaudeApi.ImagePart;
import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;
import com.gamecoach.service.claude.ClaudeApi.TextPart;
import com.gamecoach.service.claude.ClaudeApi.Usage;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Overwatch reader with a fake ClaudeClient: no network, no key, no cost. */
class ClaudeOverwatchReaderTest {

    private static final Screenshot PNG = new Screenshot(new byte[] {(byte) 0x89, 'P', 'N', 'G', 1}, "image/png");
    private static final Screenshot JPG = new Screenshot(new byte[] {(byte) 0xFF, (byte) 0xD8, 2}, "image/jpeg");

    private static class FakeClient implements ClaudeClient {
        final List<MessagesRequest> requests = new ArrayList<>();
        MessagesResponse reply;

        @Override
        public MessagesResponse send(MessagesRequest request) {
            requests.add(request);
            return reply;
        }
    }

    private final FakeClient client = new FakeClient();
    private final OverwatchHeroCatalog heroes = new OverwatchHeroCatalog();
    private final ClaudeOverwatchReader reader = new ClaudeOverwatchReader(client,
            new ClaudeProperties("test-key", null, null, null, null), heroes, new OverwatchMatchValidator(heroes));

    private static MessagesResponse reply(Map<String, Object> input) {
        return new MessagesResponse("msg", "claude-sonnet-5-5",
                List.of(new ContentBlock("tool_use", null, OverwatchReaderPrompt.TOOL_NAME, input)), "tool_use", new Usage(6000, 2000));
    }

    private static Map<String, Object> row(String name, String team, String role, String hero, Object e, int a, int d,
                                           int dmg, int heal, int mit, boolean uploader) {
        Map<String, Object> r = new HashMap<>();
        r.put("name", name);
        r.put("team", team);
        r.put("role", role);
        r.put("hero", hero);
        r.put("eliminations", e);
        r.put("assists", a);
        r.put("deaths", d);
        r.put("damage", dmg);
        r.put("healing", heal);
        r.put("mitigation", mit);
        r.put("uploader", uploader);
        return r;
    }

    @Test
    void eachScreenIsLabeledBeforeItsImageAndTheHeroNamesComeLast() {
        MessagesRequest request = reader.buildRequest(new OverwatchScreens(PNG, JPG, List.of(PNG)));
        List<?> parts = (List<?>) request.messages().get(0).content();
        assertEquals(7, parts.size());                                   // 3 x (label, image) + instructions
        assertEquals("TEAMS screen:", ((TextPart) parts.get(0)).text());
        assertInstanceOf(ImagePart.class, parts.get(1));
        assertEquals("SUMMARY screen:", ((TextPart) parts.get(2)).text());
        assertEquals("image/jpeg", ((ImagePart) parts.get(3)).source().mediaType());
        assertEquals("PERSONAL screen 1:", ((TextPart) parts.get(4)).text());
        String instructions = ((TextPart) parts.get(6)).text();
        assertTrue(instructions.contains("TEAMS, SUMMARY, 1 PERSONAL"));
        assertTrue(instructions.contains("Ramattra"));
        assertEquals(ClaudeOverwatchReader.MAX_TOKENS, request.maxTokens());
        assertEquals(OverwatchReaderPrompt.TOOL_NAME, request.tools().get(0).name());
    }

    @Test
    void theToolReplyBecomesACheckedMatch() {
        Map<String, Object> input = new HashMap<>();
        input.put("result", "VICTORY");
        input.put("finalScoreYours", 1);
        input.put("finalScoreTheirs", 0);
        input.put("map", "Colosseo");
        input.put("mode", "Push");
        input.put("gameLength", "5:30");
        input.put("yourHeroes", List.of(Map.of("hero", "Ramattra", "percentPlayed", 100, "playTime", "05:29")));
        input.put("heroStats", List.of(Map.of("hero", "Ramattra", "stats", List.of(
                Map.of("label", "Pummel kills", "value", "10", "per10", "18.21")))));
        input.put("players", List.of(
                row("PLAYERONE", "A", "TANK", "Ramattra", "21", 0, 1, 7013, 105, 4405, true),
                row("PLAYERSEVEN", "B", "UNKNOWN", "Roadhog", null, 0, 6, 4337, 1123, 3829, false)));
        input.put("warnings", List.of("Row 2: portrait unclear."));
        client.reply = reply(input);

        ParsedOverwatchMatch parsed = reader.read(new OverwatchScreens(PNG, PNG, List.of()), "ow-1");
        OverwatchMatch m = parsed.match();
        assertEquals(OverwatchResult.VICTORY, m.result());
        assertEquals(Duration.ofSeconds(330), m.duration());
        assertEquals(1, m.score().teamA());
        assertEquals(Duration.ofSeconds(329), m.yourHeroes().get(0).playTime());
        assertEquals("18.21", m.heroStats().get(0).stats().get(0).per10());
        assertEquals(21, m.players().get(0).eliminations());              // "21" as text still reads
        assertNull(m.players().get(1).role());                             // UNKNOWN icon → null
        assertTrue(parsed.warnings().contains("Row 2: portrait unclear."));
        assertTrue(parsed.warnings().stream().anyMatch(w -> w.contains("eliminations wasn't read")));
        assertTrue(parsed.warnings().stream().anyMatch(w -> w.contains("Read 1 players on your team")));
        assertEquals(OverwatchRole.TANK, m.players().get(0).role());
    }

    @Test
    void noRowsMeansUnreadable() {
        client.reply = reply(Map.of("players", List.of(), "warnings", List.of("This is the Overwatch main menu.")));
        ScoreboardUnreadableException e = assertThrows(ScoreboardUnreadableException.class,
                () -> reader.read(new OverwatchScreens(PNG, null, List.of()), "ow-2"));
        assertTrue(e.getMessage().contains("main menu"));
    }
}

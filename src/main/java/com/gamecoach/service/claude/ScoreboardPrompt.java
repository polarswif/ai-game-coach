package com.gamecoach.service.claude;

import com.gamecoach.model.Medal;
import com.gamecoach.model.PortraitStyle;
import com.gamecoach.model.ProficiencyBadge;
import com.gamecoach.service.claude.ClaudeApi.Tool;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What Claude reads when it transcribes a scoreboard screenshot: the instructions, the exact hero
 * names to use, and the submit_scoreboard tool whose input becomes MatchDetails.
 */
final class ScoreboardPrompt {

    static final String TOOL_NAME = "submit_scoreboard";

    private ScoreboardPrompt() {
    }

    static final String ZOOM_LABEL = "PORTRAITS (zoomed):";
    static final String SHEET_LABEL = "HERO PORTRAITS (reference, not part of this match):";

    /** How much of the screenshot's width the zoom keeps: role icons, portraits and names. */
    static final double ZOOM_FRACTION = 0.36;

    static String systemPrompt() {
        return """
                You transcribe Marvel Rivals end-of-match scoreboard screenshots into exact data. Accuracy \
                matters more than completeness: a wrong number produces wrong coaching, while a gap you flag \
                gets fixed by the player.

                Layout:
                - The banner at the top says VICTORY or DEFEAT for the uploader's team. The match length is \
                in the top-right corner (for example 16:07). A replay or match ID may be printed at the bottom.
                - The uploader's team is the top half (team "A"); the enemy team is the bottom half (team "B").
                - The uploader's own row is highlighted. Mark it uploader=true.
                - Each row, left to right: role icon (Vanguard, Duelist or Strategist), hero portrait (the \
                last hero the player used), a small team-up box beside the portrait on newer scoreboards, the \
                player name, then K / D / A (KOs, deaths, assists), final hits, damage, damage blocked, \
                healing, accuracy, and the Medals column. MVP and SVP tags mark at most one player each.
                - Names can be hidden (blacked out or covered by a popup): use null, never guess.
                - Team-up box: it pictures the partner hero, who must be one of the row hero's TEAM-UP \
                PARTNERS (listed after HERO NAMES); compare the small face with those heroes on HERO \
                PORTRAITS. If it doesn't clearly match one of them, leave teamUpPartner null. Full color \
                means enhanced (the partner is on the team); shaded or grey means not enhanced. Omit the \
                team-up when there's no box.
                - Medals column: per-match medals, each given to the lobby leader in one stat: crossed swords \
                (most KOs), flame (most damage), shield (most damage blocked), green plus (most healing), fist \
                (most assists). A red number there is the best multi-kill (3 to 6). Leave both out if unsure.
                - Proficiency: a Lord portrait is alternate art (static Lord). An animated Lord adds a moving \
                layer: glowing eyes, energy around both hands and a darker layer behind; in a still image, \
                choose ANIMATED_LORD only when those signs are clear, otherwise LORD. A Proficiency Badge is \
                a small emblem on the bottom-left of the portrait: COUNT (cyan), WARRIOR (lavender, small \
                wings), GUARDIAN (pink, large wings), CHAMPION (red and gold flames). Use UNREADABLE for a \
                badge you can see but not identify, and the lower tier when torn between two. Most rows \
                have neither: use DEFAULT and NONE.

                A second image, PORTRAITS (zoomed), may follow: the left part of the same screenshot, \
                enlarged so the portraits are easier to see. Use it to identify heroes, matching rows by \
                order and player name; take every other value from the full screenshot.

                HERO PORTRAITS (reference) may follow: every hero's portrait icon with their name printed \
                under it. It is a lookup chart, not part of the match. Identify each row's hero by finding \
                the matching face on it: compare hair color and style, face, mask or helmet, horns, ears and \
                costume colors. The scoreboard shows a wider crop of the same art (and Lord or animated \
                Champion versions keep the same face), so match the character, not the framing. The small \
                square in a row's team-up box is drawn like these icons. A costume can change a portrait \
                (the sheet shows Lady Loki for Loki); a hero absent from the sheet can still appear.

                """ + PortraitCheck.rules(true) + """

                Rules:
                - Use hero names exactly as in the HERO NAMES list. For Deadpool, add the role from the row's \
                role icon: "Deadpool (Vanguard)", "Deadpool (Duelist)" or "Deadpool (Strategist)".
                - Copy numbers digit by digit without commas. Accuracy "—" or blank means null; 0% is 0.
                - Never invent a value. If something is unreadable, use null (or leave it out) and add a \
                warning naming the row and field.
                - List every row you can see, top half first, in screen order.
                - Always answer by calling the submit_scoreboard tool exactly once, straight away, with no \
                text before it. If the image is not a Marvel Rivals scoreboard, call it with no players and \
                a warning saying what the image shows.
                - Keep warnings short and only for real doubts; don't repeat what heroSure already says.
                """;
    }

    /** The user message text that follows the images: the exact hero names and each hero's team-up partners. */
    static String instructions(List<String> heroNames, java.util.function.Function<String, List<String>> partners) {
        StringBuilder b = new StringBuilder("Transcribe this scoreboard.\n\nHERO NAMES\n")
                .append(String.join(", ", heroNames)).append("\n\nTEAM-UP PARTNERS (the heroes a row's team-up box can show)\n");
        java.util.Set<String> listed = new java.util.HashSet<>();
        for (String hero : heroNames) {
            String key = com.gamecoach.analysis.HeroRoleCatalog.isDeadpool(hero) ? "Deadpool" : hero;
            List<String> list = partners.apply(key);
            if (listed.add(key) && !list.isEmpty()) {
                b.append(key).append(": ").append(String.join(", ", list)).append("\n");
            }
        }
        return b.toString();
    }

    static Tool tool() {
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", nullableString("Player name as shown; null when hidden."));
        player.put("team", Map.of("type", "string", "enum", List.of("A", "B"), "description", "A = top half (uploader's team)."));
        player.put("role", Map.of("type", "string", "enum", List.of("VANGUARD", "DUELIST", "STRATEGIST", "UNKNOWN")));
        player.put("hero", Map.of("type", "string", "description", "The portrait's hero, from HERO NAMES."));
        player.put("teamUpPartner", nullableString("Hero pictured in the team-up box; null when there is no box."));
        player.put("teamUpEnhanced", Map.of("type", "boolean", "description", "True when the team-up box is in full color."));
        player.put("portrait", enumOf(PortraitStyle.class, "DEFAULT unless the portrait is Lord art."));
        player.put("badge", enumOf(ProficiencyBadge.class, "NONE", "Proficiency Badge on the portrait's bottom-left; NONE when absent."));
        for (String stat : List.of("kills", "deaths", "assists", "finalHits", "damage", "blocked", "healing")) {
            player.put(stat, Map.of("type", "integer", "minimum", 0));
        }
        player.put("accuracyPercent", Map.of("type", List.of("number", "null"), "description", "Null when shown as a dash."));
        player.put("uploader", Map.of("type", "boolean"));
        player.put("mvp", Map.of("type", "boolean"));
        player.put("svp", Map.of("type", "boolean"));
        player.put("medals", Map.of("type", "array", "items", enumOf(Medal.class, null)));
        player.put("multiKill", Map.of("type", List.of("integer", "null"), "description", "The red number in the Medals column, 3 to 6; null when none."));

        Map<String, Object> playerSchema = Map.of(
                "type", "object",
                "properties", PortraitCheck.withPortraitFields(player, true),
                "required", PortraitCheck.withPortraitRequired(true, List.of("name", "team", "role", "hero", "kills",
                        "deaths", "assists", "finalHits", "damage", "blocked", "healing", "accuracyPercent", "uploader")));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("result", Map.of("type", "string", "enum", List.of("VICTORY", "DEFEAT")));
        properties.put("duration", Map.of("type", "string", "description", "Match length as shown, minutes:seconds, e.g. 16:07."));
        properties.put("mode", nullableString("Game mode if shown (Convoy, Domination, Convergence); null otherwise."));
        properties.put("map", nullableString("Map name if shown; null otherwise."));
        properties.put("replayId", nullableString("Replay or match ID if printed; null otherwise."));
        properties.put("bans", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Banned heroes if shown."));
        properties.put("players", Map.of("type", "array", "items", playerSchema));
        properties.put("warnings", Map.of("type", "array", "items", Map.of("type", "string"),
                "description", "Every value you were unsure of, naming the row and field."));

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("result", "duration", "players", "warnings"));
        return new Tool(TOOL_NAME, "Submit the transcribed scoreboard.", schema);
    }

    private static Map<String, Object> nullableString(String description) {
        return Map.of("type", List.of("string", "null"), "description", description);
    }

    private static Map<String, Object> enumOf(Class<? extends Enum<?>> type, String description) {
        return enumOf(type, null, description);
    }

    private static Map<String, Object> enumOf(Class<? extends Enum<?>> type, String extra, String description) {
        List<String> values = new java.util.ArrayList<>(Arrays.stream(type.getEnumConstants()).map(Enum::name).toList());
        if (extra != null) {
            values.add(0, extra);
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        schema.put("enum", values);
        if (description != null) {
            schema.put("description", description);
        }
        return schema;
    }
}

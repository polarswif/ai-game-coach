package com.gamecoach.service.claude;

import com.gamecoach.service.claude.ClaudeApi.Tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What Claude reads when it transcribes Overwatch match report screenshots: the layout of each
 * screen, the exact hero names to use, and the submit_overwatch_match tool whose input becomes an
 * OverwatchMatch.
 */
final class OverwatchReaderPrompt {

    static final String TOOL_NAME = "submit_overwatch_match";

    private OverwatchReaderPrompt() {
    }

    static String systemPrompt() {
        return """
                You transcribe Overwatch end-of-match report screenshots into exact data. Accuracy matters \
                more than completeness: a wrong number produces wrong coaching, while a gap you flag gets \
                fixed by the player.

                You get up to three kinds of screen, each labeled before its image:
                - TEAMS (always sent): a table with columns E (eliminations), A (assists), D (deaths), DMG \
                (hero damage), H (healing) and MIT (damage mitigated). The top block (blue) is the \
                uploader's team, team "A"; the block below VS (red) is the enemy team, team "B". Each row, \
                left to right: role icon (shield = TANK, three bullets = DAMAGE, cross = SUPPORT), hero \
                portrait (the hero the player ended the match on), player name in large capitals, and \
                sometimes a smaller line under the name. That smaller line is a cosmetic title (such as \
                "Philosopher" or "Haunted Shadow"), never a hero: put it in title. Player names can look \
                like hero names (a player can be called "REAPER" while playing someone else): the hero \
                always comes from the portrait.
                - SUMMARY: HEROES PLAYED lists the uploader's heroes by name with their percent played and \
                play time. TOTAL PERFORMANCE repeats the uploader's eliminations, assists and deaths. The \
                right panel has the map name above the picture, then VICTORY, DEFEAT or DRAW, and lines for \
                FINAL SCORE (uploader's team first, e.g. "1 VS 0"), DATE, GAME MODE and GAME LENGTH.
                - PERSONAL: a list of tabs on the left (one per hero played, plus ALL HEROES); the \
                highlighted tab is the one shown. A hero tab shows the hero's name, percent played and \
                play time, then tiles: a value, a label, and "AVG PER 10 MIN: x". Record each tile of a \
                hero tab in heroStats (label in normal capitalization, value and per-10 exactly as \
                printed, e.g. "30%", "01:28", "4"). The ALL HEROES tab repeats the uploader's row totals: \
                use it to identify and check the uploader's row, but don't add it to heroStats.

                Finding the uploader's row: when a SUMMARY or PERSONAL screen is sent, the uploader's row \
                is the team "A" row whose numbers match those totals (eliminations, assists, deaths, and \
                damage, healing and mitigation on ALL HEROES). Trust that match. With only TEAMS, the \
                uploader's row is the one highlighted brighter than the others in the top block; if you \
                can't tell, mark no row and add a warning.

                A PORTRAITS (zoomed) image may follow TEAMS: the left part of the TEAMS screen, enlarged so \
                the portraits are easier to see. Use it to identify heroes, matching rows by order and player \
                name; take every other value from the full screens.

                """ + PortraitCheck.rules(false) + """

                Rules:
                - Use hero names exactly as in the HERO NAMES list. A hero named in text on SUMMARY or \
                PERSONAL is certain (so the uploader's row can be sure when it matches); every other \
                portrait is a judgment.
                - The uploader's row portrait must be one of the heroes on SUMMARY when it's sent (the last \
                one they played).
                - Copy numbers digit by digit without commas. Never invent a value: if a number is hidden \
                (for example behind a screenshot tool's popup) or unreadable, leave it out and add a \
                warning naming the row or tile.
                - Names can be hidden or unreadable: use null, never guess.
                - List every row you can see, team A first, in screen order.
                - Always answer by calling the submit_overwatch_match tool exactly once. If the images are \
                not Overwatch match report screens, call it with no players and a warning saying what they show.
                """;
    }

    /** The text after the images: what was sent and the exact hero names. */
    static String instructions(List<String> heroNames, boolean summary, int personal) {
        return "Transcribe these screens (TEAMS" + (summary ? ", SUMMARY" : "")
                + (personal > 0 ? ", " + personal + " PERSONAL" : "") + ").\n\nHERO NAMES\n"
                + String.join(", ", heroNames) + "\n";
    }

    static Tool tool() {
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", nullableString("Player name as shown; null when hidden or unreadable."));
        player.put("title", nullableString("The small title line under the name; null when there is none."));
        player.put("team", Map.of("type", "string", "enum", List.of("A", "B"), "description", "A = top block (uploader's team)."));
        player.put("role", Map.of("type", "string", "enum", List.of("TANK", "DAMAGE", "SUPPORT", "UNKNOWN")));
        player.put("hero", Map.of("type", "string", "description", "The portrait's hero, from HERO NAMES."));
        for (String stat : List.of("eliminations", "assists", "deaths", "damage", "healing", "mitigation")) {
            player.put(stat, Map.of("type", "integer", "minimum", 0));
        }
        player.put("uploader", Map.of("type", "boolean"));
        Map<String, Object> playerSchema = Map.of(
                "type", "object",
                "properties", PortraitCheck.withPortraitFields(player, false),
                "required", PortraitCheck.withPortraitRequired(false, List.of("name", "team", "role", "hero",
                        "eliminations", "assists", "deaths", "damage", "healing", "mitigation", "uploader")));

        Map<String, Object> heroTime = Map.of(
                "type", "object",
                "properties", Map.of(
                        "hero", Map.of("type", "string"),
                        "percentPlayed", Map.of("type", List.of("number", "null")),
                        "playTime", nullableString("As shown, e.g. 05:29.")),
                "required", List.of("hero"));
        Map<String, Object> tile = Map.of(
                "type", "object",
                "properties", Map.of(
                        "label", Map.of("type", "string"),
                        "value", Map.of("type", "string", "description", "As printed: 4, 30%, 01:28."),
                        "per10", nullableString("The AVG PER 10 MIN value as printed.")),
                "required", List.of("label", "value"));
        Map<String, Object> panel = Map.of(
                "type", "object",
                "properties", Map.of(
                        "hero", Map.of("type", "string"),
                        "stats", Map.of("type", "array", "items", tile)),
                "required", List.of("hero", "stats"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("result", Map.of("type", List.of("string", "null"), "enum", java.util.Arrays.asList("VICTORY", "DEFEAT", "DRAW", null),
                "description", "From SUMMARY; null without it."));
        properties.put("finalScoreYours", Map.of("type", List.of("integer", "null")));
        properties.put("finalScoreTheirs", Map.of("type", List.of("integer", "null")));
        properties.put("map", nullableString("From SUMMARY, e.g. Colosseo."));
        properties.put("mode", nullableString("GAME MODE from SUMMARY, e.g. Push."));
        properties.put("gameLength", nullableString("GAME LENGTH from SUMMARY, minutes:seconds."));
        properties.put("date", nullableString("DATE from SUMMARY as printed."));
        properties.put("yourHeroes", Map.of("type", "array", "items", heroTime,
                "description", "HEROES PLAYED from SUMMARY (or the hero tabs on PERSONAL)."));
        properties.put("heroStats", Map.of("type", "array", "items", panel, "description", "One entry per PERSONAL hero tab."));
        properties.put("players", Map.of("type", "array", "items", playerSchema));
        properties.put("warnings", Map.of("type", "array", "items", Map.of("type", "string"),
                "description", "Every value you were unsure of or couldn't see, naming the row, tile or field."));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("players", "warnings"));
        return new Tool(TOOL_NAME, "Submit the transcribed Overwatch match.", schema);
    }

    private static Map<String, Object> nullableString(String description) {
        return Map.of("type", List.of("string", "null"), "description", description);
    }
}

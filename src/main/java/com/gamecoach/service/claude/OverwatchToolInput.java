package com.gamecoach.service.claude;

import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchMatch.HeroStat;
import com.gamecoach.model.OverwatchMatch.HeroStatsPanel;
import com.gamecoach.model.OverwatchMatch.HeroTime;
import com.gamecoach.model.OverwatchPlayerLine;
import com.gamecoach.model.OverwatchResult;
import com.gamecoach.model.OverwatchRole;
import com.gamecoach.model.RoundScore;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the submit_overwatch_match tool's input into an OverwatchMatch. Lenient like
 * ScoreboardToolInput: a value it can't use becomes null or 0 plus a warning, so one bad field
 * never throws away a match the user could fix in review.
 */
final class OverwatchToolInput {

    private OverwatchToolInput() {
    }

    static OverwatchMatch toMatch(Map<String, Object> input, String matchId, List<String> warnings) {
        String length = ToolInput.string(input, "gameLength");
        Duration duration = ScoreboardToolInput.duration(length);
        if (length != null && duration == null) {
            warnings.add("Couldn't read the game length \"" + length + "\".");
        }

        Integer ours = ToolInput.integer(input, "finalScoreYours");
        Integer theirs = ToolInput.integer(input, "finalScoreTheirs");
        RoundScore score = ours != null && theirs != null && ours >= 0 && theirs >= 0 ? new RoundScore(ours, theirs) : null;

        List<OverwatchPlayerLine> players = new ArrayList<>();
        List<Map<String, Object>> rows = ToolInput.objects(input, "players");
        for (int i = 0; i < rows.size(); i++) {
            players.add(player(rows.get(i), i + 1, warnings));
        }

        List<HeroTime> yourHeroes = ToolInput.objects(input, "yourHeroes").stream()
                .filter(h -> ToolInput.string(h, "hero") != null)
                .map(h -> new HeroTime(ToolInput.string(h, "hero"), ToolInput.number(h, "percentPlayed"),
                        ScoreboardToolInput.duration(ToolInput.string(h, "playTime"))))
                .toList();

        List<HeroStatsPanel> panels = ToolInput.objects(input, "heroStats").stream()
                .filter(p -> ToolInput.string(p, "hero") != null)
                .map(p -> new HeroStatsPanel(ToolInput.string(p, "hero"), ToolInput.objects(p, "stats").stream()
                        .filter(t -> ToolInput.string(t, "label") != null && ToolInput.string(t, "value") != null)
                        .map(t -> new HeroStat(ToolInput.string(t, "label"), ToolInput.string(t, "value"),
                                ToolInput.string(t, "per10")))
                        .toList()))
                .toList();

        return new OverwatchMatch(matchId,
                ToolInput.string(input, "mode"),
                ToolInput.string(input, "map"),
                duration,
                ToolInput.enumValue(OverwatchResult.class, ToolInput.string(input, "result")),
                score,
                ToolInput.string(input, "date"),
                players,
                yourHeroes,
                panels);
    }

    private static OverwatchPlayerLine player(Map<String, Object> row, int number, List<String> warnings) {
        String hero = ToolInput.string(row, "hero");
        String where = "Row " + number + (hero == null ? "" : " (" + hero + ")");
        String team = ToolInput.string(row, "team");
        return new OverwatchPlayerLine(
                ToolInput.string(row, "name"),
                ToolInput.string(row, "title"),
                team == null ? null : team.toUpperCase(Locale.ROOT),
                ToolInput.enumValue(OverwatchRole.class, ToolInput.string(row, "role")),   // UNKNOWN → null
                hero,
                stat(row, "eliminations", where, warnings),
                stat(row, "assists", where, warnings),
                stat(row, "deaths", where, warnings),
                stat(row, "damage", where, warnings),
                stat(row, "healing", where, warnings),
                stat(row, "mitigation", where, warnings),
                ToolInput.bool(row, "uploader"));
    }

    private static int stat(Map<String, Object> row, String key, String where, List<String> warnings) {
        Integer value = ToolInput.integer(row, key);
        if (value == null) {
            warnings.add(where + ": " + key + " wasn't read; set to 0.");
            return 0;
        }
        return value;
    }
}

package com.gamecoach.service.claude;

import com.gamecoach.model.EquippedTeamUp;
import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchResult;
import com.gamecoach.model.Medal;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.PortraitStyle;
import com.gamecoach.model.ProficiencyBadge;
import com.gamecoach.model.ProficiencyShown;
import com.gamecoach.model.Role;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the submit_scoreboard tool's input (a Map parsed from JSON) into MatchDetails. Lenient on
 * purpose: a value it can't use becomes null or a default plus a warning, so one bad field never
 * throws away a whole scoreboard the user could fix in review.
 */
final class ScoreboardToolInput {

    private static final Pattern MIN_SEC = Pattern.compile("^(?:(\\d+):)?(\\d{1,3}):(\\d{2})$");
    private static final Pattern WORDS = Pattern.compile("^(?:(\\d+)\\s*m(?:in)?)?\\s*(?:(\\d+)\\s*s(?:ec)?)?$");

    private ScoreboardToolInput() {
    }

    /** Adds anything it had to guess or drop to warnings. */
    static MatchDetails toMatch(Map<String, Object> input, String matchId, List<String> warnings) {
        MatchResult result = ToolInput.enumValue(MatchResult.class, ToolInput.string(input, "result"));
        Duration duration = duration(ToolInput.string(input, "duration"));
        if (duration == null) {
            warnings.add("Couldn't read the match length \"" + ToolInput.string(input, "duration") + "\".");
            duration = Duration.ZERO;
        }

        List<PlayerLine> players = new ArrayList<>();
        List<Map<String, Object>> rows = ToolInput.objects(input, "players");
        for (int i = 0; i < rows.size(); i++) {
            players.add(player(rows.get(i), i + 1, warnings));
        }

        return new MatchDetails(matchId,
                ToolInput.string(input, "mode"),
                ToolInput.string(input, "map"),
                duration,
                result,
                ToolInput.string(input, "replayId"),
                ToolInput.strings(input, "bans"),
                players,
                null);   // the scoreboard doesn't show the round score; the uploader can supply it
    }

    private static PlayerLine player(Map<String, Object> row, int number, List<String> warnings) {
        String hero = ToolInput.string(row, "hero");
        String where = "Row " + number + (hero == null ? "" : " (" + hero + ")");

        String team = ToolInput.string(row, "team");
        if (team != null) {
            team = team.toUpperCase(java.util.Locale.ROOT);
        }
        Role role = ToolInput.enumValue(Role.class, ToolInput.string(row, "role"));
        if (role == null) {
            role = Role.UNKNOWN;
        }

        EquippedTeamUp teamUp = null;
        String partner = ToolInput.string(row, "teamUpPartner");
        if (partner != null) {
            teamUp = new EquippedTeamUp(partner, ToolInput.bool(row, "teamUpEnhanced"));
        }

        List<Medal> medals = new ArrayList<>();
        for (String m : ToolInput.strings(row, "medals")) {
            Medal medal = ToolInput.enumValue(Medal.class, m);
            if (medal == null) {
                warnings.add(where + ": ignored unknown medal \"" + m + "\".");
            } else if (!medals.contains(medal)) {
                medals.add(medal);
            }
        }

        return new PlayerLine(
                ToolInput.string(row, "name"),
                team,
                role,
                hero,
                heroesUsed(row),
                teamUp,
                proficiency(row, where, warnings),
                stat(row, "kills", where, warnings),
                stat(row, "deaths", where, warnings),
                stat(row, "assists", where, warnings),
                stat(row, "finalHits", where, warnings),
                stat(row, "damage", where, warnings),
                stat(row, "blocked", where, warnings),
                stat(row, "healing", where, warnings),
                ToolInput.number(row, "accuracyPercent"),
                ToolInput.bool(row, "uploader"),
                ToolInput.bool(row, "mvp"),
                ToolInput.bool(row, "svp"),
                medals,
                ToolInput.integer(row, "multiKill"),
                null,
                null);
    }

    /** A missing number becomes 0 with a warning, since the stat records need a value. */
    private static int stat(Map<String, Object> row, String key, String where, List<String> warnings) {
        Integer value = ToolInput.integer(row, key);
        if (value == null) {
            warnings.add(where + ": " + key + " wasn't read; set to 0.");
            return 0;
        }
        return value;
    }

    private static List<HeroStats> heroesUsed(Map<String, Object> row) {
        return ToolInput.strings(row, "heroesUsed").stream().map(HeroStats::nameOnly).toList();
    }

    /**
     * DEFAULT art is stored as a null portrait, matching how the rest of the app treats "nothing
     * proven". Neither shown: no proficiency at all.
     */
    private static ProficiencyShown proficiency(Map<String, Object> row, String where, List<String> warnings) {
        String portraitText = ToolInput.string(row, "portrait");
        String badgeText = ToolInput.string(row, "badge");
        PortraitStyle portrait = ToolInput.enumValue(PortraitStyle.class, portraitText);
        ProficiencyBadge badge = ToolInput.enumValue(ProficiencyBadge.class, badgeText);
        if (portraitText != null && portrait == null) {
            warnings.add(where + ": unknown portrait style \"" + portraitText + "\".");
        }
        if (badgeText != null && badge == null && !badgeText.equalsIgnoreCase("NONE")) {
            warnings.add(where + ": unknown badge \"" + badgeText + "\".");
        }
        if (portrait == PortraitStyle.DEFAULT) {
            portrait = null;
        }
        return portrait == null && badge == null ? null : new ProficiencyShown(portrait, badge);
    }

    /** "16:07", "1:02:30" or "16m 7s" → a Duration; null when it's none of those. */
    static Duration duration(String text) {
        if (text == null) {
            return null;
        }
        String t = text.strip().toLowerCase(java.util.Locale.ROOT);
        Matcher m = MIN_SEC.matcher(t);
        if (m.matches()) {
            long hours = m.group(1) == null ? 0 : Long.parseLong(m.group(1));
            return Duration.ofHours(hours).plusMinutes(Long.parseLong(m.group(2))).plusSeconds(Long.parseLong(m.group(3)));
        }
        m = WORDS.matcher(t);
        if (m.matches() && (m.group(1) != null || m.group(2) != null)) {
            long minutes = m.group(1) == null ? 0 : Long.parseLong(m.group(1));
            long seconds = m.group(2) == null ? 0 : Long.parseLong(m.group(2));
            return Duration.ofMinutes(minutes).plusSeconds(seconds);
        }
        return null;
    }
}

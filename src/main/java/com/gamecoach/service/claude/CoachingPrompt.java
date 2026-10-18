package com.gamecoach.service.claude;

import com.gamecoach.analysis.Closeness;
import com.gamecoach.analysis.HeroPlayed;
import com.gamecoach.analysis.MatchCloseness;
import com.gamecoach.analysis.PlayerAnalysis;
import com.gamecoach.analysis.ProficiencyAnalysis;
import com.gamecoach.analysis.Stat;
import com.gamecoach.analysis.StatComparison;
import com.gamecoach.analysis.TeamUpAnalysis;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.Medal;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;
import com.gamecoach.service.claude.ClaudeApi.Tool;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds what Claude reads: the system prompt (the coach's standing instructions), the user message
 * (this match's data, written out as plain labeled text), and the tool Claude must answer through.
 * Plain text is used instead of raw JSON because the labels ("rank 1 = best", "fewer is better")
 * explain each number, which makes the coaching more accurate.
 */
final class CoachingPrompt {

    static final String TOOL_NAME = "submit_coaching";

    private CoachingPrompt() {
    }

    static String systemPrompt() {
        return """
                You are an expert Marvel Rivals coach. You get one player's end-of-match scoreboard and \
                comparisons already computed from it, and you give them honest, specific coaching.

                Rules:
                - Base every point on the data given. Each strength and weakness cites at least one number.
                - The scoreboard can't show positioning, cooldown use or specific fights. Don't claim to know \
                them; when a number hints at one, say what to look for in the replay instead.
                - Judge stats against the player's role. Strategists are measured mainly by healing and \
                staying alive, Vanguards by damage blocked and creating space, Duelists by KOs, final hits \
                and damage. Low damage blocked is not a weakness for a Strategist, for example.
                - Ranks: 1 is best. For deaths, fewer is better, so rank 1 means the fewest deaths.
                - Compare with teammates and opponents in the same role when it helps, and name the heroes \
                involved. Heroes in the same role have very different typical numbers (some Strategists \
                heal far more than others), so a gap with a player on a different hero is not a weakness \
                by itself.
                - HERO NOTES, when given, are game knowledge confirmed by players. Trust them over general \
                assumptions and use them to read the stats.
                - Proficiency only proves a minimum level. Never call a player inexperienced because \
                no badge or Lord portrait is shown.
                - Team-ups: if a better option is listed, recommend it. Don't describe what team-ups do; \
                their effects change every season.
                - Mention a hero ability only if it is in the HERO NOTES or you're sure it exists in the \
                current game. Otherwise give role-level advice.
                - The match result is context, never a strength or weakness: the player can't win or lose \
                alone.
                - Each stat or point goes in only one place. Don't praise something as a strength and then \
                list it as a weakness, and don't repeat a strength in the advice.
                - Advice gives new, concrete actions for next match. Don't just restate a weakness.
                - Match closeness: hard-fought matches are the most useful to coach, because small \
                decisions decided them. Use the MATCH CLOSENESS section:
                  - CLOSE: coach normally.
                  - ONE-SIDED loss: most numbers reflect the mismatch (the team kept losing fights), so \
                don't pile on totals like deaths. Focus on what the player still controlled: when and \
                where they died, survival, and their stats relative to their teammates.
                  - ONE-SIDED win: big totals may be inflated by a weak enemy, so don't over-praise them; \
                strengths are less proven. Look for habits that would get punished by a stronger team.
                  - CLEAR EDGE: in between; mention it where it explains a number.
                  - The team that won more fights isn't always the team that won the match (objective play \
                can decide it). If they differ, say so.
                - matchContext: one or two sentences saying how close the match was and how much weight \
                this coaching deserves.
                - Compare accuracy only with the same hero or a similar weapon. Different weapons have very \
                different typical accuracy, so a gap with another hero is not a weakness.
                - Never list a stat as a weakness when it isn't part of the role's job (damage blocked for \
                a Strategist, healing for a Vanguard).
                - Before answering, check that no stat appears in both strengths and weaknesses (for \
                example, praising few deaths and then criticizing deaths).
                - One match is a small sample. Present weaknesses as things to watch, not verdicts.
                - List as many strengths and weaknesses as the data supports (usually 2 to 4 each), most \
                important first. Give 2 to 4 things to try next match.
                - Write in plain language, in the second person, one or two sentences per item.
                - Always answer by calling the submit_coaching tool exactly once, with no text before \
                or after it.
                """;
    }

    /** The tool's input schema mirrors Feedback, so the reply parses straight into it. */
    static Tool tool() {
        Map<String, Object> listOfStrings = Map.of(
                "type", "array", "minItems", 1, "items", Map.of("type", "string"));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "strengths", withDescription(listOfStrings, "What the player did well, most important first."),
                        "weaknesses", withDescription(listOfStrings, "What to improve, most important first."),
                        "advice", withDescription(listOfStrings, "2 to 4 concrete things to try next match."),
                        "matchContext", Map.of("type", "string", "description",
                                "One or two sentences on how close the match was and how much weight this coaching deserves.")),
                "required", List.of("strengths", "weaknesses", "advice", "matchContext"));
        return new Tool(TOOL_NAME, "Submit the coaching for this player and match.", schema);
    }

    private static Map<String, Object> withDescription(Map<String, Object> base, String description) {
        Map<String, Object> copy = new java.util.LinkedHashMap<>(base);
        copy.put("description", description);
        return copy;
    }

    /**
     * @param heroNotes notes for the heroes the coached player played, hero name to notes; may be empty
     */
    static String userMessage(MatchDetails match, PlayerAnalysis a, Map<String, List<String>> heroNotes) {
        StringBuilder s = new StringBuilder();

        s.append("MATCH\n");
        s.append("Mode: ").append(match.mode()).append(" | Map: ").append(match.map())
                .append(" | Length: ").append(minutesAndSeconds(match.duration()))
                .append(" | Result for the coached player's team: ").append(a.won() ? "WIN" : "LOSS").append('\n');
        s.append("Bans: ").append(match.bans().isEmpty() ? "not recorded" : String.join(", ", match.bans())).append("\n\n");

        s.append(closeness(a.closeness(), a.won())).append('\n');

        s.append("COACHED PLAYER\n");
        s.append("Name: ").append(a.player() == null ? "(hidden)" : a.player())
                .append(" | Role: ").append(roleName(a.role()))
                .append(a.mvp() ? " | MVP" : "").append(a.svp() ? " | SVP" : "").append('\n');
        s.append("Heroes: ").append(a.heroes().stream().map(CoachingPrompt::hero).collect(Collectors.joining("; "))).append('\n');
        s.append("Team composition (Vanguards-Duelists-Strategists): ").append(a.teamComposition())
                .append(" | Enemy: ").append(a.enemyComposition()).append('\n');
        s.append("KDA ((kills + assists) / deaths): ").append(a.kda()).append('\n');
        s.append("Medals: ").append(a.medals().isEmpty() ? "none"
                : a.medals().stream().map(CoachingPrompt::medal).collect(Collectors.joining(", "))).append('\n');
        s.append("Best multi-kill: ").append(a.multiKill() == null ? "none" : a.multiKill() + " KOs in quick succession").append('\n');
        s.append("Proficiency: ").append(proficiency(a.proficiency())).append('\n');
        s.append("Team-up: ").append(teamUp(a.teamUp())).append('\n');
        if (!a.teammateTeamUpsYouEnhance().isEmpty()) {
            s.append("Teammates' team-ups this player's hero enhanced: ")
                    .append(String.join(", ", a.teammateTeamUpsYouEnhance())).append('\n');
        }
        s.append("Accuracy: ").append(a.accuracyPercent() == null ? "not shown"
                : String.format(Locale.ROOT, "%.0f%% (rank %d among players with accuracy shown)",
                        a.accuracyPercent(), a.accuracyLobbyRank())).append("\n\n");

        if (!heroNotes.isEmpty()) {
            s.append("HERO NOTES (confirmed by players)\n");
            heroNotes.forEach((hero, notes) -> notes.forEach(note ->
                    s.append("- ").append(hero).append(": ").append(note).append('\n')));
            s.append('\n');
        }

        s.append("STATS (rank 1 = best; for deaths, fewer is better)\n");
        s.append("Stat | Value | Per minute | Share of team | Team rank of ").append(a.teamSize())
                .append(" | Lobby rank of ").append(a.lobbySize()).append('\n');
        for (Map.Entry<Stat, StatComparison> e : a.stats().entrySet()) {
            StatComparison c = e.getValue();
            s.append(statName(e.getKey())).append(" | ").append(c.value())
                    .append(" | ").append(String.format(Locale.ROOT, "%.1f", c.perMinute()))
                    .append(" | ").append(String.format(Locale.ROOT, "%.1f%%", c.teamSharePercent()))
                    .append(" | ").append(c.teamRank()).append(" | ").append(c.lobbyRank()).append('\n');
        }

        s.append("\nFULL SCOREBOARD (K/D/A, final hits, damage, damage blocked, healing, accuracy)\n");
        for (String team : List.of("A", "B")) {
            boolean ourTeam = team.equals(a.team());
            s.append(ourTeam ? "Coached player's team:\n" : "Opponents:\n");
            for (PlayerLine p : match.players()) {
                if (p.team().equals(team)) {
                    s.append("- ").append(scoreboardLine(p, isCoached(p, a))).append('\n');
                }
            }
        }
        return s.toString();
    }

    /** The MATCH CLOSENESS section: a plain verdict first, then the numbers behind it. */
    static String closeness(MatchCloseness c, boolean won) {
        String side = c.teamAheadInFights() ? "the coached player's team" : "the enemy team";
        String verdict = switch (c.level()) {
            case CLOSE -> "CLOSE (hard-fought)";
            case CLEAR_EDGE -> c.fromRounds() ? "CLEAR EDGE to the " + (won ? "coached player's team" : "enemy team")
                    : "CLEAR EDGE to " + side + " in fights";
            case ONE_SIDED -> c.fromRounds() ? "ONE-SIDED: a " + (won ? "sweep win" : "sweep loss")
                    : "ONE-SIDED in " + side + "'s favor" + (c.teamAheadInFights() == won
                    ? (won ? " (a likely stomp win)" : " (a likely stomp loss)")
                    : "");
        };
        StringBuilder s = new StringBuilder("MATCH CLOSENESS (computed from the scoreboard)\n");
        s.append("Verdict: ").append(verdict).append('\n');
        s.append("Based on: ").append(c.fromRounds()
                ? "the round score, " + c.roundsWon() + "-" + c.roundsLost() + " for the coached player's team "
                        + "(a sweep is one-sided, a one-round margin is close)"
                : "death totals (the round score wasn't recorded)").append('\n');
        if (c.teamAheadInFights() != won && (c.fromRounds() || c.level() != Closeness.CLOSE)) {
            s.append("Note: the team that won more fights did not win the match.\n");
        }
        s.append("Deaths: coached player's team ").append(c.teamDeaths()).append(", enemy team ").append(c.enemyDeaths())
                .append(String.format(Locale.ROOT, " (ratio %.2f; 1.00 is even, 2.00 or more is one-sided)", c.deathRatio())).append('\n');
        s.append(String.format(Locale.ROOT, "Coached team's share of all damage: %.1f%% | of all healing: %.1f%%",
                c.damageSharePercent(), c.healingSharePercent())).append('\n');
        return s.toString();
    }

    private static String scoreboardLine(PlayerLine p, boolean coached) {
        StringBuilder s = new StringBuilder();
        s.append(p.name() == null ? "(hidden)" : p.name()).append(coached ? " [COACHED PLAYER]" : "")
                .append(" | ").append(roleName(p.role())).append(" | ").append(p.lastHero())
                .append(" | ").append(p.kills()).append('/').append(p.deaths()).append('/').append(p.assists())
                .append(" | FH ").append(p.finalHits())
                .append(" | Dmg ").append(p.damage())
                .append(" | Blocked ").append(p.blocked())
                .append(" | Heal ").append(p.healing())
                .append(" | Acc ").append(p.accuracyPercent() == null ? "-" : String.format(Locale.ROOT, "%.0f%%", p.accuracyPercent()));
        if (p.mvp()) {
            s.append(" | MVP");
        }
        if (p.svp()) {
            s.append(" | SVP");
        }
        if (!p.medals().isEmpty()) {
            s.append(" | Medals: ").append(p.medals().stream().map(CoachingPrompt::medal).collect(Collectors.joining(", ")));
        }
        return s.toString();
    }

    /** Matches by name; when names are hidden, by team and last hero. */
    private static boolean isCoached(PlayerLine p, PlayerAnalysis a) {
        if (a.player() != null) {
            return a.player().equalsIgnoreCase(p.name());
        }
        String lastHero = a.heroes().stream().filter(HeroPlayed::lastPlayed).map(HeroPlayed::hero).findFirst().orElse(null);
        return p.name() == null && p.team().equals(a.team()) && p.lastHero().equals(lastHero);
    }

    private static String hero(HeroPlayed h) {
        StringBuilder s = new StringBuilder(h.hero()).append(" (").append(roleName(h.role()));
        if (h.lastPlayed()) {
            s.append(", last played");
        }
        if (h.kills() != null) {
            s.append(", ").append(h.kills()).append('/').append(h.deaths()).append('/').append(h.assists());
        }
        return s.append(')').toString();
    }

    private static String proficiency(ProficiencyAnalysis p) {
        if (p == null) {
            return "nothing shown (says nothing about experience)";
        }
        return "at least " + p.minimumRank() + " (level " + p.minimumLevel() + ") on " + p.hero()
                + "; a minimum only, the real level may be higher";
    }

    private static String teamUp(TeamUpAnalysis t) {
        if (t == null) {
            return "not shown on this scoreboard";
        }
        String name = t.name() == null ? "unknown team-up" : t.name();
        StringBuilder s = new StringBuilder(name).append(" with ").append(t.partner())
                .append(t.enhancedOnScoreboard() ? " (enhanced: partner on the team)" : " (not enhanced: partner not on the team)");
        if (t.betterOption() != null) {
            s.append(". Better option this match: ").append(t.betterOption())
                    .append(", whose partner was on the team");
        }
        return s.toString();
    }

    private static String medal(Medal m) {
        return switch (m) {
            case MOST_KOS -> "most KOs in the lobby";
            case MOST_DAMAGE -> "most damage in the lobby";
            case MOST_BLOCKED -> "most damage blocked in the lobby";
            case MOST_HEALING -> "most healing in the lobby";
            case MOST_ASSISTS -> "most assists in the lobby";
        };
    }

    private static String statName(Stat stat) {
        return switch (stat) {
            case KILLS -> "KOs";
            case DEATHS -> "Deaths";
            case ASSISTS -> "Assists";
            case FINAL_HITS -> "Final hits";
            case DAMAGE -> "Damage";
            case BLOCKED -> "Damage blocked";
            case HEALING -> "Healing";
        };
    }

    private static String roleName(Role role) {
        return role == null || role == Role.UNKNOWN ? "Unknown role"
                : role.name().charAt(0) + role.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static String minutesAndSeconds(Duration d) {
        return String.format(Locale.ROOT, "%d:%02d", d.toMinutes(), d.toSecondsPart());
    }
}

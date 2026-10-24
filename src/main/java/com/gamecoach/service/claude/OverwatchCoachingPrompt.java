package com.gamecoach.service.claude;

import com.gamecoach.analysis.OverwatchPlayerAnalysis;
import com.gamecoach.analysis.OverwatchStat;
import com.gamecoach.analysis.OverwatchStatComparison;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchMatch.HeroStat;
import com.gamecoach.model.OverwatchMatch.HeroStatsPanel;
import com.gamecoach.model.OverwatchMatch.HeroTime;
import com.gamecoach.model.OverwatchPlayerLine;
import com.gamecoach.model.OverwatchRole;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What Claude reads to coach one Overwatch match: the coach's rules (adapted from the Rivals coach,
 * with Overwatch's columns and roles), and the match written out as labeled text. The answer comes
 * back through the same submit_coaching tool as Rivals, so it parses into the same Feedback.
 */
final class OverwatchCoachingPrompt {

    private OverwatchCoachingPrompt() {
    }

    static String systemPrompt() {
        return """
                You are an expert Overwatch coach. You get one player's end-of-match report (the Teams \
                scoreboard, and when available the Summary and the hero's Personal stats) with comparisons \
                already computed from it, and you give them honest, specific coaching.

                What the columns mean:
                - Eliminations count every enemy death the player contributed to, not only final blows.
                - Assists come mostly from healing or buffing teammates who then get eliminations, so \
                supports have many and tanks and damage heroes have few or none. Low assists are never a \
                weakness for a tank or damage player.
                - Damage is hero damage. Mitigation is damage blocked, absorbed or reduced by the player's \
                abilities: a main measure for tanks, and only meaningful for other heroes whose kit blocks \
                or reduces damage.

                Rules:
                - Base every point on the data given. Each strength and weakness cites at least one number.
                - The report can't show positioning, cooldown use or specific fights. Don't claim to know \
                them; when a number hints at one, say what to look for in the replay instead.
                - Judge stats against the role. Tanks: survival, mitigation and pressure (damage and \
                eliminations), since one tank per team makes tank deaths swing fights. Damage: eliminations, \
                damage and staying alive. Supports: healing, staying alive, and the damage and assists they \
                add. Never call a stat a weakness when it isn't the role's job (healing for a tank, \
                mitigation for most damage heroes and supports).
                - The most useful comparison is the enemy in the same role (tank against tank, and so on); \
                the STATS table gives that player's best value. Heroes in the same role have different \
                typical numbers, so never base a weakness on a gap with a player on a different hero.
                - Ranks: 1 is best. For deaths, fewer is better, so rank 1 means the fewest deaths.
                - Per-10 rates are this match's rates. The Personal screen's "per 10 min" values are also \
                from this match only, not career averages.
                - HERO-SPECIFIC STATS (from the Personal screen) make the coaching specific to the hero. Use \
                them, but there is no benchmark beyond this match: don't call a percentage good or bad \
                unless typical values for that hero are well known; otherwise treat it as a number to track \
                over the next games.
                - Objective contest time counts time on the objective while the enemy also contests it, \
                not time standing on it. It is naturally low in one-sided games, and fighting away from \
                the objective to take space (ahead of the payload or robot, or on the enemy's side) is \
                often the right play, especially for tanks. Never list contest time as a weakness or its \
                own advice item; at most mention it inside another point that has real evidence (such as \
                many deaths away from the team).
                - A row's hero is the hero that player ended on, but the row's numbers cover the whole \
                match. If a player's numbers clearly don't fit their hero (for example lots of healing on \
                a tank), they probably switched heroes, so don't use them as a same-role comparison.
                - HERO NOTES, when given, are researched knowledge of how the hero plays now. Trust them over \
                general assumptions and use them to read the stats (for example, what a hero's mitigation \
                comes from, or why low damage is normal for them).
                - Mention a hero ability only if it appears in the HERO NOTES or hero-specific stats, or \
                you're sure it exists in the current game: Overwatch reworks heroes often. Otherwise give \
                role-level advice.
                - The match result is context, never a strength or weakness: the player can't win or lose alone.
                - Match closeness: use the MATCH CLOSENESS section. CLOSE: coach normally. ONE-SIDED loss: \
                most numbers reflect the mismatch, so don't pile on totals like deaths; focus on what the \
                player still controlled. ONE-SIDED win: big totals may be inflated by a weak enemy, so \
                don't over-praise them; look for habits a stronger team would punish. CLEAR EDGE: in \
                between. The team that won more fights isn't always the team that won the match \
                (objective play can decide it); if they differ, say so.
                - A strength needs a good result: a top-half rank on the team or in the lobby, beating the \
                same-role enemy, or a clear role or hero reason. A mid-pack number is never a strength.
                - Each stat appears at most once across strengths and weaknesses. If a stat is mixed, \
                choose one list and give both sides there. Don't repeat a strength in the advice.
                - Don't prescribe exact combos or button sequences; coach the decision (when to engage, \
                when to leave, what to save).
                - Name heroes exactly as given. The coached player's hero is under COACHED PLAYER.
                - One match is a small sample. Present weaknesses as things to watch, not verdicts.
                - Weaknesses are about the player's own play. How weak or strong the enemy team was belongs \
                only in matchContext, never as a strength, weakness or advice item.
                - matchContext: one or two sentences saying how close the match was and how much weight \
                this coaching deserves.
                - List as many strengths and weaknesses as the data supports (usually 2 to 4 each), most \
                important first. Give 2 to 4 concrete, new things to try next match.
                - Write in plain language, in the second person, one or two sentences per item.
                - Before answering, re-read your lists against these rules.
                - If no stat meets the bar for a strength, leave strengths empty rather than inventing one \
                (and the same for weaknesses); always give advice.
                - Always answer by calling the submit_coaching tool exactly once, with no text before or after it.
                """;
    }

    /** @param heroNotes notes for the coached player's heroes, hero name to notes; may be empty */
    static String userMessage(OverwatchMatch match, OverwatchPlayerAnalysis a, Map<String, List<String>> heroNotes) {
        StringBuilder s = new StringBuilder();
        s.append("MATCH\n");
        s.append("Mode: ").append(orUnknown(match.mode())).append(" | Map: ").append(orUnknown(match.map()))
                .append(" | Length: ").append(match.duration() == null ? "unknown" : minutesAndSeconds(match.duration()))
                .append(" | Result for the coached player's team: ").append(a.outcome() == null ? "unknown" : a.outcome())
                .append('\n');
        if (match.score() != null) {
            s.append("Final score (coached player's team first): ").append(scoreFor(match, a))
                    .append(" (rounds or points depending on the mode; in Push it only says who pushed further)\n");
        }
        s.append('\n');

        boolean won = "WIN".equals(a.outcome());
        s.append(CoachingPrompt.closeness(a.closeness(), won)
                .replace("(the round score wasn't recorded)", "(the final score isn't a round count in this mode, or wasn't recorded)")
                .replace("the round score", "the final score"))
                .append('\n');

        s.append("COACHED PLAYER\n");
        s.append("Name: ").append(a.player() == null ? "(hidden)" : a.player())
                .append(" | Role: ").append(roleName(a.role()))
                .append(" | Hero at the end: ").append(a.hero()).append('\n');
        s.append("Heroes played: ").append(a.heroesPlayed().stream().map(OverwatchCoachingPrompt::heroTime)
                .collect(Collectors.joining("; "))).append('\n');
        s.append("Team composition (tanks-damage-supports): ").append(a.teamComposition())
                .append(" | Enemy: ").append(a.enemyComposition()).append('\n');
        s.append("Eliminations per death: ").append(a.eliminationsPerDeath()).append("\n\n");

        if (!heroNotes.isEmpty()) {
            s.append("HERO NOTES (researched; how the hero plays now)\n");
            heroNotes.forEach((hero, notes) -> notes.forEach(note ->
                    s.append("- ").append(hero).append(": ").append(note).append('\n')));
            s.append('\n');
        }

        if (!a.heroStats().isEmpty()) {
            s.append("HERO-SPECIFIC STATS (Personal screen, this match only)\n");
            for (HeroStatsPanel panel : a.heroStats()) {
                for (HeroStat t : panel.stats()) {
                    s.append("- ").append(panel.hero()).append(": ").append(t.label()).append(" ").append(t.value())
                            .append(t.per10() == null ? "" : " (per 10 min this match: " + t.per10() + ")").append('\n');
                }
            }
            s.append('\n');
        }

        s.append("STATS (rank 1 = best; for deaths, fewer is better)\n");
        s.append("Stat | Value | Per 10 min | Share of team | Team rank of ").append(a.teamSize())
                .append(" | Lobby rank of ").append(a.lobbySize()).append(" | Best enemy ")
                .append(roleName(a.role()).toLowerCase(Locale.ROOT)).append('\n');
        for (Map.Entry<OverwatchStat, OverwatchStatComparison> e : a.stats().entrySet()) {
            OverwatchStatComparison c = e.getValue();
            s.append(statName(e.getKey())).append(" | ").append(c.value())
                    .append(" | ").append(c.per10() == null ? "-" : String.format(Locale.ROOT, "%.1f", c.per10()))
                    .append(" | ").append(String.format(Locale.ROOT, "%.1f%%", c.teamSharePercent()))
                    .append(" | ").append(c.teamRank()).append(" | ").append(c.lobbyRank())
                    .append(" | ").append(c.sameRoleEnemyBest() == null ? "-" : c.sameRoleEnemyBest()).append('\n');
        }

        s.append("\nFULL SCOREBOARD (E/A/D, damage, healing, mitigation)\n");
        for (String team : List.of("A", "B")) {
            s.append(team.equals(a.team()) ? "Coached player's team:\n" : "Opponents:\n");
            for (OverwatchPlayerLine p : match.players()) {
                if (team.equals(p.team())) {
                    s.append("- ").append(line(p, isCoached(p, a))).append('\n');
                }
            }
        }
        return s.toString();
    }

    private static String scoreFor(OverwatchMatch match, OverwatchPlayerAnalysis a) {
        boolean teamA = "A".equals(a.team());
        int ours = teamA ? match.score().teamA() : match.score().teamB();
        int theirs = teamA ? match.score().teamB() : match.score().teamA();
        return ours + " vs " + theirs;
    }

    private static String line(OverwatchPlayerLine p, boolean coached) {
        return (p.name() == null ? "(hidden)" : p.name()) + (coached ? " [COACHED PLAYER]" : "")
                + " | " + roleName(p.role()) + " | " + p.hero()
                + " | " + p.eliminations() + "/" + p.assists() + "/" + p.deaths()
                + " | Dmg " + p.damage() + " | Heal " + p.healing() + " | Mit " + p.mitigation();
    }

    private static boolean isCoached(OverwatchPlayerLine p, OverwatchPlayerAnalysis a) {
        if (a.player() != null) {
            return a.player().equalsIgnoreCase(p.name());
        }
        return p.name() == null && p.team().equals(a.team()) && p.hero() != null && p.hero().equals(a.hero());
    }

    private static String heroTime(HeroTime h) {
        StringBuilder s = new StringBuilder(h.hero());
        if (h.percentPlayed() != null || h.playTime() != null) {
            s.append(" (");
            if (h.percentPlayed() != null) {
                s.append(String.format(Locale.ROOT, "%.0f%%", h.percentPlayed()));
            }
            if (h.playTime() != null) {
                s.append(h.percentPlayed() != null ? ", " : "").append(minutesAndSeconds(h.playTime()));
            }
            s.append(')');
        }
        return s.toString();
    }

    private static String statName(OverwatchStat stat) {
        return switch (stat) {
            case ELIMINATIONS -> "Eliminations";
            case ASSISTS -> "Assists";
            case DEATHS -> "Deaths";
            case DAMAGE -> "Hero damage";
            case HEALING -> "Healing";
            case MITIGATION -> "Damage mitigated";
        };
    }

    private static String roleName(OverwatchRole role) {
        return role == null ? "Unknown role" : role.name().charAt(0) + role.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static String orUnknown(String s) {
        return s == null ? "unknown" : s;
    }

    private static String minutesAndSeconds(Duration d) {
        return String.format(Locale.ROOT, "%d:%02d", d.toMinutes(), d.toSecondsPart());
    }
}

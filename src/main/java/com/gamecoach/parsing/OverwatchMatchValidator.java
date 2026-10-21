package com.gamecoach.parsing;

import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchMatch.HeroStatsPanel;
import com.gamecoach.model.OverwatchMatch.HeroTime;
import com.gamecoach.model.OverwatchPlayerLine;
import com.gamecoach.model.OverwatchRole;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Consistency checks on an Overwatch match read from screenshots. Each failed check becomes a
 * warning to review; nothing is changed automatically, because a check can't tell which value is
 * wrong. Overwatch's report has less built-in redundancy than Rivals' scoreboard (no medals, no
 * final hits), so the strongest checks compare the Teams screen with the Summary screen.
 */
@Component
public class OverwatchMatchValidator {

    private final OverwatchHeroCatalog heroes;

    public OverwatchMatchValidator(OverwatchHeroCatalog heroes) {
        this.heroes = heroes;
    }

    public List<String> check(OverwatchMatch match) {
        List<String> warnings = new ArrayList<>();
        List<OverwatchPlayerLine> players = match.players();
        if (players.isEmpty()) {
            warnings.add("No player rows were read.");
            return warnings;
        }
        checkTeams(players, warnings);
        OverwatchPlayerLine you = checkUploader(players, warnings);
        checkSummary(match, warnings);
        for (int i = 0; i < players.size(); i++) {
            checkRow(players.get(i), "Row " + (i + 1) + ", " + name(players.get(i)), warnings);
        }
        if (you != null) {
            checkYourHeroes(match, you, warnings);
        }
        return warnings;
    }

    /** Role queue is 5 against 5; 6v6 modes have 6. Either way both teams match. */
    private static void checkTeams(List<OverwatchPlayerLine> players, List<String> warnings) {
        long a = players.stream().filter(p -> "A".equals(p.team())).count();
        long b = players.stream().filter(p -> "B".equals(p.team())).count();
        if (a != b || a < 5 || a > 6) {
            warnings.add("Read " + a + " players on your team and " + b + " on the enemy team; expected 5 each "
                    + "(or 6 in 6v6). A row may be missing or on the wrong team.");
        }
        players.stream().filter(p -> !"A".equals(p.team()) && !"B".equals(p.team()))
                .forEach(p -> warnings.add(name(p) + " has an unknown team \"" + p.team() + "\"."));
    }

    private static OverwatchPlayerLine checkUploader(List<OverwatchPlayerLine> players, List<String> warnings) {
        List<OverwatchPlayerLine> yours = players.stream().filter(OverwatchPlayerLine::uploader).toList();
        if (yours.isEmpty()) {
            warnings.add("Your row wasn't identified. Mark it, or name yourself with ?username= when asking for feedback.");
            return null;
        }
        if (yours.size() > 1) {
            warnings.add(yours.size() + " rows are marked as yours; only one should be.");
            return null;
        }
        if (!"A".equals(yours.get(0).team())) {
            warnings.add("Your row is on the enemy team, but your team is always the top (blue) half.");
        }
        return yours.get(0);
    }

    private static void checkSummary(OverwatchMatch match, List<String> warnings) {
        if (match.result() == null || match.duration() == null) {
            warnings.add("The result or game length is missing. Add the Summary screenshot (or fix the match) so "
                    + "the coach knows how the game went and can work out per-10-minute stats.");
        }
        if (match.duration() != null && match.duration().isZero()) {
            warnings.add("The game length reads as 0:00.");
        }
        if (match.result() != null && match.score() != null) {
            int ours = match.score().teamA();
            int theirs = match.score().teamB();
            boolean fits = switch (match.result()) {
                case VICTORY -> ours > theirs;
                case DEFEAT -> ours < theirs;
                case DRAW -> ours == theirs;
            };
            if (!fits) {
                warnings.add("The final score " + ours + " vs " + theirs + " doesn't fit " + match.result()
                        + ". The score or the result may be misread.");
            }
        }
    }

    private void checkRow(OverwatchPlayerLine p, String label, List<String> warnings) {
        if (p.hero() == null || p.hero().isBlank()) {
            warnings.add(label + ": the hero portrait wasn't read.");
        } else {
            OverwatchRole tableRole = heroes.roleOfName(p.hero());
            if (tableRole == null) {
                warnings.add(label + ": \"" + p.hero() + "\" isn't in the hero table. It may be misread, or a hero "
                        + "newer than the table.");
            } else if (p.role() != null && p.role() != tableRole) {
                warnings.add(label + ": the role icon reads " + p.role() + " but " + p.hero() + " is " + tableRole
                        + ". The hero or the icon may be misread.");
            }
        }
        if (p.eliminations() < 0 || p.assists() < 0 || p.deaths() < 0 || p.damage() < 0
                || p.healing() < 0 || p.mitigation() < 0) {
            warnings.add(label + ": a stat is negative.");
        }
    }

    /**
     * The row's portrait is the hero you ended on, so it must be among the heroes the Summary lists,
     * and every Personal panel must be for one of them too.
     */
    private static void checkYourHeroes(OverwatchMatch match, OverwatchPlayerLine you, List<String> warnings) {
        List<HeroTime> played = match.yourHeroes();
        if (!played.isEmpty() && you.hero() != null
                && played.stream().noneMatch(h -> OverwatchHeroCatalog.sameHero(h.hero(), you.hero()))) {
            warnings.add("Your row's portrait reads " + you.hero() + ", but your heroes played are "
                    + played.stream().map(HeroTime::hero).toList() + ". The portrait or your row may be misread.");
        }
        double total = played.stream().filter(h -> h.percentPlayed() != null).mapToDouble(HeroTime::percentPlayed).sum();
        if (played.stream().allMatch(h -> h.percentPlayed() != null) && !played.isEmpty() && Math.abs(total - 100) > 2) {
            warnings.add("Your heroes' play-time shares add up to " + Math.round(total) + "%, not 100%.");
        }
        for (HeroStatsPanel panel : match.heroStats()) {
            boolean listed = played.isEmpty() ? OverwatchHeroCatalog.sameHero(panel.hero(), you.hero())
                    : played.stream().anyMatch(h -> OverwatchHeroCatalog.sameHero(h.hero(), panel.hero()));
            if (!listed) {
                warnings.add("The Personal stats are for " + panel.hero() + ", which isn't one of your heroes this match.");
            }
        }
    }

    private static String name(OverwatchPlayerLine p) {
        return (p.name() == null ? "(hidden name)" : p.name()) + " (" + p.hero() + ")";
    }
}

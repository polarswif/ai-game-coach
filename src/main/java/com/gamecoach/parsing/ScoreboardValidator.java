package com.gamecoach.parsing;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchResult;
import com.gamecoach.model.Medal;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.Role;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Consistency checks on a scoreboard read from a screenshot. A vision model can misread a digit or
 * a portrait, and the scoreboard has enough built-in redundancy to catch many of those mistakes:
 * medals must go to the lobby leaders, the MVP is on the winning team, a team's final hits roughly
 * match the other team's deaths, and so on. Each failed check becomes a warning for the user to
 * review; nothing is changed automatically, because the check can't tell which number is wrong.
 */
@Component
public class ScoreboardValidator {

    private final HeroRoleCatalog roles;
    private final TeamUpCatalog teamUps;

    public ScoreboardValidator(HeroRoleCatalog roles, TeamUpCatalog teamUps) {
        this.roles = roles;
        this.teamUps = teamUps;
    }

    public List<String> check(MatchDetails match) {
        List<String> warnings = new ArrayList<>();
        List<PlayerLine> players = match.players();
        if (players.isEmpty()) {
            warnings.add("No player rows were read.");
            return warnings;
        }

        checkTeamSizes(players, warnings);
        checkUploader(players, warnings);
        checkResultAndScore(match, warnings);
        checkMvpAndSvp(match, warnings);
        for (int i = 0; i < players.size(); i++) {
            checkRow(players.get(i), label(players.get(i), i), players, warnings);
        }
        checkMedals(players, warnings);
        checkFinalHitsAgainstDeaths(players, warnings);
        return warnings;
    }

    private static void checkTeamSizes(List<PlayerLine> players, List<String> warnings) {
        long a = players.stream().filter(p -> "A".equals(p.team())).count();
        long b = players.stream().filter(p -> "B".equals(p.team())).count();
        if (a != 6 || b != 6) {
            warnings.add("Expected 6 players per team but read " + a + " on your team and " + b
                    + " on the enemy team. A row may be missing or on the wrong team.");
        }
        players.stream().filter(p -> !"A".equals(p.team()) && !"B".equals(p.team()))
                .forEach(p -> warnings.add("A row has an unknown team \"" + p.team() + "\"."));
    }

    private static void checkUploader(List<PlayerLine> players, List<String> warnings) {
        List<PlayerLine> uploaders = players.stream().filter(PlayerLine::uploader).toList();
        if (uploaders.isEmpty()) {
            warnings.add("No highlighted row (yours) was found. Mark your row, or name yourself when asking for feedback.");
        } else if (uploaders.size() > 1) {
            warnings.add(uploaders.size() + " rows are marked as yours; only one should be.");
        } else if (!"A".equals(uploaders.get(0).team())) {
            warnings.add("Your highlighted row is on the enemy team, but your team is always the top half.");
        }
    }

    private static void checkResultAndScore(MatchDetails match, List<String> warnings) {
        if (match.result() == null) {
            warnings.add("The VICTORY/DEFEAT banner wasn't read.");
            return;
        }
        if (match.score() != null && match.score().teamA() != match.score().teamB()) {
            boolean scoreSaysWin = match.score().teamA() > match.score().teamB();
            if (scoreSaysWin != (match.result() == MatchResult.VICTORY)) {
                warnings.add("The score " + match.score().teamA() + "-" + match.score().teamB() + " doesn't match the "
                        + match.result() + " banner. Enter the score from your team's side (your rounds first).");
            }
        }
        if (match.duration() == null || match.duration().isZero()) {
            warnings.add("The match length wasn't read, so per-minute stats are wrong until it's corrected.");
        }
    }

    /** The MVP goes to the best player on the winning team, the SVP to the best on the losing team. */
    private static void checkMvpAndSvp(MatchDetails match, List<String> warnings) {
        List<PlayerLine> mvps = match.players().stream().filter(PlayerLine::mvp).toList();
        List<PlayerLine> svps = match.players().stream().filter(PlayerLine::svp).toList();
        if (mvps.size() > 1) {
            warnings.add(mvps.size() + " players are marked MVP; there is only one.");
        }
        if (svps.size() > 1) {
            warnings.add(svps.size() + " players are marked SVP; there is only one.");
        }
        if (match.result() == null) {
            return;
        }
        String winners = match.result() == MatchResult.VICTORY ? "A" : "B";
        mvps.stream().filter(p -> !winners.equals(p.team())).forEach(p ->
                warnings.add(name(p) + " is marked MVP but is on the losing team; the MVP comes from the winners."));
        svps.stream().filter(p -> winners.equals(p.team())).forEach(p ->
                warnings.add(name(p) + " is marked SVP but is on the winning team; the SVP comes from the losers."));
    }

    private void checkRow(PlayerLine p, String label, List<PlayerLine> players, List<String> warnings) {
        Role tableRole = roles.roleOf(p.lastHero());
        boolean deadpool = HeroRoleCatalog.isDeadpool(p.lastHero());
        if (p.lastHero() == null || p.lastHero().isBlank()) {
            warnings.add(label + ": the hero portrait wasn't read.");
        } else if (tableRole == Role.UNKNOWN && !deadpool) {
            warnings.add(label + ": \"" + p.lastHero() + "\" isn't in the hero table. It may be misread, or a hero "
                    + "newer than the table.");
        } else if (!deadpool && p.role() != null && p.role() != Role.UNKNOWN && p.role() != tableRole) {
            warnings.add(label + ": the role icon reads " + p.role() + " but " + p.lastHero() + " is a " + tableRole
                    + ". The hero or the icon may be misread.");
        }

        if (p.kills() < 0 || p.deaths() < 0 || p.assists() < 0 || p.finalHits() < 0
                || p.damage() < 0 || p.blocked() < 0 || p.healing() < 0) {
            warnings.add(label + ": a stat is negative.");
        }
        if (p.finalHits() > p.kills()) {
            warnings.add(label + ": " + p.finalHits() + " final hits is more than " + p.kills()
                    + " KOs, which can't happen. One of them is misread.");
        }
        if (p.accuracyPercent() != null && (p.accuracyPercent() < 0 || p.accuracyPercent() > 100)) {
            warnings.add(label + ": accuracy " + p.accuracyPercent() + "% is outside 0-100.");
        }
        if (p.multiKill() != null && (p.multiKill() < 3 || p.multiKill() > 6)) {
            warnings.add(label + ": a multi-kill of " + p.multiKill() + " isn't possible (it shows 3 to 6).");
        }

        if (p.teamUp() != null && p.lastHero() != null) {
            String partner = p.teamUp().partner();
            if (teamUps.find(p.lastHero(), partner).isEmpty()) {
                warnings.add(label + ": " + p.lastHero() + " has no team-up with " + partner
                        + ". The partner portrait may be misread.");
            } else {
                boolean partnerOnTeam = players.stream().anyMatch(o -> o != p && p.team().equals(o.team())
                        && HeroRoleCatalog.sameHero(o.lastHero(), partner));
                if (p.teamUp().enhanced() && !partnerOnTeam) {
                    warnings.add(label + ": the team-up box reads as enhanced, but no teammate's portrait is "
                            + partner + ". The partner may have swapped off, or the box is misread.");
                }
            }
        }
    }

    /** Each medal goes to the single best player in the lobby for that stat. */
    private static void checkMedals(List<PlayerLine> players, List<String> warnings) {
        for (Medal medal : Medal.values()) {
            ToIntFunction<PlayerLine> stat = statFor(medal);
            int best = players.stream().mapToInt(stat).max().orElse(0);
            for (PlayerLine p : players) {
                if (p.medals().contains(medal) && stat.applyAsInt(p) < best) {
                    warnings.add(name(p) + " has the " + medalName(medal) + " medal but someone in the lobby has more ("
                            + best + " vs " + stat.applyAsInt(p) + "). The medal or a number is misread.");
                }
            }
        }
    }

    /**
     * A team's final hits should roughly equal the other team's deaths (they differ a little, for
     * example when a player falls off the map). A big gap points at a misread digit.
     */
    private static void checkFinalHitsAgainstDeaths(List<PlayerLine> players, List<String> warnings) {
        for (String team : List.of("A", "B")) {
            String other = team.equals("A") ? "B" : "A";
            int finalHits = players.stream().filter(p -> team.equals(p.team())).mapToInt(PlayerLine::finalHits).sum();
            int deaths = players.stream().filter(p -> other.equals(p.team())).mapToInt(PlayerLine::deaths).sum();
            if (Math.abs(finalHits - deaths) > Math.max(5, deaths * 0.10)) {
                warnings.add((team.equals("A") ? "Your team's" : "The enemy team's") + " final hits (" + finalHits
                        + ") are far from the other team's deaths (" + deaths + "). A final-hits or deaths number "
                        + "is probably misread.");
            }
        }
    }

    private static ToIntFunction<PlayerLine> statFor(Medal medal) {
        return switch (medal) {
            case MOST_KOS -> PlayerLine::kills;
            case MOST_DAMAGE -> PlayerLine::damage;
            case MOST_BLOCKED -> PlayerLine::blocked;
            case MOST_HEALING -> PlayerLine::healing;
            case MOST_ASSISTS -> PlayerLine::assists;
        };
    }

    private static String medalName(Medal medal) {
        return switch (medal) {
            case MOST_KOS -> "most-KOs";
            case MOST_DAMAGE -> "most-damage";
            case MOST_BLOCKED -> "most-blocked";
            case MOST_HEALING -> "most-healing";
            case MOST_ASSISTS -> "most-assists";
        };
    }

    private static String name(PlayerLine p) {
        return (p.name() == null ? "(hidden name)" : p.name()) + " (" + p.lastHero() + ")";
    }

    private static String label(PlayerLine p, int index) {
        return "Row " + (index + 1) + ", " + name(p);
    }
}

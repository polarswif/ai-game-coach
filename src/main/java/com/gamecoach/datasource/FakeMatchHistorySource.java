package com.gamecoach.datasource;

import com.gamecoach.model.EquippedTeamUp;
import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchResult;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.model.Medal;
import com.gamecoach.model.PlayerLine;
import com.gamecoach.model.PortraitStyle;
import com.gamecoach.model.ProficiencyBadge;
import com.gamecoach.model.ProficiencyShown;
import com.gamecoach.model.Role;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.gamecoach.model.Medal.MOST_ASSISTS;
import static com.gamecoach.model.Medal.MOST_BLOCKED;
import static com.gamecoach.model.Medal.MOST_DAMAGE;
import static com.gamecoach.model.Medal.MOST_HEALING;
import static com.gamecoach.model.Medal.MOST_KOS;
import static com.gamecoach.model.PortraitStyle.ANIMATED_LORD;
import static com.gamecoach.model.Role.DUELIST;
import static com.gamecoach.model.Role.STRATEGIST;
import static com.gamecoach.model.Role.VANGUARD;

/**
 * Development stand-in: three real in-game end-of-match scoreboards (Tokyo 2099: Spider-Islands, Convoy),
 * typed in by hand from screenshots. Heroes were identified from the portraits and confirmed by the
 * uploader, including every team-up box. Some portraits are Lord variants (alternate art for
 * high-proficiency players); which rows wasn't recorded, so proficiency is null on every row.
 * No "Heroes Used" hover was captured, so every heroes list is empty. Only the third match's
 * scoreboard has the team-up boxes, so the other two have no team-ups.
 * Active when application.properties has gamecoach.matches.source=fake.
 */
@Component
@ConditionalOnProperty(name = "gamecoach.matches.source", havingValue = "fake")
public class FakeMatchHistorySource implements MatchHistorySource {

    static final String PLAYER29_MATCH = "10378920354";                         // its replay ID
    static final String DEADPOOL_MATCH = "5518188_1770352206_1245014_11001_11"; // ID printed bottom-left
    static final String TEAM_UP_MATCH = "player10-convoy-2823";                  // no ID visible; made up

    private final Map<String, MatchDetails> matches = Map.of(
            PLAYER29_MATCH, player29Match(),
            DEADPOOL_MATCH, deadpoolMatch(),
            TEAM_UP_MATCH, teamUpMatch());

    @Override
    public List<MatchSummary> getRecentMatches(String username) {
        // Same list for any username. Dates are when the screenshots were shared.
        return List.of(
                new MatchSummary(PLAYER29_MATCH, LocalDate.of(2026, 10, 11), "Convoy",
                        "Tokyo 2099: Spider-Islands", "Rocket Raccoon", 7, 7, 33, true),
                new MatchSummary(DEADPOOL_MATCH, LocalDate.of(2026, 10, 11), "Convoy",
                        "Tokyo 2099: Spider-Islands", "Cloak & Dagger", 32, 0, 53, true),
                new MatchSummary(TEAM_UP_MATCH, LocalDate.of(2026, 10, 11), "Convoy",
                        "Tokyo 2099: Spider-Islands", "The Thing", 38, 19, 3, false));
    }

    @Override
    public MatchDetails getMatchDetails(String matchId) {
        MatchDetails match = matches.get(matchId);
        if (match == null) {
            throw new NoSuchElementException("No fake match with id " + matchId);
        }
        return match;
    }

    /** Uploader Player29. One name is covered by a "MENU" popup, so it's null. */
    private static MatchDetails player29Match() {
        return new MatchDetails(PLAYER29_MATCH, "Convoy", "Tokyo 2099: Spider-Islands",
                Duration.ofMinutes(16).plusSeconds(7), MatchResult.VICTORY, "10378920354", List.of(),
                List.of(
                        //   name             team role        last hero                K   D   A  FH  damage blocked healing acc
                        row("Plàyer12",         "A", VANGUARD,   "Hulk",                 19, 10,  6,  7, 11418, 42180,     0, 33.0),
                        medals(row(null,             "A", VANGUARD,   "Doctor Strange",       30,  9,  0,  9, 17697, 49185,     0, 42.0), null, MOST_BLOCKED),
                        medals(row("Player09",   "A", DUELIST,    "Squirrel Girl",        22, 10,  1, 13, 21532,  9073,     0, 33.0), 3),
                        medals(mvp(row("player44", "A", DUELIST, "Scarlet Witch",       43, 11,  0, 26, 26376, 15842,     0, 55.0)), 5, MOST_KOS),
                        medals(uploader(row("Player29", "A", STRATEGIST, "Rocket Raccoon",      7,  7, 33,  1,  1718,  6066, 31613, 20.0)), null, MOST_ASSISTS),
                        medals(row("player34",           "A", STRATEGIST, "Luna Snow",            18, 13, 20,  4,  6005, 13062, 40886, 50.0), null, MOST_HEALING),
                        row("Player15",       "B", VANGUARD,   "Groot",                22, 10,  0,  7, 21497, 42886,     0, 55.0),
                        medals(svp(row("player32",    "B", DUELIST,    "Moon Knight",          35,  8,  1, 24, 31663,  7651,     0, 34.0)), 3, MOST_DAMAGE),
                        medals(row("Player06",      "B", DUELIST,    "Star-Lord",            33, 12,  0, 16, 26521, 11791,     0, 31.0), 4),
                        row("Player11",     "B", STRATEGIST, "Cloak & Dagger",       24,  8, 14,  5, 14079, 12215, 26594, 86.0),
                        row("Player30",  "B", STRATEGIST, "Luna Snow",             5, 11, 16,  1,  5018, 10123, 14531, 33.0),
                        medals(row("player38-",          "B", STRATEGIST, "Jeff the Land Shark",  19, 12, 13,  7, 12921, 10194, 16311, 51.0), 3)));
    }

    /**
     * Uploader Player08; every other name was blacked out. The MVP is a Deadpool whose row has the
     * Strategist icon. His stats look like a Duelist's, which is why the icon beats the stats guess.
     * Six rows show a Proficiency Badge on the portrait's corner (some blue, some pink); the tiers
     * aren't confirmed, so they're UNREADABLE. Both Invisible Women have the animated Lord portrait,
     * matched to one the uploader confirmed in another match. Rows without a badge are left unread.
     */
    private static MatchDetails deadpoolMatch() {
        return new MatchDetails(DEADPOOL_MATCH, "Convoy", "Tokyo 2099: Spider-Islands",
                Duration.ofMinutes(17).plusSeconds(50), MatchResult.VICTORY, null, List.of(),
                List.of(
                        //   name         team role        last hero          K   D   A  FH  damage blocked healing acc
                        medals(row(null,         "A", VANGUARD,   "Magneto",        34,  7,  5, 12, 28387, 55292,     0, 58.0), null, MOST_BLOCKED),
                        medals(row(null,         "A", VANGUARD,   "Thor",           31,  9,  4, 14, 27365, 52370,  2422, 45.0), 3),
                        badge(medals(row(null,   "A", DUELIST,    "Namor",          34,  6,  0, 19, 30315,  8048,     0,  0.0), 4), null),
                        badge(medals(uploader(row("Player08", "A", STRATEGIST, "Cloak & Dagger", 32, 0, 53, 5, 17085, 7622, 50176, 90.0)), null, MOST_HEALING, MOST_ASSISTS), null),
                        badge(row(null,   "A", STRATEGIST, "Invisible Woman",25,  2, 37,  4, 14064, 15855, 39086, 49.0), ANIMATED_LORD),
                        badge(medals(mvp(row(null, "A", STRATEGIST, "Deadpool",     35,  7,  8, 16, 36204, 14732,  8208, 51.0)), null, MOST_KOS, MOST_DAMAGE), null),
                        row(null,         "B", VANGUARD,   "Doctor Strange", 17, 12, 18,  2, 16499, 23086, 28697,  0.0),
                        badge(svp(row(null, "B", VANGUARD,   "Peni Parker",    21,  7, 16,  8, 27144, 49370,  5888, 26.0)), null),
                        row(null,         "B", VANGUARD,   "Emma Frost",     18, 14,  3,  8, 31139, 44340,     0, 53.0),
                        medals(row(null,         "B", DUELIST,    "Phoenix",        17, 14,  1,  8, 25219, 18115,  1535, 36.0), 3),
                        row(null,         "B", STRATEGIST, "Mantis",          8, 13, 15,  1, 17375, 18208, 24720, 38.0),
                        badge(row(null,   "B", STRATEGIST, "Invisible Woman",13, 10, 23,  4, 14342, 26851, 35392, 51.0), ANIMATED_LORD)));
    }

    /**
     * Uploader Player10<3, a 28-minute defeat. This scoreboard has the Season 9 team-up box beside each
     * portrait. Two boxes are in full color: Cloak & Dagger's Frozen Haven (Luna Snow is on the team) and
     * Luna Snow's Atlas Bond (White Fox is on the team). Six heroes were banned; the bans aren't entered.
     */
    private static MatchDetails teamUpMatch() {
        return new MatchDetails(TEAM_UP_MATCH, "Convoy", "Tokyo 2099: Spider-Islands",
                Duration.ofMinutes(28).plusSeconds(23), MatchResult.DEFEAT, null, List.of(),
                List.of(
                        //          name           team role        last hero                K   D   A  FH  damage  blocked healing acc        team-up partner, enhanced
                        teamUp(row("Player 21",     "A", VANGUARD,   "Venom",               30, 21, 11, 10,  57303, 103742,   2792, 39.0), "Phoenix", false),
                        medals(svp(uploader(teamUp(row("Player10<3", "A", VANGUARD, "The Thing",   38, 19,  3, 21, 126909, 146368,     65, 36.0), "Human Torch", false))), 4, MOST_DAMAGE),
                        medals(teamUp(row("player__37",       "A", DUELIST,    "The Punisher",        30, 18,  8, 18,  71089,  35522,  11213, 54.0), "Daredevil", false), 3),
                        teamUp(row("Player20",  "A", STRATEGIST, "Cloak & Dagger",      34, 23, 17,  4,  42263,  65477,  14633, 92.0), "Luna Snow", true),
                        teamUp(row("Player 14",   "A", STRATEGIST, "Luna Snow",           36, 23, 40, 13,  42653,  23813,  76918, 63.0), "White Fox", true),
                        medals(teamUp(row("player42",      "A", STRATEGIST, "White Fox",           23, 14, 38,  5,  29549,  38781, 102910, 82.0), "Black Cat", false), null, MOST_HEALING),
                        teamUp(row("Player27",   "B", VANGUARD,   "The Thing",           68, 13, 15, 25,  91277, 111167,     84, 57.0), "Human Torch", false),
                        medals(teamUp(row("Player_16","B", VANGUARD,   "Devil Dinosaur",      69, 13, 35, 35,  84859, 173521,     75, 68.0), "The Punisher", false), 3, MOST_KOS, MOST_BLOCKED),
                        teamUp(row("Player24",     "B", DUELIST,    "Winter Soldier",      56, 14, 10, 17,  64395,  50162,     29, 55.0), "The Punisher", false),
                        teamUp(row("player40",     "B", STRATEGIST, "Jeff the Land Shark", 58, 11, 63, 11,  44627,  21585,  63069, 84.0), "Venom", false),
                        medals(mvp(teamUp(row("player36", "B", STRATEGIST, "White Fox",           68, 12, 68, 15,  54892,  42519,  63014, 76.0), "Black Cat", false)), null, MOST_ASSISTS),
                        teamUp(row("player35",     "B", STRATEGIST, "Jubilee",             55,  9, 66,  9,  39136,  16777,  94365, 43.0), "The Hood", false)));
    }

    /**
     * A plain row: no hover list, no team-up box, not the uploader, no MVP/SVP,
     * no rank (the game doesn't show one).
     */
    private static PlayerLine row(String name, String team, Role role, String lastHero,
                                  int kills, int deaths, int assists, int finalHits,
                                  int damage, int blocked, int healing, Double accuracy) {
        return new PlayerLine(name, team, role, lastHero, List.<HeroStats>of(), null, null, kills, deaths, assists,
                finalHits, damage, blocked, healing, accuracy, false, false, false, List.of(), null, null, null);
    }

    /** Adds the team-up box: the partner pictured in it, and whether it was in full color (enhanced). */
    private static PlayerLine teamUp(PlayerLine p, String partner, boolean enhanced) {
        return p.withTeamUp(new EquippedTeamUp(partner, enhanced));
    }

    /** Adds the "Medals" column: the KO streak number (null if none) and any medals. */
    private static PlayerLine medals(PlayerLine p, Integer multiKill, Medal... medals) {
        return p.withMedals(List.of(medals), multiKill);
    }

    /**
     * A Proficiency Badge whose tier isn't confirmed, plus the portrait style (null when it couldn't
     * be told whether the art is a Lord variant).
     */
    private static PlayerLine badge(PlayerLine p, PortraitStyle portrait) {
        return p.withProficiency(new ProficiencyShown(portrait, ProficiencyBadge.UNREADABLE));
    }

    private static PlayerLine uploader(PlayerLine p) {
        return new PlayerLine(p.name(), p.team(), p.role(), p.lastHero(), p.heroes(), p.teamUp(), p.proficiency(), p.kills(), p.deaths(),
                p.assists(), p.finalHits(), p.damage(), p.blocked(), p.healing(), p.accuracyPercent(),
                true, p.mvp(), p.svp(), p.medals(), p.multiKill(), p.rank(), p.rankScore());
    }

    private static PlayerLine mvp(PlayerLine p) {
        return new PlayerLine(p.name(), p.team(), p.role(), p.lastHero(), p.heroes(), p.teamUp(), p.proficiency(), p.kills(), p.deaths(),
                p.assists(), p.finalHits(), p.damage(), p.blocked(), p.healing(), p.accuracyPercent(),
                p.uploader(), true, p.svp(), p.medals(), p.multiKill(), p.rank(), p.rankScore());
    }

    private static PlayerLine svp(PlayerLine p) {
        return new PlayerLine(p.name(), p.team(), p.role(), p.lastHero(), p.heroes(), p.teamUp(), p.proficiency(), p.kills(), p.deaths(),
                p.assists(), p.finalHits(), p.damage(), p.blocked(), p.healing(), p.accuracyPercent(),
                p.uploader(), p.mvp(), true, p.medals(), p.multiKill(), p.rank(), p.rankScore());
    }
}

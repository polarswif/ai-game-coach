package com.gamecoach.datasource;

import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.MatchSummary;
import com.gamecoach.model.PlayerLine;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Development stand-in built from a real tracker.gg scoreboard (Oct 3, 2026).
 * Active with the "dev" Spring profile.
 */
@Component
@Profile("dev")
public class FakeMatchHistorySource implements MatchHistorySource {

    private static final String MATCH_ID = "5517881_1791071770_1288027_11001_11";

    @Override
    public List<MatchSummary> getRecentMatches(String username) {
        // Same list for any username. Add more rows here when you want a longer picker.
        return List.of(new MatchSummary(
                MATCH_ID, LocalDate.of(2026, 10, 3), "Competitive Domination", "Hell's Heaven",
                "Mantis", 24, 9, 30, true));
    }

    @Override
    public MatchDetails getMatchDetails(String matchId) {
        if (!MATCH_ID.equals(matchId)) {
            throw new NoSuchElementException("No fake match with id " + matchId);
        }
        return new MatchDetails(
                MATCH_ID,
                "Competitive Domination",
                "Hell's Heaven",
                Duration.ofMinutes(15).plusSeconds(20),
                2,
                1,
                List.of(
                        // Team A
                        new PlayerLine("player41",  List.of("Mister Fantastic"), "A", "G2", 4654, 39,  6,  1, 26330, 27475,     0, 56.1),
                        new PlayerLine("PlayerOne",    List.of("Mantis", "Cloak & Dagger"), "A", "G2", 4602, 24, 9, 30, 11737, 13333, 29180, 38.6),
                        new PlayerLine("Player13",        List.of("Emma Frost"), "A", "G2", 4625, 34, 11,  9, 23672, 37667,     0, 36.3),
                        new PlayerLine("Player19", List.of("Doctor Strange"), "A", "G2", 4643, 29,  8,  1, 21537, 43849,     0, 40.1),
                        new PlayerLine("-Player02-",   List.of("Rocket Raccoon", "Invisible Woman"), "A", "G3", 4555, 9, 12, 30, 3543, 9952, 33746, 41.3),
                        new PlayerLine("player33",         List.of("Winter Soldier", "Venom"), "A", "G2", 4646, 29, 11, 6, 21755, 12973, 0, 43.8),
                        // Team B
                        new PlayerLine("Player31",     List.of("Cloak & Dagger", "Rocket Raccoon", "Invisible Woman"), "B", "G2", 4643, 15, 9, 23, 8632, 11085, 36367, 85.0),
                        new PlayerLine("player 43",    List.of("Spider-Man"), "B", "G2", 4647, 26, 8, 5, 17269, 8267, 0, 27.0),
                        new PlayerLine("Player 05",  List.of("Jeff the Land Shark", "Ultron"), "B", "G2", 4662, 23, 13, 26, 14682, 10911, 28231, 74.1),
                        new PlayerLine("Player_22",    List.of("Emma Frost", "Winter Soldier", "Doctor Strange"), "B", "G2", 4606, 22, 9, 5, 25526, 35846, 0, 35.3),
                        new PlayerLine("player39",       List.of("Mister Fantastic"), "B", "G2", 4640, 25, 12,  2, 27969, 25052,     0,  0.0),
                        new PlayerLine("Player17",         List.of("Emma Frost", "The Hood", "Bruce Banner", "Magneto", "Doctor Strange", "Angela", "Jubilee"),
                                "B", "G2", 4601, 20, 13, 1, 18415, 41829, 90, 24.1)));
    }
}

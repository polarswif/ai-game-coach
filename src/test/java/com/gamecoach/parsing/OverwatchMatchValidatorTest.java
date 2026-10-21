package com.gamecoach.parsing;

import com.gamecoach.OverwatchMatchFixtures;
import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.model.OverwatchPlayerLine;
import com.gamecoach.model.OverwatchResult;
import com.gamecoach.model.OverwatchRole;
import com.gamecoach.model.RoundScore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverwatchMatchValidatorTest {

    private final OverwatchMatchValidator validator = new OverwatchMatchValidator(new OverwatchHeroCatalog());

    private static boolean has(List<String> warnings, String text) {
        return warnings.stream().anyMatch(w -> w.contains(text));
    }

    @Test
    void theRealMatchPassesEveryCheck() {
        assertEquals(List.of(), validator.check(OverwatchMatchFixtures.colosseo()));
    }

    @Test
    void withoutTheSummaryItAsksForIt() {
        assertTrue(has(validator.check(OverwatchMatchFixtures.teamsOnly()), "Add the Summary screenshot"));
    }

    @Test
    void yourPortraitMustBeOneOfYourHeroes() {
        OverwatchMatch m = OverwatchMatchFixtures.colosseo();
        List<OverwatchPlayerLine> players = new ArrayList<>(m.players());
        players.set(0, players.get(0).withHero("Orisa"));
        assertTrue(has(validator.check(m.withPlayers(players)), "Your row's portrait reads Orisa"));
    }

    @Test
    void aScoreThatContradictsTheResultIsFlagged() {
        OverwatchMatch m = OverwatchMatchFixtures.colosseo();
        OverwatchMatch wrong = new OverwatchMatch(m.matchId(), m.mode(), m.map(), m.duration(), OverwatchResult.DEFEAT,
                new RoundScore(1, 0), null, m.players(), m.yourHeroes(), m.heroStats());
        assertTrue(has(validator.check(wrong), "doesn't fit DEFEAT"));
    }

    @Test
    void aMissingRowOrWrongRoleIconIsFlagged() {
        List<OverwatchPlayerLine> players = new ArrayList<>(OverwatchMatchFixtures.players());
        players.remove(9);
        players.set(6, OverwatchMatchFixtures.row("PLAYERTWENTYTHREE", null, "B", OverwatchRole.SUPPORT, "Ashe", 5, 2, 5, 3518, 0, 0, false));
        List<String> warnings = validator.check(OverwatchMatchFixtures.colosseo().withPlayers(players));
        assertTrue(has(warnings, "Read 5 players on your team and 4"));
        assertTrue(has(warnings, "the role icon reads SUPPORT but Ashe is DAMAGE"));
    }

    @Test
    void personalStatsForAHeroYouDidntPlayAreFlagged() {
        OverwatchMatch m = OverwatchMatchFixtures.colosseo();
        OverwatchMatch wrong = new OverwatchMatch(m.matchId(), m.mode(), m.map(), m.duration(), m.result(), m.score(),
                null, m.players(), m.yourHeroes(),
                List.of(new OverwatchMatch.HeroStatsPanel("Reinhardt", List.of())));
        assertTrue(has(validator.check(wrong), "Personal stats are for Reinhardt"));
    }
}

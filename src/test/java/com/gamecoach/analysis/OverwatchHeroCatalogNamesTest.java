package com.gamecoach.analysis;

import com.gamecoach.model.OverwatchRole;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Looking heroes up by the names people type and the game prints. */
class OverwatchHeroCatalogNamesTest {

    private final OverwatchHeroCatalog heroes = new OverwatchHeroCatalog();

    @Test
    void namesAreMatchedIgnoringCaseSpacesPunctuationAndAccents() {
        assertEquals(Optional.of("Soldier: 76"), heroes.officialName("soldier 76"));
        assertEquals(Optional.of("Lúcio"), heroes.officialName("LUCIO"));
        assertEquals(Optional.of("D.Va"), heroes.officialName("dva"));
        assertEquals(Optional.of("Torbjörn"), heroes.officialName("Torbjorn"));
        assertEquals(Optional.of("Wrecking Ball"), heroes.officialName("wrecking-ball"));
        assertEquals(Optional.empty(), heroes.officialName("Daredevil"));
    }

    @Test
    void rolesAndTheNameListComeFromTheTable() {
        assertEquals(OverwatchRole.TANK, heroes.roleOfName("ramattra"));
        assertTrue(heroes.heroNames().contains("Ramattra"));
        assertTrue(OverwatchHeroCatalog.sameHero("Soldier 76", "Soldier: 76"));
    }
}

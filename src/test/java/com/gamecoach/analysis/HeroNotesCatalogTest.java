package com.gamecoach.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroNotesCatalogTest {

    private final HeroNotesCatalog notes = new HeroNotesCatalog();

    @Test
    void rocketHasHisRangeAndPositioningNotes() {
        assertEquals(2, notes.forHero("Rocket Raccoon").size());
        assertTrue(notes.forHero("Rocket Raccoon").get(0).contains("short range"));
        assertTrue(notes.forHero("Rocket Raccoon").get(1).contains("positioning trade-off"));
    }

    @Test
    void heroesWithoutNotesGetAnEmptyList() {
        assertTrue(notes.forHero("Deadpool").isEmpty());
        assertTrue(notes.forHero("Not A Hero").isEmpty());
        assertTrue(notes.forHero(null).isEmpty());
    }
}

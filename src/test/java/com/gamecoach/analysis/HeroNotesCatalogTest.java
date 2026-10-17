package com.gamecoach.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroNotesCatalogTest {

    private final HeroNotesCatalog notes = new HeroNotesCatalog();

    @Test
    void rocketHasHisRangeAndPositioningNotes() {
        List<String> rocket = notes.forHero("Rocket Raccoon");
        assertTrue(rocket.stream().anyMatch(n -> n.contains("short range")));
        assertTrue(rocket.stream().anyMatch(n -> n.contains("positioning trade-off")));
    }

    @Test
    void everyHeroTheUserCheckedHasNotes() {
        for (String hero : List.of("Magneto", "Doctor Strange", "Hulk", "Thor", "Luna Snow",
                "Invisible Woman", "Rocket Raccoon")) {
            assertFalse(notes.forHero(hero).isEmpty(), hero);
        }
    }

    @Test
    void aSlashListsNamesForTheSameHero() {
        assertEquals(notes.forHero("Hulk"), notes.forHero("Bruce Banner"));
        assertTrue(notes.forHero("bruce banner").get(0).contains("brawl tank"));   // any case
    }

    @Test
    void heroesWithoutNotesGetAnEmptyList() {
        assertTrue(notes.forHero("Deadpool").isEmpty());
        assertTrue(notes.forHero("Not A Hero").isEmpty());
        assertTrue(notes.forHero(null).isEmpty());
    }
}

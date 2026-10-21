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

    /**
     * Every hero the scoreboard can show must have notes, so the coach never meets a hero it knows
     * nothing about. When a new hero is added to the roles table, this fails until notes are written.
     */
    @Test
    void everyHeroInTheRosterHasNotes() {
        List<String> missing = new HeroRoleCatalog().heroNames().stream()
                .filter(hero -> notes.forHero(hero).isEmpty())
                .toList();
        assertEquals(List.of(), missing, "Heroes with no notes");
    }

    @Test
    void deadpoolGetsSharedNotesPlusHisRolesOwn() {
        List<String> support = notes.forHero("Deadpool (Strategist)");
        assertTrue(support.get(0).contains("any of the three roles"));                 // shared line first
        assertTrue(support.stream().anyMatch(n -> n.contains("healing hop")));
        assertFalse(support.stream().anyMatch(n -> n.contains("Ban Hammer")));        // not the tank's notes
        assertTrue(notes.forHero("Deadpool (Vanguard)").stream().anyMatch(n -> n.contains("Ban Hammer")));
    }

    @Test
    void aSlashListsNamesForTheSameHero() {
        assertEquals(notes.forHero("Hulk"), notes.forHero("Bruce Banner"));
        assertTrue(notes.forHero("bruce banner").get(0).contains("brawl tank"));   // any case
    }

    @Test
    void diveDuelistsAreJudgedByFinalHits() {
        assertTrue(notes.forHero("Black Panther").get(0).contains("final hits"));
        assertTrue(notes.forHero("Hulk").stream().anyMatch(n -> n.contains("Monster Hulk") && n.contains("loses the bubble")));
    }

    @Test
    void heroesWithoutNotesGetAnEmptyList() {
        assertTrue(notes.forHero("Not A Hero").isEmpty());
        assertTrue(notes.forHero(null).isEmpty());
    }
}

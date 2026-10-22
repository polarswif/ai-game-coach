package com.gamecoach.analysis;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverwatchHeroNotesCatalogTest {

    private final OverwatchHeroCatalog heroes = new OverwatchHeroCatalog();
    private final OverwatchHeroNotesCatalog notes = new OverwatchHeroNotesCatalog();

    @Test
    void everyHeroInTheTableHasNotes() {
        for (String hero : heroes.heroNames()) {
            assertFalse(notes.forHero(hero).isEmpty(), hero + " has no notes");
        }
    }

    @Test
    void everyNoteIsForAHeroInTheTable() throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/heroes/overwatch-hero-notes.txt"), StandardCharsets.UTF_8))) {
            r.lines().map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(l -> l.substring(0, l.indexOf('|')))
                    .forEach(h -> assertTrue(heroes.officialName(h).isPresent(), "Unknown hero in notes: " + h));
        }
    }

    @Test
    void namesMatchTheForgivingWay() {
        assertFalse(notes.forHero("torbjörn").isEmpty());
        assertFalse(notes.forHero("Soldier 76").isEmpty());
        assertEquals(List.of("A note."), OverwatchHeroNotesCatalog.fromText("Lúcio|A note.").forHero("lucio"));
    }
}

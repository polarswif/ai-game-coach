package com.gamecoach.analysis;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Researched game knowledge per Overwatch hero, from resources/heroes/overwatch-hero-notes.txt,
 * in the same hero|note format as the Rivals notes. Names are matched the forgiving way
 * OverwatchHeroCatalog matches them (case, punctuation and accents ignored).
 */
@Component
public class OverwatchHeroNotesCatalog {

    private static final String DEFAULT_FILE = "/heroes/overwatch-hero-notes.txt";

    private final Map<String, List<String>> notesByHero = new HashMap<>();

    public OverwatchHeroNotesCatalog() {
        try (InputStream in = OverwatchHeroNotesCatalog.class.getResourceAsStream(DEFAULT_FILE)) {
            if (in == null) {
                throw new IllegalStateException("Missing hero notes " + DEFAULT_FILE);
            }
            load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private OverwatchHeroNotesCatalog(InputStream in) {
        try {
            load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Notes from text in the hero|note format, so tests control which notes exist. */
    public static OverwatchHeroNotesCatalog fromText(String text) {
        return new OverwatchHeroNotesCatalog(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    /** The hero's notes in file order; empty when there are none. */
    public List<String> forHero(String hero) {
        return hero == null ? List.of() : notesByHero.getOrDefault(OverwatchHeroCatalog.squash(hero), List.of());
    }

    private void load(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] cols = line.split("\\|", 2);
            if (cols.length != 2 || cols[0].isBlank() || cols[1].isBlank()) {
                throw new IllegalStateException("Hero note needs 2 columns (hero|note): " + line);
            }
            notesByHero.computeIfAbsent(OverwatchHeroCatalog.squash(cols[0]), k -> new ArrayList<>()).add(cols[1].strip());
        }
    }
}

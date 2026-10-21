package com.gamecoach.analysis;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
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
 * Hand-written game knowledge per hero, from resources/heroes/marvel-rivals-hero-notes.txt.
 * The scoreboard only has numbers; these notes tell the coach how to read them for a given hero
 * (for example, why low accuracy on Rocket Raccoon is usually about distance, not aim).
 * This is the start of the curated knowledge base: notes confirmed by players, added one at a time.
 */
@Component
public class HeroNotesCatalog {

    private static final String DEFAULT_FILE = "/heroes/marvel-rivals-hero-notes.txt";

    private final Map<String, List<String>> notesByHero = new HashMap<>();

    /** Reads the notes file from resources; this is the one Spring uses. */
    public HeroNotesCatalog() {
        this(DEFAULT_FILE);
    }

    /** Reads notes from text in the same hero|note format. Tests use it to control which notes exist. */
    public static HeroNotesCatalog fromText(String text) {
        return new HeroNotesCatalog(new java.io.ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private HeroNotesCatalog(InputStream in) {
        try {
            load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private HeroNotesCatalog(String resource) {
        try (InputStream in = HeroNotesCatalog.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing hero notes " + resource);
            }
            load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The notes for a hero, in file order. Empty when there are none yet. Names are matched exactly
     * (ignoring case and spaces), so "Deadpool (Strategist)" gets the shared Deadpool notes plus the
     * Strategist ones, not the Vanguard's. A Deadpool label with no notes of its own falls back to
     * plain "Deadpool".
     */
    public List<String> forHero(String hero) {
        if (hero == null) {
            return List.of();
        }
        List<String> exact = notesByHero.get(HeroRoleCatalog.normalize(hero));
        if (exact != null) {
            return exact;
        }
        return HeroRoleCatalog.isDeadpool(hero) ? notesByHero.getOrDefault("deadpool", List.of()) : List.of();
    }

    private void load(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // limit 2: split only at the first "|", so a note may itself contain a bar.
            String[] cols = line.split("\\|", 2);
            if (cols.length != 2 || cols[0].isBlank() || cols[1].isBlank()) {
                throw new IllegalStateException("Hero note needs 2 columns (hero|note): " + line);
            }
            // "Hulk/Bruce Banner": the same note under every name the scoreboard can show.
            for (String name : cols[0].split("/")) {
                if (!name.isBlank()) {
                    notesByHero.computeIfAbsent(HeroRoleCatalog.normalize(name.strip()), k -> new ArrayList<>())
                            .add(cols[1].strip());
                }
            }
        }
    }
}

package com.gamecoach.analysis;

import com.gamecoach.model.OverwatchRole;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Overwatch hero keys ("soldier-76") → display name ("Soldier: 76") and role, from
 * resources/heroes/overwatch-heroes.csv. OverFast's per-hero stats are keyed by hero key and say
 * nothing about role, so this table supplies it.
 */
@Component
public class OverwatchHeroCatalog {

    private static final String TABLE = "/heroes/overwatch-heroes.csv";

    private record Entry(String name, OverwatchRole role) {
    }

    private final Map<String, Entry> byKey = new HashMap<>();
    private final Map<String, Entry> byName = new HashMap<>();
    private final List<String> names = new ArrayList<>();

    public OverwatchHeroCatalog() {
        try (InputStream in = OverwatchHeroCatalog.class.getResourceAsStream(TABLE)) {
            if (in == null) {
                throw new IllegalStateException("Missing Overwatch hero table " + TABLE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] cols = line.split(",");
                if (cols.length != 3 || OverwatchRole.fromKey(cols[2]) == null) {
                    throw new IllegalStateException("Overwatch hero row needs key,name,role: " + line);
                }
                Entry entry = new Entry(cols[1].strip(), OverwatchRole.fromKey(cols[2]));
                byKey.put(cols[0].strip().toLowerCase(Locale.ROOT), entry);
                byName.put(squash(entry.name()), entry);
                byName.put(squash(cols[0]), entry);     // "soldier-76" and "Soldier: 76" both work
                names.add(entry.name());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** "soldier-76" → "Soldier: 76". An unknown key (a hero newer than the table) comes back as is. */
    public String nameOf(String key) {
        Entry e = key == null ? null : byKey.get(key.toLowerCase(Locale.ROOT));
        return e == null ? key : e.name();
    }

    /** The hero's role, or null for a hero newer than the table. */
    public OverwatchRole roleOf(String key) {
        Entry e = key == null ? null : byKey.get(key.toLowerCase(Locale.ROOT));
        return e == null ? null : e.role();
    }

    /**
     * The hero's display name ("soldier 76", "LUCIO" → "Soldier: 76", "Lúcio"), or empty when the
     * hero isn't in the table. Accepts names as people type them and as the game prints them.
     */
    public Optional<String> officialName(String hero) {
        Entry e = hero == null ? null : byName.get(squash(hero));
        return Optional.ofNullable(e == null ? null : e.name());
    }

    /** The role of a hero given by name; null when the hero isn't in the table. */
    public OverwatchRole roleOfName(String hero) {
        Entry e = hero == null ? null : byName.get(squash(hero));
        return e == null ? null : e.role();
    }

    /** Every display name, in table order. The screenshot reader gives Claude this list. */
    public List<String> heroNames() {
        return List.copyOf(names);
    }

    /** True when two names are the same hero ("Soldier 76" and "Soldier: 76"). */
    public static boolean sameHero(String a, String b) {
        return a != null && b != null && squash(a).equals(squash(b));
    }

    /** Lowercase letters and digits only, accents removed: "Torbjörn" → "torbjorn", "D.Va" → "dva". */
    static String squash(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}

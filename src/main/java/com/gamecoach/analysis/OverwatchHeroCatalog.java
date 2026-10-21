package com.gamecoach.analysis;

import com.gamecoach.model.OverwatchRole;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
                byKey.put(cols[0].strip().toLowerCase(Locale.ROOT), new Entry(cols[1].strip(), OverwatchRole.fromKey(cols[2])));
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
}

package com.gamecoach.analysis;

import com.gamecoach.model.Role;

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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Looks up a Marvel Rivals hero's role from the table in resources/heroes/marvel-rivals-roles.csv.
 * The scoreboard's role icon only covers each player's last hero; this table covers every hero,
 * including the others in a player's "Heroes Used" list.
 */
@Component
public class HeroRoleCatalog {

    private static final String DEFAULT_TABLE = "/heroes/marvel-rivals-roles.csv";

    private final Map<String, Role> rolesByHero = new HashMap<>();
    private final List<String> names = new ArrayList<>();
    private final Map<String, String> namesByKey = new HashMap<>();

    public HeroRoleCatalog() {
        try (InputStream in = HeroRoleCatalog.class.getResourceAsStream(DEFAULT_TABLE)) {
            if (in == null) {
                throw new IllegalStateException("Missing hero role table " + DEFAULT_TABLE);
            }
            load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Role for a hero name, ignoring case and extra spaces. UNKNOWN if the hero is not listed. */
    public Role roleOf(String hero) {
        if (hero == null) {
            return Role.UNKNOWN;
        }
        return rolesByHero.getOrDefault(normalize(hero), Role.UNKNOWN);
    }

    /**
     * The hero's name spelled as in the table ("daredevil " → "Daredevil"), or empty when the hero
     * isn't listed. Used for names people type, so the rest of the app sees one spelling.
     */
    public Optional<String> officialName(String hero) {
        return hero == null ? Optional.empty() : Optional.ofNullable(namesByKey.get(normalize(hero)));
    }

    /**
     * One name per hero, as written first in the table, in file order (other names like "Bruce Banner" left out).
     * The screenshot reader gives this list to Claude so it uses the exact names.
     */
    public List<String> heroNames() {
        return List.copyOf(names);
    }

    /** True for "Deadpool" and the labeled forms like "Deadpool (Strategist)". */
    public static boolean isDeadpool(String hero) {
        return hero != null && normalize(hero).startsWith("deadpool");
    }

    /**
     * True when two hero names are the same hero, ignoring case and extra spaces.
     * All Deadpool labels count as the same hero: "Deadpool (Duelist)" matches "Deadpool".
     */
    public static boolean sameHero(String a, String b) {
        return a != null && b != null && baseName(a).equals(baseName(b));
    }

    /** The normalized name with any Deadpool role label removed, e.g. "deadpool". */
    static String baseName(String hero) {
        return isDeadpool(hero) ? "deadpool" : normalize(hero);
    }

    private void load(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;   // skip blank lines and comments
            }
            int comma = line.lastIndexOf(',');
            Role role = Role.valueOf(line.substring(comma + 1).strip());
            // "Hulk/Bruce Banner": one hero under several names. The first is the name we use;
            // the others still match it, so a reader or player saying "Bruce Banner" gets Hulk.
            String[] spellings = line.substring(0, comma).split("/");
            String hero = spellings[0].strip();
            names.add(hero);
            for (String spelling : spellings) {
                rolesByHero.put(normalize(spelling), role);
                namesByKey.put(normalize(spelling), hero);
            }
        }
    }

    static String normalize(String hero) {
        return hero.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}

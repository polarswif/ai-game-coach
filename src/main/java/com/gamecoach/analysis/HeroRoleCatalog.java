package com.gamecoach.analysis;

import com.gamecoach.model.Role;

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
 * Looks up a Marvel Rivals hero's role. The scoreboard never shows roles,
 * so they come from the table in resources/heroes/marvel-rivals-roles.csv.
 */
@Component
public class HeroRoleCatalog {

    private static final String DEFAULT_TABLE = "/heroes/marvel-rivals-roles.csv";

    private final Map<String, Role> rolesByHero = new HashMap<>();

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

    /** True for "Deadpool" and the labeled forms like "Deadpool (Strategist)". */
    public static boolean isDeadpool(String hero) {
        return hero != null && normalize(hero).startsWith("deadpool");
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
            String hero = line.substring(0, comma);
            Role role = Role.valueOf(line.substring(comma + 1).strip());
            rolesByHero.put(normalize(hero), role);
        }
    }

    private static String normalize(String hero) {
        return hero.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}

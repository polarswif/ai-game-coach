package com.gamecoach.analysis;

import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeroRoleCatalogTest {

    private final HeroRoleCatalog catalog = new HeroRoleCatalog();

    @Test
    void knownHeroesHaveTheirRoles() {
        assertEquals(Role.STRATEGIST, catalog.roleOf("Mantis"));
        assertEquals(Role.DUELIST, catalog.roleOf("Spider-Man"));
        assertEquals(Role.VANGUARD, catalog.roleOf("Bruce Banner"));
    }

    @Test
    void lookupIgnoresCaseAndExtraSpaces() {
        assertEquals(Role.STRATEGIST, catalog.roleOf("Jeff The Land Shark"));
        assertEquals(Role.STRATEGIST, catalog.roleOf("  cloak  &  dagger "));
    }

    @Test
    void unlistedHeroesAreUnknown() {
        assertEquals(Role.UNKNOWN, catalog.roleOf("Not A Hero"));
        assertEquals(Role.UNKNOWN, catalog.roleOf(null));
    }
}

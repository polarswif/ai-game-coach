package com.gamecoach.analysis;

import com.gamecoach.model.Role;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroRoleCatalogTest {

    private final HeroRoleCatalog catalog = new HeroRoleCatalog();

    @Test
    void knownHeroesHaveTheirRoles() {
        assertEquals(Role.STRATEGIST, catalog.roleOf("Mantis"));
        assertEquals(Role.DUELIST, catalog.roleOf("Spider-Man"));
        assertEquals(Role.VANGUARD, catalog.roleOf("Bruce Banner"));
    }

    @Test
    void newerHeroesAreListed() {
        assertEquals(Role.DUELIST, catalog.roleOf("Gorr"));
        assertEquals(Role.DUELIST, catalog.roleOf("Gorr the God Butcher"));
        assertEquals(Role.VANGUARD, catalog.roleOf("The Hood"));
        assertEquals(Role.STRATEGIST, catalog.roleOf("Jubilee"));
        assertEquals(Role.VANGUARD, catalog.roleOf("Devil Dinosaur"));
    }

    @Test
    void deadpoolUsesTheCommunityLabels() {
        assertEquals(Role.VANGUARD, catalog.roleOf("Deadpool (Vanguard)"));
        assertEquals(Role.DUELIST, catalog.roleOf("deadpool (duelist)"));
        assertEquals(Role.STRATEGIST, catalog.roleOf("Deadpool (Strategist)"));
        assertEquals(Role.UNKNOWN, catalog.roleOf("Deadpool"));   // no label: the row's role icon decides
        assertTrue(HeroRoleCatalog.isDeadpool("Deadpool (Duelist)"));
        assertFalse(HeroRoleCatalog.isDeadpool("Daredevil"));
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

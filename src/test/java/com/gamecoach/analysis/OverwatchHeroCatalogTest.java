package com.gamecoach.analysis;

import com.gamecoach.model.OverwatchRole;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OverwatchHeroCatalogTest {

    private final OverwatchHeroCatalog heroes = new OverwatchHeroCatalog();

    @Test
    void keysBecomeNamesAndRoles() {
        assertEquals("Soldier: 76", heroes.nameOf("soldier-76"));
        assertEquals(OverwatchRole.DAMAGE, heroes.roleOf("soldier-76"));
        assertEquals(OverwatchRole.SUPPORT, heroes.roleOf("ANA"));               // any case
        assertEquals(OverwatchRole.TANK, heroes.roleOf("junker-queen"));
    }

    @Test
    void aHeroNewerThanTheTableKeepsItsKey() {
        assertEquals("brand-new-hero", heroes.nameOf("brand-new-hero"));
        assertNull(heroes.roleOf("brand-new-hero"));
    }
}

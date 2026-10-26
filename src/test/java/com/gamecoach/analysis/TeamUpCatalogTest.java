package com.gamecoach.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamUpCatalogTest {

    private final TeamUpCatalog catalog = new TeamUpCatalog();
    private final HeroRoleCatalog roles = new HeroRoleCatalog();

    @Test
    void everyHeroHasExactlyTwoTeamUps() {
        for (String hero : List.of("The Thing", "Luna Snow", "Gorr", "Devil Dinosaur", "Jubilee", "White Fox")) {
            assertEquals(2, catalog.forHero(hero).size(), hero);
        }
    }

    @Test
    void findsATeamUpFromHeroAndPartner() {
        TeamUp t = catalog.find("Devil Dinosaur", "Jeff the Land Shark").orElseThrow();
        assertEquals("Surf & Turf", t.name());
        assertEquals("Devil Dinosaur", t.hero());
    }

    @Test
    void lookupIgnoresCaseAndDeadpoolLabels() {
        assertEquals("Frozen Haven", catalog.find("cloak & dagger", "LUNA SNOW").orElseThrow().name());
        assertEquals(2, catalog.forHero("Deadpool (Strategist)").size());
        assertEquals("Loudmouth Mercs", catalog.find("Elsa Bloodstone", "Deadpool (Duelist)").orElseThrow().name());
    }

    @Test
    void everyPartnerIsARealHero() {
        for (String hero : List.of("Angela", "Venom", "Hela", "Storm", "Cloak & Dagger", "Deadpool")) {
            for (TeamUp t : catalog.forHero(hero)) {
                assertTrue(HeroRoleCatalog.isDeadpool(t.partner()) || roles.roleOf(t.partner()) != com.gamecoach.model.Role.UNKNOWN,
                        t.name() + " partner " + t.partner());
            }
        }
    }

    @Test
    void unknownHeroHasNoTeamUps() {
        assertTrue(catalog.forHero("Not A Hero").isEmpty());
        assertTrue(catalog.find("Venom", "Not A Hero").isEmpty());
    }

    @Test
    void teamUpsGoOneWay() {
        assertEquals("Chain of Cyttorak", catalog.find("Magik", "Doctor Strange").orElseThrow().name());
        assertTrue(catalog.find("Doctor Strange", "Magik").isEmpty());   // Strange's box: Hulk or Invisible Woman
        assertTrue(catalog.find("Thor", "Loki").isEmpty());
        assertTrue(catalog.find("Psylocke", "Magik").isEmpty());
        assertEquals(List.of("Hulk", "Invisible Woman"), catalog.partnersOf("Doctor Strange"));
    }
}

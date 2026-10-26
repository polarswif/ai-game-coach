package com.gamecoach.service.claude;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.parsing.Screenshot;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroPortraitSheetTest {

    private final HeroPortraitSheet sheet = new HeroPortraitSheet();
    private final HeroRoleCatalog roles = new HeroRoleCatalog();

    @Test
    void everyHeroHasAPortraitAndEveryPortraitIsAHero() {
        Set<String> pictured = sheet.entries().stream()
                .filter(e -> !HeroRoleCatalog.isDeadpool(e.label()))      // listed per role in the table
                .map(e -> roles.officialName(e.label()).orElseThrow(() -> new AssertionError("Not a hero: " + e.label())))
                .collect(Collectors.toSet());
        List<String> missing = roles.heroNames().stream()
                .filter(h -> !HeroRoleCatalog.isDeadpool(h))
                .filter(h -> !pictured.contains(h))
                .toList();
        assertEquals(List.of(), missing);
        assertTrue(sheet.entries().stream().anyMatch(e -> e.label().equals("Deadpool")));
    }

    @Test
    void theSheetIsOneImageTheApiKeepsAtFullSize() throws Exception {
        Screenshot image = sheet.image().orElseThrow();
        assertEquals("image/png", image.mediaType());
        BufferedImage picture = ImageIO.read(new ByteArrayInputStream(image.bytes()));
        assertNotNull(picture);
        assertTrue((long) picture.getWidth() * picture.getHeight() <= PortraitZoom.MAX_PIXELS);
        assertTrue(Math.max(picture.getWidth(), picture.getHeight()) <= PortraitZoom.MAX_EDGE);
        assertTrue(sheet.image().get() == image);                          // drawn once, then reused
    }
}

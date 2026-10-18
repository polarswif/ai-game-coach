package com.gamecoach.parsing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotTest {

    @Test
    void acceptsTheFormatsClaudeReads() {
        assertEquals("image/jpeg", new Screenshot(new byte[] {1}, "image/JPG").mediaType());
        assertEquals("image/png", new Screenshot(new byte[] {1}, "image/png").mediaType());
    }

    @Test
    void rejectsEmptyHugeAndUnsupportedFiles() {
        assertThrows(BadUploadException.class, () -> new Screenshot(new byte[0], "image/png"));
        assertThrows(BadUploadException.class, () -> new Screenshot(new byte[Screenshot.MAX_BYTES + 1], "image/png"));
        BadUploadException heic = assertThrows(BadUploadException.class, () -> new Screenshot(new byte[] {1}, "image/heic"));
        assertTrue(heic.getMessage().contains("HEIC"));
    }
}

package com.gamecoach.controller;

import com.gamecoach.model.OverwatchMode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverwatchControllerTest {

    @Test
    void modeDefaultsToQuickPlayAndAcceptsShortNames() {
        assertEquals(OverwatchMode.QUICKPLAY, OverwatchController.mode(null));
        assertEquals(OverwatchMode.COMPETITIVE, OverwatchController.mode("Comp"));
        assertTrue(assertThrows(BadRequestException.class, () -> OverwatchController.mode("arcade"))
                .getMessage().contains("quickplay or competitive"));
    }

    @Test
    void platformIsPcConsoleOrBoth() {
        assertNull(OverwatchController.platform(""));
        assertEquals("pc", OverwatchController.platform(" PC "));
        assertThrows(BadRequestException.class, () -> OverwatchController.platform("switch"));
    }
}

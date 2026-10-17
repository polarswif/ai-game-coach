package com.gamecoach.service.claude;

import com.gamecoach.service.CoachingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Starts the whole app with the Claude provider switched on (and a dummy key), to prove Spring wires
 * the Claude classes together and reads the settings. No request is sent.
 */
@SpringBootTest(properties = {
        "gamecoach.coach.provider=claude",
        "gamecoach.claude.api-key=dummy-key-for-tests",
        "gamecoach.claude.model=claude-haiku-5-5"})
class ClaudeWiringTest {

    @Autowired
    private CoachingService coach;

    @Autowired
    private ClaudeProperties properties;

    @Test
    void theClaudeCoachIsUsedAndTheSettingsAreRead() {
        assertInstanceOf(ClaudeCoachingService.class, coach);
        assertEquals("claude-haiku-5-5", properties.model());
        assertEquals(2000, properties.maxTokens());
        assertEquals(60, properties.timeout().toSeconds());
    }
}

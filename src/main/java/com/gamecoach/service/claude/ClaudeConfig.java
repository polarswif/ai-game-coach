package com.gamecoach.service.claude;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Tells Spring to build a ClaudeProperties from the "gamecoach.claude.*" settings, so other classes
 * can ask for it in their constructors. Harmless when the fake coach is active: nothing reads it.
 */
@Configuration
@EnableConfigurationProperties(ClaudeProperties.class)
public class ClaudeConfig {
}

package com.gamecoach.datasource.overfast;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings for OverFast API, from application.properties keys starting with "gamecoach.overfast.".
 *
 * @param baseUrl the public instance by default; point it at a self-hosted copy for heavy use
 * @param timeout how long to wait. A player OverFast hasn't cached yet means a fresh read of
 *                Blizzard's page, which can take several seconds
 */
@ConfigurationProperties(prefix = "gamecoach.overfast")
public record OverFastProperties(String baseUrl, Duration timeout) {

    public static final String PUBLIC_INSTANCE = "https://overfast-api.tekrop.fr";

    public OverFastProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = PUBLIC_INSTANCE;
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(20);
        }
    }
}

package com.gamecoach.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Account settings, from application.properties and environment variables (never hard-coded).
 *
 * @param jwtSecret      key that signs login tokens (GAMECOACH_JWT_SECRET, at least 32 characters). When
 *                       empty, a random one is made at startup, so logins end when the app restarts.
 * @param inviteCodes    the codes that allow signing up (GAMECOACH_INVITE_CODES, comma-separated); each
 *                       works once. The site is invite-only because every upload costs Claude credits.
 * @param tokenLifetime  how long a login lasts before signing in again
 * @param allowedOrigins websites allowed to call the API from a browser (the GitHub Pages address)
 * @param uploadsPerDay  screenshot uploads each account may make per day (UTC), a cap on spending
 */
@ConfigurationProperties(prefix = "gamecoach.auth")
public record AuthProperties(
        String jwtSecret,
        List<String> inviteCodes,
        Duration tokenLifetime,
        List<String> allowedOrigins,
        Integer uploadsPerDay) {

    public AuthProperties {
        jwtSecret = jwtSecret == null ? "" : jwtSecret.strip();
        inviteCodes = clean(inviteCodes);
        tokenLifetime = tokenLifetime == null ? Duration.ofDays(7) : tokenLifetime;
        allowedOrigins = clean(allowedOrigins);
        uploadsPerDay = uploadsPerDay == null || uploadsPerDay < 0 ? 15 : uploadsPerDay;
    }

    /** "a, b," → [a, b]: blank entries come from an empty or trailing-comma environment variable. */
    private static List<String> clean(List<String> values) {
        return values == null ? List.of() : values.stream().map(String::strip).filter(v -> !v.isEmpty()).toList();
    }
}

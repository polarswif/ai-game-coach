package com.gamecoach.persistence;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Turns match and coaching records into JSON text for the database and back, and fingerprints a
 * match. Uses its own Jackson mapper so what's stored doesn't change if the web API's JSON settings do.
 */
@Component
public class MatchJson {

    private final JsonMapper mapper = JsonMapper.builder().build();

    public String write(Object value) {
        return mapper.writeValueAsString(value);
    }

    public <T> T read(String json, Class<T> type) {
        return mapper.readValue(json, type);
    }

    /**
     * A SHA-256 of the record's JSON: the same match always gives the same 64 hex characters, and
     * any change (a corrected hero) gives different ones. Unlike hashCode(), it's the same after a
     * restart, so saved coaching can be found again.
     */
    public String fingerprint(Object value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(write(value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}

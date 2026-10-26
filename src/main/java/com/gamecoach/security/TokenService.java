package com.gamecoach.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Makes login tokens. A JWT is three base64 parts, header.claims.signature: the claims say who
 * ("sub") and until when ("exp"), and the signature proves the server made it, so it can't be edited
 * or forged without the secret key.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final AuthProperties properties;

    public TokenService(JwtEncoder encoder, AuthProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public Token issue(String username) {
        Instant now = Instant.now();
        Instant expires = now.plus(properties.tokenLifetime());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("gamecoach")
                .subject(username)
                .issuedAt(now)
                .expiresAt(expires)
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Token(value, expires);
    }

    public record Token(String value, Instant expiresAt) {
    }
}

package com.example.clientsapi.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Positive Long expirationSeconds
) {

    public static final long DEFAULT_EXPIRATION_SECONDS = 900L;

    public JwtProperties {
        if (expirationSeconds == null) {
            expirationSeconds = DEFAULT_EXPIRATION_SECONDS;
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must not be blank");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) for HS256");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("JWT expiration must be positive");
        }
    }
}

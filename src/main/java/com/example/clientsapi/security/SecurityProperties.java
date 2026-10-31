package com.example.clientsapi.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.auth")
public record SecurityProperties(
        @NotBlank String username,
        @NotBlank String passwordHash
) {
}

package com.example.clientsapi.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credentials used to sign in and obtain a JWT.")
public record LoginRequest(
        @Schema(description = "Username configured for the application.", example = "api-user", maxLength = 100)
        @NotBlank @Size(max = 100) String username,
        @Schema(description = "Plain-text password sent only in the TLS login request.", format = "password", maxLength = 128)
        @NotBlank @Size(max = 128) String password
) {
}

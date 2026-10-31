package com.example.clientsapi.security.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token and its lifetime.")
public record LoginResponse(
        @Schema(description = "Signed JWT to send in the Authorization header.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(example = "Bearer")
        String tokenType,
        @Schema(description = "Token lifetime in seconds.", example = "900", minimum = "1")
        long expiresInSeconds
) {
}

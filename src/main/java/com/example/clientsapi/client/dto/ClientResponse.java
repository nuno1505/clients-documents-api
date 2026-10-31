package com.example.clientsapi.client.dto;

import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Complete client representation, including associated documents.")
public record ClientResponse(
        @Schema(example = "1")
        Long id,
        @Schema(example = "Ada")
        String firstName,
        @Schema(example = "Lovelace")
        String lastName,
        @Schema(example = "PT123456789")
        String taxIdentifier,
        @Schema(example = "ada@example.com", format = "email")
        String email,
        @Schema(example = "+351912345678")
        String phoneNumber,
        List<DocumentResponse> documents,
        @Schema(example = "2026-10-30T12:00:00Z", format = "date-time")
        Instant createdAt,
        @Schema(example = "2026-10-30T12:00:00Z", format = "date-time")
        Instant updatedAt,
        @Schema(example = "0", minimum = "0")
        Long version
) {
}

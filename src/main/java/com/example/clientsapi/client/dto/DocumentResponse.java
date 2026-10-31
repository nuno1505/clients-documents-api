package com.example.clientsapi.client.dto;

import java.time.Instant;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation of a document associated with a client.")
public record DocumentResponse(
        @Schema(example = "10")
        Long id,
        @Schema(example = "DOC-001")
        String number,
        @Schema(example = "Passport")
        String description,
        @Schema(example = "2030-12-31", format = "date")
        LocalDate expirationDate,
        @Schema(example = "2026-10-30T12:00:00Z", format = "date-time")
        Instant createdAt,
        @Schema(example = "2026-10-30T12:00:00Z", format = "date-time")
        Instant updatedAt,
        @Schema(example = "0", minimum = "0")
        Long version
) {
}

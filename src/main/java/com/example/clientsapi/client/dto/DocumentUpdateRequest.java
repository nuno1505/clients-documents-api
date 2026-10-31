package com.example.clientsapi.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Updated editable document data.")
public record DocumentUpdateRequest(
        @Schema(description = "Globally unique document number; required, up to 50 characters.", example = "DOC-001", maxLength = 50)
        @NotBlank @Size(max = 50) String number,
        @Schema(description = "Optional description, up to 255 characters.", example = "Passport", maxLength = 255)
        @Size(max = 255) String description,
        @Schema(description = "ISO-8601 expiration date; required. Past dates are accepted.", example = "2030-12-31", format = "date")
        @NotNull LocalDate expirationDate
) {
}

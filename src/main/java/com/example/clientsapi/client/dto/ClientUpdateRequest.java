package com.example.clientsapi.client.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Updated client data; replaces the editable fields and document list.")
public record ClientUpdateRequest(
        @Schema(description = "First name; required, up to 100 characters.", example = "Ada", maxLength = 100)
        @NotBlank @Size(max = 100) String firstName,
        @Schema(description = "Last name; required, up to 100 characters.", example = "Lovelace", maxLength = 100)
        @NotBlank @Size(max = 100) String lastName,
        @Schema(description = "Alphanumeric tax identifier; required, up to 32 characters.", example = "PT123456789", pattern = "^[A-Za-z0-9]+$", maxLength = 32)
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]+$", message = "taxIdentifier must be alphanumeric") @Size(max = 32) String taxIdentifier,
        @Schema(description = "Valid email address; required, up to 254 characters. It is normalized and unique.", example = "ada@example.com", format = "email", maxLength = 254)
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(description = "Phone number; required, up to 20 characters.", example = "+351912345678", maxLength = 20)
        @NotBlank @Size(max = 20) String phoneNumber,
        @Schema(description = "The document list that will be associated with the client.")
        @Valid List<DocumentUpdateRequest> documents
) {
}

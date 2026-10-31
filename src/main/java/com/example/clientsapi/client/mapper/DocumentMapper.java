package com.example.clientsapi.client.mapper;

import com.example.clientsapi.client.domain.Document;
import com.example.clientsapi.client.dto.DocumentResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document document) {
        if (document == null) {
            return null;
        }
        return new DocumentResponse(
                document.getId(),
                document.getNumber(),
                document.getDescription(),
                document.getExpirationDate(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getVersion()
        );
    }

    public List<DocumentResponse> toResponses(List<Document> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream().map(this::toResponse).toList();
    }
}

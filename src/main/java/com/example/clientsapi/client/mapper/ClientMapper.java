package com.example.clientsapi.client.mapper;

import com.example.clientsapi.client.domain.Client;
import com.example.clientsapi.client.dto.ClientResponse;
import com.example.clientsapi.client.dto.ClientSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    private final DocumentMapper documentMapper;

    public ClientMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public ClientResponse toResponse(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getTaxIdentifier(),
                client.getEmail(),
                client.getPhoneNumber(),
                client.getDocuments() == null ? List.of() : documentMapper.toResponses(client.getDocuments()),
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getVersion()
        );
    }

    public ClientSummaryResponse toSummary(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientSummaryResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getTaxIdentifier(),
                client.getEmail(),
                client.getPhoneNumber(),
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getVersion()
        );
    }
}

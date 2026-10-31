package com.example.clientsapi.client.service;

import com.example.clientsapi.client.domain.Client;
import com.example.clientsapi.client.domain.Document;
import com.example.clientsapi.client.dto.ClientCreateRequest;
import com.example.clientsapi.client.dto.ClientUpdateRequest;
import com.example.clientsapi.client.dto.ClientResponse;
import com.example.clientsapi.client.dto.ClientSummaryResponse;
import com.example.clientsapi.client.dto.DocumentCreateRequest;
import com.example.clientsapi.client.dto.DocumentUpdateRequest;
import com.example.clientsapi.client.mapper.ClientMapper;
import com.example.clientsapi.client.repository.ClientRepository;
import com.example.clientsapi.common.error.DuplicateResourceException;
import com.example.clientsapi.common.error.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    @Transactional
    public ClientResponse create(ClientCreateRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        clientRepository.findByEmail(normalizedEmail)
                .ifPresent(client -> {
                    throw new DuplicateResourceException("Client with email already exists: " + normalizedEmail);
                });

        clientRepository.findByTaxIdentifier(request.taxIdentifier())
                .ifPresent(client -> {
                    throw new DuplicateResourceException("Client with taxIdentifier already exists: " + request.taxIdentifier());
                });

        Client client = new Client(
                request.firstName(),
                request.lastName(),
                request.taxIdentifier(),
                normalizedEmail,
                request.phoneNumber()
        );

        if (request.documents() != null) {
            for (DocumentCreateRequest documentRequest : request.documents()) {
                client.addDocument(new Document(
                        documentRequest.number(),
                        documentRequest.description(),
                        documentRequest.expirationDate(),
                        client
                ));
            }
        }

        Client savedClient = clientRepository.saveAndFlush(client);
        return clientMapper.toResponse(savedClient);
    }

    @Transactional(readOnly = true)
    public List<ClientSummaryResponse> findAll() {
        return clientRepository.findAll().stream()
                .map(clientMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(Long clientId) {
        return clientMapper.toResponse(getClient(clientId));
    }

    @Transactional
    public ClientResponse update(Long clientId, ClientUpdateRequest request) {
        Client client = getClient(clientId);

        String normalizedEmail = normalizeEmail(request.email());

        clientRepository.findByEmail(normalizedEmail)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(clientId)) {
                        throw new DuplicateResourceException("Client with email already exists: " + normalizedEmail);
                    }
                });

        clientRepository.findByTaxIdentifier(request.taxIdentifier())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(clientId)) {
                        throw new DuplicateResourceException("Client with taxIdentifier already exists: " + request.taxIdentifier());
                    }
                });

        client.setFirstName(request.firstName());
        client.setLastName(request.lastName());
        client.setTaxIdentifier(request.taxIdentifier());
        client.setEmail(normalizedEmail);
        client.setPhoneNumber(request.phoneNumber());

        Map<String, Document> existingDocumentsByNumber = client.getDocuments().stream()
                .collect(Collectors.toMap(Document::getNumber, Function.identity()));

        List<DocumentUpdateRequest> requestedDocuments = request.documents() == null
                ? List.of()
                : request.documents();
        List<Document> retainedDocuments = new ArrayList<>();
        for (DocumentUpdateRequest documentRequest : requestedDocuments) {
            Document document = existingDocumentsByNumber.remove(documentRequest.number());
            if (document == null) {
                document = new Document(
                        documentRequest.number(),
                        documentRequest.description(),
                        documentRequest.expirationDate(),
                        client
                );
            } else {
                document.setDescription(documentRequest.description());
                document.setExpirationDate(documentRequest.expirationDate());
            }
            retainedDocuments.add(document);
        }

        for (Document removedDocument : List.copyOf(existingDocumentsByNumber.values())) {
            client.removeDocument(removedDocument);
        }
        for (Document document : retainedDocuments) {
            if (!client.getDocuments().contains(document)) {
                client.addDocument(document);
            }
        }

        Client savedClient = clientRepository.save(client);
        return clientMapper.toResponse(savedClient);
    }

    @Transactional
    public void delete(Long clientId) {
        Client client = getClient(clientId);
        clientRepository.delete(client);
    }

    private Client getClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}


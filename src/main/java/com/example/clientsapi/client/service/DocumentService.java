package com.example.clientsapi.client.service;

import com.example.clientsapi.client.domain.Client;
import com.example.clientsapi.client.domain.Document;
import com.example.clientsapi.client.dto.DocumentCreateRequest;
import com.example.clientsapi.client.dto.DocumentResponse;
import com.example.clientsapi.client.dto.DocumentUpdateRequest;
import com.example.clientsapi.client.mapper.DocumentMapper;
import com.example.clientsapi.client.repository.ClientRepository;
import com.example.clientsapi.client.repository.DocumentRepository;
import com.example.clientsapi.common.error.DuplicateResourceException;
import com.example.clientsapi.common.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;

    public DocumentService(ClientRepository clientRepository, DocumentRepository documentRepository, DocumentMapper documentMapper) {
        this.clientRepository = clientRepository;
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
    }

    @Transactional
    public DocumentResponse add(Long clientId, DocumentCreateRequest request) {
        Client client = getClient(clientId);

        documentRepository.findByNumber(request.number())
                .ifPresent(document -> {
                    throw new DuplicateResourceException("Document number already exists: " + request.number());
                });

        Document document = new Document(request.number(), request.description(), request.expirationDate(), client);
        client.addDocument(document);
        documentRepository.saveAndFlush(document);

        return documentMapper.toResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long clientId) {
        Client client = getClient(clientId);
        return documentMapper.toResponses(client.getDocuments());
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(Long clientId, Long documentId) {
        Client client = getClient(clientId);

        Document document = documentRepository.findByClientIdAndId(clientId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId + " for client: " + clientId));

        return documentMapper.toResponse(document);
    }

    @Transactional
    public DocumentResponse update(Long clientId, Long documentId, DocumentUpdateRequest request) {
        Client client = getClient(clientId);

        Document document = documentRepository.findByClientIdAndId(clientId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId + " for client: " + clientId));

        documentRepository.findByNumber(request.number())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(documentId)) {
                        throw new DuplicateResourceException("Document number already exists: " + request.number());
                    }
                });

        document.setNumber(request.number());
        document.setDescription(request.description());
        document.setExpirationDate(request.expirationDate());

        documentRepository.save(document);

        return documentMapper.toResponse(document);
    }

    @Transactional
    public void delete(Long clientId, Long documentId) {
        Client client = getClient(clientId);

        Document document = documentRepository.findByClientIdAndId(clientId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId + " for client: " + clientId));

        documentRepository.delete(document);
    }

    private Client getClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
    }
}



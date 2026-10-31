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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void add_returnsDocumentResponse() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);
        Document document = new Document("DOC-001", "Passport", LocalDate.of(2030, 12, 31), client);
        document.setId(10L);

        DocumentCreateRequest request = new DocumentCreateRequest("DOC-001", "Passport", LocalDate.of(2030, 12, 31));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(documentRepository.findByNumber("DOC-001")).thenReturn(Optional.empty());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> {
            Document savedDocument = invocation.getArgument(0);
            savedDocument.setId(10L);
            return savedDocument;
        });
        when(documentMapper.toResponse(any(Document.class))).thenReturn(new DocumentResponse(
                10L,
                "DOC-001",
                "Passport",
                LocalDate.of(2030, 12, 31),
                Instant.now(),
                Instant.now(),
                0L
        ));

        var response = documentService.add(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        verify(documentRepository).saveAndFlush(any(Document.class));
    }

    @Test
    void add_duplicateNumber_throwsDuplicateResourceException() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);

        DocumentCreateRequest request = new DocumentCreateRequest("DOC-001", "Passport", LocalDate.of(2030, 12, 31));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(documentRepository.findByNumber("DOC-001")).thenReturn(Optional.of(new Document(
                "DOC-001",
                "Passport",
                LocalDate.of(2030, 12, 31),
                client
        )));

        assertThatThrownBy(() -> documentService.add(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void get_documentNotFoundForClient_throwsResourceNotFoundException() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(documentRepository.findByClientIdAndId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.get(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_duplicateNumberFromOtherDocument_throwsDuplicateResourceException() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);
        Document document = new Document("DOC-001", "Passport", LocalDate.of(2030, 12, 31), client);
        document.setId(10L);
        Document otherDocument = new Document("DOC-999", "Other", LocalDate.of(2032, 12, 31), client);
        otherDocument.setId(99L);

        DocumentUpdateRequest request = new DocumentUpdateRequest("DOC-999", "New desc", LocalDate.of(2031, 1, 1));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(documentRepository.findByClientIdAndId(1L, 10L)).thenReturn(Optional.of(document));
        when(documentRepository.findByNumber("DOC-999")).thenReturn(Optional.of(otherDocument));

        assertThatThrownBy(() -> documentService.update(1L, 10L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_success_setsFieldsAndSaves() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);
        Document document = new Document("DOC-001", "Passport", LocalDate.of(2030, 12, 31), client);
        document.setId(10L);

        DocumentUpdateRequest request = new DocumentUpdateRequest("DOC-002", "New desc", LocalDate.of(2031, 1, 1));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(documentRepository.findByClientIdAndId(1L, 10L)).thenReturn(Optional.of(document));
        when(documentRepository.findByNumber("DOC-002")).thenReturn(Optional.of(document));
        when(documentRepository.save(document)).thenReturn(document);
        when(documentMapper.toResponse(any(Document.class))).thenReturn(new DocumentResponse(
                10L,
                "DOC-002",
                "New desc",
                LocalDate.of(2031, 1, 1),
                Instant.now(),
                Instant.now(),
                0L
        ));

        DocumentResponse response = documentService.update(1L, 10L, request);

        assertThat(document.getNumber()).isEqualTo("DOC-002");
        assertThat(document.getDescription()).isEqualTo("New desc");
        assertThat(response).isNotNull();
        verify(documentRepository).save(document);
    }

    @Test
    void delete_documentNotFoundForClient_throwsResourceNotFoundException() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(documentRepository.findByClientIdAndId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.delete(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_returnsDocuments() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);
        Document document = new Document("DOC-001", "Passport", LocalDate.of(2030, 12, 31), client);
        document.setId(10L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(documentMapper.toResponses(any())).thenReturn(List.of(new DocumentResponse(
                10L,
                "DOC-001",
                "Passport",
                LocalDate.of(2030, 12, 31),
                Instant.now(),
                Instant.now(),
                0L
        )));

        List<DocumentResponse> responses = documentService.list(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).number()).isEqualTo("DOC-001");
    }
}


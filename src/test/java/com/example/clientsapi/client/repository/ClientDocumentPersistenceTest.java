package com.example.clientsapi.client.repository;

import com.example.clientsapi.client.domain.Client;
import com.example.clientsapi.client.domain.Document;
import com.example.clientsapi.client.repository.ClientRepository;
import com.example.clientsapi.client.repository.DocumentRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ClientDocumentPersistenceTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void saveClientWithDocuments_persistsClientAndDocumentsTogether() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.addDocument(new Document("DOC-001", "Passport", LocalDate.of(2030, 12, 31), client));
        client.addDocument(new Document("DOC-002", "ID Card", LocalDate.of(2031, 12, 31), client));

        Client savedClient = clientRepository.saveAndFlush(client);

        assertThat(savedClient.getId()).isNotNull();
        assertThat(savedClient.getDocuments()).hasSize(2);
    }

    @Test
    void deleteClient_removesDocuments() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.addDocument(new Document("DOC-001", "Passport", LocalDate.of(2030, 12, 31), client));
        Client savedClient = clientRepository.saveAndFlush(client);
        Long clientId = savedClient.getId();
        Long documentId = savedClient.getDocuments().get(0).getId();

        assertThat(documentRepository.findByClientIdAndId(clientId, documentId)).isPresent();

        clientRepository.delete(savedClient);
        clientRepository.flush();

        assertThat(documentRepository.findByClientIdAndId(clientId, documentId)).isEmpty();
    }

    @Test
    void saveClientWithDuplicateEmail_throwsDataIntegrityViolation() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        clientRepository.saveAndFlush(client);

        Client duplicateClient = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");

        assertThatThrownBy(() -> clientRepository.saveAndFlush(duplicateClient))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

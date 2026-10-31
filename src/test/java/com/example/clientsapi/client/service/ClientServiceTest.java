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
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    @Test
    void create_returnsClientResponse() {
        ClientCreateRequest request = new ClientCreateRequest(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ADA@example.com",
                "+351912345678",
                List.of(new DocumentCreateRequest("DOC-001", "Passport", LocalDate.of(2030, 12, 31)))
        );

        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);

        when(clientRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(clientRepository.findByTaxIdentifier("PT123456789")).thenReturn(Optional.empty());
        when(clientRepository.saveAndFlush(any(Client.class))).thenReturn(client);
        when(clientMapper.toResponse(any(Client.class))).thenReturn(new ClientResponse(
                1L,
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                List.of(),
                Instant.now(),
                Instant.now(),
                0L
        ));

        ClientResponse response = clientService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void create_duplicateEmail_throwsDuplicateResourceException() {
        ClientCreateRequest request = new ClientCreateRequest(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                null
        );

        when(clientRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(new Client(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678"
        )));

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void create_duplicateTaxIdentifier_throwsDuplicateResourceException() {
        ClientCreateRequest request = new ClientCreateRequest(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                null
        );

        when(clientRepository.findByTaxIdentifier("PT123456789")).thenReturn(Optional.of(new Client(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678"
        )));

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_fullReplaceDocuments_returnsResponse() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);
        client.addDocument(new Document("DOC-001", "Passport", LocalDate.of(2030, 12, 31), client));

        ClientUpdateRequest request = new ClientUpdateRequest(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                List.of(new DocumentUpdateRequest("DOC-002", "Passport updated", LocalDate.of(2031, 1, 1)))
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(clientRepository.findByTaxIdentifier("PT123456789")).thenReturn(Optional.empty());
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toResponse(any(Client.class))).thenReturn(new ClientResponse(
                1L,
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                List.of(),
                Instant.now(),
                Instant.now(),
                0L
        ));

        ClientResponse response = clientService.update(1L, request);

        assertThat(client.getDocuments()).hasSize(1);
        assertThat(client.getDocuments().get(0).getNumber()).isEqualTo("DOC-002");
        assertThat(response).isNotNull();
        verify(clientRepository).save(client);
    }

    @Test
    void update_duplicateEmailFromOtherClient_throwsDuplicateResourceException() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);
        Client otherClient = new Client("Grace", "Hopper", "PT876543211", "ada@example.com", "+351912345679");
        otherClient.setId(2L);

        ClientUpdateRequest request = new ClientUpdateRequest(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                null
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(otherClient));

        assertThatThrownBy(() -> clientService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_clientNotFound_throwsResourceNotFoundException() {
        ClientUpdateRequest request = new ClientUpdateRequest(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                null
        );

        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_clientNotFound_throwsResourceNotFoundException() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_returnsClientResponse() {
        Client client = new Client("Ada", "Lovelace", "PT123456789", "ada@example.com", "+351912345678");
        client.setId(1L);
        ClientResponse response = new ClientResponse(
                1L,
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                List.of(),
                Instant.now(),
                Instant.now(),
                0L
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientMapper.toResponse(client)).thenReturn(response);

        assertThat(clientService.findById(1L)).isSameAs(response);
    }

    @Test
    void findAll_returnsClientSummaries() {
        ClientSummaryResponse summary = new ClientSummaryResponse(
                1L,
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678",
                Instant.now(),
                Instant.now(),
                0L
        );

        when(clientRepository.findAll()).thenReturn(List.of(new Client(
                "Ada",
                "Lovelace",
                "PT123456789",
                "ada@example.com",
                "+351912345678"
        )));

        when(clientMapper.toSummary(any(Client.class))).thenReturn(summary);

        List<ClientSummaryResponse> summaries = clientService.findAll();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).firstName()).isEqualTo("Ada");
    }
}




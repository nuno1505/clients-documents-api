package com.example.clientsapi.client.api;

import com.example.clientsapi.client.domain.Client;
import com.example.clientsapi.client.domain.Document;
import com.example.clientsapi.client.repository.ClientRepository;
import com.example.clientsapi.client.repository.DocumentRepository;
import com.example.clientsapi.security.AbstractSecurityTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientApiIntegrationTest extends AbstractSecurityTest {

    private static final String CLIENTS = "/api/v1/clients";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClientRepository clientRepository;
    @Autowired private DocumentRepository documentRepository;

    private String token;

    @BeforeEach
    void isolateDatabaseAndAuthenticate() throws Exception {
        documentRepository.deleteAll();
        clientRepository.deleteAll();
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "username", TEST_USERNAME, "password", TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        token = objectMapper.readTree(login.getResponse().getContentAsByteArray()).get("accessToken").asText();
    }

    @Test
    void createClientWithDocuments_persistsClientAndDocuments() throws Exception {
        String email = email();
        MvcResult result = mockMvc.perform(post(CLIENTS).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson(email, "TX" + suffix().substring(0, 20), List.of(document("DOC-" + suffix(), "Passport")))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.documents[0].description").value("Passport"))
                .andReturn();
        long id = responseId(result);
        Client persisted = clientRepository.findById(id).orElseThrow();
        assertThat(persisted.getEmail()).isEqualTo(email);
        String documentNumber = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("documents").get(0).get("number").asText();
        assertThat(documentRepository.findByNumber(documentNumber)).isPresent();
    }

    @Test
    void listClients_returnsPersistedClient() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20));
        mockMvc.perform(get(CLIENTS).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + client.getId() + ")].email").value(org.hamcrest.Matchers.hasItem(client.getEmail())));
    }

    @Test
    void getClient_returnsPersistedClientAndDocuments() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Passport"));
        mockMvc.perform(get(CLIENTS + "/" + client.getId()).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(client.getId()))
                .andExpect(jsonPath("$.documents[0].number").value(client.getDocuments().get(0).getNumber()));
    }

    @Test
    void updateClient_replacesDocumentCollection() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Old"));
        Long oldDocumentId = client.getDocuments().get(0).getId();
        String replacementNumber = "DOC-" + suffix();
        mockMvc.perform(put(CLIENTS + "/" + client.getId()).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson(email(), client.getTaxIdentifier(), List.of(document(replacementNumber, "New")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documents[0].number").value(replacementNumber));
        assertThat(documentRepository.findById(oldDocumentId)).isEmpty();
        assertThat(documentRepository.findByNumber(replacementNumber)).isPresent();
    }

    @Test
    void deleteClient_cascadesToDocuments() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Passport"));
        Long clientId = client.getId();
        Long documentId = client.getDocuments().get(0).getId();
        mockMvc.perform(delete(CLIENTS + "/" + clientId).header("Authorization", bearer()))
                .andExpect(status().isNoContent());
        assertThat(clientRepository.findById(clientId)).isEmpty();
        assertThat(documentRepository.findById(documentId)).isEmpty();
    }

    @Test
    void listDocuments_returnsClientDocuments() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Passport"), document("DOC-" + suffix(), "ID"));
        mockMvc.perform(get(CLIENTS + "/" + client.getId() + "/documents").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void addDocument_persistsDocumentForClient() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20));
        String number = "DOC-" + suffix();
        MvcResult result = mockMvc.perform(post(CLIENTS + "/" + client.getId() + "/documents").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(document(number, "Passport"))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.number").value(number))
                .andExpect(jsonPath("$.id").isNumber()).andReturn();
        long documentId = responseId(result);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(CLIENTS + "/" + client.getId() + "/documents/" + documentId);
        Document persisted = documentRepository.findById(documentId).orElseThrow();
        assertThat(persisted.getNumber()).isEqualTo(number);
        assertThat(persisted.getClient().getId()).isEqualTo(client.getId());
    }

    @Test
    void getDocument_returnsOwnedDocument() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Passport"));
        Document document = client.getDocuments().get(0);
        mockMvc.perform(get(documentPath(client, document)).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(document.getId()))
                .andExpect(jsonPath("$.number").value(document.getNumber()));
    }

    @Test
    void updateDocument_persistsChanges() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Old"));
        Document document = client.getDocuments().get(0);
        String updatedNumber = "DOC-" + suffix();
        mockMvc.perform(put(documentPath(client, document)).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(document(updatedNumber, "Updated"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(updatedNumber))
                .andExpect(jsonPath("$.description").value("Updated"));
        assertThat(documentRepository.findById(document.getId()).orElseThrow().getNumber()).isEqualTo(updatedNumber);
    }

    @Test
    void deleteDocument_removesOnlyDocument() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Passport"));
        Long documentId = client.getDocuments().get(0).getId();
        mockMvc.perform(delete(documentPath(client, client.getDocuments().get(0))).header("Authorization", bearer()))
                .andExpect(status().isNoContent());
        assertThat(documentRepository.findById(documentId)).isEmpty();
        assertThat(clientRepository.findById(client.getId())).isPresent();
    }

    @Test
    void updateClientWithEmptyDocuments_removesOrphans() throws Exception {
        Client client = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Passport"));
        Long documentId = client.getDocuments().get(0).getId();
        mockMvc.perform(put(CLIENTS + "/" + client.getId()).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(clientJson(email(), client.getTaxIdentifier(), List.of())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documents").isEmpty());
        assertThat(clientRepository.findById(client.getId())).isPresent();
        assertThat(documentRepository.findById(documentId)).isEmpty();
    }

    @Test
    void duplicateClientEmail_returns409WithoutAddingClient() throws Exception {
        Client existing = createClient(email(), "TX" + suffix().substring(0, 20));
        mockMvc.perform(post(CLIENTS).header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson(existing.getEmail(), "TX" + suffix().substring(0, 20), List.of())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(clientRepository.findAll()).hasSize(1);
    }

    @Test
    void duplicateDocumentNumber_returns409WithoutAddingDocument() throws Exception {
        String number = "DOC-" + suffix();
        Client first = createClient(email(), "TX" + suffix().substring(0, 20), document(number, "Passport"));
        Client second = createClient(email(), "TX" + suffix().substring(0, 20));
        mockMvc.perform(post(CLIENTS + "/" + second.getId() + "/documents").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(document(number, "Duplicate"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(documentRepository.findByNumber(number)).get().extracting(d -> d.getClient().getId()).isEqualTo(first.getId());
        assertThat(documentRepository.count()).isEqualTo(1);
    }

    @Test
    void documentCannotBeAccessedThroughAnotherClientsPath() throws Exception {
        Client owner = createClient(email(), "TX" + suffix().substring(0, 20), document("DOC-" + suffix(), "Passport"));
        Client other = createClient(email(), "TX" + suffix().substring(0, 20));
        Document document = owner.getDocuments().get(0);
        mockMvc.perform(get(CLIENTS + "/" + other.getId() + "/documents/" + document.getId()).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
        assertThat(documentRepository.findById(document.getId())).isPresent();
        assertThat(documentRepository.findById(document.getId()).orElseThrow().getClient().getId()).isEqualTo(owner.getId());
    }

    @Test
    void duplicateDocumentDuringClientCreation_rollsBackEntireOperation() throws Exception {
        String number = "DOC-" + suffix();
        createClient(email(), "TX" + suffix().substring(0, 20), document(number, "Existing"));
        long clientCount = clientRepository.count();
        long documentCount = documentRepository.count();
        String attemptedEmail = email();
        mockMvc.perform(post(CLIENTS).header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson(attemptedEmail, "TX" + suffix().substring(0, 20), List.of(document(number, "Duplicate")))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(clientRepository.findByEmail(attemptedEmail)).isEmpty();
        assertThat(clientRepository.count()).isEqualTo(clientCount);
        assertThat(documentRepository.count()).isEqualTo(documentCount);
    }

    private Client createClient(String email, String taxIdentifier, java.util.Map<String, Object>... documents) throws Exception {
        String content = clientJson(email, taxIdentifier, List.of(documents));
        MvcResult result = mockMvc.perform(post(CLIENTS).header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isCreated()).andReturn();
        Client client = clientRepository.findById(responseId(result)).orElseThrow();
        client.setDocuments(documentRepository.findAll().stream()
                .filter(document -> document.getClient().getId().equals(client.getId())).toList());
        return client;
    }

    private String clientJson(String email, String taxIdentifier, List<java.util.Map<String, Object>> documents) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("firstName", "Ada", "lastName", "Lovelace",
                "taxIdentifier", taxIdentifier, "email", email, "phoneNumber", "+351912345678", "documents", documents));
    }

    private java.util.Map<String, Object> document(String number, String description) {
        return java.util.Map.of("number", number, "description", description, "expirationDate", LocalDate.of(2030, 12, 31).toString());
    }

    private String documentPath(Client client, Document document) {
        return CLIENTS + "/" + client.getId() + "/documents/" + document.getId();
    }

    private long responseId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return json.get("id").asLong();
    }

    private String bearer() { return "Bearer " + token; }
    private String email() { return "client-" + suffix() + "@example.com"; }
    private String suffix() { return UUID.randomUUID().toString().replace("-", ""); }
}

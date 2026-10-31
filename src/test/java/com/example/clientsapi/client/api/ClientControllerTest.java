package com.example.clientsapi.client.api;

import com.example.clientsapi.client.dto.ClientCreateRequest;
import com.example.clientsapi.client.dto.ClientResponse;
import com.example.clientsapi.client.dto.ClientSummaryResponse;
import com.example.clientsapi.client.dto.ClientUpdateRequest;
import com.example.clientsapi.client.service.ClientService;
import com.example.clientsapi.common.error.DuplicateResourceException;
import com.example.clientsapi.common.error.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import com.example.clientsapi.security.SecurityConfig;
import com.example.clientsapi.security.JwtService;
import com.example.clientsapi.common.error.SecurityProblemWriter;
import com.example.clientsapi.security.AbstractSecurityTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@ActiveProfiles("test")
@WithMockUser
@Import({SecurityConfig.class, SecurityProblemWriter.class})
class ClientControllerTest extends AbstractSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createClient_returns201WithLocation() throws Exception {
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

        when(clientService.create(any(ClientCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"taxIdentifier\":\"PT123456789\",\"email\":\"ada@example.com\",\"phoneNumber\":\"+351912345678\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/clients/1"))
                .andExpect(jsonPath("$.id").value(1));

        verify(clientService).create(any(ClientCreateRequest.class));
    }

    @Test
    void getClient_returns200() throws Exception {
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

        when(clientService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteClient_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/1"))
                .andExpect(status().isNoContent());

        verify(clientService).delete(1L);
    }

    @Test
    void getClient_notFound_returns404WithProblemDetail() throws Exception {
        when(clientService.findById(99L)).thenThrow(new ResourceNotFoundException("Client not found with id: 99"));

        mockMvc.perform(get("/api/v1/clients/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void createClient_duplicateEmail_returns409() throws Exception {
        when(clientService.create(any(ClientCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Client with email already exists: ada@example.com"));

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"taxIdentifier\":\"PT123456789\",\"email\":\"ada@example.com\",\"phoneNumber\":\"+351912345678\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void updateClient_invalidDto_returns400WithFieldDetails() throws Exception {
        mockMvc.perform(put("/api/v1/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"Lovelace\",\"taxIdentifier\":\"PT123456789\",\"email\":\"ada@example.com\",\"phoneNumber\":\"+351912345678\",\"documents\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail", containsString("firstName")));


    }

    @Test
    void createClient_malformedJson_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void getClient_unexpectedError_returns500WithSafeResponse() throws Exception {
        when(clientService.findById(1L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void listClients_returns200() throws Exception {
        ClientSummaryResponse response = new ClientSummaryResponse(
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

        when(clientService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Ada"));

        verify(clientService).findAll();
    }

    @Test
    void updateClient_returns200() throws Exception {
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

        when(clientService.update(eq(1L), any(ClientUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"taxIdentifier\":\"PT123456789\",\"email\":\"ada@example.com\",\"phoneNumber\":\"+351912345678\",\"documents\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ada"));

        verify(clientService).update(eq(1L), any(ClientUpdateRequest.class));
    }
}







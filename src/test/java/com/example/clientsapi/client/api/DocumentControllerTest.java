package com.example.clientsapi.client.api;

import com.example.clientsapi.client.dto.DocumentCreateRequest;
import com.example.clientsapi.client.dto.DocumentResponse;
import com.example.clientsapi.client.dto.DocumentUpdateRequest;
import com.example.clientsapi.client.service.DocumentService;
import com.example.clientsapi.common.error.DuplicateResourceException;
import com.example.clientsapi.common.error.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(DocumentController.class)
@ActiveProfiles("test")
@WithMockUser
@Import({SecurityConfig.class, SecurityProblemWriter.class})
class DocumentControllerTest extends AbstractSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void addDocument_returns201WithLocation() throws Exception {
        DocumentResponse response = new DocumentResponse(
                10L,
                "DOC-001",
                "Passport",
                LocalDate.of(2030, 12, 31),
                Instant.now(),
                Instant.now(),
                0L
        );

        when(documentService.add(eq(1L), any(DocumentCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients/1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"DOC-001\",\"description\":\"Passport\",\"expirationDate\":\"2030-12-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/clients/1/documents/10"))
                .andExpect(jsonPath("$.id").value(10));

        verify(documentService).add(eq(1L), any(DocumentCreateRequest.class));
    }

    @Test
    void listDocuments_returns200() throws Exception {
        DocumentResponse response = new DocumentResponse(
                10L,
                "DOC-001",
                "Passport",
                LocalDate.of(2030, 12, 31),
                Instant.now(),
                Instant.now(),
                0L
        );

        when(documentService.list(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/clients/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].number").value("DOC-001"));
    }

    @Test
    void getDocument_returns200() throws Exception {
        DocumentResponse response = new DocumentResponse(
                10L,
                "DOC-001",
                "Passport",
                LocalDate.of(2030, 12, 31),
                Instant.now(),
                Instant.now(),
                0L
        );

        when(documentService.get(1L, 10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/clients/1/documents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.number").value("DOC-001"));
    }

    @Test
    void deleteDocument_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/1/documents/10"))
                .andExpect(status().isNoContent());

        verify(documentService).delete(1L, 10L);
    }

    @Test
    void getDocument_notFound_returns404() throws Exception {
        when(documentService.get(1L, 10L)).thenThrow(new ResourceNotFoundException("Document not found with id: 10 for client: 1"));

        mockMvc.perform(get("/api/v1/clients/1/documents/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void addDocument_duplicateNumber_returns409() throws Exception {
        when(documentService.add(eq(1L), any(DocumentCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Document number already exists: DOC-001"));

        mockMvc.perform(post("/api/v1/clients/1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"DOC-001\",\"description\":\"Passport\",\"expirationDate\":\"2030-12-31\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void updateDocument_returns200() throws Exception {
        DocumentResponse response = new DocumentResponse(
                10L,
                "DOC-002",
                "Passport",
                LocalDate.of(2030, 12, 31),
                Instant.now(),
                Instant.now(),
                0L
        );

        when(documentService.update(eq(1L), eq(10L), any(DocumentUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/clients/1/documents/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"DOC-002\",\"description\":\"Passport\",\"expirationDate\":\"2030-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.number").value("DOC-002"));

        verify(documentService).update(eq(1L), eq(10L), any(DocumentUpdateRequest.class));
    }
}




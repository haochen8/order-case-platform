package com.example.caseplatform.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.caseplatform.repository.CaseRepository;
import com.example.caseplatform.service.AuditClient;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class CaseControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("caseplatform")
            .withUsername("caseuser")
            .withPassword("casepass");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseRepository caseRepository;

    @MockBean
    private AuditClient auditClient;

    @BeforeEach
    void setUp() {
        caseRepository.deleteAll();
        doNothing().when(auditClient).publishEvent(any());
        when(auditClient.getEvents(anyString(), any(UUID.class))).thenReturn(Collections.emptyList());
    }

    @Test
    void postCases_ShouldReturnCreated() throws Exception {
        String payload = """
                {
                  "title": "Provision new connection",
                  "description": "Handle migration ticket"
                }
                """;

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Provision new connection"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getCases_ShouldReturnPagedContent() throws Exception {
        mockMvc.perform(post("/api/cases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Case 1\",\"description\":\"d1\"}"));
        mockMvc.perform(post("/api/cases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Case 2\",\"description\":\"d2\"}"));
        mockMvc.perform(post("/api/cases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Case 3\",\"description\":\"d3\"}"));

        mockMvc.perform(get("/api/cases")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}

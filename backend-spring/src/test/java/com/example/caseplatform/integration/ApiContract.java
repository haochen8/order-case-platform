package com.example.caseplatform.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.caseplatform.dto.CaseCreateRequest;
import com.example.caseplatform.repository.AuditEventRepository;
import com.example.caseplatform.repository.CaseRepository;
import com.example.caseplatform.repository.OrderRepository;
import com.example.caseplatform.service.CaseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

abstract class ApiContract {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CaseRepository cases;
    @Autowired OrderRepository orders;
    @Autowired AuditEventRepository audits;
    @Autowired CaseService service;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JwtDecoder decoder;

    @BeforeEach
    void clearDatabase() {
        orders.deleteAll();
        cases.deleteAll();
        audits.deleteAll();
    }

    RequestPostProcessor operator() {
        return jwt().jwt(j -> j.subject("operator-123")).authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"));
    }

    RequestPostProcessor viewer() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }

    JsonNode createCase() throws Exception {
        return mapper.readTree(mvc.perform(post("/api/cases").with(operator())
                .header("X-Actor", "spoofed-admin")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Install fiber\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString());
    }

    JsonNode createOrder(String caseId) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/orders").with(operator())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("caseId", caseId, "type", "FIBER_INSTALL"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void anonymousAndViewerCannotWrite() throws Exception {
        mvc.perform(get("/api/cases")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cases").with(viewer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Forbidden\"}")).andExpect(status().isForbidden());
        JsonNode entity = createCase();
        String path = "/api/cases/" + entity.get("id").asText();
        mvc.perform(put(path).with(viewer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"title\":\"Forbidden\"}")).andExpect(status().isForbidden());
        mvc.perform(delete(path).param("version", "0").with(viewer())).andExpect(status().isForbidden());
        mvc.perform(get(path).with(viewer())).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void authenticatedRoleMappingAndInvalidBearer() throws Exception {
        var token = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("valid")
                .header("alg", "RS256").subject("verified-subject").claim("roles", java.util.List.of("VIEWER")).build();
        org.mockito.Mockito.when(decoder.decode("valid")).thenReturn(token);
        mvc.perform(get("/api/cases").header("Authorization", "Bearer valid")).andExpect(status().isOk());
        mvc.perform(post("/api/cases").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Forbidden\"}"))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.when(decoder.decode("invalid"))
                .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("invalid"));
        mvc.perform(get("/api/cases").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void auditActorComesFromIdentityAndSurvivesDeletion() throws Exception {
        String id = createCase().get("id").asText();
        mvc.perform(delete("/api/cases/" + id).param("version", "0").with(operator()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/cases/" + id + "/audit").with(viewer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].actor").value("operator-123"))
                .andExpect(jsonPath("$.content[1].eventType").value("CASE_DELETED"));
    }


    @Test
    void unchangedUpdateDoesNotAddHistoryOrIncrementVersion() throws Exception {
        String id = createCase().get("id").asText();
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"OPEN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
        assertThat(audits.count()).isEqualTo(1);
    }

    @Test
    void staleUpdateAndDeleteReturnConflictWithoutExtraAudit() throws Exception {
        String id = createCase().get("id").asText();
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"title\":\"Changed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"title\":\"Stale\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/cases/" + id).param("version", "0").with(operator()))
                .andExpect(status().isConflict());
        assertThat(audits.count()).isEqualTo(2);
        mvc.perform(get("/api/cases/" + id).with(viewer())).andExpect(jsonPath("$.title").value("Changed"));
    }

    @Test
    void fullFulfillmentWorkflowEnforcesOutstandingOrderRule() throws Exception {
        String id = createCase().get("id").asText();
        String orderId = createOrder(id).get("id").asText();
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"CLOSED\"}")).andExpect(status().isConflict());
        mvc.perform(delete("/api/cases/" + id).param("version", "0").with(operator()))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/orders/" + orderId).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"COMPLETED\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/orders/" + orderId).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"SENT\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/orders/" + orderId).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"FAILED\"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/orders/" + orderId).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1,\"status\":\"COMPLETED\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"CLOSED\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1,\"status\":\"OPEN\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"caseId\":\"" + id + "\",\"type\":\"EXTRA\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/cases/" + id + "/audit").with(viewer()))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void failedOrderAllowsCaseClosureAndCannotBeRestarted() throws Exception {
        String id = createCase().get("id").asText();
        String orderId = createOrder(id).get("id").asText();
        mvc.perform(put("/api/orders/" + orderId).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"FAILED\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/orders/" + orderId).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1,\"status\":\"PENDING\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"CLOSED\"}")).andExpect(status().isOk());
    }

    @Test
    void invalidInputsProduceClientErrors() throws Exception {
        mvc.perform(get("/api/cases/not-a-uuid").with(viewer())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cases").param("page", "abc").with(viewer())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cases").param("size", "201").with(viewer())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cases").param("status", "UNKNOWN").with(viewer())).andExpect(status().isBadRequest());
        String id = createCase().get("id").asText();
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Missing version\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/cases/" + id).with(operator()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"title\":\"  \"}")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/cases/" + id).with(operator())).andExpect(status().isBadRequest());
    }

    @Test
    void filteredPaginationAndMissingHistory() throws Exception {
        createCase();
        mvc.perform(get("/api/cases").with(viewer()).param("search", "fiber").param("status", "OPEN").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/cases").with(viewer()).param("search", "nonexistent"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/cases/" + UUID.randomUUID() + "/audit").with(viewer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rollbackRemovesBothBusinessRecordAndAudit() {
        CaseCreateRequest request = new CaseCreateRequest();
        request.setTitle("Rolled back");
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            service.createCase(request, "operator-123");
            assertThat(audits.count()).isEqualTo(1);
            tx.setRollbackOnly();
        });
        assertThat(cases.count()).isZero();
        assertThat(audits.count()).isZero();
    }

    @Test
    void auditFailureRollsBackBusinessWrite() {
        CaseCreateRequest request = new CaseCreateRequest();
        request.setTitle("Must not survive");
        assertThatThrownBy(() -> service.createCase(request, "x".repeat(256)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(cases.count()).isZero();
        assertThat(audits.count()).isZero();
    }

    @Test
    void allowedCorsOriginReceivesPreflightAndUntrustedOriginDoesNot() throws Exception {
        mvc.perform(options("/api/cases").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/cases").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
}

package com.example.caseplatform.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.Test;
import com.example.caseplatform.domain.Order;
import com.example.caseplatform.domain.enums.OrderStatus;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIT extends ApiContract {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void databaseRejectsOrphanOrderWithoutServiceValidation() {
        Order orphan = new Order();
        orphan.setCaseId(UUID.randomUUID());
        orphan.setType("FIBER_INSTALL");
        orphan.setStatus(OrderStatus.PENDING);
        assertThatThrownBy(() -> orders.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDeletionOfParentWithOrders() throws Exception {
        String id = createCase().get("id").asText();
        createOrder(id);
        assertThatThrownBy(() -> cases.deleteById(UUID.fromString(id)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void twoWritersCannotBothOverwriteTheSameVersion() throws Exception {
        String id = createCase().get("id").asText();
        Callable<Integer> first = () -> mvc.perform(put("/api/cases/" + id).with(operator())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"title\":\"First edit\"}"))
                .andReturn().getResponse().getStatus();
        Callable<Integer> second = () -> mvc.perform(put("/api/cases/" + id).with(operator())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"title\":\"Second edit\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(race(first, second)).containsExactlyInAnyOrder(200, 409);
        assertThat(audits.count()).isEqualTo(2);
    }

    @Test
    void caseClosureRacingOrderCreationCannotLeaveAnActiveOrderOnClosedCase() throws Exception {
        String id = createCase().get("id").asText();
        Callable<Integer> close = () -> mvc.perform(put("/api/cases/" + id).with(operator())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"status\":\"CLOSED\"}"))
                .andReturn().getResponse().getStatus();
        Callable<Integer> create = () -> mvc.perform(post("/api/orders").with(operator())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"caseId\":\"" + id + "\",\"type\":\"FIBER\"}"))
                .andReturn().getResponse().getStatus();
        List<Integer> results = race(close, create);
        assertThat(results.equals(List.of(200, 409)) || results.equals(List.of(409, 201))).isTrue();
        boolean closed = cases.findById(UUID.fromString(id)).orElseThrow().getStatus()
                == com.example.caseplatform.domain.enums.CaseStatus.CLOSED;
        assertThat(closed && orders.existsByCaseId(UUID.fromString(id))).isFalse();
    }

    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> { ready.countDown(); start.await(); return first.call(); });
            var b = executor.submit(() -> { ready.countDown(); start.await(); return second.call(); });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}

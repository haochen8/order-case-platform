package com.example.caseplatform.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.caseplatform.domain.Case;
import com.example.caseplatform.domain.enums.CaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@org.springframework.test.context.ActiveProfiles("test")
class CaseRepositoryTest {

    @Autowired
    private CaseRepository caseRepository;

    @Test
    void findByFilters_ShouldFilterByStatusAndSearch() {
        Case first = new Case();
        first.setTitle("Fiber install");
        first.setDescription("Install new broadband line");
        first.setStatus(CaseStatus.OPEN);
        caseRepository.save(first);

        Case second = new Case();
        second.setTitle("Copper migration");
        second.setDescription("Legacy migration work");
        second.setStatus(CaseStatus.CLOSED);
        caseRepository.save(second);

        Page<Case> openCases = caseRepository.findByFilters(CaseStatus.OPEN, null, PageRequest.of(0, 10));
        Page<Case> fiberCases = caseRepository.findByFilters(null, "fiber", PageRequest.of(0, 10));

        assertThat(openCases.getTotalElements()).isEqualTo(1);
        assertThat(openCases.getContent().get(0).getTitle()).isEqualTo("Fiber install");

        assertThat(fiberCases.getTotalElements()).isEqualTo(1);
        assertThat(fiberCases.getContent().get(0).getStatus()).isEqualTo(CaseStatus.OPEN);
    }
}

package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Accesseurs de l'entité JPA (§8.4 onglet 7) — couverture des getters/setters. */
class MockAuditRunJpaEntityTest {

    @Test
    void accessors_roundTrip() {
        MockAuditRunJpaEntity e = new MockAuditRunJpaEntity();
        UUID id = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        UUID adoption = UUID.randomUUID();
        UUID standard = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Instant now = Instant.now();

        e.setId(id);
        e.setTenantId(tenant);
        e.setAdoptionId(adoption);
        e.setStandardId(standard);
        e.setStandardCode("iso-9001");
        e.setStandardName("ISO 9001:2015");
        e.setReadiness(42.5);
        e.setMajorCount(1);
        e.setMinorCount(2);
        e.setObservationCount(3);
        e.setQuestionCount(4);
        e.setQuestionsJson("[]");
        e.setGapsJson("[]");
        e.setRemediationJson("[]");
        e.setAiProvider("ollama");
        e.setCreatedByUserId(actor);
        e.setCreatedAt(now);

        assertThat(e.getId()).isEqualTo(id);
        assertThat(e.getTenantId()).isEqualTo(tenant);
        assertThat(e.getAdoptionId()).isEqualTo(adoption);
        assertThat(e.getStandardId()).isEqualTo(standard);
        assertThat(e.getStandardCode()).isEqualTo("iso-9001");
        assertThat(e.getStandardName()).isEqualTo("ISO 9001:2015");
        assertThat(e.getReadiness()).isEqualTo(42.5);
        assertThat(e.getMajorCount()).isEqualTo(1);
        assertThat(e.getMinorCount()).isEqualTo(2);
        assertThat(e.getObservationCount()).isEqualTo(3);
        assertThat(e.getQuestionCount()).isEqualTo(4);
        assertThat(e.getQuestionsJson()).isEqualTo("[]");
        assertThat(e.getGapsJson()).isEqualTo("[]");
        assertThat(e.getRemediationJson()).isEqualTo("[]");
        assertThat(e.getAiProvider()).isEqualTo("ollama");
        assertThat(e.getCreatedByUserId()).isEqualTo(actor);
        assertThat(e.getCreatedAt()).isEqualTo(now);
    }
}

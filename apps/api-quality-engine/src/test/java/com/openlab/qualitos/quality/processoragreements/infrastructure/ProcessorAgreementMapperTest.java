package com.openlab.qualitos.quality.processoragreements.infrastructure;

import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre les branches du mapper (CSV null/non-null) qui ne sont pas exercées
 * autrement (les autres modules sont testés par les adapters intégrés mais
 * pas ce module en particulier).
 */
class ProcessorAgreementMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_emptySets_csvFieldsNull() throws Exception {
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, null, "S",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72, false, null, null, U, NOW);
        ProcessorAgreementJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getSubProcessorCategoriesCsv()).isNull();
        assertThat(e.getThirdCountryTransfersCsv()).isNull();
        assertThat(e.getLinkedProcessingActivityIdsCsv()).isNull();

        ProcessorAgreement back = invokeToDomain(e);
        assertThat(back.getSubProcessorCategories()).isEmpty();
        assertThat(back.getThirdCountryTransfers()).isEmpty();
        assertThat(back.getLinkedProcessingActivityIds()).isEmpty();
    }

    @Test
    void roundtrip_filledSets_csvPopulated() throws Exception {
        UUID linkA = UUID.randomUUID();
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-2",
                "Acme", null, null, null, "US", "S",
                Set.of("cloud", "storage"), Set.of(linkA),
                Set.of("US", "IN"), "SCC",
                null, null, null, null, null, 72, false, null, null, U, NOW);
        ProcessorAgreementJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getSubProcessorCategoriesCsv()).contains("cloud").contains("storage");
        assertThat(e.getThirdCountryTransfersCsv()).contains("US").contains("IN");
        assertThat(e.getLinkedProcessingActivityIdsCsv()).contains(linkA.toString());

        ProcessorAgreement back = invokeToDomain(e);
        assertThat(back.getSubProcessorCategories()).containsExactlyInAnyOrder("cloud", "storage");
        assertThat(back.getThirdCountryTransfers()).containsExactlyInAnyOrder("US", "IN");
        assertThat(back.getLinkedProcessingActivityIds()).containsExactly(linkA);
    }

    @Test
    void roundtrip_updatesExistingEntity_inPlace() throws Exception {
        ProcessorAgreementJpaEntity existing = new ProcessorAgreementJpaEntity();
        existing.setId(UUID.randomUUID());
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-3",
                "Acme", null, null, null, null, "S",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72, false, null, null, U, NOW);
        a.assignId(existing.getId());
        ProcessorAgreementJpaEntity out = invokeToEntity(a, existing);
        assertThat(out).isSameAs(existing);
        assertThat(out.getId()).isEqualTo(a.getId());
    }

    @Test
    void domain_status_preservedViaMapper() throws Exception {
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-4",
                "Acme", null, "ops@x", null, "US", "S",
                Set.of(), Set.of(), Set.of(), null, null,
                NOW, NOW, null, null, 72, false, null, null, U, NOW);
        a.activate(NOW.plusSeconds(60));
        a.terminate("end", NOW.plusSeconds(120));
        ProcessorAgreementJpaEntity e = invokeToEntity(a, null);
        assertThat(e.getStatus()).isEqualTo(ProcessorAgreementStatus.TERMINATED);
        ProcessorAgreement back = invokeToDomain(e);
        assertThat(back.getStatus()).isEqualTo(ProcessorAgreementStatus.TERMINATED);
        assertThat(back.getTerminationReason()).isEqualTo("end");
    }

    // Le mapper est package-private. On invoque via réflexion pour le tester
    // depuis ce package de tests sans changer la visibilité.
    private static ProcessorAgreementJpaEntity invokeToEntity(
            ProcessorAgreement a, ProcessorAgreementJpaEntity target) throws Exception {
        Method m = ProcessorAgreementMapper.class.getDeclaredMethod(
                "toEntity", ProcessorAgreement.class, ProcessorAgreementJpaEntity.class);
        m.setAccessible(true);
        return (ProcessorAgreementJpaEntity) m.invoke(null, a, target);
    }

    private static ProcessorAgreement invokeToDomain(ProcessorAgreementJpaEntity e) throws Exception {
        Method m = ProcessorAgreementMapper.class.getDeclaredMethod(
                "toDomain", ProcessorAgreementJpaEntity.class);
        m.setAccessible(true);
        return (ProcessorAgreement) m.invoke(null, e);
    }
}

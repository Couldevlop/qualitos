package com.openlab.qualitos.quality.crossbordertransfers.infrastructure;

import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;
import com.openlab.qualitos.quality.crossbordertransfers.domain.TransferMechanism;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrossBorderTransferMapperTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void roundtrip_emptySets_csvFieldsNull() throws Exception {
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-1",
                "Acme", null, null, Set.of(),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), U, NOW);
        CrossBorderTransferJpaEntity e = invokeToEntity(t, null);
        assertThat(e.getDestinationCountriesCsv()).isNull();
        assertThat(e.getDataCategoriesCsv()).isNull();
        assertThat(e.getLinkedProcessingActivityIdsCsv()).isNull();
        assertThat(e.getLinkedProcessorAgreementIdsCsv()).isNull();

        CrossBorderTransfer back = invokeToDomain(e);
        assertThat(back.getDestinationCountries()).isEmpty();
        assertThat(back.getDataCategories()).isEmpty();
        assertThat(back.getLinkedProcessingActivityIds()).isEmpty();
        assertThat(back.getLinkedProcessorAgreementIds()).isEmpty();
    }

    @Test
    void roundtrip_filledSets_csvPopulated() throws Exception {
        UUID act = UUID.randomUUID();
        UUID dpa = UUID.randomUUID();
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-2",
                "Acme", null, null, Set.of("US", "IN"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC 2021", null, null,
                Set.of("identity", "contact"), Set.of(act), Set.of(dpa), U, NOW);
        CrossBorderTransferJpaEntity e = invokeToEntity(t, null);
        assertThat(e.getDestinationCountriesCsv()).contains("US").contains("IN");
        assertThat(e.getDataCategoriesCsv()).contains("identity");
        assertThat(e.getLinkedProcessingActivityIdsCsv()).contains(act.toString());
        assertThat(e.getLinkedProcessorAgreementIdsCsv()).contains(dpa.toString());

        CrossBorderTransfer back = invokeToDomain(e);
        assertThat(back.getDestinationCountries()).containsExactlyInAnyOrder("US", "IN");
        assertThat(back.getLinkedProcessingActivityIds()).containsExactly(act);
        assertThat(back.getLinkedProcessorAgreementIds()).containsExactly(dpa);
    }

    @Test
    void roundtrip_terminatedTransfer_metadataPreserved() throws Exception {
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-3",
                "Acme", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), U, NOW);
        t.activate(NOW);
        t.terminate("Fin contrat", NOW.plusSeconds(86400));
        CrossBorderTransferJpaEntity e = invokeToEntity(t, null);
        assertThat(e.getStatus().name()).isEqualTo("TERMINATED");
        assertThat(e.getTerminationReason()).isEqualTo("Fin contrat");
        CrossBorderTransfer back = invokeToDomain(e);
        assertThat(back.isTerminal()).isTrue();
        assertThat(back.getEffectiveTo()).isEqualTo(NOW.plusSeconds(86400));
    }

    private static CrossBorderTransferJpaEntity invokeToEntity(
            CrossBorderTransfer t, CrossBorderTransferJpaEntity target) throws Exception {
        Method m = CrossBorderTransferMapper.class.getDeclaredMethod(
                "toEntity", CrossBorderTransfer.class, CrossBorderTransferJpaEntity.class);
        m.setAccessible(true);
        return (CrossBorderTransferJpaEntity) m.invoke(null, t, target);
    }

    private static CrossBorderTransfer invokeToDomain(CrossBorderTransferJpaEntity e) throws Exception {
        Method m = CrossBorderTransferMapper.class.getDeclaredMethod(
                "toDomain", CrossBorderTransferJpaEntity.class);
        m.setAccessible(true);
        return (CrossBorderTransfer) m.invoke(null, e);
    }
}

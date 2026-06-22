package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument.SectionPlan;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DossierMapperTest {

    private final ObjectMapper json = new ObjectMapper();

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID STD = UUID.randomUUID();
    private static final UUID AUTHOR = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-06-22T10:00:00Z");

    private DocumentationDossier sample() {
        DossierDocument m = DossierDocument.planned("manuel", NormDocKind.MANUAL, "Manuel",
                List.of(new SectionPlan("ctx", "Contexte", List.of("4.1"), "consigne")));
        m.markGenerated(UUID.randomUUID());
        DossierDocument p = DossierDocument.planned("pol", NormDocKind.POLICY, "Politique",
                List.of(new SectionPlan("eng", "Engagement", List.of("5.2"), "")));
        p.markFailed("boom");
        return DocumentationDossier.start(TENANT, STD, "iso-9001", "ISO 9001:2015",
                "ACME", "fr", List.of(m, p), AUTHOR, NOW);
    }

    @Test
    void roundTrip_preservesAggregate() {
        DocumentationDossier original = sample();
        original.assignId(UUID.randomUUID());
        original.recordProvider("ollama", NOW);

        DossierJpaEntity entity = DossierMapper.toEntity(original, null, json);
        DocumentationDossier back = DossierMapper.toDomain(entity, json);

        assertThat(back.getTenantId()).isEqualTo(TENANT);
        assertThat(back.getStandardCode()).isEqualTo("iso-9001");
        assertThat(back.getOrganizationName()).isEqualTo("ACME");
        assertThat(back.getAiProvider()).isEqualTo("ollama");
        assertThat(back.getDocuments()).hasSize(2);
        DossierDocument m = back.document("manuel");
        assertThat(m.getStatus()).isEqualTo(DossierDocStatus.GENERE);
        assertThat(m.getNormDocId()).isNotNull();
        assertThat(m.getSections().get(0).clauses()).containsExactly("4.1");
        assertThat(back.document("pol").getStatus()).isEqualTo(DossierDocStatus.ECHEC);
    }

    @Test
    void toEntity_reusesTarget_onUpdate() {
        DocumentationDossier d = sample();
        d.assignId(UUID.randomUUID());
        DossierJpaEntity target = new DossierJpaEntity();
        DossierJpaEntity out = DossierMapper.toEntity(d, target, json);
        assertThat(out).isSameAs(target);
        assertThat(out.getStatus()).isEqualTo(DossierStatus.GENERATION_EN_COURS);
    }
}

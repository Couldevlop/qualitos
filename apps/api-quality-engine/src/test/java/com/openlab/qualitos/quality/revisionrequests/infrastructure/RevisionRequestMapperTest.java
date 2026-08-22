package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.revisionrequests.domain.ProposedChange;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStatus;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'aller-retour domaine → entité → domaine, changement proposé compris.
 *
 * <p>Le changement voyage en JSON dans une colonne texte : un guillemet mal
 * échappé le rendrait illisible au redémarrage suivant, sans qu'aucun test de
 * service ne s'en aperçoive.
 */
class RevisionRequestMapperTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID TARGET = UUID.randomUUID();
    static final UUID TRIGGER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    @Test
    void aRatingProposalSurvivesTheRoundTrip() {
        RevisionRequest source = RevisionRequest.rehydrate(TARGET, TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, TARGET, RevisionTriggerType.NC_CREATED, TRIGGER,
                "NC-2026-0143", "3 NC en 12 mois — occurrence 4 → 6",
                ProposedChange.rating("occurrence", 4, 6), RevisionRequestStatus.PENDING,
                null, null, null, NOW, NOW);

        RevisionRequest back = RevisionRequestMapper.toDomain(
                RevisionRequestMapper.toEntity(source, null));

        assertThat(back.getId()).isEqualTo(TARGET);
        assertThat(back.getTenantId()).isEqualTo(TENANT);
        assertThat(back.getProductId()).isEqualTo(PRODUCT);
        assertThat(back.getTargetType()).isEqualTo(RevisionTargetType.PFMEA_ITEM);
        assertThat(back.getTargetId()).isEqualTo(TARGET);
        assertThat(back.getTriggerType()).isEqualTo(RevisionTriggerType.NC_CREATED);
        assertThat(back.getTriggerRefId()).isEqualTo(TRIGGER);
        assertThat(back.getTriggerRefLabel()).isEqualTo("NC-2026-0143");
        assertThat(back.getRationale()).contains("occurrence 4 → 6");
        assertThat(back.getChange()).isEqualTo(ProposedChange.rating("occurrence", 4, 6));
        assertThat(back.getStatus()).isEqualTo(RevisionRequestStatus.PENDING);
    }

    @Test
    void aDecidedProposalKeepsItsDecision() {
        RevisionRequest source = RevisionRequest.rehydrate(TARGET, TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, TARGET, RevisionTriggerType.CAPA_CLOSED, TRIGGER,
                "CAPA-12", "justifié", ProposedChange.rating("detection", 7, 6),
                RevisionRequestStatus.REJECTED, USER, NOW, "revue le 12/08", NOW, NOW);

        RevisionRequest back = RevisionRequestMapper.toDomain(
                RevisionRequestMapper.toEntity(source, null));

        assertThat(back.getStatus()).isEqualTo(RevisionRequestStatus.REJECTED);
        assertThat(back.getDecidedBy()).isEqualTo(USER);
        assertThat(back.getDecidedAt()).isEqualTo(NOW);
        assertThat(back.getDecisionNote()).isEqualTo("revue le 12/08");
    }

    @Test
    void aCreationProposalKeepsItsDraftAndItsQuotes() {
        RevisionRequest source = RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.CONTROL_PLAN_LINE_CREATE, null, RevisionTriggerType.CAPA_CLOSED,
                TRIGGER, "CAPA-12", "justifié",
                ProposedChange.creation("{\"characteristicLabel\":\"Cote \\\"A\\\"\"}"), NOW);

        RevisionRequest back = RevisionRequestMapper.toDomain(
                RevisionRequestMapper.toEntity(source, null));

        assertThat(back.getTargetId()).isNull();
        assertThat(back.getChange().draftJson()).isEqualTo("{\"characteristicLabel\":\"Cote \\\"A\\\"\"}");
        assertThat(back.getChange().field()).isNull();
    }

    @Test
    void aFreshProposalWithoutAnIdLeavesTheGeneratedOneToTheEntity() {
        RevisionRequest source = RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, TARGET, RevisionTriggerType.NC_CREATED,
                TRIGGER, "NC-1", "justifié", ProposedChange.rating("occurrence", 4, 6), NOW);

        RevisionRequestJpaEntity entity = RevisionRequestMapper.toEntity(source, null);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void anExistingEntityIsUpdatedInPlaceRatherThanReplaced() {
        RevisionRequestJpaEntity existing = new RevisionRequestJpaEntity();
        existing.setId(TARGET);
        RevisionRequest source = RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, TARGET, RevisionTriggerType.NC_CREATED,
                TRIGGER, "NC-1", "justifié", ProposedChange.rating("occurrence", 4, 6), NOW);
        source.assignId(TARGET);

        RevisionRequestJpaEntity target = RevisionRequestMapper.toEntity(source, existing);

        assertThat(target).isSameAs(existing);
        assertThat(target.getRationale()).isEqualTo("justifié");
    }

    @Test
    void aCorruptedChangeInDatabaseFailsLoudlyRatherThanSilently() {
        RevisionRequestJpaEntity entity = RevisionRequestMapper.toEntity(
                RevisionRequest.propose(TENANT, PRODUCT, RevisionTargetType.PFMEA_ITEM, TARGET,
                        RevisionTriggerType.NC_CREATED, TRIGGER, "NC-1", "justifié",
                        ProposedChange.rating("occurrence", 4, 6), NOW),
                null);
        entity.setProposedChange("{ceci n'est pas du JSON");

        assertThatThrownBy(() -> RevisionRequestMapper.toDomain(entity))
                .isInstanceOf(IllegalStateException.class);
    }
}

package com.openlab.qualitos.quality.objective;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QualityObjectiveProposalRepository
        extends JpaRepository<QualityObjectiveProposal, UUID> {

    Optional<QualityObjectiveProposal> findByIdAndTenantId(UUID id, UUID tenantId);

    /** L'historique d'un objectif, du plus récent au plus ancien. */
    List<QualityObjectiveProposal> findByTenantIdAndObjectiveIdOrderByCreatedAtDesc(
            UUID tenantId, UUID objectiveId);

    /** « Qu'est-ce qui attend une décision ? » — la question de l'écran de revue. */
    List<QualityObjectiveProposal> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, ProposalStatus status);

    /**
     * La propositions déjà émise pour cette période et ce niveau, s'il y en a une.
     *
     * <p>Réévaluer la même période ne doit pas créer une seconde proposition :
     * un écran rafraîchi trois fois proposerait trois fois le même dossier, et
     * le pilote aurait à rejeter deux doublons pour prouver qu'il a lu le
     * premier. C'est la même règle que la contrainte
     * {@code uk_qo_proposals_periode} en base — ici pour l'éviter, là pour
     * l'interdire.
     */
    Optional<QualityObjectiveProposal> findByTenantIdAndObjectiveIdAndLevelAndPeriodStart(
            UUID tenantId, UUID objectiveId, ObjectiveLevel level, Instant periodStart);

    /** Les propositions acceptées d'un objectif : celles qui ont donné un dossier. */
    List<QualityObjectiveProposal> findByTenantIdAndObjectiveIdAndStatus(
            UUID tenantId, UUID objectiveId, ProposalStatus status);

    /** L'objectif dont vient ce dossier CAPA, pour que le dossier sache ce qu'il doit prouver. */
    Optional<QualityObjectiveProposal> findByTenantIdAndCapaCaseId(UUID tenantId, UUID capaCaseId);
}

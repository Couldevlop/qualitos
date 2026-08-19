package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanLine;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.revisionrequests.application.ControlPlanDraftPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Trouve — ou ouvre — le brouillon de control plan dans lequel une proposition
 * acceptée vient atterrir.
 *
 * <p>La phase visée est la production : c'est le plan qui pilote ce qui est
 * réellement contrôlé au poste, et donc le seul qu'une non-conformité ou une CAPA
 * puisse légitimement faire évoluer. Un plan de prototype vit le temps d'une
 * étude et n'a pas à recevoir des retours de série.
 */
@Component
public class ControlPlanDraftAdapter implements ControlPlanDraftPort {

    private static final ControlPlanPhase PHASE = ControlPlanPhase.PRODUCTION;

    private final ControlPlanRepository repo;
    private final Clock clock;

    public ControlPlanDraftAdapter(ControlPlanRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Optional<UUID> draftPlanFor(UUID tenantId, UUID productId) {
        Optional<ControlPlan> draft = repo.findDraft(tenantId, productId, PHASE);
        if (draft.isPresent()) {
            // Une révision est déjà ouverte : on y ajoute. Ouvrir la seconde ferait
            // rejeter l'écriture par l'index partiel d'unicité.
            return draft.map(ControlPlan::getId);
        }
        return repo.findActive(tenantId, productId, PHASE).map(this::openRevisionOf);
    }

    private UUID openRevisionOf(ControlPlan active) {
        ControlPlan next = repo.save(active.nextRevision(active.getCreatedBy(), Instant.now(clock)));
        for (ControlPlanLine source : repo.linesOf(active.getId())) {
            repo.saveLine(copyOf(source, next.getId()));
        }
        return next.getId();
    }

    @Override
    @Transactional
    public UUID addLine(UUID tenantId, UUID planId, String characteristicLabel,
                        String controlMethod, UUID fmeaItemId) {
        List<ControlPlanLine> existing = repo.linesOf(planId);
        int nextSequence = existing.stream()
                .mapToInt(ControlPlanLine::getSequenceNo)
                .max()
                .orElse(0) + 10;

        ControlPlanLine line = ControlPlanLine.create(tenantId, planId, nextSequence,
                label(characteristicLabel), CharacteristicType.PROCESS);
        line.describe(null, null, null, null, null, null, null, null, null, null, null,
                controlMethod, null);
        line.justifiedBy(fmeaItemId);
        return repo.saveLine(line).getId();
    }

    /** Une ligne sans intitulé serait refusée par le domaine : on nomme l'ignorance. */
    private static String label(String characteristicLabel) {
        return characteristicLabel == null || characteristicLabel.isBlank()
                ? "Caractéristique à préciser"
                : characteristicLabel;
    }

    private static ControlPlanLine copyOf(ControlPlanLine source, UUID targetPlanId) {
        ControlPlanLine copy = ControlPlanLine.create(source.getTenantId(), targetPlanId,
                source.getSequenceNo(), source.getCharacteristicLabel(), source.getCharacteristicType());
        copy.describe(source.getOperationId(), source.getMachine(), source.getCharacteristicNo(),
                source.getSpecialClass(), source.getSpecification(), source.getToleranceLower(),
                source.getToleranceUpper(), source.getUnit(), source.getMeasurementTechnique(),
                source.getSampleSize(), source.getSampleFrequency(), source.getControlMethod(),
                source.getReactionPlan());
        copy.justifiedBy(source.getFmeaItemId());
        return copy;
    }
}

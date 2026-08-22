package com.openlab.qualitos.quality.controlplan.application;

import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanLine;
import com.openlab.qualitos.quality.controlplan.domain.InputOutput;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import com.openlab.qualitos.quality.risk.CharacteristicClass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Commandes et vues du control plan. Conteneur de {@code record}s, aucune logique.
 * Les commandes ne portent jamais de {@code tenantId} : le tenant vient toujours
 * du contexte de sécurité côté service, jamais d'une valeur forgeable par le client.
 */
public final class ControlPlanDto {

    private ControlPlanDto() {}

    public record CreateCommand(ControlPlanPhase phase, String code, UUID ownerUserId) {}

    public record LineCommand(
            int sequenceNo, UUID operationId, String machine, String characteristicNo,
            String characteristicLabel, CharacteristicType characteristicType,
            CharacteristicClass specialClass, String specification,
            BigDecimal toleranceLower, BigDecimal toleranceUpper, String unit,
            String measurementTechnique, String sampleSize, String sampleFrequency,
            String controlMethod, String reactionPlan, UUID fmeaItemId,
            String sopReference, InputOutput inputOutput,
            String whoMeasures, String recordingLocation) {}

    /**
     * Le plan tel qu'il se lit. {@code sealSha256} et {@code anchorTxRef}
     * accompagnent un plan approuvé : c'est ce qu'un auditeur recopie pour
     * vérifier lui-même, sans nous croire sur parole.
     *
     * <p>La SIGNATURE n'est pas rendue ici. Elle pèse plusieurs kilo-octets sur
     * chaque ligne d'une liste, et personne ne la vérifie à l'œil : elle se
     * demande sur le document, pas sur son résumé.
     */
    public record View(
            UUID id, UUID productId, ControlPlanPhase phase, String code, int revision,
            ControlPlanStatus status, UUID ownerUserId, UUID approvedBy, Instant approvedAt,
            Instant createdAt, Instant updatedAt, String sealSha256, String anchorTxRef) {

        public static View of(ControlPlan p) {
            return new View(
                    p.getId(), p.getProductId(), p.getPhase(), p.getCode(), p.getRevision(),
                    p.getStatus(), p.getOwnerUserId(), p.getApprovedBy(), p.getApprovedAt(),
                    p.getCreatedAt(), p.getUpdatedAt(), p.getSealSha256(), p.getAnchorTxRef());
        }
    }

    public record LineView(
            UUID id, int sequenceNo, UUID operationId, String machine, String characteristicNo,
            String characteristicLabel, CharacteristicType characteristicType,
            CharacteristicClass specialClass, String specification,
            BigDecimal toleranceLower, BigDecimal toleranceUpper, String unit,
            String measurementTechnique, String sampleSize, String sampleFrequency,
            String controlMethod, String reactionPlan, UUID fmeaItemId,
            String sopReference, InputOutput inputOutput,
            String whoMeasures, String recordingLocation) {

        public static LineView of(ControlPlanLine l) {
            return new LineView(
                    l.getId(), l.getSequenceNo(), l.getOperationId(), l.getMachine(),
                    l.getCharacteristicNo(), l.getCharacteristicLabel(), l.getCharacteristicType(),
                    l.getSpecialClass(), l.getSpecification(), l.getToleranceLower(),
                    l.getToleranceUpper(), l.getUnit(), l.getMeasurementTechnique(),
                    l.getSampleSize(), l.getSampleFrequency(), l.getControlMethod(),
                    l.getReactionPlan(), l.getFmeaItemId(), l.getSopReference(),
                    l.getInputOutput(), l.getWhoMeasures(), l.getRecordingLocation());
        }
    }

    public record Detail(View plan, List<LineView> lines) {}
}

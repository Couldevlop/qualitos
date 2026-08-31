package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanLine;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import com.openlab.qualitos.quality.controlplan.domain.InputOutput;
import com.openlab.qualitos.quality.risk.CharacteristicClass;

/**
 * Traduction explicite entre le domaine et la persistance.
 *
 * <p>Un mapper écrit à la main plutôt qu'une entité JPA annotée directement dans
 * le domaine : c'est le prix — modeste — pour que les invariants métier ne
 * dépendent pas du cycle de vie d'Hibernate.
 */
final class ControlPlanMapper {

    private ControlPlanMapper() {}

    static ControlPlanJpaEntity toEntity(ControlPlan p, ControlPlanJpaEntity target) {
        ControlPlanJpaEntity e = target != null ? target : new ControlPlanJpaEntity();
        if (p.getId() != null) e.setId(p.getId());
        e.setTenantId(p.getTenantId());
        e.setProductId(p.getProductId());
        e.setCode(p.getCode());
        e.setPhase(p.getPhase().name());
        e.setRevision(p.getRevision());
        e.setStatus(p.getStatus().name());
        e.setOwnerUserId(p.getOwnerUserId());
        e.setApprovedBy(p.getApprovedBy());
        e.setApprovedAt(p.getApprovedAt());
        e.setCreatedBy(p.getCreatedBy());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        e.setSealSha256(p.getSealSha256());
        e.setSealSignature(p.getSealSignature());
        e.setAnchorTxRef(p.getAnchorTxRef());
        e.setSealVersion(p.getSealVersion());
        return e;
    }

    static ControlPlan toDomain(ControlPlanJpaEntity e) {
        return ControlPlan.rehydrate(
                e.getId(), e.getTenantId(), e.getProductId(),
                ControlPlanPhase.valueOf(e.getPhase()), e.getCode(), e.getRevision(),
                ControlPlanStatus.valueOf(e.getStatus()), e.getOwnerUserId(),
                e.getApprovedBy(), e.getApprovedAt(), e.getCreatedBy(),
                e.getCreatedAt(), e.getUpdatedAt(),
                e.getSealSha256(), e.getSealSignature(), e.getAnchorTxRef(),
                e.getSealVersion());
    }

    static ControlPlanLineJpaEntity toEntity(ControlPlanLine l, ControlPlanLineJpaEntity target) {
        ControlPlanLineJpaEntity e = target != null ? target : new ControlPlanLineJpaEntity();
        if (l.getId() != null) e.setId(l.getId());
        e.setTenantId(l.getTenantId());
        e.setPlanId(l.getPlanId());
        e.setSequenceNo(l.getSequenceNo());
        e.setOperationId(l.getOperationId());
        e.setMachine(l.getMachine());
        e.setCharacteristicNo(l.getCharacteristicNo());
        e.setCharacteristicLabel(l.getCharacteristicLabel());
        e.setSpecifiedCharacteristic(l.getSpecifiedCharacteristic());
        e.setCharacteristicType(l.getCharacteristicType().name());
        e.setSpecialClass(l.getSpecialClass().name());
        e.setSpecification(l.getSpecification());
        e.setToleranceLower(l.getToleranceLower());
        e.setToleranceUpper(l.getToleranceUpper());
        e.setUnit(l.getUnit());
        e.setMeasurementTechnique(l.getMeasurementTechnique());
        e.setSampleSize(l.getSampleSize());
        e.setSampleFrequency(l.getSampleFrequency());
        e.setControlMethod(l.getControlMethod());
        e.setReactionPlan(l.getReactionPlan());
        e.setFmeaItemId(l.getFmeaItemId());
        e.setSopReference(l.getSopReference());
        e.setInputOutput(l.getInputOutput() == null ? null : l.getInputOutput().name());
        e.setWhoMeasures(l.getWhoMeasures());
        e.setRecordingLocation(l.getRecordingLocation());
        return e;
    }

    static ControlPlanLine toDomain(ControlPlanLineJpaEntity e) {
        ControlPlanLine l = ControlPlanLine.rehydrate(e.getId(), e.getTenantId(), e.getPlanId(),
                e.getSequenceNo(), e.getCharacteristicLabel(),
                CharacteristicType.valueOf(e.getCharacteristicType()));
        l.describe(new ControlPlanLine.Details(e.getOperationId(), e.getMachine(),
                e.getCharacteristicNo(), e.getSpecifiedCharacteristic(),
                e.getSpecialClass() == null ? null : CharacteristicClass.valueOf(e.getSpecialClass()),
                e.getSpecification(), e.getToleranceLower(), e.getToleranceUpper(), e.getUnit(),
                e.getMeasurementTechnique(), e.getSampleSize(), e.getSampleFrequency(),
                e.getControlMethod(), e.getReactionPlan(), e.getSopReference(),
                e.getInputOutput() == null ? null : InputOutput.valueOf(e.getInputOutput()),
                e.getWhoMeasures(), e.getRecordingLocation()));
        l.justifiedBy(e.getFmeaItemId());
        return l;
    }
}

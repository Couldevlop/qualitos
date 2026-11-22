package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.domain.Analysis;
import com.openlab.qualitos.quality.derogation.domain.CustomerAnswer;
import com.openlab.qualitos.quality.derogation.domain.CustomerDecision;
import com.openlab.qualitos.quality.derogation.domain.CustomerDispatch;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DerogationLine;
import com.openlab.qualitos.quality.derogation.domain.DerogationStatus;
import com.openlab.qualitos.quality.derogation.domain.DeviationOrigin;
import com.openlab.qualitos.quality.derogation.domain.DurationKind;
import com.openlab.qualitos.quality.derogation.domain.Identification;
import com.openlab.qualitos.quality.derogation.domain.ManagerApproval;
import com.openlab.qualitos.quality.derogation.domain.Nature;
import com.openlab.qualitos.quality.derogation.domain.Occurrence;
import com.openlab.qualitos.quality.derogation.domain.ShipmentStage;

import java.util.List;
import java.util.UUID;

/**
 * Traduit entre l'agrégat et son enregistrement.
 *
 * <p>Cette classe existe pour que le domaine n'ait aucune annotation de
 * persistance : c'est le prix de cette séparation, et il se paie une fois. Elle
 * ne contient aucune règle — si une décision se glissait ici, elle serait
 * invisible aux tests du domaine.
 */
final class DerogationMapper {

    private DerogationMapper() {
    }

    /** Le dossier reconstruit depuis la base, lignes comprises. */
    static Derogation toDomain(DerogationJpaEntity e, List<DerogationLineJpaEntity> lignes) {
        return Derogation.rehydrate(
                e.getId(), e.getTenantId(), e.getReference(),
                DerogationStatus.valueOf(e.getStatus()),
                new Identification(e.getCustomerName(), e.getCustomerQualityContact(),
                        e.getCustomerOrderNo(), e.getPartReference(), e.getPartIndex(),
                        e.getBatchNumber(), e.getQuantity(), e.getRequestDate()),
                new Nature(ShipmentStage.valueOf(e.getShipmentStage()),
                        DeviationOrigin.valueOf(e.getDeviationOrigin()),
                        Occurrence.valueOf(e.getOccurrence()),
                        DurationKind.valueOf(e.getDurationKind()),
                        e.getDurationDetail()),
                lignes.stream().map(DerogationMapper::toDomain).toList(),
                e.getJustification(),
                new Analysis(e.getRootCause(), e.getContainment(), e.getCorrectiveAction(),
                        e.getCapaReference(), e.getAttachmentsNote()),
                new ManagerApproval(e.getManagerAccountId(), e.getManagerAccountName(),
                        e.getSignedByName(), e.getSignaturePng(), e.getSignedAt(),
                        e.getAmendReason()),
                new CustomerDispatch(e.getCustomerEmail(), e.getSendComment(),
                        e.getPdfLanguage() == null
                                ? null : DerogationLanguage.of(e.getPdfLanguage()),
                        e.getSentAt()),
                new CustomerAnswer(CustomerDecision.valueOf(e.getCustomerDecision()),
                        e.getCustomerDecisionDate()),
                e.getCreatedByUserId(), e.getCreatedByName(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    static DerogationLine toDomain(DerogationLineJpaEntity l) {
        return new DerogationLine(l.getPosition(), l.getSpecification(),
                l.getActualValue(), l.getNonConformingQty());
    }

    /**
     * Reporte l'état du dossier sur l'enregistrement.
     *
     * <p>On écrit sur l'entité <b>gérée</b> quand elle existe : charger puis
     * copier laisse Hibernate comparer les versions, ce qui est exactement la
     * protection recherchée contre deux gestes concurrents.
     */
    static void copyToEntity(Derogation d, DerogationJpaEntity e) {
        e.setTenantId(d.getTenantId());
        e.setReference(d.getReference());
        e.setStatus(d.getStatus().name());

        Identification ident = d.getIdentification();
        e.setCustomerName(ident.customerName());
        e.setCustomerQualityContact(ident.customerQualityContact());
        e.setCustomerOrderNo(ident.customerOrderNo());
        e.setPartReference(ident.partReference());
        e.setPartIndex(ident.partIndex());
        e.setBatchNumber(ident.batchNumber());
        e.setQuantity(ident.quantity());
        e.setRequestDate(ident.requestDate());

        Nature nat = d.getNature();
        e.setShipmentStage(nat.shipmentStage().name());
        e.setDeviationOrigin(nat.deviationOrigin().name());
        e.setOccurrence(nat.occurrence().name());
        e.setDurationKind(nat.durationKind().name());
        e.setDurationDetail(nat.durationDetail());

        e.setJustification(d.getJustification());

        Analysis ana = d.getAnalysis();
        e.setRootCause(ana.rootCause());
        e.setContainment(ana.containment());
        e.setCorrectiveAction(ana.correctiveAction());
        e.setCapaReference(ana.capaReference());
        e.setAttachmentsNote(ana.attachmentsNote());

        ManagerApproval app = d.getApproval();
        e.setManagerAccountId(app.managerAccountId());
        e.setManagerAccountName(app.managerAccountName());
        e.setSignedByName(app.signedByName());
        e.setSignaturePng(app.signaturePng());
        e.setSignedAt(app.signedAt());
        e.setAmendReason(app.amendReason());

        CustomerDispatch env = d.getDispatch();
        e.setCustomerEmail(env.customerEmail());
        e.setSendComment(env.comment());
        e.setPdfLanguage(env.language() == null ? null : env.language().code());
        e.setSentAt(env.sentAt());

        e.setCustomerDecision(d.getAnswer().decision().name());
        e.setCustomerDecisionDate(d.getAnswer().decidedOn());

        e.setCreatedByUserId(d.getCreatedByUserId());
        e.setCreatedByName(d.getCreatedByName());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
    }

    static DerogationLineJpaEntity toEntity(UUID tenantId, UUID derogationId, DerogationLine l) {
        DerogationLineJpaEntity e = new DerogationLineJpaEntity();
        e.setTenantId(tenantId);
        e.setDerogationId(derogationId);
        e.setPosition((short) l.position());
        e.setSpecification(l.specification());
        e.setActualValue(l.actualValue());
        e.setNonConformingQty(l.nonConformingQty());
        return e;
    }
}

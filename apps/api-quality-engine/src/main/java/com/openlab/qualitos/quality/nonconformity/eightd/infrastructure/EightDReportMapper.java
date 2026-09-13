package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReport;

/**
 * Traduit l'agrégat en ligne et la ligne en agrégat.
 *
 * <p>Classe à part plutôt que deux méthodes dans l'adaptateur : c'est le seul
 * endroit où une colonne oubliée passerait inaperçue, et un test de correspondance
 * aller-retour le vérifie une fois pour toutes.
 */
public final class EightDReportMapper {

    private EightDReportMapper() {
    }

    public static EightDReportJpaEntity toEntity(EightDReport report, EightDReportJpaEntity cible) {
        EightDReportJpaEntity entity = cible == null ? new EightDReportJpaEntity() : cible;
        entity.setId(report.getId());
        entity.setTenantId(report.getTenantId());
        entity.setNcId(report.getNcId());
        entity.setStatus(report.getStatus());
        entity.setTeam(report.getTeam());
        entity.setContainment(report.getContainment());
        entity.setRecognition(report.getRecognition());
        entity.setSnapshotJson(report.getSnapshotJson());
        entity.setSha256Hex(report.getSha256Hex());
        entity.setSignature(report.getSignature());
        entity.setAnchorTxRef(report.getAnchorTxRef());
        entity.setVerificationCode(report.getVerificationCode());
        entity.setIssuedAt(report.getIssuedAt());
        entity.setIssuedBy(report.getIssuedBy());
        entity.setIssuedByName(report.getIssuedByName());
        entity.setCreatedAt(report.getCreatedAt());
        entity.setUpdatedAt(report.getUpdatedAt());
        return entity;
    }

    public static EightDReport toDomain(EightDReportJpaEntity entity) {
        return EightDReport.reconstituer(
                entity.getId(), entity.getTenantId(), entity.getNcId(), entity.getStatus(),
                entity.getTeam(), entity.getContainment(), entity.getRecognition(),
                entity.getSnapshotJson(), entity.getSha256Hex(), entity.getSignature(),
                entity.getAnchorTxRef(), entity.getVerificationCode(), entity.getIssuedAt(),
                entity.getIssuedBy(), entity.getIssuedByName(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

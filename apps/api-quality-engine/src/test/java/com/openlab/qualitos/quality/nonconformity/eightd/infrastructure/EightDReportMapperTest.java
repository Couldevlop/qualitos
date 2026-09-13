package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReport;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'aller-retour agrégat ↔ ligne. Une colonne oubliée dans le mapper ne se verrait
 * nulle part ailleurs : la preuve partirait simplement à zéro après un redémarrage.
 */
class EightDReportMapperTest {

    private static final Instant T0 = Instant.parse("2026-09-13T10:00:00Z");

    @Test
    void un_rapport_emis_traverse_la_persistance_sans_rien_perdre() {
        UUID id = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        UUID nc = UUID.randomUUID();
        UUID acteur = UUID.randomUUID();
        EightDReport origine = EightDReport.reconstituer(
                id, tenant, nc, EightDStatus.ISSUED, "Ada", "Tri 100 %", "Merci",
                "{\"sections\":[]}", "c".repeat(64), "enveloppe", "tx-9",
                "ABCDEFGHIJKLMNOP", T0, acteur, "Ada Lovelace", T0.minusSeconds(60), T0);

        EightDReport relu = EightDReportMapper.toDomain(
                EightDReportMapper.toEntity(origine, null));

        assertThat(relu.getId()).isEqualTo(id);
        assertThat(relu.getTenantId()).isEqualTo(tenant);
        assertThat(relu.getNcId()).isEqualTo(nc);
        assertThat(relu.getStatus()).isEqualTo(EightDStatus.ISSUED);
        assertThat(relu.getTeam()).isEqualTo("Ada");
        assertThat(relu.getContainment()).isEqualTo("Tri 100 %");
        assertThat(relu.getRecognition()).isEqualTo("Merci");
        assertThat(relu.getSnapshotJson()).isEqualTo("{\"sections\":[]}");
        assertThat(relu.getSha256Hex()).isEqualTo("c".repeat(64));
        assertThat(relu.getSignature()).isEqualTo("enveloppe");
        assertThat(relu.getAnchorTxRef()).isEqualTo("tx-9");
        assertThat(relu.getVerificationCode()).isEqualTo("ABCDEFGHIJKLMNOP");
        assertThat(relu.getIssuedAt()).isEqualTo(T0);
        assertThat(relu.getIssuedBy()).isEqualTo(acteur);
        assertThat(relu.getIssuedByName()).isEqualTo("Ada Lovelace");
        assertThat(relu.getCreatedAt()).isEqualTo(T0.minusSeconds(60));
        assertThat(relu.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void un_brouillon_traverse_la_persistance_avec_ses_colonnes_vides() {
        EightDReport origine = EightDReport.brouillon(
                UUID.randomUUID(), UUID.randomUUID(), T0);

        EightDReport relu = EightDReportMapper.toDomain(
                EightDReportMapper.toEntity(origine, null));

        assertThat(relu.getStatus()).isEqualTo(EightDStatus.DRAFT);
        assertThat(relu.estEmis()).isFalse();
        assertThat(relu.getSnapshotJson()).isNull();
        assertThat(relu.getVerificationCode()).isNull();
    }

    @Test
    void une_ligne_existante_est_mise_a_jour_en_place() {
        EightDReportJpaEntity existante = new EightDReportJpaEntity();
        existante.setTeam("ancienne équipe");
        EightDReport report = EightDReport.brouillon(UUID.randomUUID(), UUID.randomUUID(), T0);
        report.saisir("nouvelle équipe", null, null, T0);

        EightDReportJpaEntity resultat = EightDReportMapper.toEntity(report, existante);

        // La même instance, et non une seconde : sans cela, Hibernate tenterait un
        // second INSERT sur la contrainte d'unicité (tenant, NC).
        assertThat(resultat).isSameAs(existante);
        assertThat(resultat.getTeam()).isEqualTo("nouvelle équipe");
    }
}

package com.openlab.qualitos.quality.nonconformity.eightd.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** L'agrégat : ce qui se saisit, ce qui se scelle, et ce qui ne se reprend plus. */
class EightDReportTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID NC = UUID.randomUUID();
    private static final UUID ACTEUR = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-09-13T10:00:00Z");
    private static final String SHA = "a".repeat(64);
    private static final String CODE = "ABCDEFGHIJKLMNOP";

    @Test
    void un_brouillon_naissant_ne_porte_aucune_preuve() {
        EightDReport report = EightDReport.brouillon(TENANT, NC, T0);

        assertThat(report.getStatus()).isEqualTo(EightDStatus.DRAFT);
        assertThat(report.estEmis()).isFalse();
        assertThat(report.getSha256Hex()).isNull();
        assertThat(report.getVerificationCode()).isNull();
        assertThat(report.getCreatedAt()).isEqualTo(T0);
    }

    @Test
    void la_saisie_se_reprend_autant_qu_on_veut_et_les_blancs_deviennent_nuls() {
        EightDReport report = EightDReport.brouillon(TENANT, NC, T0);

        report.saisir("  Ada, Grace  ", "   ", null, T0.plusSeconds(60));

        assertThat(report.getTeam()).isEqualTo("Ada, Grace");
        // Trois espaces ne sont pas un endiguement : on range null, pour que la
        // discipline se déclare sans source au lieu de paraître renseignée.
        assertThat(report.getContainment()).isNull();
        assertThat(report.getRecognition()).isNull();
        assertThat(report.getUpdatedAt()).isEqualTo(T0.plusSeconds(60));
    }

    @Test
    void une_saisie_trop_longue_est_refusee() {
        EightDReport report = EightDReport.brouillon(TENANT, NC, T0);

        assertThatThrownBy(() -> report.saisir("x".repeat(EightDReport.MAX_SAISIE + 1), null, null, T0))
                .isInstanceOf(EightDValidationException.class)
                .hasMessageContaining("D1");
    }

    @Test
    void l_emission_fige_le_contenu_et_attache_la_preuve() {
        EightDReport report = EightDReport.brouillon(TENANT, NC, T0);
        report.saisir("Ada", "Tri 100 %", "Merci", T0);

        report.emettre("{\"x\":1}", SHA, "env", "tx-1", CODE, ACTEUR, "Ada Lovelace", T0.plusSeconds(5));

        assertThat(report.estEmis()).isTrue();
        assertThat(report.getStatus()).isEqualTo(EightDStatus.ISSUED);
        assertThat(report.getSnapshotJson()).isEqualTo("{\"x\":1}");
        assertThat(report.getSha256Hex()).isEqualTo(SHA);
        assertThat(report.getAnchorTxRef()).isEqualTo("tx-1");
        assertThat(report.getIssuedBy()).isEqualTo(ACTEUR);
        assertThat(report.getIssuedByName()).isEqualTo("Ada Lovelace");
        assertThat(report.getIssuedAt()).isEqualTo(T0.plusSeconds(5));
    }

    @Test
    void un_rapport_emis_ne_se_saisit_plus() {
        EightDReport report = emis();

        assertThatThrownBy(() -> report.saisir("autre équipe", null, null, T0))
                .isInstanceOf(EightDStateException.class)
                .hasMessageContaining("scellé");
    }

    @Test
    void un_rapport_emis_ne_se_reemet_pas() {
        EightDReport report = emis();

        assertThatThrownBy(() -> report.emettre("{}", SHA, "env", "tx-2", CODE, ACTEUR, null, T0))
                .isInstanceOf(EightDStateException.class);
    }

    @Test
    void une_empreinte_mal_formee_est_refusee() {
        EightDReport report = EightDReport.brouillon(TENANT, NC, T0);

        assertThatThrownBy(() -> report.emettre("{}", "PAS-UNE-EMPREINTE", "env", "tx", CODE, ACTEUR, null, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sha256Hex");
    }

    @Test
    void un_code_de_verification_trop_court_est_refuse() {
        EightDReport report = EightDReport.brouillon(TENANT, NC, T0);

        assertThatThrownBy(() -> report.emettre("{}", SHA, "env", "tx", "trop-court", ACTEUR, null, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verificationCode");
    }

    @Test
    void une_ligne_emise_relue_sans_sceau_complet_est_refusee() {
        // Une ligne ISSUED sans ancrage n'est pas une demi-preuve : ce n'est pas une
        // preuve. La reconstitution doit échouer plutôt que porter la contradiction.
        assertThatThrownBy(() -> EightDReport.reconstituer(
                UUID.randomUUID(), TENANT, NC, EightDStatus.ISSUED, null, null, null,
                "{}", SHA, "env", null, CODE, T0, ACTEUR, null, T0, T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anchorTxRef");
    }

    private EightDReport emis() {
        EightDReport report = EightDReport.brouillon(TENANT, NC, T0);
        report.emettre("{}", SHA, "env", "tx-1", CODE, ACTEUR, "Ada", T0);
        return report;
    }
}

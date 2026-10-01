package com.openlab.qualitos.quality.riskregister.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTEUR = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-10-01T08:00:00Z");
    static final Instant T1 = Instant.parse("2026-10-02T08:00:00Z");

    static Identification ident() {
        return new Identification("  Dérive du procédé de soudure  ", RegisterType.QUALITY, "Production",
                "Usine A", "M. Kone", RegisterOrigin.FMEA, "AMDEC-12",
                Set.of(RegisterRequirement.ISO_9001_6_1, RegisterRequirement.IATF_16949_6_1_2));
    }

    static RiskDetails details(int g, int p) {
        return new RiskDetails(ident(), "Usure buse", "Soudure non conforme", g, p, null, null,
                null, null, null, null);
    }

    static RiskDetails avecResiduelle(int g, int p, Integer rg, Integer rp) {
        return new RiskDetails(ident(), null, null, g, p, rg, rp, RiskDecision.REDUCE,
                RiskStatus.IN_TREATMENT, LocalDate.of(2027, 1, 15), "Cpk > 1,33 sur trois mois");
    }

    @Test
    void unRisqueNeufEstATraiterSansDecisionEtSansResiduelle() {
        Risk r = Risk.create(TENANT, "R-001", details(4, 3), AUTEUR, T0);

        assertThat(r.getReference()).isEqualTo("R-001");
        assertThat(r.getIdentification().title()).isEqualTo("Dérive du procédé de soudure");
        assertThat(r.getStatus()).isEqualTo(RiskStatus.TO_TREAT);
        assertThat(r.getDecision()).isEqualTo(RiskDecision.UNDECIDED);
        assertThat(r.getGross().score()).isEqualTo(12);
        assertThat(r.grossLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.getResidual()).isNull();
        assertThat(r.residualLevel()).isNull();
        assertThat(r.getCreatedAt()).isEqualTo(T0);
        assertThat(r.getCreatedBy()).isEqualTo(AUTEUR);
    }

    @ParameterizedTest
    @CsvSource({"1,1,LOW", "2,2,LOW", "1,5,MEDIUM", "3,3,MEDIUM", "2,5,HIGH", "3,4,HIGH",
                "3,5,CRITICAL", "5,5,CRITICAL"})
    void leNiveauSuitLesSeuilsDeLaMaquette(int g, int p, RiskLevel attendu) {
        assertThat(Risk.create(TENANT, "R-001", details(g, p), AUTEUR, T0).grossLevel()).isEqualTo(attendu);
    }

    @Test
    void uneNoteHorsDeUnACinqEstRefuseeSurSonChamp() {
        assertThatThrownBy(() -> Risk.create(TENANT, "R-001", details(6, 3), AUTEUR, T0))
                .isInstanceOf(RegisterValidationException.class)
                .extracting("field").isEqualTo("grossSeverity");
        assertThatThrownBy(() -> Risk.create(TENANT, "R-001", details(3, 0), AUTEUR, T0))
                .extracting("field").isEqualTo("grossProbability");
    }

    @Test
    void uneNoteAbsenteEstRefusee() {
        RiskDetails sansNote = new RiskDetails(ident(), null, null, null, 3, null, null, null, null, null, null);
        assertThatThrownBy(() -> Risk.create(TENANT, "R-001", sansNote, AUTEUR, T0))
                .extracting("field").isEqualTo("grossSeverity");
    }

    @Test
    void laResiduelleSeDonneParDeuxNotesOuPasDuTout() {
        assertThatThrownBy(() -> Risk.create(TENANT, "R-001", avecResiduelle(4, 3, 4, null), AUTEUR, T0))
                .extracting("field").isEqualTo("residualProbability");
    }

    @Test
    void laResiduelleNeDepassePasLaBrute() {
        assertThatThrownBy(() -> Risk.create(TENANT, "R-001", avecResiduelle(2, 2, 3, 2), AUTEUR, T0))
                .isInstanceOf(RegisterValidationException.class)
                .extracting("field").isEqualTo("residualSeverity");
    }

    @Test
    void laResiduelleEgaleALaBruteEstAdmise() {
        Risk r = Risk.create(TENANT, "R-001", avecResiduelle(4, 3, 3, 4), AUTEUR, T0);
        assertThat(r.getResidual().score()).isEqualTo(12);
        assertThat(r.residualLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.getEffectivenessCriterion()).isEqualTo("Cpk > 1,33 sur trois mois");
        assertThat(r.getNextReviewOn()).isEqualTo(LocalDate.of(2027, 1, 15));
    }

    @Test
    void reviserRendLesChangementsQuiRacontentLaFiche() {
        Risk r = Risk.create(TENANT, "R-001", details(3, 3), AUTEUR, T0);

        List<RegisterChange> changes = r.revise(avecResiduelle(4, 3, 4, 2), T1);

        assertThat(changes).containsExactly(
                new RegisterChange(RegisterEventType.RATING_CHANGED, "3x3", "4x3"),
                new RegisterChange(RegisterEventType.RESIDUAL_CHANGED, null, "4x2"),
                new RegisterChange(RegisterEventType.STATUS_CHANGED, "TO_TREAT", "IN_TREATMENT"),
                new RegisterChange(RegisterEventType.DECISION_CHANGED, "UNDECIDED", "REDUCE"));
        assertThat(r.getUpdatedAt()).isEqualTo(T1);
        assertThat(r.getCreatedAt()).isEqualTo(T0);
    }

    @Test
    void corrigerLaCauseNeProduitAucunChangementDeSuivi() {
        Risk r = Risk.create(TENANT, "R-001", details(3, 3), AUTEUR, T0);
        RiskDetails autreCause = new RiskDetails(ident(), "Autre cause", "Autre effet", 3, 3,
                null, null, null, null, null, null);

        assertThat(r.revise(autreCause, T1)).isEmpty();
        assertThat(r.getCause()).isEqualTo("Autre cause");
    }

    @Test
    void retirerLaResiduelleSeTrace() {
        Risk r = Risk.create(TENANT, "R-001", avecResiduelle(4, 3, 4, 2), AUTEUR, T0);
        RiskDetails sans = new RiskDetails(ident(), null, null, 4, 3, null, null, RiskDecision.REDUCE,
                RiskStatus.IN_TREATMENT, null, null);

        assertThat(r.revise(sans, T1)).containsExactly(
                new RegisterChange(RegisterEventType.RESIDUAL_CHANGED, "4x2", null));
        assertThat(r.getResidual()).isNull();
    }

    @Test
    void uneRevisionInvalideNeTouchePasALaFiche() {
        Risk r = Risk.create(TENANT, "R-001", details(3, 3), AUTEUR, T0);

        assertThatThrownBy(() -> r.revise(details(9, 3), T1)).isInstanceOf(RegisterValidationException.class);
        assertThat(r.getGross().code()).isEqualTo("3x3");
        assertThat(r.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void sansIdentificationLIntituleEstRéclamé() {
        RiskDetails vide = new RiskDetails(null, null, null, 3, 3, null, null, null, null, null, null);
        assertThatThrownBy(() -> Risk.create(TENANT, "R-001", vide, AUTEUR, T0))
                .extracting("field").isEqualTo("title");
        assertThatThrownBy(() -> Risk.create(TENANT, "R-001", null, AUTEUR, T0))
                .extracting("field").isEqualTo("title");
    }

    @Test
    void uneCauseTropLongueEstRefusee() {
        RiskDetails longue = new RiskDetails(ident(), "x".repeat(Risk.CAUSE_MAX + 1), null, 3, 3,
                null, null, null, null, null, null);
        assertThatThrownBy(() -> Risk.create(TENANT, "R-001", longue, AUTEUR, T0))
                .extracting("field").isEqualTo("cause");
    }

    @Test
    void leTenantEtLaReferenceSontExiges() {
        assertThatThrownBy(() -> Risk.create(null, "R-001", details(3, 3), AUTEUR, T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Risk.create(TENANT, null, details(3, 3), AUTEUR, T0))
                .isInstanceOf(NullPointerException.class);
    }
}

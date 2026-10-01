package com.openlab.qualitos.quality.riskregister.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpportunityTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OPP = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-10-01T08:00:00Z");
    static final Instant T1 = Instant.parse("2026-10-05T08:00:00Z");

    static OpportunityDetails details(int gain, int faisabilite, OpportunityStatus statut) {
        return new OpportunityDetails(
                new Identification("Automatiser la saisie SPC", RegisterType.QUALITY, "Production", null,
                        "M. Kone", RegisterOrigin.MANAGEMENT_REVIEW, "RDD-2026",
                        Set.of(RegisterRequirement.ISO_9001_6_1, RegisterRequirement.ISO_9001_10_3)),
                LocalDate.of(2027, 3, 31), "Instruments déjà connectables", "Gain de temps", gain, faisabilite,
                null, statut, null);
    }

    @Test
    void uneOpportuniteNeuveEstEnEtude() {
        Opportunity o = Opportunity.create(TENANT, "O-003", details(4, 4, null), null, T0);

        assertThat(o.getStatus()).isEqualTo(OpportunityStatus.UNDER_STUDY);
        assertThat(o.getDecision()).isEqualTo(OpportunityDecision.UNDECIDED);
        assertThat(o.getEvaluation().score()).isEqualTo(16);
        assertThat(o.level()).isEqualTo(OpportunityLevel.PRIORITY);
        assertThat(o.getTargetDate()).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @ParameterizedTest
    @CsvSource({"1,4,LOW", "3,3,MEDIUM", "3,4,HIGH", "5,3,PRIORITY"})
    void leSommetSeDitPrioritaire(int gain, int faisabilite, OpportunityLevel attendu) {
        assertThat(Opportunity.create(TENANT, "O-001", details(gain, faisabilite, null), null, T0).level())
                .isEqualTo(attendu);
    }

    @Test
    void lesNotesSontNommeesGainEtFaisabilite() {
        assertThatThrownBy(() -> Opportunity.create(TENANT, "O-001", details(0, 3, null), null, T0))
                .extracting("field").isEqualTo("gain");
        assertThatThrownBy(() -> Opportunity.create(TENANT, "O-001", details(3, 6, null), null, T0))
                .extracting("field").isEqualTo("feasibility");
    }

    @Test
    void reviserTraceLEvaluationEtLeStatut() {
        Opportunity o = Opportunity.create(TENANT, "O-001", details(4, 3, null), null, T0);

        assertThat(o.revise(details(4, 4, OpportunityStatus.IN_PROGRESS), T1)).containsExactly(
                new RegisterChange(RegisterEventType.RATING_CHANGED, "4x3", "4x4"),
                new RegisterChange(RegisterEventType.STATUS_CHANGED, "UNDER_STUDY", "IN_PROGRESS"));
        assertThat(o.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void laDecisionSeTraceAussi() {
        Opportunity o = Opportunity.create(TENANT, "O-001", details(4, 3, null), null, T0);
        OpportunityDetails etudier = new OpportunityDetails(details(4, 3, null).identification(), null,
                null, null, 4, 3, OpportunityDecision.STUDY, null, "Gain mesuré > 2 h/semaine");

        assertThat(o.revise(etudier, T1)).containsExactly(
                new RegisterChange(RegisterEventType.DECISION_CHANGED, "UNDECIDED", "STUDY"));
        assertThat(o.getBenefitCriterion()).isEqualTo("Gain mesuré > 2 h/semaine");
    }

    @Test
    void sansIdentificationLIntituleEstRéclamé() {
        assertThatThrownBy(() -> Opportunity.create(TENANT, "O-001", null, null, T0))
                .extracting("field").isEqualTo("title");
    }

    @Test
    void uneActionSOuvreAFaireEtSeRevise() {
        OpportunityAction a = OpportunityAction.open(TENANT, OPP, 3, "  Chiffrer le raccordement ",
                LocalDate.of(2026, 11, 30), null, T0);
        assertThat(a.getTitle()).isEqualTo("Chiffrer le raccordement");
        assertThat(a.getStatus()).isEqualTo(OpportunityActionStatus.TO_START);
        assertThat(a.getNumber()).isEqualTo(3);

        a.revise("Chiffrer le raccordement", null, OpportunityActionStatus.IN_PROGRESS, T1);
        assertThat(a.getStatus()).isEqualTo(OpportunityActionStatus.IN_PROGRESS);
        assertThat(a.getDueDate()).isNull();
        assertThat(a.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void uneActionSansIntituleEstRefusee() {
        assertThatThrownBy(() -> OpportunityAction.open(TENANT, OPP, 1, " ", null, null, T0))
                .extracting("field").isEqualTo("title");
        assertThatThrownBy(() -> OpportunityAction.open(TENANT, OPP, 1,
                "x".repeat(OpportunityAction.TITLE_MAX + 1), null, null, T0))
                .extracting("field").isEqualTo("title");
    }

    @Test
    void leDetailDUnEvenementEstTronque() {
        RegisterEvent e = RegisterEvent.of(TENANT, RegisterItemKind.RISK, OPP, RegisterEventType.ACTION_OPENED,
                null, null, "y".repeat(400), null, T0);
        assertThat(e.detail()).hasSize(RegisterEvent.DETAIL_MAX);
        assertThat(RegisterEvent.of(TENANT, RegisterItemKind.RISK, OPP, RegisterEventType.CREATED,
                null, "DIRECT", null, null, T0).detail()).isNull();
    }
}

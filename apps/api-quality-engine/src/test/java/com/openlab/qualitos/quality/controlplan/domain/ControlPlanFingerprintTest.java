package com.openlab.qualitos.quality.controlplan.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'empreinte d'un control plan.
 *
 * <p>Ce banc protège une promesse simple à énoncer et facile à casser : deux
 * documents de même contenu ont la même empreinte, deux documents de contenus
 * différents en ont deux. Tout le reste — ordre de lecture de la base, précision
 * rendue par le pilote, identifiants techniques attribués à la recopie — ne doit
 * rien y changer, sans quoi la preuve accuserait des documents intacts.
 */
class ControlPlanFingerprintTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID PLAN = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    @Test
    void twoIdenticalPlansShareTheSameFingerprint() {
        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(line(10, "Diamètre"))))
                .isEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(line(10, "Diamètre"))));
    }

    @Test
    void theOrderInWhichTheDatabaseReturnsTheLinesChangesNothing() {
        ControlPlanLine first = line(10, "Diamètre");
        ControlPlanLine second = line(20, "Rugosité");

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(first, second)))
                .isEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(second, first)));
    }

    @Test
    void twoLinesSharingARankAreStillOrderedTheSameWay() {
        ControlPlanLine a = line(10, "Alésage");
        ControlPlanLine b = line(10, "Rugosité");

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(a, b)))
                .isEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(b, a)));
    }

    @Test
    void theTechnicalIdentifiersOfTheLinesDoNotEnterTheFingerprint() {
        ControlPlanLine one = line(10, "Diamètre");
        ControlPlanLine other = line(10, "Diamètre");
        other.assignId(UUID.randomUUID());

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(one)))
                .isEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(other)));
    }

    @Test
    void aToleranceWrittenWithMoreZeroesIsTheSameTolerance() {
        ControlPlanLine plain = line(10, "Diamètre");
        plain.describe(new ControlPlanLine.Details(null, null, null, null, null, null,
                new BigDecimal("10.0"), null, "mm", null, null, null, null, null,
                null, null, null, null));
        ControlPlanLine padded = line(10, "Diamètre");
        padded.describe(new ControlPlanLine.Details(null, null, null, null, null, null,
                new BigDecimal("10.000"), null, "mm", null, null, null, null, null,
                null, null, null, null));

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(plain)))
                .isEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(padded)));
    }

    @Test
    void changingAControlChangesTheFingerprint() {
        ControlPlanLine before = line(10, "Diamètre");
        ControlPlanLine after = line(10, "Diamètre");
        after.describe(new ControlPlanLine.Details(null, null, null, null, null, "Ø 20 ±0,1",
                null, null, "mm", null, null, null, null, null, null, null, null, null));

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(before)))
                .isNotEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(after)));
    }

    @Test
    void removingALineChangesTheFingerprint() {
        List<ControlPlanLine> two = List.of(line(10, "Diamètre"), line(20, "Rugosité"));

        assertThat(ControlPlanFingerprint.of(approvedPlan(), two))
                .isNotEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(line(10, "Diamètre"))));
    }

    @Test
    void attachingAJustificationToAControlChangesTheDocument() {
        ControlPlanLine unjustified = line(10, "Diamètre");
        ControlPlanLine justified = line(10, "Diamètre");
        justified.justifiedBy(UUID.randomUUID());

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(unjustified)))
                .isNotEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(justified)));
    }

    @Test
    void theRevisionIsPartOfTheIdentityOfTheDocument() {
        ControlPlan revised = ControlPlan.rehydrate(PLAN, TENANT, PRODUCT,
                ControlPlanPhase.PRODUCTION, "CP-4471", 2, ControlPlanStatus.ACTIVE,
                null, USER, NOW, USER, NOW, NOW, null, null, null, 0);

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(line(10, "Diamètre"))))
                .isNotEqualTo(ControlPlanFingerprint.of(revised, List.of(line(10, "Diamètre"))));
    }

    @Test
    void whoApprovedIsPartOfTheProof() {
        ControlPlan bySomeoneElse = ControlPlan.rehydrate(PLAN, TENANT, PRODUCT,
                ControlPlanPhase.PRODUCTION, "CP-4471", 1, ControlPlanStatus.ACTIVE,
                null, UUID.randomUUID(), NOW, USER, NOW, NOW, null, null, null, 0);

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(line(10, "Diamètre"))))
                .isNotEqualTo(ControlPlanFingerprint.of(bySomeoneElse, List.of(line(10, "Diamètre"))));
    }

    @Test
    void aPlanWithoutAnyLineStillHasAFingerprint() {
        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of()))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    /**
     * Le texte signé ne doit pas laisser déplacer du contenu d'un champ à
     * l'autre : c'est ce que garantit le séparateur de contrôle, et c'est ce que
     * ce test vérifie sur le seul cas qui compte — deux documents différents dont
     * la concaténation naïve serait identique.
     */
    @Test
    void textMovedFromOneFieldToTheNextDoesNotProduceTheSameFingerprint() {
        // `machine` et `characteristicNo` se suivent dans le texte canonique :
        // c'est exactement la paire qu'un séparateur faible laisserait confondre.
        ControlPlanLine split = line(10, "Diamètre");
        split.describe(new ControlPlanLine.Details(null, "Tour", "CN 3", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null));
        ControlPlanLine joined = line(10, "Diamètre");
        joined.describe(new ControlPlanLine.Details(null, "TourCN 3", "", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(split)))
                .isNotEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(joined)));
    }

    @Test
    void theCanonicalTextNamesTheDocumentItDescribes() {
        String canonical = ControlPlanFingerprint.canonical(approvedPlan(), List.of(line(10, "Diamètre")));

        assertThat(canonical).startsWith("control-plan");
        assertThat(canonical).contains("CP-4471");
        assertThat(canonical).contains("Diamètre");
    }

    // ---------- le calcul versionné ----------

    /**
     * Cinq colonnes de la trame échappaient à l'empreinte : la référence de
     * procédure, entrée/sortie, qui mesure, le lieu d'enregistrement et la
     * caractéristique spécifiée. On pouvait donc déplacer la preuve d'un
     * contrôle sans que le document scellé n'en dise rien.
     */
    @Test
    void movingTheRecordingLocationNowChangesTheFingerprint() {
        ControlPlanLine before = line(10, "Diamètre");
        ControlPlanLine after = line(10, "Diamètre");
        after.describe(details("Journal atelier"));

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(before)))
                .isNotEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(after)));
    }

    @Test
    void theSpecifiedCharacteristicIsPartOfTheDocument() {
        ControlPlanLine before = line(10, "Diamètre");
        ControlPlanLine after = line(10, "Diamètre");
        after.describe(new ControlPlanLine.Details(null, null, null, "Cote de coupe", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(before)))
                .isNotEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(after)));
    }

    /**
     * Le point qui compte pour un auditeur : un plan scellé l'an dernier reste
     * vérifiable. Le calcul d'origine doit continuer de rendre EXACTEMENT ce
     * qu'il rendait, sans quoi un document intact serait déclaré falsifié.
     */
    @Test
    void theOriginalComputationKeepsIgnoringWhatItIgnored() {
        ControlPlanLine plain = line(10, "Diamètre");
        ControlPlanLine annotated = line(10, "Diamètre");
        annotated.describe(details("Journal atelier"));

        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(plain),
                ControlPlanFingerprint.VERSION_1))
                .isEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(annotated),
                        ControlPlanFingerprint.VERSION_1));
    }

    @Test
    void theTwoVersionsNeverProduceTheSameText() {
        List<ControlPlanLine> lines = List.of(line(10, "Diamètre"));

        String v1 = ControlPlanFingerprint.canonical(approvedPlan(), lines,
                ControlPlanFingerprint.VERSION_1);
        String v2 = ControlPlanFingerprint.canonical(approvedPlan(), lines,
                ControlPlanFingerprint.VERSION_2);

        // Le préfixe suffit à les séparer : sans lui, deux calculs pourraient
        // converger sur une ligne dont toutes les colonnes tardives sont vides,
        // et le versionnement ne protégerait plus de rien.
        assertThat(v1).startsWith("control-plan");
        assertThat(v2).startsWith("control-plan/2");
        assertThat(v1).isNotEqualTo(v2);
    }

    @Test
    void aNewSealUsesTheCompleteComputation() {
        assertThat(ControlPlanFingerprint.CURRENT_VERSION)
                .isEqualTo(ControlPlanFingerprint.VERSION_2);
        assertThat(ControlPlanFingerprint.of(approvedPlan(), List.of(line(10, "Diamètre"))))
                .isEqualTo(ControlPlanFingerprint.of(approvedPlan(), List.of(line(10, "Diamètre")),
                        ControlPlanFingerprint.VERSION_2));
    }

    @Test
    void anUnknownVersionIsRefusedRatherThanSilentlyApproximated() {
        assertThatThrownBy(() -> ControlPlanFingerprint.of(approvedPlan(),
                List.of(line(10, "Diamètre")), 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    /** Une ligne dont seule la colonne « où se trouve la preuve » varie. */
    private ControlPlanLine.Details details(String recordingLocation) {
        return new ControlPlanLine.Details(null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, recordingLocation);
    }

    // ---------- montage ----------

    private ControlPlan approvedPlan() {
        return ControlPlan.rehydrate(PLAN, TENANT, PRODUCT, ControlPlanPhase.PRODUCTION,
                "CP-4471", 1, ControlPlanStatus.ACTIVE, null, USER, NOW, USER, NOW, NOW,
                null, null, null, 0);
    }

    private ControlPlanLine line(int rank, String characteristic) {
        return ControlPlanLine.create(TENANT, PLAN, rank, characteristic, CharacteristicType.PRODUCT);
    }
}

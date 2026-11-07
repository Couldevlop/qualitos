package com.openlab.qualitos.quality.capa.effectiveness.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Une évaluation d'efficacité : un jugement, son critère, son relevé, son
 * auteur, sa date (ADR 0108).
 *
 * <p>Ce banc protège deux choses. D'abord les règles d'écriture : un critère et
 * un relevé obligatoires, parce qu'un verdict sans mesure n'est pas un jugement
 * mais une opinion, et qu'un critère absent se choisit toujours après coup, en
 * regardant le résultat. Ensuite la différence entre {@code record} — qui les
 * applique — et {@code rehydrate} — qui ne les applique pas : une ligne écrite
 * sous d'anciennes règles doit continuer d'être LUE, sans quoi durcir une règle
 * ferait disparaître de l'historique les évaluations qu'elle condamne.
 */
class EffectivenessReviewTest {

    static final UUID ID = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID CASE = UUID.randomUUID();
    static final UUID REVIEWER = UUID.randomUUID();
    static final Instant AT = Instant.parse("2026-11-07T10:15:00Z");

    @Test
    void itRecordsAFullEvaluation() {
        EffectivenessReview r = EffectivenessReview.record(
                ID, TENANT, CASE, EffectivenessVerdict.EFFECTIVE,
                "Moins de 2 rebuts pour 1000 pièces sur 3 mois",
                "7,4 ‰", "< 2 ‰", "1,1 ‰", "Mesuré sur les lots 2411 à 2418",
                REVIEWER, "A. Diallo", AT);

        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.tenantId()).isEqualTo(TENANT);
        assertThat(r.capaCaseId()).isEqualTo(CASE);
        assertThat(r.verdict()).isEqualTo(EffectivenessVerdict.EFFECTIVE);
        assertThat(r.criterion()).isEqualTo("Moins de 2 rebuts pour 1000 pièces sur 3 mois");
        assertThat(r.baseline()).isEqualTo("7,4 ‰");
        assertThat(r.target()).isEqualTo("< 2 ‰");
        assertThat(r.observed()).isEqualTo("1,1 ‰");
        assertThat(r.comment()).isEqualTo("Mesuré sur les lots 2411 à 2418");
        assertThat(r.reviewerId()).isEqualTo(REVIEWER);
        assertThat(r.reviewerName()).isEqualTo("A. Diallo");
        assertThat(r.reviewedAt()).isEqualTo(AT);
    }

    @Test
    void theCriterionIsRequiredAndSaysWhereToPutIt() {
        // Le message nomme le geste à faire — poser le critère SUR LE DOSSIER —
        // plutôt que de signaler un champ vide. L'utilisateur n'a pas à deviner
        // qu'il doit remonter d'un écran.
        assertThatThrownBy(() -> record(null, "1,1 ‰"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("critère")
                .hasMessageContaining("avant");
        assertThatThrownBy(() -> record("   ", "1,1 ‰"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("critère");
    }

    @Test
    void theObservedReadingIsRequired() {
        assertThatThrownBy(() -> record("critère", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relevé");
        assertThatThrownBy(() -> record("critère", "\t "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relevé");
    }

    @Test
    void textIsTrimmedRatherThanStoredWithItsWhitespace() {
        EffectivenessReview r = record("  critère  ", "  1,1 ‰  ");
        assertThat(r.criterion()).isEqualTo("critère");
        assertThat(r.observed()).isEqualTo("1,1 ‰");
    }

    @Test
    void anEmptyOptionalFieldBecomesAbsentRatherThanBlank() {
        // Une chaîne d'espaces afficherait une colonne vide SANS le repère « — »
        // qui dit qu'elle l'est : l'écran aurait deux cas à distinguer pour un
        // seul fait, et finirait par en traiter un seul.
        EffectivenessReview r = EffectivenessReview.record(
                ID, TENANT, CASE, EffectivenessVerdict.PARTIAL, "critère",
                "  ", "", "1,1 ‰", "   ", null, "  ", AT);

        assertThat(r.baseline()).isNull();
        assertThat(r.target()).isNull();
        assertThat(r.comment()).isNull();
        assertThat(r.reviewerName()).isNull();
        assertThat(r.reviewerId()).isNull();
    }

    @Test
    void textTooLongIsRefusedRatherThanSilentlyTruncated() {
        // Tronquer serait pire que refuser : l'utilisateur croirait avoir
        // enregistré ce qu'il a écrit, et le critère conservé ne serait pas
        // celui contre lequel il pense avoir jugé.
        assertThatThrownBy(() -> record("c".repeat(EffectivenessReview.MAX_CRITERION + 1), "1 ‰"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trop long");
        assertThatThrownBy(() -> record("critère", "o".repeat(EffectivenessReview.MAX_READING + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trop long");
        assertThatThrownBy(() -> EffectivenessReview.record(
                ID, TENANT, CASE, EffectivenessVerdict.EFFECTIVE, "critère",
                "b".repeat(EffectivenessReview.MAX_READING + 1), null, "1 ‰", null, null, null, AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseline");
        assertThatThrownBy(() -> EffectivenessReview.record(
                ID, TENANT, CASE, EffectivenessVerdict.EFFECTIVE, "critère",
                null, "t".repeat(EffectivenessReview.MAX_READING + 1), "1 ‰", null, null, null, AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target");
        assertThatThrownBy(() -> EffectivenessReview.record(
                ID, TENANT, CASE, EffectivenessVerdict.EFFECTIVE, "critère",
                null, null, "1 ‰", "x".repeat(EffectivenessReview.MAX_COMMENT + 1), null, null, AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comment");
        assertThatThrownBy(() -> EffectivenessReview.record(
                ID, TENANT, CASE, EffectivenessVerdict.EFFECTIVE, "critère",
                null, null, "1 ‰", null, REVIEWER,
                "n".repeat(EffectivenessReview.MAX_REVIEWER_NAME + 1), AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reviewerName");
    }

    @Test
    void theIdentityFieldsAreRequiredAndNamed() {
        assertThatThrownBy(() -> EffectivenessReview.record(
                null, TENANT, CASE, EffectivenessVerdict.EFFECTIVE, "c", null, null, "o", null,
                null, null, AT)).isInstanceOf(NullPointerException.class).hasMessage("id");
        assertThatThrownBy(() -> EffectivenessReview.record(
                ID, null, CASE, EffectivenessVerdict.EFFECTIVE, "c", null, null, "o", null,
                null, null, AT)).isInstanceOf(NullPointerException.class).hasMessage("tenantId");
        assertThatThrownBy(() -> EffectivenessReview.record(
                ID, TENANT, null, EffectivenessVerdict.EFFECTIVE, "c", null, null, "o", null,
                null, null, AT)).isInstanceOf(NullPointerException.class).hasMessage("capaCaseId");
        assertThatThrownBy(() -> EffectivenessReview.record(
                ID, TENANT, CASE, null, "c", null, null, "o", null,
                null, null, AT)).isInstanceOf(NullPointerException.class).hasMessage("verdict");
        assertThatThrownBy(() -> EffectivenessReview.record(
                ID, TENANT, CASE, EffectivenessVerdict.EFFECTIVE, "c", null, null, "o", null,
                null, null, null)).isInstanceOf(NullPointerException.class).hasMessage("reviewedAt");
    }

    @Test
    void rehydrateDeliberatelySkipsTheWritingRules() {
        // Une ligne écrite avant que le critère ne devienne obligatoire doit
        // rester LISIBLE. Si la relecture appliquait les règles d'écriture,
        // durcir une règle effacerait de l'historique les évaluations qu'elle
        // condamne — exactement ce qu'un auditeur a besoin de voir.
        EffectivenessReview r = EffectivenessReview.rehydrate(
                ID, TENANT, CASE, EffectivenessVerdict.INEFFECTIVE,
                null, null, null, null, null, null, null, AT);

        assertThat(r.criterion()).isNull();
        assertThat(r.observed()).isNull();
        assertThat(r.verdict()).isEqualTo(EffectivenessVerdict.INEFFECTIVE);
        assertThat(r.reviewedAt()).isEqualTo(AT);
    }

    private static EffectivenessReview record(String criterion, String observed) {
        return EffectivenessReview.record(
                ID, TENANT, CASE, EffectivenessVerdict.EFFECTIVE, criterion,
                null, null, observed, null, REVIEWER, "A. Diallo", AT);
    }
}

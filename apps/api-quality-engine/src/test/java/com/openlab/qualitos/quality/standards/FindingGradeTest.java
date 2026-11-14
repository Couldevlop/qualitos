package com.openlab.qualitos.quality.standards;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le classement d’un écart, tenu sur ses définitions.
 *
 * <p>Ce banc ne vérifie pas un calcul : il garde une frontière normative. La
 * plateforme classait « majeur » ce qui portait un risque étiqueté HIGH ou
 * CRITICAL sur la clause, une échelle qu’aucune norme ne publie, et deux
 * services en gardaient chacun leur copie. Un client lisait alors « majeure »
 * sur une clause dont il détenait la moitié des preuves, et préparait une
 * réponse à un reproche qu’un auditeur n’allait pas lui faire.
 */
class FindingGradeTest {

    @Test
    @DisplayName("une clause sans aucune preuve : la disposition est absente, majeure (17021-1, 3.12)")
    void aucunePreuveDansLaClause_majeure() {
        assertThat(FindingGrade.of(ObligationLevel.MUST, false)).isEqualTo(FindingGrade.MAJOR);
    }

    @Test
    @DisplayName("une clause démontrée en partie : manquement isolé, mineure (17021-1, 3.13)")
    void preuveAilleursDansLaClause_mineure() {
        assertThat(FindingGrade.of(ObligationLevel.MUST, true)).isEqualTo(FindingGrade.MINOR);
    }

    @Test
    @DisplayName("ce que la norme recommande ne peut pas être manqué : observation")
    void recommandation_jamaisUneNonConformite() {
        assertThat(FindingGrade.of(ObligationLevel.SHOULD, false))
                .isEqualTo(FindingGrade.OBSERVATION);
        assertThat(FindingGrade.of(ObligationLevel.SHOULD, true))
                .isEqualTo(FindingGrade.OBSERVATION);
        assertThat(FindingGrade.of(ObligationLevel.MAY, false))
                .isEqualTo(FindingGrade.OBSERVATION);
        assertThat(FindingGrade.of(ObligationLevel.MAY, true))
                .isEqualTo(FindingGrade.OBSERVATION);
    }

    @Test
    @DisplayName("les majeures se traitent d’abord : elles bloquent le certificat")
    void ordreDeTraitement() {
        assertThat(FindingGrade.MAJOR.remediationPriority())
                .isLessThan(FindingGrade.MINOR.remediationPriority());
        assertThat(FindingGrade.MINOR.remediationPriority())
                .isLessThan(FindingGrade.OBSERVATION.remediationPriority());
    }

    @Test
    @DisplayName("les trois degrés, et pas un quatrième : « critique » n’existe pas en certification")
    void troisDegresSeulement() {
        assertThat(FindingGrade.values())
                .containsExactly(FindingGrade.MAJOR, FindingGrade.MINOR, FindingGrade.OBSERVATION);
        assertThat(FindingGrade.values()).extracting(Enum::name)
                .doesNotContain("CRITICAL", "HIGH", "LOW");
    }
}

package com.openlab.qualitos.quality.apqp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le contenu d'un livrable n'est pas un fourre-tout.
 *
 * <p>Une seule colonne {@code jsonb} porte deux formes — des sous-points, ou des
 * mesures. Ce qui l'empêche de devenir un dépotoir n'est pas son type mais ce
 * validateur : c'est donc ici que se joue la garantie, et c'est ici qu'on
 * l'éprouve.
 */
class ApqpDeliverableDataValidatorTest {

    private final ApqpDeliverableDataValidator validator = new ApqpDeliverableDataValidator();

    @Test
    @DisplayName("une checklist garde ses points et leur état")
    void checklist_gardePointsEtEtat() {
        String json = validator.valider(ApqpDeliverableKind.CHECKLIST, List.of(
                new ApqpDto.DataRow("safety", null, null, null, true),
                new ApqpDto.DataRow("cost", null, null, null, false)));

        assertThat(json).isEqualTo("[{\"label\":\"safety\",\"checked\":true},"
                                  + "{\"label\":\"cost\",\"checked\":false}]");
    }

    @Test
    @DisplayName("une checklist sans état coché vaut non coché, pas « indéterminé »")
    void checklist_etatAbsentVautNonCoche() {
        String json = validator.valider(ApqpDeliverableKind.CHECKLIST,
                List.of(new ApqpDto.DataRow("packaging", null, null, null, null)));

        assertThat(json).isEqualTo("[{\"label\":\"packaging\",\"checked\":false}]");
    }

    @Test
    @DisplayName("une saisie de mesures garde valeur, unité et date")
    void mesures_gardentValeurUniteEtDate() {
        String json = validator.valider(ApqpDeliverableKind.DATA_ENTRY, List.of(
                new ApqpDto.DataRow("Cpk", "1.42", "", LocalDate.parse("2026-09-12"), null)));

        assertThat(json).isEqualTo("[{\"label\":\"Cpk\",\"value\":\"1.42\",\"unit\":\"\","
                                  + "\"measuredAt\":\"2026-09-12\"}]");
    }

    @Test
    @DisplayName("une mesure sans date reste une mesure : la date est facultative")
    void mesures_dateFacultative() {
        String json = validator.valider(ApqpDeliverableKind.DATA_ENTRY,
                List.of(new ApqpDto.DataRow("PPM", "120", "ppm", null, null)));

        assertThat(json).contains("\"measuredAt\":null");
    }

    @Test
    @DisplayName("un genre qui ne porte pas de contenu refuse d'en recevoir")
    void genreSansContenu_refuse() {
        List<ApqpDto.DataRow> lignes = List.of(new ApqpDto.DataRow("x", "1", null, null, null));

        // Accepter silencieusement laisserait une saisie qu'aucun formulaire ne
        // sait rendre, donc invisible jusqu'au jour où l'on change le genre.
        assertThatThrownBy(() -> validator.valider(ApqpDeliverableKind.ATTACHMENT, lignes))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("ATTACHMENT");

        assertThatThrownBy(() -> validator.valider(ApqpDeliverableKind.MODULE_LINK, lignes))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("MODULE_LINK");
    }

    @Test
    @DisplayName("un libellé vide est refusé : une mesure anonyme n'apprend rien")
    void libelleVide_refuse() {
        assertThatThrownBy(() -> validator.valider(ApqpDeliverableKind.CHECKLIST,
                List.of(new ApqpDto.DataRow("  ", null, null, null, true))))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("label");
    }

    @Test
    @DisplayName("les plafonds de longueur et de nombre sont tenus")
    void plafonds_tenus() {
        assertThatThrownBy(() -> validator.valider(ApqpDeliverableKind.DATA_ENTRY,
                List.of(new ApqpDto.DataRow("Cp", "x".repeat(61), null, null, null))))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("value");

        assertThatThrownBy(() -> validator.valider(ApqpDeliverableKind.DATA_ENTRY,
                List.of(new ApqpDto.DataRow("Cp", "1", "u".repeat(21), null, null))))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("unit");

        assertThatThrownBy(() -> validator.valider(ApqpDeliverableKind.DATA_ENTRY,
                List.of(new ApqpDto.DataRow("L".repeat(121), "1", null, null, null))))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("Label");

        List<ApqpDto.DataRow> treize = IntStream.range(0, 13)
                .mapToObj(i -> new ApqpDto.DataRow("m" + i, "1", null, null, null))
                .toList();
        assertThatThrownBy(() -> validator.valider(ApqpDeliverableKind.DATA_ENTRY, treize))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("12");

        List<ApqpDto.DataRow> trenteEtUn = IntStream.range(0, 31)
                .mapToObj(i -> new ApqpDto.DataRow("p" + i, null, null, null, true))
                .toList();
        assertThatThrownBy(() -> validator.valider(ApqpDeliverableKind.CHECKLIST, trenteEtUn))
                .isInstanceOf(ApqpDeliverableValidationException.class)
                .hasMessageContaining("30");
    }

    @Test
    @DisplayName("ce qui casserait le JSON est échappé, et les caractères de contrôle retirés")
    void echappement() {
        assertThat(validator.valider(ApqpDeliverableKind.CHECKLIST,
                List.of(new ApqpDto.DataRow("dit \"oui\"", null, null, null, true))))
                .isEqualTo("[{\"label\":\"dit \\\"oui\\\"\",\"checked\":true}]");

        assertThat(validator.valider(ApqpDeliverableKind.CHECKLIST,
                List.of(new ApqpDto.DataRow("chemin C:\\temp", null, null, null, false))))
                .isEqualTo("[{\"label\":\"chemin C:\\\\temp\",\"checked\":false}]");

        // Un saut de ligne dans l'intitulé d'un point ne veut rien dire et casserait
        // la ligne : on le retire plutôt que de l'encoder.
        assertThat(validator.valider(ApqpDeliverableKind.CHECKLIST,
                List.of(new ApqpDto.DataRow("deux\nlignes", null, null, null, false))))
                .isEqualTo("[{\"label\":\"deuxlignes\",\"checked\":false}]");
    }

    @Test
    @DisplayName("une liste absente ou vide ne stocke rien")
    void listeVide_neStockeRien() {
        // `null` plutôt qu'un tableau vide : l'écran distingue « pas de sous-points »
        // de « des sous-points tous décochés ».
        assertThat(validator.valider(ApqpDeliverableKind.CHECKLIST, null)).isNull();
        assertThat(validator.valider(ApqpDeliverableKind.CHECKLIST, List.of())).isNull();
        assertThat(validator.valider(ApqpDeliverableKind.ATTACHMENT, null)).isNull();
    }
}

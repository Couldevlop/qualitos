package com.openlab.qualitos.quality.nonconformity.eightd.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** L'instantané figé : huit sections, ni sept ni neuf, et un libellé de source partout. */
class EightDSnapshotTest {

    @Test
    void un_instantane_porte_exactement_huit_sections() {
        List<EightDSnapshot.Section> sept = new ArrayList<>(sections(true)).subList(0, 7);

        assertThatThrownBy(() -> new EightDSnapshot("NC-1", "Titre", null, null, null, false, sept))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 sections");
    }

    @Test
    void une_section_sans_libelle_de_source_est_refusee() {
        // Le libellé est obligatoire dans les DEUX cas : il dit d'où vient le contenu,
        // ou ce qui manque. Une section muette laisse croire qu'il n'y avait rien à dire.
        assertThatThrownBy(() -> new EightDSnapshot.Section("D1", "Équipe", false, "  ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceLabel");
    }

    @Test
    void les_codes_manquants_sont_ceux_des_sections_sans_contenu() {
        EightDSnapshot snapshot =
                new EightDSnapshot("NC-1", "Titre", "t", null, null, true, sections(false));

        assertThat(snapshot.missingCodes()).containsExactly("D1", "D3", "D8");
    }

    @Test
    void un_instantane_complet_n_a_aucun_code_manquant() {
        EightDSnapshot snapshot =
                new EightDSnapshot("NC-1", "Titre", "t", null, null, false, sections(true));

        assertThat(snapshot.missingCodes()).isEmpty();
        assertThat(snapshot.sections()).hasSize(8);
    }


    /**
     * UNE LIGNE PLUS COURTE QUE SES COLONNES EST REFUSEE.
     *
     * <p>Rendue telle quelle, elle decalerait silencieusement les valeurs d'une
     * colonne : le porteur passerait sous « echeance », l'avancement sous
     * « statut de preuve ». Le document serait signe, ancre, et faux -- et rien
     * dans sa chaine de confiance ne le dirait, puisque l'empreinte couvrirait
     * fidelement une donnee deja fausse.
     */
    @Test
    void un_tableau_refuse_une_ligne_qui_n_a_pas_le_compte_de_cellules() {
        List<String> colonnes = List.of("Kind", "Action", "Owner");
        List<EightDSnapshot.Row> rangs =
                List.of(new EightDSnapshot.Row(List.of("CORRECTIVE", "Remplacer le joint")));

        assertThatThrownBy(() -> new EightDSnapshot.Table("Dossier A", colonnes, rangs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 cellules pour 3 colonnes");
    }

    /** Un tableau sans colonne ne se rend pas : il n'y a rien a aligner. */
    @Test
    void un_tableau_sans_colonne_est_refuse() {
        assertThatThrownBy(() -> new EightDSnapshot.Table("Dossier A", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Une section relue d'un rapport emis AVANT les tableaux porte {@code null}
     * et se relit comme une section sans tableau -- son empreinte continue de
     * tomber juste.
     */
    @Test
    void une_section_relue_sans_tableau_se_lit_comme_une_section_vide_de_tableau() {
        EightDSnapshot.Section section = new EightDSnapshot.Section(
                "D5", "Actions", true, "source", List.of("x"), null, null);

        assertThat(section.tables()).isEmpty();
        assertThat(section.figures()).isEmpty();
    }

    private List<EightDSnapshot.Section> sections(boolean complet) {
        List<EightDSnapshot.Section> sections = new ArrayList<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            boolean servi = complet || !discipline.estSaisie();
            sections.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), servi, "source", List.of("x")));
        }
        return sections;
    }
}

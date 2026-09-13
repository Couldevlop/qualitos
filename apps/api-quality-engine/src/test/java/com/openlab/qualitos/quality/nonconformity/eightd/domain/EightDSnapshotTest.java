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

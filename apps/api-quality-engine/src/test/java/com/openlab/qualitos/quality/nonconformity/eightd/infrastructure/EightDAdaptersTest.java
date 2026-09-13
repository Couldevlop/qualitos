package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDDiscipline;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Le codec de l'instantané et l'URL publique du QR code. */
class EightDAdaptersTest {

    private final EightDAdapters.JacksonSnapshotCodec codec = new EightDAdapters.JacksonSnapshotCodec();

    @Test
    void l_instantane_traverse_le_json_sans_rien_perdre() {
        EightDSnapshot origine = snapshot();

        EightDSnapshot relu = codec.decode(codec.encode(origine));

        assertThat(relu).isEqualTo(origine);
        assertThat(relu.sections()).hasSize(8);
        assertThat(relu.missingCodes()).containsExactly("D1", "D3", "D8");
        assertThat(relu.issuedAtText()).isEqualTo("13/09/2026 10:00 UTC");
    }

    @Test
    void les_accents_survivent_a_l_aller_retour() {
        // Le PDF dégrade les accents faute de police, mais l'instantané STOCKÉ doit
        // les garder : c'est lui que l'écran affiche.
        EightDSnapshot relu = codec.decode(codec.encode(snapshot()));

        assertThat(relu.ncTitle()).isEqualTo("Fuite au presse-étoupe");
        assertThat(relu.sections().get(3).sourceLabel()).contains("Agrégé");
    }

    @Test
    void un_json_illisible_est_signale_et_non_avale() {
        assertThatThrownBy(() -> codec.decode("{ceci n'est pas du json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("illisible");
    }

    @Test
    void l_url_publique_porte_le_code_et_la_base_configuree() {
        EightDAdapters.ConfigurableVerifyUrlBuilder builder =
                new EightDAdapters.ConfigurableVerifyUrlBuilder("https://qualitos.example/");

        // La barre finale de la base est absorbée : sinon l'URL porterait « //api ».
        assertThat(builder.verifyUrl("ABCDEFGHIJKLMNOP"))
                .isEqualTo("https://qualitos.example/api/v1/nc/public/8d/ABCDEFGHIJKLMNOP/verify");
    }

    private EightDSnapshot snapshot() {
        List<EightDSnapshot.Section> sections = new ArrayList<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            boolean servi = !discipline.estSaisie();
            sections.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), servi,
                    servi ? "Agrégé depuis : sources du dossier" : "Aucune source — se saisit",
                    servi ? List.of("Contenu", "    sous-point") : List.of()));
        }
        return new EightDSnapshot("NC-2026-0007", "Fuite au presse-étoupe", "tenant-1",
                "13/09/2026 10:00 UTC", "Ada Lovelace", true, sections);
    }
}

package com.openlab.qualitos.quality.riskregister.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentificationTest {

    static Identification avec(String title, RegisterType type, String process, String site, String owner,
                               RegisterOrigin origin, String ref, RegisterRequirement... exigences) {
        return new Identification(title, type, process, site, owner, origin, ref,
                new HashSet<>(Arrays.asList(exigences)));
    }

    static Identification correcte() {
        return avec("Titre", RegisterType.QUALITY, "Achats", " ", "Mme Diallo", null, "  ");
    }

    @Test
    void lesChampsVidesFacultatifsDeviennentNulsEtLOrigineParDefautEstLaSaisieDirecte() {
        Identification i = correcte().validated(true);
        assertThat(i.site()).isNull();
        assertThat(i.originRef()).isNull();
        assertThat(i.origin()).isEqualTo(RegisterOrigin.DIRECT);
        assertThat(i.requirements()).isEmpty();
    }

    @Test
    void lesChampsObligatoiresSontNommes() {
        assertThatThrownBy(() -> avec(" ", RegisterType.QUALITY, "P", null, "O", null, null).validated(true))
                .extracting("field").isEqualTo("title");
        assertThatThrownBy(() -> avec("T", null, "P", null, "O", null, null).validated(true))
                .extracting("field").isEqualTo("type");
        assertThatThrownBy(() -> avec("T", RegisterType.QUALITY, "", null, "O", null, null).validated(true))
                .extracting("field").isEqualTo("process");
        assertThatThrownBy(() -> avec("T", RegisterType.QUALITY, "P", null, null, null, null).validated(true))
                .extracting("field").isEqualTo("owner");
    }

    @Test
    void unTexteTropLongEstRefuse() {
        assertThatThrownBy(() -> avec("T", RegisterType.QUALITY, "P", "s".repeat(Identification.SITE_MAX + 1),
                "O", null, null).validated(true))
                .extracting("field").isEqualTo("site");
        assertThatThrownBy(() -> avec("t".repeat(Identification.TITLE_MAX + 1), RegisterType.QUALITY, "P",
                null, "O", null, null).validated(true))
                .extracting("field").isEqualTo("title");
    }

    @Test
    void uneAmdecNeFaitPasNaitreUneOpportunite() {
        Identification depuisAmdec = avec("T", RegisterType.QUALITY, "P", null, "O", RegisterOrigin.FMEA, null);
        assertThat(depuisAmdec.validated(true).origin()).isEqualTo(RegisterOrigin.FMEA);
        assertThatThrownBy(() -> depuisAmdec.validated(false))
                .extracting("field").isEqualTo("origin");
    }

    @Test
    void chaqueRegistreNAccepteQueSesExigences() {
        Identification iatf = avec("T", RegisterType.QUALITY, "P", null, "O", null, null,
                RegisterRequirement.IATF_16949_6_1_2);
        assertThat(iatf.validated(true).requirements()).containsExactly(RegisterRequirement.IATF_16949_6_1_2);
        assertThatThrownBy(() -> iatf.validated(false)).extracting("field").isEqualTo("requirements");

        Identification amelioration = avec("T", RegisterType.QUALITY, "P", null, "O", null, null,
                RegisterRequirement.ISO_9001_10_3);
        assertThat(amelioration.validated(false).requirements()).containsExactly(RegisterRequirement.ISO_9001_10_3);
        assertThatThrownBy(() -> amelioration.validated(true)).extracting("field").isEqualTo("requirements");
    }

    @Test
    void uneExigenceNulleEstIgnoreeEtUneListeAbsenteVautVide() {
        Set<RegisterRequirement> avecNul = new HashSet<>();
        avecNul.add(null);
        avecNul.add(RegisterRequirement.ISO_14001_6_1);
        Identification i = new Identification("T", RegisterType.ENVIRONMENT, "P", null, "O", null, null, avecNul);
        assertThat(i.validated(true).requirements()).containsExactly(RegisterRequirement.ISO_14001_6_1);

        Identification sans = new Identification("T", RegisterType.ENVIRONMENT, "P", null, "O", null, null, null);
        assertThat(sans.validated(false).requirements()).isEmpty();
    }
}

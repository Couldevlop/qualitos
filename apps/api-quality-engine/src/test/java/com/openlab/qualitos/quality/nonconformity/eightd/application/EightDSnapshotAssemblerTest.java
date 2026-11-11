package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce qu'un lecteur du rapport verra.
 *
 * <p>Le point vérifié avec le plus d'insistance : une discipline sans source le DIT.
 * C'est la différence entre un 8D et un formulaire aux cases vides.
 */
class EightDSnapshotAssemblerTest {

    private final EightDSnapshotAssembler assembler = new EightDSnapshotAssembler();

    private static final Instant DETECTE = Instant.parse("2026-09-01T08:30:00Z");
    private static final Instant CLOTURE = Instant.parse("2026-09-12T16:00:00Z");

    @Test
    void d2_reprend_la_non_conformite_avec_ses_dates_deja_formatees() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        EightDSnapshot.Section d2 = section(snapshot, "D2");
        assertThat(d2.sourced()).isTrue();
        assertThat(d2.sourceLabel()).isEqualTo("Non-conformity NC-2026-0007");
        assertThat(d2.lines()).contains("Reference : NC-2026-0007");
        assertThat(d2.lines()).contains("Severity : MAJOR");
        // La date est du TEXTE dans l'instantané : c'est ce qui rend le rendu, donc
        // l'empreinte, recalculables à l'identique des années plus tard.
        assertThat(d2.lines()).contains("Detected on : 01/09/2026 08:30 UTC");
        assertThat(d2.lines()).contains("Closed on : 12/09/2026 16:00 UTC");
    }

    @Test
    void les_disciplines_sans_source_le_disent_quand_rien_n_est_saisi() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        assertThat(snapshot.partial()).isTrue();
        assertThat(snapshot.missingCodes()).contains("D1", "D3");
        for (String code : List.of("D1", "D3")) {
            EightDSnapshot.Section s = section(snapshot, code);
            assertThat(s.sourced()).isFalse();
            assertThat(s.lines()).isEmpty();
            assertThat(s.sourceLabel()).contains("filled in by hand");
        }
    }

    // ---------- D8 : la reconnaissance de l'equipe ----------

    @Test
    void d8_porte_la_reconnaissance_standard_des_que_l_ecart_est_solde() {
        // L'ecart de la fixture est clos et n'a escalade aucune CAPA : rien ne reste
        // a attendre, la reconnaissance a sa place.
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        EightDSnapshot.Section d8 = section(snapshot, "D8");
        assertThat(d8.sourced()).isTrue();
        assertThat(d8.lines()).isEqualTo(EightDSnapshotAssembler.RECONNAISSANCE_STANDARD);
        assertThat(d8.sourceLabel()).isEqualTo(
                "Standard acknowledgement — the deviation and its corrective actions are closed");
        assertThat(snapshot.missingCodes()).doesNotContain("D8");
    }

    @Test
    void d8_ajoute_la_reconnaissance_standard_apres_le_remerciement_saisi() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null,
                "Merci a Ada et Grace, qui ont tenu l'astreinte du week-end.",
                "tenant-1", null, null);

        EightDSnapshot.Section d8 = section(snapshot, "D8");
        assertThat(d8.sourced()).isTrue();
        // La saisie passe DEVANT : un remerciement nominatif vaut plus que la
        // phrase de la plateforme, qui le complete au lieu de le remplacer.
        assertThat(d8.lines().get(0)).isEqualTo("Merci a Ada et Grace, qui ont tenu l'astreinte du week-end.");
        assertThat(d8.lines()).containsAll(EightDSnapshotAssembler.RECONNAISSANCE_STANDARD);
        assertThat(d8.sourceLabel()).isEqualTo(
                "Filled in by the quality team, completed by the standard acknowledgement");
    }

    @Test
    void d8_reste_muet_quand_la_capa_escaladee_court_toujours() {
        // La prevention reste a finir : feliciter l'equipe daterait le document
        // d'un optimisme que la suite peut dementir.
        EightDSources sources = new EightDSources(nc(null), List.of(), List.of(),
                capa(null), null, List.of());

        EightDSnapshot.Section d8 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D8");

        assertThat(d8.sourced()).isFalse();
        assertThat(d8.lines()).isEmpty();
        assertThat(d8.sourceLabel()).contains("filled in by hand");
    }

    @Test
    void d8_porte_la_reconnaissance_quand_l_ecart_et_sa_capa_sont_clos() {
        EightDSources sources = new EightDSources(nc(null), List.of(), List.of(),
                capa(Instant.parse("2026-09-13T10:00:00Z")), null, List.of());

        EightDSnapshot.Section d8 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D8");

        assertThat(d8.sourced()).isTrue();
        assertThat(d8.lines()).isEqualTo(EightDSnapshotAssembler.RECONNAISSANCE_STANDARD);
    }

    @Test
    void d8_reste_muet_tant_que_l_ecart_n_est_pas_clos() {
        EightDSources sources = new EightDSources(ncOuverte(), List.of(), List.of(),
                null, null, List.of());

        EightDSnapshot.Section d8 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D8");

        assertThat(d8.sourced()).isFalse();
        assertThat(d8.sourceLabel()).contains("filled in by hand");
    }

    @Test
    void d8_garde_le_remerciement_saisi_meme_sur_un_ecart_encore_ouvert() {
        EightDSources sources = new EightDSources(ncOuverte(), List.of(), List.of(),
                null, null, List.of());

        EightDSnapshot.Section d8 = section(
                assembler.assemble(sources, null, null, "Bravo a l'equipe d'astreinte.",
                        "t", null, null), "D8");

        assertThat(d8.sourced()).isTrue();
        assertThat(d8.lines()).containsExactly("Bravo a l'equipe d'astreinte.");
        assertThat(d8.sourceLabel()).isEqualTo("Filled in by the quality team");
    }

    @Test
    void une_saisie_multiligne_devient_autant_de_lignes() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(),
                "Ada Lovelace (animatrice)\n\nGrace Hopper (production)", null, null,
                "tenant-1", null, null);

        EightDSnapshot.Section d1 = section(snapshot, "D1");
        assertThat(d1.sourced()).isTrue();
        assertThat(d1.lines()).containsExactly(
                "Ada Lovelace (animatrice)", "Grace Hopper (production)");
    }

    @Test
    void d4_agrege_l_ishikawa_les_cinq_pourquoi_et_la_cause_racine_de_la_nc() {
        EightDSources sources = new EightDSources(
                nc("Joint hors tolérance"),
                List.of(new EightDSources.CauseTree("Fuite au presse-étoupe", "ACTIVE", List.of(
                        new EightDSources.Cause("MATERIEL", "Joint usé", "Au-delà de 2000 h", 0.82),
                        new EightDSources.Cause("METHODE", "Pas de plan de remplacement", null, null)))),
                List.of(new EightDSources.WhysChain("Fuite constatée", "Absence de plan préventif",
                        List.of("Le joint a cédé", "Il n'a jamais été remplacé"))),
                null, null, List.of());

        EightDSnapshot.Section d4 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D4");

        assertThat(d4.sourced()).isTrue();
        assertThat(d4.sourceLabel())
                .contains("1 Ishikawa")
                .contains("1 5-whys analysis(es)")
                .contains("root cause of the non-conformity");
        assertThat(d4.lines()).anyMatch(l -> l.contains("Joint usé") && l.contains("0,82")
                || l.contains("Joint usé") && l.contains("0.82"));
        assertThat(d4.lines()).anyMatch(l -> l.startsWith("    Why 1 : Le joint a cédé"));
        assertThat(d4.lines()).anyMatch(l -> l.contains("Root cause: Absence de plan préventif"));
    }

    @Test
    void d4_sans_aucune_analyse_explique_ce_qui_manque() {
        EightDSnapshot.Section d4 = section(
                assembler.assemble(sourcesMinimales(), null, null, null, "t", null, null), "D4");

        assertThat(d4.sourced()).isFalse();
        assertThat(d4.sourceLabel())
                .contains("No Ishikawa")
                .contains("5-whys")
                .contains("root cause");
    }

    @Test
    void d5_et_d6_disent_l_absence_de_capa_plutot_que_de_rester_vides() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "t", null, null);

        assertThat(section(snapshot, "D5").sourceLabel()).contains("No CAPA");
        assertThat(section(snapshot, "D6").sourceLabel()).contains("No CAPA");
        assertThat(section(snapshot, "D5").sourced()).isFalse();
    }

    @Test
    void d5_liste_les_actions_decidees_et_d6_celles_qui_sont_menees_avec_leurs_preuves() {
        EightDSources.Action faite = new EightDSources.Action(
                "Remplacer le joint", "Référence 44-B", "CORRECTIVE", "DONE", "Grace",
                LocalDate.parse("2026-09-10"), Instant.parse("2026-09-09T09:00:00Z"), 2);
        EightDSources.Action aFaire = new EightDSources.Action(
                "Former les opérateurs", null, "PREVENTIVE", "IN_PROGRESS", null, null, null, 0);
        EightDSources sources = new EightDSources(
                nc(null), List.of(), List.of(),
                new EightDSources.Capa("Fuite presse-étoupe", "CORRECTIVE", "HIGH", "CLOSED",
                        LocalDate.parse("2026-09-30"), Instant.parse("2026-09-11T10:00:00Z"),
                        Boolean.TRUE, Instant.parse("2026-12-01T10:00:00Z"), 1,
                        List.of(faite, aFaire)),
                null, List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, null, null, null, "t", null, null);

        EightDSnapshot.Section d5 = section(snapshot, "D5");
        assertThat(d5.sourced()).isTrue();
        assertThat(d5.sourceLabel()).contains("2 action(s) decided");
        assertThat(d5.lines()).anyMatch(l -> l.contains("[PREVENTIVE] Former les opérateurs"));
        assertThat(d5.lines()).anyMatch(l -> l.contains("due 10/09/2026"));

        EightDSnapshot.Section d6 = section(snapshot, "D6");
        assertThat(d6.sourced()).isTrue();
        assertThat(d6.sourceLabel()).contains("1 action(s) completed").contains("3 evidence item(s)");
        assertThat(d6.lines()).anyMatch(l -> l.contains("Remplacer le joint")
                && l.contains("09/09/2026 09:00 UTC"));
        assertThat(d6.lines()).anyMatch(l -> l.contains("Effectiveness verified"));
        // Une action encore en cours n'a pas sa place dans D6 : elle n'est pas mise en œuvre.
        assertThat(d6.lines()).noneMatch(l -> l.contains("Former les opérateurs"));
    }

    @Test
    void d7_agrege_le_pfmea_et_les_plans_de_surveillance() {
        EightDSources sources = new EightDSources(
                nc(null), List.of(), List.of(), null,
                new EightDSources.Fmea("Fuite au joint", "Perte de pression", "Usure",
                        "Contrôle visuel hebdomadaire", 160, 40, "MEDIUM",
                        "Plan de remplacement préventif", "Plan déployé en août"),
                List.of(new EightDSources.Surveillance("CP-4471", 3, "PRODUCTION", "ACTIVE", 12,
                        "b".repeat(64))));

        EightDSnapshot.Section d7 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D7");

        assertThat(d7.sourced()).isTrue();
        assertThat(d7.sourceLabel()).contains("PFMEA failure mode")
                .contains("1 control plan(s)");
        assertThat(d7.lines()).anyMatch(l -> l.contains("RPN: 160 → 40 after actions"));
        assertThat(d7.lines()).anyMatch(l -> l.contains("CP-4471") && l.contains("rev. 3")
                && l.contains("12 line(s)") && l.contains("sealed"));
    }

    @Test
    void d7_sans_source_dit_ce_qui_manque_et_rien_de_plus() {
        EightDSnapshot.Section d7 = section(
                assembler.assemble(sourcesMinimales(), null, null, null, "t", null, null), "D7");

        assertThat(d7.sourced()).isFalse();
        assertThat(d7.sourceLabel())
                .contains("No PFMEA failure mode")
                .contains("control plan");
        // La phrase qui expliquait pourquoi le Poka-Yoke n'y figurait pas exposait
        // une limite d'outillage interne dans un document remis au client, là où
        // il attend un constat de prévention.
        assertThat(d7.sourceLabel()).doesNotContain("Poka-Yoke").doesNotContain("DMAIC");
    }

    @Test
    void un_rapport_dont_les_huit_disciplines_sont_servies_n_est_pas_partiel() {
        EightDSources sources = new EightDSources(
                nc("Cause retenue"),
                List.of(new EightDSources.CauseTree("Problème", "ACTIVE", List.of())),
                List.of(),
                new EightDSources.Capa("CAPA", "CORRECTIVE", "HIGH", "CLOSED", null, null, null, null, 1,
                        List.of(new EightDSources.Action("Action", null, "CORRECTIVE", "DONE", null,
                                null, Instant.parse("2026-09-09T09:00:00Z"), 1))),
                new EightDSources.Fmea("Mode", null, null, null, 100, null, null, null, null),
                List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, "Ada", "Tri 100 %", "Merci",
                "t", Instant.parse("2026-09-13T10:00:00Z"), "Ada Lovelace");

        assertThat(snapshot.partial()).isFalse();
        assertThat(snapshot.missingCodes()).isEmpty();
        assertThat(snapshot.issuedAtText()).isEqualTo("13/09/2026 10:00 UTC");
        assertThat(snapshot.issuedByName()).isEqualTo("Ada Lovelace");
    }

    // ---------- fixtures ----------

    private EightDSources sourcesMinimales() {
        return new EightDSources(nc(null), List.of(), List.of(), null, null, List.of());
    }

    private EightDSources.Nc nc(String causeRacine) {
        return new EightDSources.Nc("NC-2026-0007", "Fuite au presse-étoupe",
                "Flaque d'huile sous la pompe P-12", "PROCESS", "MAJOR", "INTERNAL",
                NcStatus.CLOSED, DETECTE, CLOTURE, "Atelier 3", "Ada Lovelace", 2,
                causeRacine, "Joint remplacé, étanchéité contrôlée");
    }

    /** Le meme ecart, mais encore ouvert : rien n'est solde. */
    private EightDSources.Nc ncOuverte() {
        return new EightDSources.Nc("NC-2026-0008", "Fuite au presse-etoupe",
                "Flaque d'huile sous la pompe P-12", "PROCESS", "MAJOR", "INTERNAL",
                NcStatus.UNDER_ANALYSIS, DETECTE, null, "Atelier 3", "Ada Lovelace", 0,
                null, null);
    }

    /** Une CAPA escaladee, close ou non selon la date passee. */
    private EightDSources.Capa capa(Instant cloture) {
        return new EightDSources.Capa("Fuite presse-etoupe", "CORRECTIVE", "HIGH",
                cloture == null ? "IN_PROGRESS" : "CLOSED",
                LocalDate.parse("2026-09-30"), cloture, null, null, 0, List.of());
    }

    private EightDSnapshot.Section section(EightDSnapshot snapshot, String code) {
        return snapshot.sections().stream()
                .filter(s -> s.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("section " + code + " absente"));
    }
}

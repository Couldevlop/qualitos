package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
        EightDSources sources = new EightDSources(nc(null), List.of(), List.of(), List.of(),
                List.of(capa(null)), null, List.of());

        EightDSnapshot.Section d8 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D8");

        assertThat(d8.sourced()).isFalse();
        assertThat(d8.lines()).isEmpty();
        assertThat(d8.sourceLabel()).contains("filled in by hand");
    }

    @Test
    void d8_porte_la_reconnaissance_quand_l_ecart_et_sa_capa_sont_clos() {
        EightDSources sources = new EightDSources(nc(null), List.of(), List.of(), List.of(),
                List.of(capa(Instant.parse("2026-09-13T10:00:00Z"))), null, List.of());

        EightDSnapshot.Section d8 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D8");

        assertThat(d8.sourced()).isTrue();
        assertThat(d8.lines()).isEqualTo(EightDSnapshotAssembler.RECONNAISSANCE_STANDARD);
    }

    @Test
    void d8_reste_muet_tant_que_l_ecart_n_est_pas_clos() {
        EightDSources sources = new EightDSources(ncOuverte(), List.of(), List.of(), List.of(),
                List.of(), null, List.of());

        EightDSnapshot.Section d8 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D8");

        assertThat(d8.sourced()).isFalse();
        assertThat(d8.sourceLabel()).contains("filled in by hand");
    }

    @Test
    void d8_garde_le_remerciement_saisi_meme_sur_un_ecart_encore_ouvert() {
        EightDSources sources = new EightDSources(ncOuverte(), List.of(), List.of(), List.of(),
                List.of(), null, List.of());

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
                EQUIPE,
                List.of(new EightDSources.CauseTree("Fuite au presse-étoupe", "ACTIVE", List.of(
                        new EightDSources.Cause("MATERIEL", "Joint usé", "Au-delà de 2000 h", 0.82),
                        new EightDSources.Cause("METHODE", "Pas de plan de remplacement", null, null)))),
                List.of(new EightDSources.WhysChain("Fuite constatée", "Absence de plan préventif",
                        List.of("Le joint a cédé", "Il n'a jamais été remplacé"))),
                List.of(), null, List.of());

        EightDSnapshot.Section d4 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D4");

        assertThat(d4.sourced()).isTrue();
        assertThat(d4.sourceLabel())
                .contains("1 Ishikawa")
                .contains("1 5-whys analysis(es)")
                .contains("root cause of the non-conformity");

        // LE TRACE REMPLACE L'ENUMERATION. Les causes s'ecrivaient ligne a
        // ligne (« MATERIEL : Joint use », « Why 1 : ... ») : un 8D se lit en
        // revue, et cette liste ne montrait ni les branches ni la descente.
        // Seule la cause racine PORTEE PAR LA FICHE reste en texte -- aucune
        // figure ne la dit.
        assertThat(d4.lines()).containsExactly(
                "Root cause recorded on the non-conformity : Joint hors tolérance");
        assertThat(d4.figures()).hasSize(2);

        EightDSnapshot.Figure arete = d4.figures().get(0);
        assertThat(arete.kind()).isEqualTo(EightDSnapshot.FigureKind.FISHBONE);
        assertThat(arete.caption()).isEqualTo("Fuite au presse-étoupe");
        assertThat(arete.nodes()).containsExactly(
                new EightDSnapshot.Node("MATERIEL", "Joint usé", 1),
                new EightDSnapshot.Node("METHODE", "Pas de plan de remplacement", 1));

        EightDSnapshot.Figure echelle = d4.figures().get(1);
        assertThat(echelle.kind()).isEqualTo(EightDSnapshot.FigureKind.WHY_CHAIN);
        assertThat(echelle.caption()).isEqualTo("Fuite constatée");
        // Le rang EST l'ordre de la descente ; la cause racine la ferme, sous
        // son propre groupe, pour que le trace la distingue.
        assertThat(echelle.nodes()).containsExactly(
                new EightDSnapshot.Node("WHY", "Le joint a cédé", 1),
                new EightDSnapshot.Node("WHY", "Il n'a jamais été remplacé", 2),
                new EightDSnapshot.Node("ROOT", "Absence de plan préventif", 3));
    }

    /**
     * Une descente interrompue et une descente conclue ne se lisent pas pareil :
     * sans conclusion, l'echelle n'a pas de noeud ROOT, et le trace ne doit pas
     * en inventer un.
     */
    @Test
    void d4_une_descente_sans_conclusion_ne_porte_pas_de_cause_racine() {
        EightDSources sources = new EightDSources(
                nc("Joint hors tolérance"), EQUIPE, List.of(),
                List.of(new EightDSources.WhysChain("Fuite constatée", null,
                        List.of("Le joint a cédé"))),
                List.of(), null, List.of());

        EightDSnapshot.Section d4 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D4");

        assertThat(d4.figures()).singleElement()
                .satisfies(f -> assertThat(f.nodes())
                        .containsExactly(new EightDSnapshot.Node("WHY", "Le joint a cédé", 1)));
    }

    /**
     * Un Ishikawa ouvert puis laisse vide EXISTE. La figure est alors sans
     * noeud -- et la discipline reste servie : on a cherche la cause, et le
     * rapport doit le dire plutot que d'afficher « rien a montrer ».
     */
    @Test
    void d4_un_ishikawa_vide_donne_une_figure_sans_noeud_mais_servie() {
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE,
                List.of(new EightDSources.CauseTree("Fuite au presse-étoupe", "DRAFT", List.of())),
                List.of(), List.of(), null, List.of());

        EightDSnapshot.Section d4 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D4");

        assertThat(d4.sourced()).isTrue();
        assertThat(d4.lines()).isEmpty();
        assertThat(d4.figures()).singleElement()
                .satisfies(f -> assertThat(f.nodes()).isEmpty());
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
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Fuite presse-étoupe", "CORRECTIVE", "HIGH", "CLOSED",
                        LocalDate.parse("2026-09-30"), Instant.parse("2026-09-11T10:00:00Z"),
                        Boolean.TRUE, Instant.parse("2026-12-01T10:00:00Z"), 1,
                        List.of(faite, aFaire))),
                null, List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, null, null, null, "t", null, null);

        EightDSnapshot.Section d5 = section(snapshot, "D5");
        assertThat(d5.sourced()).isTrue();
        assertThat(d5.sourceLabel()).contains("2 action(s) decided");
        // Les actions sont en COLONNES, pas en phrases : c'est ce que l'écran
        // montre, et le document émis doit se présenter comme l'écran.
        assertThat(d5.lines()).isEmpty();
        assertThat(d5.tables()).hasSize(1);
        EightDSnapshot.Table decidees = d5.tables().get(0);
        assertThat(decidees.caption()).isEqualTo("Fuite presse-étoupe");
        assertThat(decidees.columns())
                .containsExactly("Kind", "Action", "Owner", "Due date", "Status", "Evidence");
        assertThat(decidees.rows()).hasSize(2);
        assertThat(decidees.rows().get(0).cells()).containsExactly(
                "CORRECTIVE", "Remplacer le joint", "Grace", "10/09/2026", "DONE", "2");
        // Ce qu'on ne SAIT pas s'écrit « - » : une cellule vide se lirait comme
        // une valeur qu'on aurait omise.
        assertThat(decidees.rows().get(1).cells()).containsExactly(
                "PREVENTIVE", "Former les opérateurs", "-", "-", "IN_PROGRESS", "-");

        EightDSnapshot.Section d6 = section(snapshot, "D6");
        assertThat(d6.sourced()).isTrue();
        assertThat(d6.sourceLabel()).contains("1 action(s) completed").contains("3 evidence item(s)");
        assertThat(d6.lines()).anyMatch(l -> l.contains("Remplacer le joint")
                && l.contains("09/09/2026 09:00 UTC"));
        assertThat(d6.lines()).anyMatch(l -> l.contains("Effectiveness verified"));
        // Une action encore en cours n'a pas sa place dans le RÉCIT du D6 : elle
        // n'est pas mise en œuvre.
        assertThat(d6.lines()).noneMatch(l -> l.contains("Former les opérateurs"));
        // Elle a en revanche sa place dans son TABLEAU, qui donne l'état de
        // chaque action préventive à la date d'émission -- y compris celles qui
        // restent à faire. Les taire laisserait croire la prévention terminée.
        assertThat(d6.tables()).hasSize(1);
        assertThat(d6.tables().get(0).rows()).hasSize(1);
        assertThat(d6.tables().get(0).rows().get(0).cells())
                .containsExactly("PREVENTIVE", "Former les opérateurs", "-", "-",
                        "IN_PROGRESS", "-");
    }

    @Test
    void d7_agrege_le_pfmea_et_les_plans_de_surveillance() {
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(), List.of(),
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
                EQUIPE,
                List.of(new EightDSources.CauseTree("Problème", "ACTIVE", List.of())),
                List.of(),
                List.of(new EightDSources.Capa("CAPA", "CORRECTIVE", "HIGH", "CLOSED", null, null, null, null, 1,
                        List.of(new EightDSources.Action("Action", null, "CORRECTIVE", "DONE", null,
                                null, Instant.parse("2026-09-09T09:00:00Z"), 1)))),
                new EightDSources.Fmea("Mode", null, null, null, 100, null, null, null, null),
                List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, "Ada", "Tri 100 %", "Merci",
                "t", Instant.parse("2026-09-13T10:00:00Z"), "Ada Lovelace");

        assertThat(snapshot.partial()).isFalse();
        assertThat(snapshot.missingCodes()).isEmpty();
        assertThat(snapshot.issuedAtText()).isEqualTo("13/09/2026 10:00 UTC");
        assertThat(snapshot.issuedByName()).isEqualTo("Ada Lovelace");
    }

    /**
     * L'INVARIANT DE CETTE D\u00c9CISION. Un \u00e9cart peut porter PLUSIEURS dossiers,
     * et la cl\u00f4ture de l'\u00e9cart les attend TOUS (ADR 0121). Le D5 n'en lisait
     * qu'un : les actions du second retenaient donc la cl\u00f4ture sans figurer
     * dans le document remis au client, qui contredisait alors le refus.
     */
    @Test
    void d5_montre_les_actions_de_TOUS_les_dossiers_de_l_ecart() {
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(dossier("Dossier de la fiche", "Remplacer le joint"),
                        dossier("Dossier ouvert depuis la liste", "Revoir la gamme")),
                null, List.of());

        EightDSnapshot.Section d5 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D5");

        assertThat(d5.sourced()).isTrue();
        // UN TABLEAU PAR DOSSIER, chacun nommé : les actions du second dossier
        // retenaient la clôture sans figurer dans le document remis au client.
        assertThat(d5.tables()).hasSize(2);
        assertThat(d5.tables()).extracting(EightDSnapshot.Table::caption)
                .containsExactly("Dossier de la fiche", "Dossier ouvert depuis la liste");
        assertThat(d5.tables().get(0).rows().get(0).cells()).contains("Remplacer le joint");
        assertThat(d5.tables().get(1).rows().get(0).cells()).contains("Revoir la gamme");
        // Le COMPTE, pas le singulier : « Escalated CAPA » mentait d\u00e8s qu'un
        // second dossier citait la r\u00e9f\u00e9rence, et rien ne le disait au lecteur.
        assertThat(d5.sourceLabel()).contains("2 CAPA cases").contains("2 action(s) decided");
    }

    /**
     * Deux dossiers colles se lisaient comme UN SEUL : leurs champs « Type »,
     * « Status » et « Due date » s'enchainaient, et le rapport devenait faux a
     * la lecture sans qu'aucune ligne ne soit fausse.
     *
     * <p>Le tableau resout cela par construction -- un tableau par dossier,
     * portant son titre -- la ou la ligne vide demandait au lecteur de
     * remarquer un blanc. On verifie donc la SEPARATION, pas le moyen.
     */
    @Test
    void d5_ne_confond_pas_deux_dossiers() {
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(dossier("Premier", "A faire"), dossier("Second", "A faire aussi")),
                null, List.of());

        EightDSnapshot.Section d5 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D5");

        assertThat(d5.tables()).hasSize(2);
        assertThat(d5.tables()).extracting(EightDSnapshot.Table::caption)
                .containsExactly("Premier", "Second");
        // Et l'action de chacun reste chez lui.
        assertThat(d5.tables().get(0).rows().get(0).cells()).contains("A faire");
        assertThat(d5.tables().get(1).rows().get(0).cells()).contains("A faire aussi");
    }

    /**
     * Un \u00e9cart clos dont le SECOND dossier court encore n'est pas sold\u00e9 : D7 et
     * D8 le disent. Ne regarder que le dossier de la fiche faisait d\u00e9clarer
     * sold\u00e9 un \u00e9cart dont la pr\u00e9vention restait \u00e0 finir.
     */
    @Test
    void un_second_dossier_encore_ouvert_empeche_de_declarer_l_ecart_solde() {
        EightDSources.Capa clos = new EightDSources.Capa("Clos", "CORRECTIVE", "HIGH", "CLOSED",
                null, Instant.parse("2026-09-20T10:00:00Z"), Boolean.TRUE,
                Instant.parse("2026-10-01T10:00:00Z"), 0, List.of());
        EightDSources.Capa courant = dossier("Encore ouvert", "A faire");

        EightDSnapshot avecLesDeux = assembler.assemble(
                new EightDSources(nc(null), EQUIPE, List.of(), List.of(),
                        List.of(clos, courant), null, List.of()),
                null, null, null, "t", null, null);
        EightDSnapshot avecLeClosSeul = assembler.assemble(
                new EightDSources(nc(null), EQUIPE, List.of(), List.of(),
                        List.of(clos), null, List.of()),
                null, null, null, "t", null, null);

        assertThat(section(avecLesDeux, "D8").lines())
                .as("un dossier qui court interdit de dire l'\u00e9cart sold\u00e9")
                .isNotEqualTo(section(avecLeClosSeul, "D8").lines());
    }

    /** Un dossier de l'ecart, avec une action a mener. */

    /**
     * CHAQUE DISCIPLINE NE MONTRE QUE LES ACTIONS DE SA NATURE, exactement comme
     * l'ecran : le D3 endigue, le D6 previent, le D5 nomme les trois.
     *
     * <p>Melanger les natures ferait lire, sous « actions immediates
     * d'endiguement », une action preventive a echeance dans six mois -- et
     * c'est le document que l'auditeur ou le client lit pour savoir ce qui a
     * ete fait TOUT DE SUITE.
     */
    @Test
    void d3_n_affiche_que_l_endiguement_et_d6_que_la_prevention() {
        EightDSources.Action endiguer = new EightDSources.Action(
                "Isoler le lot", null, "CONTAINMENT", "DONE", "Ada", null, null, 0);
        EightDSources.Action corriger = new EightDSources.Action(
                "Remplacer le joint", null, "CORRECTIVE", "DONE", "Grace", null, null, 0);
        EightDSources.Action prevenir = new EightDSources.Action(
                "Revoir la gamme", null, "PREVENTIVE", "TODO", "Alan", null, null, 0);
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Dossier A", "CORRECTIVE", "HIGH", "OPEN",
                        null, null, null, null, 0, List.of(endiguer, corriger, prevenir))),
                null, List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, null, null, null, "t", null, null);

        assertThat(cellulesDe(snapshot, "D3")).containsExactly("Isoler le lot");
        assertThat(cellulesDe(snapshot, "D6")).containsExactly("Revoir la gamme");
        assertThat(cellulesDe(snapshot, "D5"))
                .containsExactly("Isoler le lot", "Remplacer le joint", "Revoir la gamme");
    }

    /**
     * Un dossier sans action de la nature de la discipline N'ECRIT PAS de
     * tableau vide : un en-tete de colonnes sous un titre de dossier se lit
     * comme un dossier qu'on aurait ouvert sans rien y mettre.
     */
    @Test
    void une_discipline_sans_action_de_sa_nature_n_ecrit_pas_de_tableau_vide() {
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(dossier("Dossier A", "Remplacer le joint")), null, List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, null, null, null, "t", null, null);

        // Le dossier n'a qu'une action CORRECTIVE : le D5 la montre, le D3 et le
        // D6 n'ont rien a tabuler.
        assertThat(section(snapshot, "D5").tables()).hasSize(1);
        assertThat(section(snapshot, "D3").tables()).isEmpty();
        assertThat(section(snapshot, "D6").tables()).isEmpty();
    }

    /**
     * Le libelle de source du D3 N'ATTRIBUE PAS A L'EQUIPE QUALITE un tableau
     * qu'elle n'a pas ecrit : le texte vient d'elle, les actions du dossier
     * CAPA. Les confondre ferait signer a quelqu'un une phrase qui n'est pas
     * la sienne, dans un document scelle.
     */
    @Test
    void d3_ne_dit_pas_saisi_par_l_equipe_quand_seul_le_tableau_est_servi() {
        EightDSources.Action endiguer = new EightDSources.Action(
                "Isoler le lot", null, "CONTAINMENT", "DONE", "Ada", null, null, 0);
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Dossier A", "CORRECTIVE", "HIGH", "OPEN",
                        null, null, null, null, 0, List.of(endiguer))),
                null, List.of());

        EightDSnapshot.Section d3 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D3");

        assertThat(d3.sourced()).isTrue();
        assertThat(d3.sourceLabel()).doesNotContain("quality team").contains("CAPA");
    }

    /** Les intitules d'action tabules sous une discipline, dans l'ordre. */
    private List<String> cellulesDe(EightDSnapshot snapshot, String code) {
        List<String> intitules = new ArrayList<>();
        for (EightDSnapshot.Table tableau : section(snapshot, code).tables()) {
            for (EightDSnapshot.Row rang : tableau.rows()) {
                intitules.add(rang.cells().get(1));
            }
        }
        return intitules;
    }

    private static EightDSources.Capa dossier(String titre, String action) {
        return new EightDSources.Capa(titre, "CORRECTIVE", "HIGH", "OPEN", null, null, null, null, 0,
                List.of(new EightDSources.Action(action, null, "CORRECTIVE", "PENDING",
                        "Ada Lovelace", null, null, 0)));
    }

    // ---------- fixtures ----------

    private EightDSources sourcesMinimales() {
        // Aucune equipe : c'est le cas d'un ecart ne d'un constat d'audit ou d'un
        // import systeme, ou D1 reste a saisir.
        return new EightDSources(nc(null), List.of(), List.of(), List.of(), List.of(), null, List.of());
    }

    /** Les memes sources, mais avec l'equipe nommee sur la fiche. */
    private EightDSources sourcesAvecEquipe(List<String> equipe) {
        return new EightDSources(nc(null), equipe, List.of(), List.of(), List.of(), null, List.of());
    }

    // ---------- D1 : l'equipe nommee sur la fiche ----------

    // L'equipe se retapait dans le rapport alors que la fiche sait depuis le
    // premier jour qui prend l'ecart en charge.
    @Test
    void d1_part_de_l_equipe_nommee_sur_la_fiche() {
        EightDSnapshot snapshot = assembler.assemble(sourcesAvecEquipe(EQUIPE), null, null, null,
                "tenant-1", null, null);

        EightDSnapshot.Section d1 = section(snapshot, "D1");
        assertThat(d1.sourced()).isTrue();
        assertThat(d1.lines()).containsExactly(
                "Champion : Marie Durand", "Member : Ali Benali");
        assertThat(d1.sourceLabel()).contains("2 member(s)");
        assertThat(snapshot.missingCodes()).doesNotContain("D1");
    }

    // Le premier nomme pilote : l'ordre de saisie le porte, un tri le perdrait.
    @Test
    void d1_annonce_le_premier_nomme_comme_pilote() {
        EightDSnapshot snapshot = assembler.assemble(
                sourcesAvecEquipe(List.of("Ali Benali", "Marie Durand")), null, null, null,
                "tenant-1", null, null);

        assertThat(section(snapshot, "D1").lines())
                .containsExactly("Champion : Ali Benali", "Member : Marie Durand");
    }

    // La saisie libre dit ce que l'annuaire ne sait pas (un renfort exterieur,
    // le role de chacun) : elle complete les noms, elle ne les remplace pas.
    @Test
    void d1_garde_les_noms_et_ajoute_la_saisie_libre_ensuite() {
        EightDSnapshot snapshot = assembler.assemble(sourcesAvecEquipe(EQUIPE),
                "Renfort : Jean Petit (fournisseur)", null, null, "tenant-1", null, null);

        assertThat(section(snapshot, "D1").lines()).containsExactly(
                "Champion : Marie Durand", "Member : Ali Benali",
                "Renfort : Jean Petit (fournisseur)");
    }

    // Un ecart ne d'un constat d'audit ou d'un import systeme ne designe
    // personne : le rapport le DIT au lieu de le taire.
    @Test
    void d1_sans_equipe_ni_saisie_dit_qu_il_reste_a_remplir() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        EightDSnapshot.Section d1 = section(snapshot, "D1");
        assertThat(d1.sourced()).isFalse();
        assertThat(d1.lines()).isEmpty();
        assertThat(d1.sourceLabel()).contains("filled in by hand");
    }

    /** L'equipe nommee sur la fiche : la source de D1, dans son ordre de saisie. */
    private static final List<String> EQUIPE = List.of("Marie Durand", "Ali Benali");

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

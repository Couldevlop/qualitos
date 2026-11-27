package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;

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
        // SERVIE, ELLE N'ANNONCE PLUS SA PROVENANCE : « Non-conformité NC-… »
        // au-dessus d'une fiche qui commence par « Référence : NC-… ».
        assertThat(d2.sourceLabel()).isEmpty();
        // UNE FICHE, et non des phrases : libellé à gauche, valeur en regard.
        assertThat(d2.lines()).isEmpty();
        assertThat(d2.fields()).extracting(EightDSnapshot.Field::label)
                .containsExactly("Référence", "Intitulé", "Constat", "Catégorie",
                        "Gravité", "Détecté le");
        assertThat(d2.fields()).contains(
                new EightDSnapshot.Field("Référence", "NC-2026-0007"),
                new EightDSnapshot.Field("Gravité", "Majeure"));
        // LE JOUR SEUL : « 01/09/2026 08:30 UTC » datait une saisie, là où la
        // discipline demande quand le problème est apparu. La valeur reste du
        // TEXTE dans l'instantané : c'est ce qui rend le rendu, donc
        // l'empreinte, recalculables à l'identique des années plus tard.
        assertThat(d2.fields()).contains(new EightDSnapshot.Field("Détecté le", "01/09/2026"));
    }

    /**
     * SEPT LIGNES, PAS QUINZE. Le D2 recopiait la fiche : l'origine, la date de
     * cloture, la zone, le client, le lot, qui a signale, le nombre de photos,
     * la note de resolution. Chacune se defendait seule ; ensemble, elles
     * noyaient les faits que le destinataire du rapport cherche.
     */
    @Test
    void d2_s_en_tient_aux_sept_lignes_de_la_description() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        assertThat(section(snapshot, "D2").fields())
                .extracting(EightDSnapshot.Field::label)
                .doesNotContain("Origine", "Clos le", "Zone", "Signalé par", "Photos",
                        "Note de résolution");
    }

    /**
     * LE RAPPORT NOMME LE PRODUIT. C'est ce que le destinataire cherche en
     * premier, et il s'ecrit sur la fiche depuis qu'on a cesse de le noyer dans
     * la description. Le client et le lot, eux, ont quitte la description : ils
     * identifient le dossier, ils ne decrivent pas le probleme.
     */
    @Test
    void d2_nomme_le_produit() {
        EightDSources sources = new EightDSources(
                nc(null, null, "Pompes Atlantique", "L-2411", "CAR-400 — Carter de pompe"),
                List.of(), List.of(), List.of(), List.of(), null, List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, null, null, null,
                "tenant-1", null, null);

        EightDSnapshot.Section d2 = section(snapshot, "D2");
        assertThat(d2.fields())
                .contains(new EightDSnapshot.Field("Produit", "CAR-400 — Carter de pompe"));
        assertThat(d2.fields()).extracting(EightDSnapshot.Field::label)
                .doesNotContain("Client", "Lot");
    }

    /** Un constat sans produit : pas de ligne vide pour autant. */
    @Test
    void d2_ne_laisse_pas_de_ligne_vide_quand_le_produit_manque() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        assertThat(section(snapshot, "D2").fields())
                .extracting(EightDSnapshot.Field::label).doesNotContain("Produit");
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
            // ET ELLE DIT CE QUI MANQUE, sans envoyer chercher un champ de
            // saisie : ces deux disciplines viennent de la fiche et de son
            // dossier CAPA, elles ne se tapent plus.
            assertThat(s.sourceLabel()).doesNotContain("se renseigne à la main");
        }
        assertThat(section(snapshot, "D1").sourceLabel())
                .isEqualTo("Aucun membre nommé sur la non-conformité");
        assertThat(section(snapshot, "D3").sourceLabel())
                .isEqualTo("Aucune action immédiate sur les dossiers CAPA de cet écart");
    }

    // ---------- D8 : la reconnaissance de l'equipe ----------

    /**
     * RIEN D'ECRIT, et l'ecart solde : la phrase de la plateforme vaut mieux
     * qu'une discipline vide dans un document remis au client. Elle n'ecrase
     * alors aucune parole, puisqu'il n'y en a pas.
     */
    @Test
    void d8_porte_la_reconnaissance_standard_des_que_l_ecart_est_solde() {
        // L'ecart de la fixture est clos et n'a escalade aucune CAPA : rien ne reste
        // a attendre, la reconnaissance a sa place.
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        EightDSnapshot.Section d8 = section(snapshot, "D8");
        assertThat(d8.sourced()).isTrue();
        assertThat(d8.lines()).isEqualTo(EightDSnapshotAssembler.RECONNAISSANCE_STANDARD);
        assertThat(d8.sourceLabel()).isEmpty();
        assertThat(snapshot.missingCodes()).doesNotContain("D8");
    }

    /**
     * CE QU'ON PREPARE EST CE QUI SORT. Le document ajoutait deux paragraphes de
     * son cru sous le remerciement saisi : on ecrivait une phrase, on relisait
     * le rapport, et on y trouvait trois fois plus de texte -- signe par
     * l'equipe, qui ne l'avait pas ecrit. Relire un D8 avant de l'emettre
     * n'avait alors plus de sens.
     */
    @Test
    void d8_ne_porte_que_le_remerciement_saisi() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null,
                "Merci a Ada et Grace, qui ont tenu l'astreinte du week-end.",
                "tenant-1", null, null);

        EightDSnapshot.Section d8 = section(snapshot, "D8");
        assertThat(d8.sourced()).isTrue();
        assertThat(d8.lines())
                .containsExactly("Merci a Ada et Grace, qui ont tenu l'astreinte du week-end.");
        assertThat(d8.lines()).doesNotContainAnyElementsOf(
                EightDSnapshotAssembler.RECONNAISSANCE_STANDARD);
        // « Renseignée par l'équipe qualité » ne se lit plus au-dessus du
        // remerciement : qui l'a écrit se lit sur la fiche, pas en préambule.
        assertThat(d8.sourceLabel()).isEmpty();
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
        assertThat(d8.sourceLabel()).contains("se renseigne à la main");
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
        assertThat(d8.sourceLabel()).contains("se renseigne à la main");
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
        assertThat(d8.sourceLabel()).isEmpty();
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
        // SERVIE, LA DISCIPLINE N'ANNONCE PLUS SA PROVENANCE : « Agrégé depuis :
        // 1 Ishikawa, 1 analyse(s) 5 pourquoi » se lisait au-dessus des figures
        // qu'on vient regarder, et les repoussait d'une ligne.
        assertThat(d4.sourceLabel()).isEmpty();

        // LE TRACE REMPLACE L'ENUMERATION. Les causes s'ecrivaient ligne a
        // ligne (« MATERIEL : Joint use », « Why 1 : ... ») : un 8D se lit en
        // revue, et cette liste ne montrait ni les branches ni la descente.
        // Seule la cause racine PORTEE PAR LA FICHE reste en texte -- aucune
        // figure ne la dit.
        assertThat(d4.lines()).containsExactly(
                "Cause racine consignée sur la non-conformité : Joint hors tolérance");
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

    /**
     * LA CONCLUSION, PUIS LE RAISONNEMENT. La note du D4 porte l'hypothese
     * ecartee et la raison de son abandon ; sans elle, le rapport affirmait une
     * cause sans montrer ce qui avait ete essaye avant -- c'est-a-dire ce qu'un
     * auditeur demande des qu'il la conteste.
     *
     * <p>L'ordre compte : la cause retenue se lit d'abord, la note ensuite.
     */
    @Test
    void d4_porte_la_note_d_analyse_apres_la_cause_racine() {
        EightDSources sources = new EightDSources(
                nc("Joint hors tolérance",
                   "Piste fournisseur écartée : lot conforme au contrôle d'entrée"),
                EQUIPE, List.of(), List.of(), List.of(), null, List.of());

        EightDSnapshot.Section d4 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D4");

        assertThat(d4.sourced()).isTrue();
        assertThat(d4.lines()).containsExactly(
                "Cause racine consignée sur la non-conformité : Joint hors tolérance",
                "Notes d'analyse consignées sur la non-conformité :"
                        + " Piste fournisseur écartée : lot conforme au contrôle d'entrée");
        assertThat(d4.sourceLabel()).isEmpty();
    }

    /**
     * La note SEULE sert la discipline : on a cherche la cause et consigne ce
     * qu'on a ecarte, meme sans outil ouvert ni conclusion ecrite. Afficher
     * « rien a montrer » au-dessus d'un raisonnement deja ecrit le perdrait.
     */
    @Test
    void d4_servi_par_la_seule_note_d_analyse() {
        EightDSources sources = new EightDSources(
                nc(null, "Deux hypotheses ecartees, outillage et matiere"),
                EQUIPE, List.of(), List.of(), List.of(), null, List.of());

        EightDSnapshot.Section d4 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D4");

        assertThat(d4.sourced()).isTrue();
        assertThat(d4.lines()).singleElement()
                .asString().contains("Notes d'analyse");
        assertThat(d4.figures()).isEmpty();
    }

    @Test
    void d4_sans_aucune_analyse_explique_ce_qui_manque() {
        EightDSnapshot.Section d4 = section(
                assembler.assemble(sourcesMinimales(), null, null, null, "t", null, null), "D4");

        assertThat(d4.sourced()).isFalse();
        assertThat(d4.sourceLabel())
                .contains("Aucun Ishikawa")
                .contains("5 pourquoi")
                .contains("cause racine")
                .contains("note d'analyse");
    }

    @Test
    void d5_et_d6_disent_l_absence_de_capa_plutot_que_de_rester_vides() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "t", null, null);

        assertThat(section(snapshot, "D5").sourceLabel()).contains("Aucun dossier CAPA");
        assertThat(section(snapshot, "D6").sourceLabel()).contains("Aucun dossier CAPA");
        assertThat(section(snapshot, "D5").sourced()).isFalse();
    }

    @Test
    void d5_liste_les_actions_decidees_et_d6_celles_qui_sont_menees_avec_leurs_preuves() {
        EightDSources.Action faite = new EightDSources.Action(
                "Remplacer le joint", "Référence 44-B", "CORRECTIVE", "DONE", "Grace",
                LocalDate.parse("2026-09-10"), Instant.parse("2026-09-09T09:00:00Z"),
                List.of("rapport-essai.pdf", "photo-joint.jpg"));
        EightDSources.Action aFaire = new EightDSources.Action(
                "Former les opérateurs", null, "PREVENTIVE", "IN_PROGRESS", null, null, null,
                List.of());
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Fuite presse-étoupe", "CORRECTIVE", "HIGH", "CLOSED",
                        LocalDate.parse("2026-09-30"), Instant.parse("2026-09-11T10:00:00Z"),
                        Boolean.TRUE, Instant.parse("2026-12-01T10:00:00Z"),
                        List.of("pv-cloture.pdf"),
                        List.of(faite, aFaire))),
                null, List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, null, null, null, "t", null, null);

        EightDSnapshot.Section d5 = section(snapshot, "D5");
        assertThat(d5.sourced()).isTrue();
        assertThat(d5.sourceLabel()).isEmpty();
        // Les actions sont en COLONNES, pas en phrases : c'est ce que l'écran
        // montre, et le document émis doit se présenter comme l'écran.
        assertThat(d5.lines()).isEmpty();
        assertThat(d5.tables()).hasSize(1);
        EightDSnapshot.Table decidees = d5.tables().get(0);
        // SANS LÉGENDE : chaque action ouvre son propre dossier, et le titre du
        // dossier répétait donc l'intitulé de sa ligne unique.
        assertThat(decidees.caption()).isEmpty();
        // SANS LA COLONNE DES PREUVES, comme a l'ecran : la piece d'une action
        // atteste que CE geste-la a ete fait, et le D5 n'a pas a prouver qu'une
        // action a ete faite mais que la cause a disparu -- cela se juge au D6,
        // sur le dossier. Le document emis se presente comme la fiche.
        assertThat(decidees.columns())
                .containsExactly("Action", "Responsable", "Début", "Échéance", "État");
        assertThat(decidees.rows()).hasSize(1);
        // LA NATURE N'EST PLUS UNE COLONNE : ce tableau ne montre que du
        // correctif, et le mot se répétait sur chacune de ses lignes, en tête
        // de page, devant l'intitulé qu'on vient lire.
        assertThat(decidees.rows().get(0).cells()).containsExactly(
                "Remplacer le joint", "Grace", "-", "10/09/2026", "Faite");

        // LA PREVENTIVE N'EST PLUS ICI : elle figurait au D5 **en plus** du D7,
        // de sorte que chaque action preventive se lisait deux fois dans le
        // meme rapport, sous deux disciplines, comme si l'ecart avait ete
        // traite deux fois. Son tableau, le seul, est verifie au D7 plus bas.
        assertThat(decidees.rows().get(0).cells())
                .doesNotContain("Former les opérateurs");

        EightDSnapshot.Section d6 = section(snapshot, "D6");
        assertThat(d6.sourced()).isTrue();
        assertThat(d6.sourceLabel()).isEmpty();
        // IL NOMME LES PIÈCES, il ne les compte plus. « menée le 09/09 — 2
        // preuve(s) » donnait une date que le tableau du D5 porte déjà et un
        // compte invérifiable : le lecteur ne savait pas ce qui avait été
        // joint. Un nom de fichier se réclame quand il manque.
        assertThat(d6.lines()).containsSubsequence(
                "Remplacer le joint",
                "    Pièces jointes : rapport-essai.pdf, photo-joint.jpg");
        // ET RIEN D'AUTRE : la liste de recette ne veut ici que l'intitulé de
        // l'action et le nom de ses pièces.
        assertThat(d6.lines()).noneMatch(l -> l.startsWith("Efficacité"));
        assertThat(d6.lines()).contains("Pièces du dossier : pv-cloture.pdf");
        assertThat(d6.lines()).noneMatch(l -> l.contains("preuve(s)"));
        // Une action encore en cours n'a pas sa place dans le RÉCIT du D6 : elle
        // n'est pas mise en œuvre.
        assertThat(d6.lines()).noneMatch(l -> l.contains("Former les opérateurs"));
        // ET AUCUN TABLEAU : le D6 portait celui des actions préventives, qui
        // est le D7. Le client lisait la prévention sous « mise en oeuvre et
        // preuves », et le D7 se déclarait sans contenu juste en dessous.
        assertThat(d6.tables()).isEmpty();

        // Elle a sa place dans le TABLEAU DU D7, qui donne l'état de chaque
        // action préventive à la date d'émission -- y compris celles qui
        // restent à faire. Les taire laisserait croire la prévention terminée.
        EightDSnapshot.Section d7 = section(snapshot, "D7");
        assertThat(d7.sourced()).isTrue();
        assertThat(d7.tables()).hasSize(1);
        // AVEC LA COLONNE DES PREUVES, comme le D3 et contrairement au D5 : une
        // barrière de prévention s'installe et se montre, et la pièce atteste
        // que CE geste-là a été fait.
        assertThat(d7.tables().get(0).columns())
                .containsExactly("Action", "Responsable", "Début", "Échéance", "État", "Preuves");
        assertThat(d7.tables().get(0).rows()).hasSize(1);
        assertThat(d7.tables().get(0).rows().get(0).cells())
                .containsExactly("Former les opérateurs", "-", "-", "-", "En cours", "-");
        assertThat(d7.sourceLabel()).isEmpty();
    }

    /**
     * LE D7 NE RECOPIE PLUS L'AMDEC NI LES PLANS DE SURVEILLANCE.
     *
     * <p>C'est l'outillage INTERNE de l'analyse -- mode de défaillance, effet,
     * cause, IPR, révisions de plan --, pas la prévention. Le client vient lire
     * ce qui empêche l'écart de revenir, et il lisait d'abord une page de
     * référentiel où les gestes décidés se perdaient. Les deux restent entiers
     * sur leurs écrans.
     */
    @Test
    void d7_ne_recopie_plus_l_amdec_ni_les_plans_de_surveillance() {
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(), List.of(),
                new EightDSources.Fmea("Fuite au joint", "Perte de pression", "Usure",
                        "Contrôle visuel hebdomadaire", 160, 40, "MEDIUM",
                        "Plan de remplacement préventif", "Plan déployé en août"),
                List.of(new EightDSources.Surveillance("CP-4471", 3, "PRODUCTION", "ACTIVE", 12,
                        "b".repeat(64))));

        EightDSnapshot.Section d7 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D7");

        assertThat(d7.lines()).isEmpty();
        // Et la discipline le DIT : un AMDEC rattaché ne tient pas lieu d'action
        // préventive décidée, et un D7 annoncé servi sur ce seul motif laisserait
        // croire la récidive traitée.
        assertThat(d7.sourced()).isFalse();
        assertThat(d7.sourceLabel()).isEqualTo("Aucune action préventive décidée sur cet écart");
    }

    @Test
    void d7_sans_source_dit_ce_qui_manque_et_rien_de_plus() {
        EightDSnapshot.Section d7 = section(
                assembler.assemble(sourcesMinimales(), null, null, null, "t", null, null), "D7");

        assertThat(d7.sourced()).isFalse();
        // IL NE PARLE PLUS QUE DES ACTIONS : l'AMDEC et les plans de
        // surveillance ne composent plus la discipline, et annoncer leur
        // absence enverrait chercher dans le document ce qu'il ne porte pas.
        assertThat(d7.sourceLabel()).isEqualTo("Aucune action préventive décidée sur cet écart");
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
                List.of(new EightDSources.Capa("CAPA", "CORRECTIVE", "HIGH", "CLOSED", null, null, null, null,
                        List.of("bilan.pdf"),
                        // UNE ACTION PAR DISCIPLINE : le D3, le D5 et le D7 ne
                        // sont plus servis par un texte tapé à la main mais par
                        // les gestes tracés de leur nature.
                        List.of(new EightDSources.Action("Bloquer le lot", null, "CONTAINMENT", "DONE",
                                null, null, Instant.parse("2026-09-08T09:00:00Z"), List.of()),
                                new EightDSources.Action("Action", null, "CORRECTIVE", "DONE", null,
                                null, Instant.parse("2026-09-09T09:00:00Z"), List.of("constat.pdf")),
                                new EightDSources.Action("Former les opérateurs", null, "PREVENTIVE",
                                "DONE", null, null, Instant.parse("2026-09-10T09:00:00Z"), List.of())))),
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
        // UN SEUL TABLEAU, toutes les actions : les actions du second dossier
        // retenaient la clôture sans figurer dans le document remis au client,
        // et un tableau par dossier en donnait deux d'une ligne, chacun titré
        // du même texte que sa ligne.
        assertThat(d5.tables()).hasSize(1);
        assertThat(d5.tables().get(0).caption()).isEmpty();
        assertThat(d5.tables().get(0).rows()).hasSize(2);
        assertThat(d5.tables().get(0).rows().get(0).cells()).contains("Remplacer le joint");
        assertThat(d5.tables().get(0).rows().get(1).cells()).contains("Revoir la gamme");
        // Le COMPTE, pas le singulier : « Escalated CAPA » mentait d\u00e8s qu'un
        // second dossier citait la r\u00e9f\u00e9rence, et rien ne le disait au lecteur.
        assertThat(d5.sourceLabel()).isEmpty();
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

        // LES DEUX DOSSIERS, DANS L'ORDRE, SANS SE MÊ'LER : ce que le tableau
        // par dossier garantissait par la séparation, le tableau unique le
        // garantit par la ligne -- chaque action garde la sienne.
        assertThat(d5.tables()).hasSize(1);
        assertThat(d5.tables().get(0).rows()).hasSize(2);
        assertThat(d5.tables().get(0).rows().get(0).cells()).contains("A faire");
        assertThat(d5.tables().get(0).rows().get(1).cells()).contains("A faire aussi");
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
                Instant.parse("2026-10-01T10:00:00Z"), List.of(), List.of());
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
     * l'ecran : le D3 endigue, le D7 previent, le D5 montre les CAPA definies
     * -- correctives et preventives, et plus l'endiguement, deja porte par le
     * tableau du D3 juste au-dessus.
     *
     * <p>Melanger les natures ferait lire, sous « actions immediates
     * d'endiguement », une action preventive a echeance dans six mois -- et
     * c'est le document que l'auditeur ou le client lit pour savoir ce qui a
     * ete fait TOUT DE SUITE.
     */
    @Test
    void d3_n_affiche_que_l_endiguement_et_d7_que_la_prevention() {
        EightDSources.Action endiguer = new EightDSources.Action(
                "Isoler le lot", null, "CONTAINMENT", "DONE", "Ada", null, null, List.of());
        EightDSources.Action corriger = new EightDSources.Action(
                "Remplacer le joint", null, "CORRECTIVE", "DONE", "Grace", null, null, List.of());
        EightDSources.Action prevenir = new EightDSources.Action(
                "Revoir la gamme", null, "PREVENTIVE", "TODO", "Alan", null, null, List.of());
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Dossier A", "CORRECTIVE", "HIGH", "OPEN",
                        null, null, null, null, List.of(), List.of(endiguer, corriger, prevenir))),
                null, List.of());

        EightDSnapshot snapshot = assembler.assemble(sources, null, null, null, "t", null, null);

        assertThat(cellulesDe(snapshot, "D3")).containsExactly("Isoler le lot");
        assertThat(cellulesDe(snapshot, "D7")).containsExactly("Revoir la gamme");
        assertThat(section(snapshot, "D6").tables()).isEmpty();
        // Et le D5 ne reprend NI l'un NI l'autre : il ne montre que ce qui
        // supprime la cause de l'ecart survenu.
        assertThat(cellulesDe(snapshot, "D5")).containsExactly("Remplacer le joint");
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
        // D7 n'ont rien a tabuler.
        assertThat(section(snapshot, "D5").tables()).hasSize(1);
        assertThat(section(snapshot, "D3").tables()).isEmpty();
        assertThat(section(snapshot, "D7").tables()).isEmpty();
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
                "Isoler le lot", null, "CONTAINMENT", "DONE", "Ada", null, null, List.of());
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Dossier A", "CORRECTIVE", "HIGH", "OPEN",
                        null, null, null, null, List.of(), List.of(endiguer))),
                null, List.of());

        EightDSnapshot.Section d3 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D3");

        assertThat(d3.sourced()).isTrue();
        // Elle ne dit plus RIEN : ni « renseignée par l'équipe qualité » -- ce
        // que personne n'a fait ici --, ni la provenance du tableau, qui porte
        // déjà le titre de son dossier.
        assertThat(d3.sourceLabel()).isEmpty();
    }

    /** Les intitules d'action tabules sous une discipline, dans l'ordre. */
    private List<String> cellulesDe(EightDSnapshot snapshot, String code) {
        List<String> intitules = new ArrayList<>();
        for (EightDSnapshot.Table tableau : section(snapshot, code).tables()) {
            for (EightDSnapshot.Row rang : tableau.rows()) {
                // LA PREMIERE CELLULE : la nature a quitte les colonnes, elle
                // est le titre de la discipline.
                intitules.add(rang.cells().get(0));
            }
        }
        return intitules;
    }

    private static EightDSources.Capa dossier(String titre, String action) {
        return new EightDSources.Capa(titre, "CORRECTIVE", "HIGH", "OPEN", null, null, null, null,
                List.of(),
                List.of(new EightDSources.Action(action, null, "CORRECTIVE", "PENDING",
                        "Ada Lovelace", null, null, List.of())));
    }

    // ---------- fixtures ----------

    private EightDSources sourcesMinimales() {
        // Aucune equipe : c'est le cas d'un ecart ne d'un constat d'audit ou d'un
        // import systeme, ou D1 reste a saisir.
        return new EightDSources(nc(null), List.of(), List.of(), List.of(), List.of(), null, List.of());
    }

    /** Les memes sources, mais avec l'equipe nommee sur la fiche. */
    private EightDSources sourcesAvecEquipe(List<EightDSources.Member> equipe) {
        return new EightDSources(nc(null), equipe, List.of(), List.of(), List.of(), null, List.of());
    }

    /**
     * LE D6 NE RAPPELLE QUE LES ACTIONS CORRECTIVES (diapo 7 de la recette).
     *
     * <p>L'endiguement se lit au D3 et la prevention au D7, chacun avec la
     * piece de son action : les reprendre ici rappelait trois fois le meme
     * geste dans le meme document. Le D6 demontre que la CAUSE a disparu, et
     * c'est l'action corrective qui en repond.
     */
    @Test
    void d6_ne_rappelle_que_les_actions_correctives() {
        EightDSources.Action endiguement = new EightDSources.Action(
                "Bloquer le lot", null, "CONTAINMENT", "DONE", "Ada", null,
                Instant.parse("2026-09-08T09:00:00Z"), List.of("tri.pdf"));
        EightDSources.Action corrective = new EightDSources.Action(
                "Remplacer le joint", null, "CORRECTIVE", "DONE", "Grace", null,
                Instant.parse("2026-09-09T09:00:00Z"), List.of());
        EightDSources.Action preventive = new EightDSources.Action(
                "Former les operateurs", null, "PREVENTIVE", "DONE", "Ada", null,
                Instant.parse("2026-09-10T09:00:00Z"), List.of());
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Fuite presse-etoupe", "CORRECTIVE", "HIGH",
                        "OPEN", null, null, null, null, List.of(),
                        List.of(endiguement, corrective, preventive))),
                null, List.of());

        EightDSnapshot.Section d6 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D6");

        assertThat(d6.lines()).contains("Remplacer le joint");
        assertThat(d6.lines()).doesNotContain("Bloquer le lot", "Former les operateurs");
    }

    /**
     * LE VERDICT D'EFFICACITE NE S'ECRIT PLUS AU D6.
     *
     * <p>La liste de recette ne veut, sous cette discipline, que l'intitule de
     * l'action et le nom de ses pieces -- « enlever tout le texte ». Le
     * jugement porte (ADR 0108, ADR 0131) reste lisible sur le dossier CAPA et
     * sur le tableau d'efficacite ; c'est le document remis au client qui ne le
     * reprend plus. Ce banc tient la suppression : sans lui, la ligne
     * reviendrait au premier qui trouverait l'absence anormale.
     */
    @Test
    void d6_ne_reprend_pas_le_verdict_d_efficacite() {
        EightDSources.Action jugee = new EightDSources.Action(
                "Remplacer le joint", null, "CORRECTIVE", "DONE", "Grace",
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-10"),
                Instant.parse("2026-09-09T09:00:00Z"),
                List.of("rapport-essai.pdf"), "PARTIAL",
                Instant.parse("2026-10-02T09:00:00Z"));
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Fuite presse-etoupe", "CORRECTIVE", "HIGH",
                        "OPEN", null, null, null, null, List.of(), List.of(jugee))),
                null, List.of());

        EightDSnapshot.Section d6 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D6");

        assertThat(d6.lines()).containsSubsequence(
                "Remplacer le joint", "    Pièces jointes : rapport-essai.pdf");
        assertThat(d6.lines()).noneMatch(l -> l.startsWith("Efficacité"));
    }

    /**
     * L'ABSENCE DE PIECE NE S'ECRIT PAS.
     *
     * <p>« Aucune piece jointe » posait une ligne de manque sous chaque action,
     * et la discipline se lisait comme un releve de ce qui manque la ou elle
     * doit montrer ce qui a ete produit.
     */
    @Test
    void d6_ne_dit_pas_l_absence_de_piece() {
        EightDSources.Action sansPiece = new EightDSources.Action(
                "Reprendre la gamme", null, "CORRECTIVE", "DONE", "Ada", null,
                Instant.parse("2026-09-10T09:00:00Z"), List.of());
        EightDSources sources = new EightDSources(
                nc(null), EQUIPE, List.of(), List.of(),
                List.of(new EightDSources.Capa("Fuite presse-etoupe", "CORRECTIVE", "HIGH",
                        "OPEN", null, null, null, null, List.of(), List.of(sansPiece))),
                null, List.of());

        EightDSnapshot.Section d6 = section(
                assembler.assemble(sources, null, null, null, "t", null, null), "D6");

        assertThat(d6.lines()).contains("Reprendre la gamme");
        assertThat(d6.lines()).doesNotContain("Aucune pièce jointe");
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
        // EN COLONNES, et non en lignes « Pilote : … » : qui est qui, et
        // comment le joindre, se cherchait d'une ligne a l'autre. Le
        // destinataire du rapport sait desormais a qui ecrire.
        assertThat(d1.lines()).isEmpty();
        assertThat(d1.tables()).hasSize(1);
        EightDSnapshot.Table equipe = d1.tables().get(0);
        assertThat(equipe.columns())
                .containsExactly("Nom et prénom", "Adresse mail", "Rôle et fonction");
        assertThat(equipe.rows()).hasSize(2);
        assertThat(equipe.rows().get(0).cells())
                .containsExactly("Marie Durand", "marie.durand@exemple.fr", "Pilote");
        assertThat(equipe.rows().get(1).cells())
                .containsExactly("Ali Benali", "ali.benali@exemple.fr", "Membre");
        // UN TABLEAU SANS TITRE : « Actions - » au-dessus d'une liste de
        // personnes annoncerait ce qu'il ne montre pas.
        assertThat(equipe.caption()).isEmpty();
        assertThat(snapshot.missingCodes()).doesNotContain("D1");
    }

    // Le premier nomme pilote : l'ordre de saisie le porte, un tri le perdrait.
    @Test
    void d1_annonce_le_premier_nomme_comme_pilote() {
        EightDSnapshot snapshot = assembler.assemble(
                sourcesAvecEquipe(List.of(
                        new EightDSources.Member("Ali Benali", "ali.benali@exemple.fr"),
                        new EightDSources.Member("Marie Durand", "marie.durand@exemple.fr"))),
                null, null, null,
                "tenant-1", null, null);

        EightDSnapshot.Table equipe = section(snapshot, "D1").tables().get(0);
        assertThat(equipe.rows().get(0).cells())
                .containsExactly("Ali Benali", "ali.benali@exemple.fr", "Pilote");
        assertThat(equipe.rows().get(1).cells())
                .containsExactly("Marie Durand", "marie.durand@exemple.fr", "Membre");
    }

    // La saisie libre dit ce que l'annuaire ne sait pas (un renfort exterieur,
    // le role de chacun) : elle complete les noms, elle ne les remplace pas.
    @Test
    void d1_garde_les_noms_et_ajoute_la_saisie_libre_ensuite() {
        EightDSnapshot snapshot = assembler.assemble(sourcesAvecEquipe(EQUIPE),
                "Renfort : Jean Petit (fournisseur)", null, null, "tenant-1", null, null);

        // LA SAISIE LIBRE VIENT APRES LE TABLEAU : elle dit ce que l'annuaire ne
        // sait pas -- un renfort exterieur --, elle ne remplace pas les noms.
        assertThat(section(snapshot, "D1").tables()).hasSize(1);
        assertThat(section(snapshot, "D1").lines())
                .containsExactly("Renfort : Jean Petit (fournisseur)");
    }

    // Un ecart ne d'un constat d'audit ou d'un import systeme ne designe
    // personne : le rapport le DIT au lieu de le taire.
    @Test
    void d1_sans_equipe_ni_saisie_dit_que_personne_n_est_nomme() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        EightDSnapshot.Section d1 = section(snapshot, "D1");
        assertThat(d1.sourced()).isFalse();
        assertThat(d1.lines()).isEmpty();
        assertThat(d1.sourceLabel()).isEqualTo("Aucun membre nommé sur la non-conformité");
    }

    /**
     * L'instantane neuf PORTE SA LANGUE : c'est elle que le rendu PDF lit pour
     * choisir ses propres mots, et la stocker avec l'instantane est ce qui
     * permet a un rapport deja emis de se relire a l'octet pres.
     */
    @Test
    void l_instantane_neuf_porte_la_langue_du_document() {
        EightDSnapshot snapshot = assembler.assemble(sourcesMinimales(), null, null, null,
                "tenant-1", null, null);

        assertThat(snapshot.langue()).isEqualTo("fr");
    }

    /** L'equipe nommee sur la fiche : la source de D1, dans son ordre de saisie. */
    private static final List<EightDSources.Member> EQUIPE = List.of(
            new EightDSources.Member("Marie Durand", "marie.durand@exemple.fr"),
            new EightDSources.Member("Ali Benali", "ali.benali@exemple.fr"));

    private EightDSources.Nc nc(String causeRacine) {
        return nc(causeRacine, null);
    }

    private EightDSources.Nc nc(String causeRacine, String noteDAnalyse) {
        return nc(causeRacine, noteDAnalyse, null, null, null);
    }

    /** Le meme ecart, avec ce qui designe le client, le produit et le lot. */
    private EightDSources.Nc nc(String causeRacine, String noteDAnalyse,
                                String client, String lot, String produit) {
        return new EightDSources.Nc("NC-2026-0007", "Fuite au presse-étoupe",
                "Flaque d'huile sous la pompe P-12", "PROCESS", "MAJOR", "INTERNAL",
                NcStatus.CLOSED, DETECTE, CLOTURE, "Atelier 3", "Ada Lovelace", 2,
                causeRacine, "Joint remplacé, étanchéité contrôlée", noteDAnalyse,
                client, lot, produit);
    }

    /** Le meme ecart, mais encore ouvert : rien n'est solde. */
    private EightDSources.Nc ncOuverte() {
        return new EightDSources.Nc("NC-2026-0008", "Fuite au presse-etoupe",
                "Flaque d'huile sous la pompe P-12", "PROCESS", "MAJOR", "INTERNAL",
                NcStatus.UNDER_ANALYSIS, DETECTE, null, "Atelier 3", "Ada Lovelace", 0,
                null, null, null, null, null, null);
    }

    /** Une CAPA escaladee, close ou non selon la date passee. */
    private EightDSources.Capa capa(Instant cloture) {
        return new EightDSources.Capa("Fuite presse-etoupe", "CORRECTIVE", "HIGH",
                cloture == null ? "IN_PROGRESS" : "CLOSED",
                LocalDate.parse("2026-09-30"), cloture, null, null, List.of(), List.of());
    }

    private EightDSnapshot.Section section(EightDSnapshot snapshot, String code) {
        return snapshot.sections().stream()
                .filter(s -> s.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("section " + code + " absente"));
    }
}

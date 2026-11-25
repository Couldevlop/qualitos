package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDDiscipline;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Met les sources en texte : de huit disciplines à huit sections rendues.
 *
 * <p>Classe à part du service, et sans aucune dépendance technique : c'est ici
 * que se joue tout ce qu'un lecteur du rapport verra, et cela doit se tester sans
 * base, sans jeton et sans PDF.
 *
 * <p><b>Une discipline sans source le DIT.</b> Chaque section porte un
 * {@code sourceLabel} renseigné dans les deux cas : d'où vient le contenu quand il
 * y en a, et ce qui manque quand il n'y en a pas. Une section vide et muette
 * laisserait croire qu'il n'y avait rien à dire, alors qu'elle dit seulement que
 * personne n'a cherché — et c'est exactement le reproche qu'un auditeur fait aux
 * 8D de complaisance.
 *
 * <p>Toutes les dates sont formatées ICI, en UTC, et stockées sous forme de texte
 * dans l'instantané : l'empreinte scellée doit rester calculable à l'identique des
 * années plus tard, ce qu'un {@code Instant} relu d'une colonne à précision
 * variable ne garantit pas (ADR 0062).
 */
public class EightDSnapshotAssembler {

    private static final DateTimeFormatter HORODATAGE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * La langue du document, écrite dans l'instantané de chaque rapport neuf.
     *
     * <p>Le rendu PDF la lit pour choisir ses propres mots — titre, en-tête,
     * mentions d'intégrité. Un rapport scellé avant elle n'en porte pas et se
     * relit donc dans la langue où il a été émis : son empreinte est calculée
     * sur les octets du PDF, et ces octets ne doivent plus changer.
     */
    static final String LANGUE = "fr";

    private static final String A_SAISIR =
            "Aucune source dans la plateforme \u2014 cette discipline se renseigne à la main";
    private static final String SAISI = "Renseignée par l'équipe qualité";

    /**
     * La reconnaissance que porte TOUT rapport 8D dont l'écart est soldé.
     *
     * <p>D8 n'est pas une politesse : la méthode en fait une étape, parce qu'une
     * équipe qui a tenu l'endiguement, établi la cause et installé la prévention a
     * produit un résultat que le document doit nommer. Laissée à la seule saisie
     * libre, la discipline restait vide sur la plupart des rapports — et un 8D
     * remis à un client avec un D8 muet se lit comme un dossier abandonné en route.
     *
     * <p>Stockée dans l'instantané, et non relue d'une configuration : l'empreinte
     * scellée doit rester vérifiable à l'identique des années plus tard, ce qu'un
     * texte relu d'une configuration mouvante ne garantirait pas.
     */
    static final List<String> RECONNAISSANCE_STANDARD = List.of(
            "L'organisation remercie chaque membre de l'équipe pour le travail mené sur"
            + " cet écart : l'endiguement qui a protégé le client, l'analyse qui a établi"
            + " la cause, et les mesures qui empêchent son retour.",
            "Cette clôture est la leur. Le savoir acquis a été versé aux standards de"
            + " l'organisation, pour que la prochaine équipe en parte.");

    /** Pourquoi la reconnaissance figure au rapport. */
    private static final String RECONNAISSANCE_AUTO =
            "Reconnaissance standard — l'écart et ses actions correctives sont clos";

    /**
     * @param sources      ce que les autres modules savent de l'écart
     * @param team         D1, tel que saisi (peut être nul)
     * @param containment  D3, tel que saisi (peut être nul)
     * @param recognition  D8, tel que saisi (peut être nul)
     * @param tenantLabel  l'identifiant du tenant, reporté en pied de page
     * @param issuedAt     l'instant d'émission, ou {@code null} pour un aperçu de brouillon
     * @param issuedByName le nom de l'émetteur, ou {@code null}
     */
    public EightDSnapshot assemble(EightDSources sources, String team, String containment,
                                   String recognition, String tenantLabel,
                                   Instant issuedAt, String issuedByName) {
        List<EightDSnapshot.Section> sections = List.of(
                equipe(sources.team(), team),
                probleme(sources.nc()),
                securisation(containment, sources.capas()),
                causeRacine(sources),
                actionsDecidees(sources.capas()),
                miseEnOeuvre(sources.capas()),
                prevention(sources),
                reconnaissance(sources, recognition));

        boolean partiel = sections.stream().anyMatch(s -> !s.sourced());
        return new EightDSnapshot(
                sources.nc().reference(),
                sources.nc().title(),
                tenantLabel,
                issuedAt == null ? null : HORODATAGE.format(issuedAt),
                issuedByName,
                partiel,
                sections,
                LANGUE);
    }

    // ---------- D1 : l'équipe nommée sur la fiche ----------

    /**
     * D1 part de l'équipe nommée à l'ouverture de la fiche.
     *
     * <p>Elle se retapait ici à la main, alors que la fiche sait depuis le
     * premier jour qui prend l'écart en charge : une discipline qu'on ressaisit
     * est une discipline qu'on oublie, et un D1 vide se discute mal en revue.
     *
     * <p>Le premier nommé est annoncé comme pilote — convention d'atelier, et
     * c'est l'ordre de saisie qui la porte. La saisie libre vient ENSUITE et ne
     * remplace pas les noms : elle dit ce que l'annuaire ne sait pas (un renfort
     * extérieur, le rôle de chacun).
     *
     * @param nommes l'équipe de la fiche, dans son ordre de saisie
     * @param texte  la saisie libre du rapport, le cas échéant
     */
    private EightDSnapshot.Section equipe(List<String> nommes, String texte) {
        List<String> lignes = new ArrayList<>();
        for (int i = 0; i < nommes.size(); i++) {
            ajouter(lignes, i == 0 ? "Pilote" : "Membre", nommes.get(i));
        }
        lignes.addAll(lignesDe(texte));
        // « Renseignée » veut dire « il y a quelque chose à montrer », comme pour
        // D3 et D8 : une équipe tapée à la main compte, sans quoi un D1 rempli
        // serait annoncé manquant. Seule l'ORIGINE change le libellé.
        String origine = nommes.isEmpty()
                // Aucun nom sur la fiche : l'écart est né d'un constat d'audit ou
                // d'un import système, et D1 se remplit à la main.
                ? (lignes.isEmpty() ? A_SAISIR : SAISI)
                : "Équipe nommée sur la non-conformité \u2014 " + nommes.size()
                  + " membre(s)";
        return section(EightDDiscipline.D1, !lignes.isEmpty(), origine, lignes);
    }

    // ---------- D3, D8 : ce que personne d'autre ne sait ----------

    /**
     * D3 : ce que le responsable a ecrit, ET les actions d'endiguement du dossier.
     *
     * <p>Les deux, parce qu'ils ne disent pas la meme chose : le texte raconte
     * ce qu'on a fait pour arreter l'hemorragie, le tableau porte les gestes
     * tracables -- qui, pour quand, fait ou non, prouve ou non. C'est aussi ce
     * que l'ecran montre sous cette discipline.
     */
    private EightDSnapshot.Section securisation(String texte, List<EightDSources.Capa> capas) {
        List<String> lignes = lignesDe(texte);
        List<EightDSnapshot.Table> tableaux = tableauxDesActions(capas, NATURES_D3);
        // La provenance n'est pas la meme des deux cotes, et le libelle de source
        // ne doit pas mentir : le texte vient de l'equipe qualite, les actions du
        // dossier CAPA. Dire « rempli par l'equipe qualite » d'un D3 ou seul le
        // tableau est servi attribuerait a quelqu'un une phrase qu'il n'a pas
        // ecrite.
        String label;
        if (!lignes.isEmpty()) {
            label = tableaux.isEmpty() ? SAISI : SAISI + " — " + libelleCapas(capas);
        } else if (!tableaux.isEmpty()) {
            label = libelleCapas(capas);
        } else {
            label = A_SAISIR;
        }
        boolean servi = !lignes.isEmpty() || !tableaux.isEmpty();
        return new EightDSnapshot.Section(
                EightDDiscipline.D3.code(), EightDDiscipline.D3.titre(), servi,
                label, lignes, List.of(), tableaux);
    }

    private EightDSnapshot.Section saisie(EightDDiscipline discipline, String texte) {
        List<String> lignes = lignesDe(texte);
        return section(discipline, !lignes.isEmpty(), lignes.isEmpty() ? A_SAISIR : SAISI, lignes);
    }

    // ---------- D8 : la reconnaissance de l'équipe ----------

    /**
     * D8 : ce que l'équipe qualité a écrit, puis — dès que l'écart est soldé — la
     * reconnaissance standard.
     *
     * <p>La saisie libre passe DEVANT : un remerciement nominatif, écrit par le
     * responsable, vaut plus que la phrase de la plateforme, et la reconnaissance
     * standard la complète au lieu de la remplacer.
     *
     * <p>Tant que l'écart n'est pas soldé, aucune reconnaissance n'est ajoutée :
     * féliciter une équipe sur un dossier encore ouvert daterait le document d'un
     * optimisme que la suite peut démentir.
     */
    private EightDSnapshot.Section reconnaissance(EightDSources sources, String texte) {
        List<String> lignes = new ArrayList<>(lignesDe(texte));
        boolean saisi = !lignes.isEmpty();
        boolean solde = toutEstSolde(sources);
        if (solde) {
            lignes.addAll(RECONNAISSANCE_STANDARD);
        }
        String label;
        if (saisi && solde) {
            label = SAISI + ", complétée par la reconnaissance standard";
        } else if (saisi) {
            label = SAISI;
        } else if (solde) {
            label = RECONNAISSANCE_AUTO;
        } else {
            label = A_SAISIR;
        }
        return section(EightDDiscipline.D8, !lignes.isEmpty(), label, lignes);
    }

    /**
     * Vrai quand l'écart ET les actions correctives qu'il a engendrées sont clos.
     *
     * <p>Une non-conformité close dont la CAPA escaladée court toujours n'est pas
     * soldée : c'est la prévention qui reste à finir, et c'est précisément ce que le
     * client lira en D7. Un écart clos sans CAPA l'est, lui : rien n'a été
     * escaladé, il n'y a rien à attendre.
     */
    private boolean toutEstSolde(EightDSources sources) {
        if (sources.nc().closedAt() == null) {
            return false;
        }
        // TOUS les dossiers, pas seulement celui que la fiche porte en lien :
        // un écart dont le second dossier court encore n'est pas soldé, et
        // c'est ce que le client lira en D7.
        return sources.capas().stream().allMatch(c -> c.closedAt() != null);
    }

    // ---------- D2 : la non-conformité ----------

    private EightDSnapshot.Section probleme(EightDSources.Nc nc) {
        List<String> lignes = new ArrayList<>();
        ajouter(lignes, "Référence", nc.reference());
        ajouter(lignes, "Intitulé", nc.title());
        ajouter(lignes, "Constat", nc.description());
        ajouter(lignes, "Catégorie", libelle(CATEGORIES, nc.category()));
        ajouter(lignes, "Gravité", libelle(GRAVITES, nc.severity()));
        ajouter(lignes, "Origine", libelle(ORIGINES, nc.origin()));
        ajouter(lignes, "Détecté le", horodatage(nc.detectedAt()));
        ajouter(lignes, "Clos le", horodatage(nc.closedAt()));
        ajouter(lignes, "Zone", nc.zone());
        // Le client, le lot, le produit : ce que le destinataire du rapport
        // cherche en premier, et qui vit sur la fiche depuis qu'on a cesse de
        // l'ecrire dans la description. Absents, ils ne laissent pas de ligne
        // vide -- un 8D interne n'a pas de client.
        ajouter(lignes, "Client", nc.customerName());
        ajouter(lignes, "Produit", nc.productLabel());
        ajouter(lignes, "Lot", nc.batchNumber());
        ajouter(lignes, "Signalé par", nc.reporterName());
        // Le NOMBRE de photos, et non les images : un 8D se relit des années plus
        // tard, et un PDF qui embarquerait les clichés terrain pèserait des dizaines
        // de mégaoctets pour des preuves qui vivent déjà dans la fiche.
        ajouter(lignes, "Photos du terrain",
                nc.photoCount() == 0 ? null : nc.photoCount() + " fichier(s)");
        ajouter(lignes, "Note de résolution", nc.resolutionNote());
        // D2 est toujours servi : la non-conformité existe, sinon rien de tout ceci
        // n'aurait été demandé.
        return section(EightDDiscipline.D2, true,
                "Non-conformité " + nc.reference(), lignes);
    }

    // ---------- D4 : Ishikawa, 5 pourquoi, cause racine de la NC ----------

    private EightDSnapshot.Section causeRacine(EightDSources sources) {
        List<String> lignes = new ArrayList<>();
        ajouter(lignes, "Cause racine consignée sur la non-conformité",
                sources.nc().rootCause());
        // La conclusion d'abord, le raisonnement ensuite : la note du D4 porte
        // l'hypothese ecartee et la raison de son abandon. Sans elle, le rapport
        // affirmait une cause sans montrer ce qui avait ete essaye avant --
        // c'est-a-dire ce qu'un auditeur demande des qu'il la conteste.
        ajouter(lignes, "Notes d'analyse consignées sur la non-conformité",
                sources.nc().causeAnalysisComment());

        // LES FIGURES DISENT, LE TEXTE NE REDIT PLUS. L'arête et la descente
        // étaient énumérées ligne à ligne : « MACHINES : jeu au palier »,
        // « Why 3 : … ». Un 8D se lit en revue, souvent debout, et cette
        // énumération ne montrait ni les branches ni la descente — c'est-à-dire
        // exactement ce que ces deux outils servent à voir. Le tracé remplace
        // donc le texte, il ne s'y ajoute pas : répéter l'un sous l'autre
        // doublerait la page sans rien apprendre.
        List<EightDSnapshot.Figure> figures = new ArrayList<>();
        for (EightDSources.CauseTree arbre : sources.ishikawas()) {
            figures.add(areteDePoisson(arbre));
        }
        for (EightDSources.WhysChain chaine : sources.fiveWhys()) {
            figures.add(descenteDesPourquoi(chaine));
        }

        boolean servi = !lignes.isEmpty() || !figures.isEmpty();
        String label = servi
                ? libelleD4(sources)
                : "Aucun Ishikawa et aucune analyse 5 pourquoi rattachés à cet écart, et"
                  + " aucune cause racine ni note d'analyse consignée sur la"
                  + " non-conformité";
        return new EightDSnapshot.Section(EightDDiscipline.D4.code(), EightDDiscipline.D4.titre(),
                servi, label, lignes, figures);
    }

    /**
     * L'arête de poisson d'un Ishikawa : une épine, des branches par catégorie.
     *
     * <p>Les causes gardent l'ordre du diagramme, et leur catégorie porte la
     * branche. Le niveau vaut 1 : l'arborescence des sous-causes n'est pas lue
     * ici — la source les livre à plat, et inventer une hiérarchie absente
     * dessinerait un diagramme que personne n'a construit.
     *
     * <p>La description et le score de cause racine ne passent PAS dans la
     * figure : une étiquette de branche doit se lire d'un coup d'œil. Ils
     * restent sur la fiche de l'écart, où on va les chercher.
     */
    private EightDSnapshot.Figure areteDePoisson(EightDSources.CauseTree arbre) {
        List<EightDSnapshot.Node> noeuds = new ArrayList<>();
        for (EightDSources.Cause cause : arbre.causes()) {
            noeuds.add(new EightDSnapshot.Node(
                    texteOu(cause.category(), "?"),
                    texteOu(cause.label(), "(sans intitulé)"),
                    1));
        }
        return new EightDSnapshot.Figure(EightDSnapshot.FigureKind.FISHBONE,
                texteOu(arbre.problemStatement(), "(sans énoncé)"), noeuds);
    }

    /**
     * La descente des 5 pourquoi : une échelle, du symptôme à la cause racine.
     *
     * <p>Le rang EST l'ordre : c'est lui qui fait la descente, et une réponse
     * sortie de son rang ne veut plus rien dire. La cause racine conclue ferme
     * l'échelle sous son propre groupe, pour que le tracé la distingue — une
     * descente qui s'arrête sans conclusion et une descente conclue ne se
     * lisent pas pareil.
     */
    private EightDSnapshot.Figure descenteDesPourquoi(EightDSources.WhysChain chaine) {
        List<EightDSnapshot.Node> noeuds = new ArrayList<>();
        int rang = 1;
        for (String reponse : chaine.answers()) {
            noeuds.add(new EightDSnapshot.Node(
                    "WHY", texteOu(reponse, "(sans réponse)"), rang++));
        }
        if (chaine.rootCause() != null && !chaine.rootCause().isBlank()) {
            noeuds.add(new EightDSnapshot.Node("ROOT", chaine.rootCause().trim(), rang));
        }
        return new EightDSnapshot.Figure(EightDSnapshot.FigureKind.WHY_CHAIN,
                texteOu(chaine.problem(), "(sans énoncé)"), noeuds);
    }

    private String libelleD4(EightDSources sources) {
        List<String> parts = new ArrayList<>();
        if (!sources.ishikawas().isEmpty()) {
            parts.add(sources.ishikawas().size() + " Ishikawa");
        }
        if (!sources.fiveWhys().isEmpty()) {
            parts.add(sources.fiveWhys().size() + " analyse(s) 5 pourquoi");
        }
        if (sources.nc().rootCause() != null && !sources.nc().rootCause().isBlank()) {
            parts.add("cause racine de la non-conformité");
        }
        if (sources.nc().causeAnalysisComment() != null
                && !sources.nc().causeAnalysisComment().isBlank()) {
            parts.add("notes d'analyse de la non-conformité");
        }
        return "Agrégé depuis : " + String.join(", ", parts);
    }

    // ---------- D5 : les actions décidées ----------

    private EightDSnapshot.Section actionsDecidees(List<EightDSources.Capa> capas) {
        if (capas.isEmpty()) {
            return section(EightDDiscipline.D5, false, sansCapa(), List.of());
        }
        // LE TABLEAU REMPLACE L'ENUMERATION, comme a l'ecran. Le D5 enumerait
        // ses actions en phrases -- « Action [CORRECTIVE] Remplacer le joint —
        // DONE — due 12/09 » --, c'est-a-dire les colonnes d'un tableau
        // aplaties dans une ligne de texte. On ne compare plus rien d'une
        // action a l'autre, et c'est pourtant ce qu'on vient faire dans cette
        // discipline. Les memes donnees, mises en colonnes, se lisent d'un
        // coup d'oeil.
        List<EightDSnapshot.Table> tableaux = tableauxDesActions(capas, NATURES_D5, false);
        int decidees = 0;
        for (EightDSnapshot.Table tableau : tableaux) {
            decidees += tableau.rows().size();
        }
        boolean servi = decidees > 0;
        String label = libelleCapas(capas) + (servi
                ? " \u2014 " + decidees + " action(s) décidée(s)"
                : " \u2014 aucune action décidée à ce jour");
        return new EightDSnapshot.Section(EightDDiscipline.D5.code(), EightDDiscipline.D5.titre(),
                servi, label, List.of(), List.of(), tableaux);
    }

    /**
     * Les natures d'action que chaque discipline montre, EXACTEMENT comme
     * l'ecran ({@code NATURES_PAR_DISCIPLINE}, cote web).
     *
     * <p>Le document emis doit se presenter comme l'ecran : une action lue sous
     * le D3 et absente du D3 imprime ferait douter du document -- et c'est lui
     * qui part chez le client.
     */
    private static final List<String> NATURES_D3 = List.of("CONTAINMENT");
    // CORRECTIVE SEULE. La prevention etait ici en plus du D7 : chaque action
    // preventive se lisait donc deux fois dans le meme rapport, sous deux
    // disciplines, comme si l'ecart avait ete traite deux fois. Le D5 supprime
    // la cause de l'ecart SURVENU ; le D7 previent celui qui ne l'est pas
    // encore (8D, ISO 9001 10.2). L'ecran a corrige le meme defaut.
    private static final List<String> NATURES_D5 = List.of("CORRECTIVE");
    private static final List<String> NATURES_D7 = List.of("PREVENTIVE");

    /** Les colonnes, dans l'ordre de l'ecran, moins celle des gestes. */
    private static final List<String> COLONNES_ACTIONS =
            List.of("Nature", "Action", "Responsable", "Échéance", "État", "Preuves");

    /**
     * LES MEMES, SANS LA COLONNE DES PREUVES : les colonnes du D5.
     *
     * <p>La piece d'une action atteste que CE geste-la a ete fait. Le D5
     * supprime la cause, et ce qu'il a a demontrer n'est pas « l'action a ete
     * faite » mais « le probleme a cesse » : cela se prouve au D6, sur le
     * dossier. L'ecran ne montre donc la colonne qu'au D3 et au D7, et ce
     * document se presente comme lui.
     */
    private static final List<String> COLONNES_ACTIONS_SANS_PREUVE =
            COLONNES_ACTIONS.subList(0, COLONNES_ACTIONS.size() - 1);

    /**
     * Les codes du modèle mis en mots, avec les libellés de l'écran.
     *
     * <p>« Catégorie : PRODUCT », « Gravité : MAJOR », « État : DONE » : le
     * rapport sortait les constantes de l'énumération, lisibles d'un
     * développeur et d'aucun destinataire. Les libellés sont ceux de la fiche,
     * mot pour mot, parce que le document doit se présenter comme elle.
     *
     * <p>Un code absent de la table se rend TEL QUEL. Une valeur ajoutée au
     * modèle sans passer ici doit se voir dans le document plutôt que d'y
     * disparaître — on perdrait une information en croyant la traduire.
     */
    private static final Map<String, String> CATEGORIES = Map.of(
            "PRODUCT", "Produit",
            "PROCESS", "Processus",
            "DOCUMENTATION", "Documentation",
            "SUPPLIER", "Fournisseur",
            "SAFETY", "Sécurité",
            "ENVIRONMENT", "Environnement",
            "OTHER", "Autre");

    private static final Map<String, String> GRAVITES = Map.of(
            "MINOR", "Mineure",
            "MAJOR", "Majeure",
            "CRITICAL", "Critique");

    private static final Map<String, String> ORIGINES = Map.of(
            "INTERNAL", "Interne",
            "EXTERNAL", "Externe");

    private static final Map<String, String> NATURES = Map.of(
            "CONTAINMENT", "Endiguement",
            "CORRECTIVE", "Corrective",
            "PREVENTIVE", "Préventive");

    private static final Map<String, String> ETATS = Map.of(
            "PENDING", "À faire",
            "IN_PROGRESS", "En cours",
            "DONE", "Faite");

    /** Le libellé du code, ou le code lui-même si la table ne le connaît pas. */
    private static String libelle(Map<String, String> libelles, String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        String propre = code.strip();
        return libelles.getOrDefault(propre, propre);
    }

    /**
     * Un tableau par dossier, limite aux natures de la discipline.
     *
     * <p>Un dossier dont aucune action n'est de cette nature n'ecrit pas de
     * tableau vide : un en-tete de colonnes sous un titre de dossier se lit
     * comme un dossier qu'on aurait ouvert sans rien y mettre.
     */
    private List<EightDSnapshot.Table> tableauxDesActions(List<EightDSources.Capa> capas,
                                                          List<String> natures) {
        return tableauxDesActions(capas, natures, true);
    }

    private List<EightDSnapshot.Table> tableauxDesActions(List<EightDSources.Capa> capas,
                                                          List<String> natures,
                                                          boolean colonnePreuve) {
        List<String> colonnes = colonnePreuve ? COLONNES_ACTIONS : COLONNES_ACTIONS_SANS_PREUVE;
        List<EightDSnapshot.Table> tableaux = new ArrayList<>();
        for (EightDSources.Capa capa : capas) {
            List<EightDSnapshot.Row> lignes = new ArrayList<>();
            for (EightDSources.Action action : capa.actions()) {
                if (!natures.contains(texteOu(action.actionType(), "?"))) {
                    continue;
                }
                List<String> cellules = new ArrayList<>(List.of(
                        libelle(NATURES, texteOu(action.actionType(), "?")),
                        texteOu(action.title(), "(sans intitulé)"),
                        texteOu(action.assigneeName(), "-"),
                        action.dueDate() == null ? "-" : jour(action.dueDate()),
                        libelle(ETATS, texteOu(action.status(), "?"))));
                if (colonnePreuve) {
                    cellules.add(action.evidenceCount() == 0
                            ? "-" : String.valueOf(action.evidenceCount()));
                }
                lignes.add(new EightDSnapshot.Row(List.copyOf(cellules)));
            }
            if (!lignes.isEmpty()) {
                tableaux.add(new EightDSnapshot.Table(
                        texteOu(capa.title(), "(sans intitulé)"), colonnes, lignes));
            }
        }
        return tableaux;
    }

    /**
     * \u00ab Escalated CAPA \u00bb, ou le COMPTE quand l'\u00e9cart en porte plusieurs.
     *
     * <p>Le singulier mentait d\u00e8s qu'un second dossier citait la r\u00e9f\u00e9rence de
     * l'\u00e9cart, et le lecteur du rapport n'avait aucun moyen de le savoir.
     */
    private String libelleCapas(List<EightDSources.Capa> capas) {
        return capas.size() == 1
                ? "Dossier CAPA escaladé"
                : capas.size() + " dossiers CAPA sur cette non-conformité";
    }

    // ---------- D6 : la mise en œuvre et ses preuves ----------

    private EightDSnapshot.Section miseEnOeuvre(List<EightDSources.Capa> capas) {
        if (capas.isEmpty()) {
            return section(EightDDiscipline.D6, false, sansCapa(), List.of());
        }
        List<String> lignes = new ArrayList<>();
        long menees = 0;
        long preuves = 0;
        for (EightDSources.Capa capa : capas) {
            List<String> duDossier = new ArrayList<>();
            preuves += capa.caseEvidenceCount();
            for (EightDSources.Action action : capa.actions()) {
                preuves += action.evidenceCount();
                if (action.completedAt() == null) {
                    continue;
                }
                menees++;
                duDossier.add(texteOu(action.title(), "(sans intitulé)")
                        + " \u2014 menée le " + horodatage(action.completedAt())
                        + " — " + action.evidenceCount() + " preuve(s)");
            }
            if (capa.caseEvidenceCount() > 0) {
                duDossier.add("Preuves déposées sur le dossier CAPA : "
                        + capa.caseEvidenceCount());
            }
            ajouter(duDossier, "Dossier CAPA clos le", horodatage(capa.closedAt()));
            if (Boolean.TRUE.equals(capa.effectivenessVerified())) {
                duDossier.add("Efficacité vérifiée le "
                        + horodatage(capa.effectivenessVerifiedAt()));
            }
            // Un dossier dont RIEN n'a encore \u00e9t\u00e9 men\u00e9 n'\u00e9crit pas son titre :
            // un intitul\u00e9 seul, sous une discipline de mise en \u0153uvre, se lit
            // comme un travail fait.
            if (duDossier.isEmpty()) {
                continue;
            }
            if (!lignes.isEmpty()) {
                lignes.add("");
            }
            if (capas.size() > 1) {
                lignes.add(texteOu(capa.title(), "(sans intitulé)") + " :");
            }
            lignes.addAll(duDossier);
        }
        boolean servi = menees > 0 || preuves > 0;
        String label = servi
                ? libelleCapas(capas) + " \u2014 " + menees + " action(s) menée(s), "
                  + preuves + " preuve(s)"
                : "Aucune action menée et aucune preuve déposée sur le dossier CAPA"
                  + " escaladé";
        // AUCUN TABLEAU D'ACTIONS ICI. Le D6 portait celui des actions
        // préventives, qui est le D7 — l'écran l'a corrigé, le document
        // l'ignorait encore : le client lisait la prévention sous « mise en
        // œuvre et preuves », et le D7 se déclarait sans contenu juste en
        // dessous. Le D6 dit ce qui a été mené et prouvé ; les gestes de
        // prévention se lisent sous la discipline qui les porte.
        return section(EightDDiscipline.D6, servi, label, lignes);
    }

    private String sansCapa() {
        return "Aucun dossier CAPA n'a été escaladé depuis cette non-conformité :"
               + " rien à agréger";
    }

    // ---------- D7 : ce qui empêche le retour de l'écart ----------

    private EightDSnapshot.Section prevention(EightDSources sources) {
        List<String> lignes = new ArrayList<>();
        EightDSources.Fmea fmea = sources.fmea();
        if (fmea != null) {
            lignes.add("AMDEC procédé \u2014 mode de défaillance : "
                    + texteOu(fmea.failureMode(), "(non décrit)"));
            ajouter(lignes, "    Effet", fmea.failureEffect());
            ajouter(lignes, "    Cause", fmea.failureCause());
            ajouter(lignes, "    Contrôles en place", fmea.currentControls());
            lignes.add("    IPR : " + fmea.rpn()
                    + (fmea.rpnAfter() == null
                       ? "" : " → " + fmea.rpnAfter() + " après actions"));
            ajouter(lignes, "    Priorité d'action", fmea.actionPriority());
            ajouter(lignes, "    Action recommandée", fmea.recommendedAction());
            ajouter(lignes, "    Actions menées", fmea.actionsTaken());
        }
        for (EightDSources.Surveillance plan : sources.surveillance()) {
            lignes.add("Plan de surveillance " + texteOu(plan.code(), "(sans code)")
                    + " rév. " + plan.revision()
                    + " — " + texteOu(plan.phase(), "?")
                    + " — " + texteOu(plan.status(), "?")
                    + " — " + plan.lineCount() + " ligne(s)"
                    + (plan.sealSha256() == null ? "" : " \u2014 scellé"));
        }
        // LES ACTIONS PRÉVENTIVES SONT LE D7, comme à l'écran. Elles étaient
        // tabulées sous le D6, et le D7 ne lisait que l'AMDEC et le plan de
        // surveillance : un écart dont l'équipe avait décidé une prévention —
        // sans toucher à l'AMDEC ni au plan — sortait un rapport qui se
        // déclarait « partiel, disciplines sans contenu : D7 » tout en
        // imprimant cette prévention une page plus haut.
        List<EightDSnapshot.Table> tableaux = tableauxDesActions(sources.capas(), NATURES_D7);
        boolean servi = !lignes.isEmpty() || !tableaux.isEmpty();
        String label = servi
                ? libelleD7(fmea, sources.surveillance().size(), tableaux, sources.capas())
                // Dit ce qui manque, et rien de plus. La phrase qui expliquait
                // pourquoi le Poka-Yoke n'y figurait pas — un dispositif se
                // rattache à un projet DMAIC, pas à un écart — exposait une
                // limite d'outillage interne dans un document remis au client,
                // là où il attend un constat de prévention.
                : "Aucune action préventive décidée sur cet écart, aucun mode de"
                  + " défaillance AMDEC rattaché, et aucun plan de surveillance sur le"
                  + " produit concerné.";
        return new EightDSnapshot.Section(
                EightDDiscipline.D7.code(), EightDDiscipline.D7.titre(),
                servi, label, lignes, List.of(), tableaux);
    }

    /**
     * D'où vient le D7 : les actions préventives d'abord, puis le référentiel.
     *
     * <p>L'ordre suit ce que le lecteur voit : le tableau des gestes est en
     * tête de la discipline, et le libellé de source ne doit pas annoncer
     * l'AMDEC pour un rapport qui n'en porte pas.
     */
    private String libelleD7(EightDSources.Fmea fmea, int plans,
                             List<EightDSnapshot.Table> tableaux,
                             List<EightDSources.Capa> capas) {
        List<String> parts = new ArrayList<>();
        int preventives = 0;
        for (EightDSnapshot.Table tableau : tableaux) {
            preventives += tableau.rows().size();
        }
        if (preventives > 0) {
            parts.add(libelleCapas(capas) + " — " + preventives
                      + " action(s) préventive(s)");
        }
        if (fmea != null) {
            parts.add("mode de défaillance AMDEC");
        }
        if (plans > 0) {
            parts.add(plans + " plan(s) de surveillance");
        }
        return "Agrégé depuis : " + String.join(", ", parts);
    }

    // ---------- fabrique de lignes ----------

    private EightDSnapshot.Section section(EightDDiscipline discipline, boolean servi,
                                          String label, List<String> lignes) {
        return new EightDSnapshot.Section(
                discipline.code(), discipline.titre(), servi, label, lignes);
    }

    /** N'ajoute rien quand la valeur est absente : une étiquette sans valeur ne dit rien. */
    private void ajouter(List<String> lignes, String etiquette, String valeur) {
        if (valeur != null && !valeur.isBlank()) {
            lignes.add(etiquette + " : " + valeur.trim());
        }
    }

    /** Une saisie multiligne devient autant de lignes, les vides retirées. */
    private List<String> lignesDe(String texte) {
        if (texte == null || texte.isBlank()) {
            return List.of();
        }
        List<String> lignes = new ArrayList<>();
        for (String brute : texte.split("\\R")) {
            String ligne = brute.strip();
            if (!ligne.isEmpty()) {
                lignes.add(ligne);
            }
        }
        return lignes;
    }

    private String horodatage(Instant instant) {
        return instant == null ? null : HORODATAGE.format(instant);
    }

    private String jour(LocalDate date) {
        return date == null ? null : JOUR.format(date);
    }

    private String texteOu(String valeur, String defaut) {
        return valeur == null || valeur.isBlank() ? defaut : valeur.trim();
    }
}

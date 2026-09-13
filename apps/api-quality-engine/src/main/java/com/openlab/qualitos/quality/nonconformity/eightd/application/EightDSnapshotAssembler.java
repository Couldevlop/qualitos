package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDDiscipline;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    private static final String A_SAISIR =
            "Aucune source dans la plateforme — cette discipline se saisit";
    private static final String SAISI = "Saisi par l'équipe qualité";

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
                saisie(EightDDiscipline.D1, team),
                probleme(sources.nc()),
                saisie(EightDDiscipline.D3, containment),
                causeRacine(sources),
                actionsDecidees(sources.capa()),
                miseEnOeuvre(sources.capa()),
                prevention(sources),
                saisie(EightDDiscipline.D8, recognition));

        boolean partiel = sections.stream().anyMatch(s -> !s.sourced());
        return new EightDSnapshot(
                sources.nc().reference(),
                sources.nc().title(),
                tenantLabel,
                issuedAt == null ? null : HORODATAGE.format(issuedAt),
                issuedByName,
                partiel,
                sections);
    }

    // ---------- D1, D3, D8 : ce que personne d'autre ne sait ----------

    private EightDSnapshot.Section saisie(EightDDiscipline discipline, String texte) {
        List<String> lignes = lignesDe(texte);
        return section(discipline, !lignes.isEmpty(), lignes.isEmpty() ? A_SAISIR : SAISI, lignes);
    }

    // ---------- D2 : la non-conformité ----------

    private EightDSnapshot.Section probleme(EightDSources.Nc nc) {
        List<String> lignes = new ArrayList<>();
        ajouter(lignes, "Référence", nc.reference());
        ajouter(lignes, "Intitulé", nc.title());
        ajouter(lignes, "Constat", nc.description());
        ajouter(lignes, "Catégorie", nc.category());
        ajouter(lignes, "Gravité", nc.severity());
        ajouter(lignes, "Origine", nc.origin());
        ajouter(lignes, "Détecté le", horodatage(nc.detectedAt()));
        ajouter(lignes, "Clôturé le", horodatage(nc.closedAt()));
        ajouter(lignes, "Zone", nc.zone());
        ajouter(lignes, "Signalé par", nc.reporterName());
        // Le NOMBRE de photos, et non les images : un 8D se relit des années plus
        // tard, et un PDF qui embarquerait les clichés terrain pèserait des dizaines
        // de mégaoctets pour des preuves qui vivent déjà dans la fiche.
        ajouter(lignes, "Photos terrain", nc.photoCount() == 0 ? null : nc.photoCount() + " pièce(s)");
        ajouter(lignes, "Note de résolution", nc.resolutionNote());
        // D2 est toujours servi : la non-conformité existe, sinon rien de tout ceci
        // n'aurait été demandé.
        return section(EightDDiscipline.D2, true,
                "Non-conformité " + nc.reference(), lignes);
    }

    // ---------- D4 : Ishikawa, 5 pourquoi, cause racine de la NC ----------

    private EightDSnapshot.Section causeRacine(EightDSources sources) {
        List<String> lignes = new ArrayList<>();
        ajouter(lignes, "Cause racine retenue sur la non-conformité", sources.nc().rootCause());

        for (EightDSources.CauseTree arbre : sources.ishikawas()) {
            lignes.add("Ishikawa — " + texteOu(arbre.problemStatement(), "(sans énoncé)")
                    + " [" + texteOu(arbre.status(), "?") + "]");
            for (EightDSources.Cause cause : arbre.causes()) {
                StringBuilder ligne = new StringBuilder("    ")
                        .append(texteOu(cause.category(), "?")).append(" : ")
                        .append(texteOu(cause.label(), "(sans libellé)"));
                if (cause.description() != null && !cause.description().isBlank()) {
                    ligne.append(" — ").append(cause.description().trim());
                }
                if (cause.rootCauseScore() != null) {
                    ligne.append(" (score cause racine ")
                            .append(String.format(java.util.Locale.ROOT, "%.2f", cause.rootCauseScore()))
                            .append(')');
                }
                lignes.add(ligne.toString());
            }
        }

        for (EightDSources.WhysChain chaine : sources.fiveWhys()) {
            lignes.add("5 pourquoi — " + texteOu(chaine.problem(), "(sans énoncé)"));
            int rang = 1;
            for (String reponse : chaine.answers()) {
                lignes.add("    Pourquoi " + rang++ + " : " + texteOu(reponse, "(sans réponse)"));
            }
            if (chaine.rootCause() != null && !chaine.rootCause().isBlank()) {
                lignes.add("    Cause racine : " + chaine.rootCause().trim());
            }
        }

        boolean servi = !lignes.isEmpty();
        String label = servi
                ? libelleD4(sources)
                : "Aucun Ishikawa ni analyse 5 pourquoi rattachés à cet écart, et aucune"
                  + " cause racine saisie sur la non-conformité";
        return section(EightDDiscipline.D4, servi, label, lignes);
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
        return "Agrégé depuis : " + String.join(", ", parts);
    }

    // ---------- D5 : les actions décidées ----------

    private EightDSnapshot.Section actionsDecidees(EightDSources.Capa capa) {
        if (capa == null) {
            return section(EightDDiscipline.D5, false, sansCapa(), List.of());
        }
        List<String> lignes = new ArrayList<>();
        ajouter(lignes, "CAPA", capa.title());
        ajouter(lignes, "Type", capa.type());
        ajouter(lignes, "Criticité", capa.criticity());
        ajouter(lignes, "Statut", capa.status());
        ajouter(lignes, "Échéance", jour(capa.dueDate()));
        for (EightDSources.Action action : capa.actions()) {
            lignes.add("Action [" + texteOu(action.actionType(), "?") + "] "
                    + texteOu(action.title(), "(sans intitulé)")
                    + " — " + texteOu(action.status(), "?")
                    + (action.assigneeName() == null ? "" : " — " + action.assigneeName())
                    + (action.dueDate() == null ? "" : " — échéance " + jour(action.dueDate())));
            if (action.description() != null && !action.description().isBlank()) {
                lignes.add("    " + action.description().trim());
            }
        }
        boolean servi = !capa.actions().isEmpty();
        String label = servi
                ? "CAPA escaladée — " + capa.actions().size() + " action(s) décidée(s)"
                : "La CAPA escaladée ne porte encore aucune action";
        return section(EightDDiscipline.D5, servi, label, lignes);
    }

    // ---------- D6 : la mise en œuvre et ses preuves ----------

    private EightDSnapshot.Section miseEnOeuvre(EightDSources.Capa capa) {
        if (capa == null) {
            return section(EightDDiscipline.D6, false, sansCapa(), List.of());
        }
        List<String> lignes = new ArrayList<>();
        long menees = 0;
        long preuves = capa.caseEvidenceCount();
        for (EightDSources.Action action : capa.actions()) {
            preuves += action.evidenceCount();
            if (action.completedAt() == null) {
                continue;
            }
            menees++;
            lignes.add(texteOu(action.title(), "(sans intitulé)")
                    + " — menée à terme le " + horodatage(action.completedAt())
                    + " — " + action.evidenceCount() + " preuve(s)");
        }
        if (capa.caseEvidenceCount() > 0) {
            lignes.add("Preuves versées au dossier CAPA : " + capa.caseEvidenceCount());
        }
        ajouter(lignes, "CAPA clôturée le", horodatage(capa.closedAt()));
        if (Boolean.TRUE.equals(capa.effectivenessVerified())) {
            lignes.add("Efficacité vérifiée le " + horodatage(capa.effectivenessVerifiedAt()));
        }
        boolean servi = menees > 0 || preuves > 0;
        String label = servi
                ? "CAPA escaladée — " + menees + " action(s) menée(s) à terme, " + preuves + " preuve(s)"
                : "Aucune action menée à terme et aucune preuve versée sur la CAPA escaladée";
        return section(EightDDiscipline.D6, servi, label, lignes);
    }

    private String sansCapa() {
        return "Aucune CAPA n'a été escaladée depuis cette non-conformité : rien à agréger";
    }

    // ---------- D7 : ce qui empêche le retour de l'écart ----------

    private EightDSnapshot.Section prevention(EightDSources sources) {
        List<String> lignes = new ArrayList<>();
        EightDSources.Fmea fmea = sources.fmea();
        if (fmea != null) {
            lignes.add("PFMEA — mode de défaillance : " + texteOu(fmea.failureMode(), "(non décrit)"));
            ajouter(lignes, "    Effet", fmea.failureEffect());
            ajouter(lignes, "    Cause", fmea.failureCause());
            ajouter(lignes, "    Maîtrise en place", fmea.currentControls());
            lignes.add("    RPN : " + fmea.rpn()
                    + (fmea.rpnAfter() == null ? "" : " → " + fmea.rpnAfter() + " après actions"));
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
                    + (plan.sealSha256() == null ? "" : " — scellé"));
        }
        boolean servi = !lignes.isEmpty();
        String label = servi
                ? libelleD7(fmea, sources.surveillance().size())
                // Dit explicitement ce qui n'existe pas ET pourquoi le Poka-Yoke
                // n'y figure pas : un dispositif se rattache aujourd'hui à un projet
                // DMAIC, pas à un écart. Le taire laisserait croire qu'on l'a cherché.
                : "Aucun mode de défaillance PFMEA rattaché à l'écart et aucun plan de"
                  + " surveillance sur le produit concerné. Les dispositifs Poka-Yoke se"
                  + " rattachent à un projet DMAIC et non à une non-conformité : ils ne"
                  + " sont pas agrégeables ici.";
        return section(EightDDiscipline.D7, servi, label, lignes);
    }

    private String libelleD7(EightDSources.Fmea fmea, int plans) {
        List<String> parts = new ArrayList<>();
        if (fmea != null) {
            parts.add("mode de défaillance PFMEA");
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

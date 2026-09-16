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
            "No source in the platform \u2014 this discipline is filled in by hand";
    private static final String SAISI = "Filled in by the quality team";

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
        ajouter(lignes, "Reference", nc.reference());
        ajouter(lignes, "Title", nc.title());
        ajouter(lignes, "Finding", nc.description());
        ajouter(lignes, "Category", nc.category());
        ajouter(lignes, "Severity", nc.severity());
        ajouter(lignes, "Origin", nc.origin());
        ajouter(lignes, "Detected on", horodatage(nc.detectedAt()));
        ajouter(lignes, "Closed on", horodatage(nc.closedAt()));
        ajouter(lignes, "Area", nc.zone());
        ajouter(lignes, "Reported by", nc.reporterName());
        // Le NOMBRE de photos, et non les images : un 8D se relit des années plus
        // tard, et un PDF qui embarquerait les clichés terrain pèserait des dizaines
        // de mégaoctets pour des preuves qui vivent déjà dans la fiche.
        ajouter(lignes, "Field photos", nc.photoCount() == 0 ? null : nc.photoCount() + " file(s)");
        ajouter(lignes, "Resolution note", nc.resolutionNote());
        // D2 est toujours servi : la non-conformité existe, sinon rien de tout ceci
        // n'aurait été demandé.
        return section(EightDDiscipline.D2, true,
                "Non-conformity " + nc.reference(), lignes);
    }

    // ---------- D4 : Ishikawa, 5 pourquoi, cause racine de la NC ----------

    private EightDSnapshot.Section causeRacine(EightDSources sources) {
        List<String> lignes = new ArrayList<>();
        ajouter(lignes, "Root cause recorded on the non-conformity", sources.nc().rootCause());

        for (EightDSources.CauseTree arbre : sources.ishikawas()) {
            lignes.add("Ishikawa \u2014 " + texteOu(arbre.problemStatement(), "(no statement)")
                    + " [" + texteOu(arbre.status(), "?") + "]");
            for (EightDSources.Cause cause : arbre.causes()) {
                StringBuilder ligne = new StringBuilder("    ")
                        .append(texteOu(cause.category(), "?")).append(" : ")
                        .append(texteOu(cause.label(), "(no label)"));
                if (cause.description() != null && !cause.description().isBlank()) {
                    ligne.append(" — ").append(cause.description().trim());
                }
                if (cause.rootCauseScore() != null) {
                    ligne.append(" (root-cause score ")
                            .append(String.format(java.util.Locale.ROOT, "%.2f", cause.rootCauseScore()))
                            .append(')');
                }
                lignes.add(ligne.toString());
            }
        }

        for (EightDSources.WhysChain chaine : sources.fiveWhys()) {
            lignes.add("5 whys \u2014 " + texteOu(chaine.problem(), "(no statement)"));
            int rang = 1;
            for (String reponse : chaine.answers()) {
                lignes.add("    Why " + rang++ + " : " + texteOu(reponse, "(no answer)"));
            }
            if (chaine.rootCause() != null && !chaine.rootCause().isBlank()) {
                lignes.add("    Root cause: " + chaine.rootCause().trim());
            }
        }

        boolean servi = !lignes.isEmpty();
        String label = servi
                ? libelleD4(sources)
                : "No Ishikawa and no 5-whys analysis linked to this deviation, and no"
                  + " root cause recorded on the non-conformity";
        return section(EightDDiscipline.D4, servi, label, lignes);
    }

    private String libelleD4(EightDSources sources) {
        List<String> parts = new ArrayList<>();
        if (!sources.ishikawas().isEmpty()) {
            parts.add(sources.ishikawas().size() + " Ishikawa");
        }
        if (!sources.fiveWhys().isEmpty()) {
            parts.add(sources.fiveWhys().size() + " 5-whys analysis(es)");
        }
        if (sources.nc().rootCause() != null && !sources.nc().rootCause().isBlank()) {
            parts.add("root cause of the non-conformity");
        }
        return "Aggregated from: " + String.join(", ", parts);
    }

    // ---------- D5 : les actions décidées ----------

    private EightDSnapshot.Section actionsDecidees(EightDSources.Capa capa) {
        if (capa == null) {
            return section(EightDDiscipline.D5, false, sansCapa(), List.of());
        }
        List<String> lignes = new ArrayList<>();
        ajouter(lignes, "CAPA", capa.title());
        ajouter(lignes, "Type", capa.type());
        ajouter(lignes, "Criticality", capa.criticity());
        ajouter(lignes, "Status", capa.status());
        ajouter(lignes, "Due date", jour(capa.dueDate()));
        for (EightDSources.Action action : capa.actions()) {
            lignes.add("Action [" + texteOu(action.actionType(), "?") + "] "
                    + texteOu(action.title(), "(no title)")
                    + " — " + texteOu(action.status(), "?")
                    + (action.assigneeName() == null ? "" : " — " + action.assigneeName())
                    + (action.dueDate() == null ? "" : " \u2014 due " + jour(action.dueDate())));
            if (action.description() != null && !action.description().isBlank()) {
                lignes.add("    " + action.description().trim());
            }
        }
        boolean servi = !capa.actions().isEmpty();
        String label = servi
                ? "Escalated CAPA \u2014 " + capa.actions().size() + " action(s) decided"
                : "The escalated CAPA carries no action yet";
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
            lignes.add(texteOu(action.title(), "(no title)")
                    + " \u2014 completed on " + horodatage(action.completedAt())
                    + " — " + action.evidenceCount() + " evidence item(s)");
        }
        if (capa.caseEvidenceCount() > 0) {
            lignes.add("Evidence filed on the CAPA case: " + capa.caseEvidenceCount());
        }
        ajouter(lignes, "CAPA closed on", horodatage(capa.closedAt()));
        if (Boolean.TRUE.equals(capa.effectivenessVerified())) {
            lignes.add("Effectiveness verified on " + horodatage(capa.effectivenessVerifiedAt()));
        }
        boolean servi = menees > 0 || preuves > 0;
        String label = servi
                ? "Escalated CAPA \u2014 " + menees + " action(s) completed, " + preuves + " evidence item(s)"
                : "No completed action and no evidence filed on the escalated CAPA";
        return section(EightDDiscipline.D6, servi, label, lignes);
    }

    private String sansCapa() {
        return "No CAPA has been escalated from this non-conformity: nothing to aggregate";
    }

    // ---------- D7 : ce qui empêche le retour de l'écart ----------

    private EightDSnapshot.Section prevention(EightDSources sources) {
        List<String> lignes = new ArrayList<>();
        EightDSources.Fmea fmea = sources.fmea();
        if (fmea != null) {
            lignes.add("PFMEA \u2014 failure mode: " + texteOu(fmea.failureMode(), "(not described)"));
            ajouter(lignes, "    Effect", fmea.failureEffect());
            ajouter(lignes, "    Cause", fmea.failureCause());
            ajouter(lignes, "    Controls in place", fmea.currentControls());
            lignes.add("    RPN: " + fmea.rpn()
                    + (fmea.rpnAfter() == null ? "" : " → " + fmea.rpnAfter() + " after actions"));
            ajouter(lignes, "    Action priority", fmea.actionPriority());
            ajouter(lignes, "    Recommended action", fmea.recommendedAction());
            ajouter(lignes, "    Actions taken", fmea.actionsTaken());
        }
        for (EightDSources.Surveillance plan : sources.surveillance()) {
            lignes.add("Control plan " + texteOu(plan.code(), "(no code)")
                    + " rev. " + plan.revision()
                    + " — " + texteOu(plan.phase(), "?")
                    + " — " + texteOu(plan.status(), "?")
                    + " — " + plan.lineCount() + " line(s)"
                    + (plan.sealSha256() == null ? "" : " \u2014 sealed"));
        }
        boolean servi = !lignes.isEmpty();
        String label = servi
                ? libelleD7(fmea, sources.surveillance().size())
                // Dit explicitement ce qui n'existe pas ET pourquoi le Poka-Yoke
                // n'y figure pas : un dispositif se rattache aujourd'hui à un projet
                // DMAIC, pas à un écart. Le taire laisserait croire qu'on l'a cherché.
                : "No PFMEA failure mode linked to the deviation, and no control plan on"
                  + " the product concerned. Poka-Yoke devices attach to a DMAIC project"
                  + " and not to a non-conformity: they cannot be aggregated here.";
        return section(EightDDiscipline.D7, servi, label, lignes);
    }

    private String libelleD7(EightDSources.Fmea fmea, int plans) {
        List<String> parts = new ArrayList<>();
        if (fmea != null) {
            parts.add("PFMEA failure mode");
        }
        if (plans > 0) {
            parts.add(plans + " control plan(s)");
        }
        return "Aggregated from: " + String.join(", ", parts);
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

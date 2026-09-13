package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.NcStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Ce que les autres modules savent déjà d'une non-conformité, rassemblé pour le
 * rapport 8D.
 *
 * <p>Le rapport <b>agrège</b>, il ne recopie pas : cinq des huit disciplines
 * n'ont aucune donnée propre, elles relisent la non-conformité, les analyses de
 * cause, la CAPA escaladée, le mode de défaillance PFMEA et les plans de
 * surveillance. C'est le même parti que le dossier PPAP du cycle APQP — tenir une
 * seconde liste, c'est garantir qu'elle divergera.
 *
 * <p>Les champs absents sont {@code null} ou vides, et ce vide est une
 * information : il dit qu'une discipline n'a pas de source, ce que le rapport
 * annonce au lieu de le taire.
 */
public record EightDSources(
        Nc nc,
        List<CauseTree> ishikawas,
        List<WhysChain> fiveWhys,
        Capa capa,
        Fmea fmea,
        List<Surveillance> surveillance) {

    public EightDSources {
        Objects.requireNonNull(nc, "nc");
        ishikawas = List.copyOf(Objects.requireNonNull(ishikawas, "ishikawas"));
        fiveWhys = List.copyOf(Objects.requireNonNull(fiveWhys, "fiveWhys"));
        surveillance = List.copyOf(Objects.requireNonNull(surveillance, "surveillance"));
    }

    /** La non-conformité elle-même — la source de D2, et l'arbitre de la clôture. */
    public record Nc(
            String reference,
            String title,
            String description,
            String category,
            String severity,
            String origin,
            NcStatus status,
            Instant detectedAt,
            Instant closedAt,
            String zone,
            String reporterName,
            int photoCount,
            String rootCause,
            String resolutionNote) {
    }

    /** Un Ishikawa parti de cette non-conformité, et ses causes. */
    public record CauseTree(String problemStatement, String status, List<Cause> causes) {

        public CauseTree {
            causes = List.copyOf(Objects.requireNonNull(causes, "causes"));
        }
    }

    /** Une cause, avec sa branche et, le cas échéant, son score de cause racine. */
    public record Cause(String category, String label, String description, Double rootCauseScore) {
    }

    /** Une analyse 5 pourquoi, ses réponses dans l'ordre, et sa conclusion. */
    public record WhysChain(String problem, String rootCause, List<String> answers) {

        public WhysChain {
            answers = List.copyOf(Objects.requireNonNull(answers, "answers"));
        }
    }

    /** La CAPA escaladée depuis la non-conformité : source de D5 et de D6. */
    public record Capa(
            String title,
            String type,
            String criticity,
            String status,
            LocalDate dueDate,
            Instant closedAt,
            Boolean effectivenessVerified,
            Instant effectivenessVerifiedAt,
            long caseEvidenceCount,
            List<Action> actions) {

        public Capa {
            actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        }
    }

    /** Une action de la CAPA, avec ce qui prouve son exécution. */
    public record Action(
            String title,
            String description,
            String actionType,
            String status,
            String assigneeName,
            LocalDate dueDate,
            Instant completedAt,
            long evidenceCount) {
    }

    /** Le mode de défaillance PFMEA que l'écart a illustré : source de D7. */
    public record Fmea(
            String failureMode,
            String failureEffect,
            String failureCause,
            String currentControls,
            int rpn,
            Integer rpnAfter,
            String actionPriority,
            String recommendedAction,
            String actionsTaken) {
    }

    /** Un plan de surveillance du produit concerné : l'autre source de D7. */
    public record Surveillance(
            String code,
            int revision,
            String phase,
            String status,
            int lineCount,
            String sealSha256) {
    }
}

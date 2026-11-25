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
        /**
         * L'équipe nommée sur la fiche, dans son ordre de saisie — la source de
         * D1. Le premier nommé pilote, par convention d'atelier.
         *
         * <p>Vide pour un écart né d'un constat d'audit ou d'un import système :
         * personne n'a encore été désigné, et le rapport le dit.
         */
        List<Member> team,
        List<CauseTree> ishikawas,
        List<WhysChain> fiveWhys,
        /**
         * TOUS les dossiers CAPA qui traitent cet écart, du plus ancien au plus
         * récent — pas seulement celui que la fiche porte en lien.
         *
         * <p>Un écart peut en avoir plusieurs : le premier est ouvert depuis sa
         * fiche, et un second ouvert depuis la liste en citant sa référence ne
         * remplace pas ce lien (ADR 0121). La résolution et la clôture de
         * l'écart les attendent tous ; n'en lire qu'un ici faisait disparaître
         * du rapport remis au client les actions réellement menées sur l'écart,
         * et le rapport contredisait la garde qui bloquait sa clôture.
         */
        List<Capa> capas,
        Fmea fmea,
        List<Surveillance> surveillance) {

    public EightDSources {
        Objects.requireNonNull(nc, "nc");
        team = List.copyOf(Objects.requireNonNull(team, "team"));
        ishikawas = List.copyOf(Objects.requireNonNull(ishikawas, "ishikawas"));
        fiveWhys = List.copyOf(Objects.requireNonNull(fiveWhys, "fiveWhys"));
        capas = List.copyOf(Objects.requireNonNull(capas, "capas"));
        surveillance = List.copyOf(Objects.requireNonNull(surveillance, "surveillance"));
    }

    /**
     * Un membre de l'équipe nommée sur la fiche : son nom, son adresse.
     *
     * <p>Le nom est celui FIGÉ à la saisie, et non une relecture de l'annuaire :
     * un rapport relu trois ans plus tard porte les noms de l'époque. L'adresse,
     * elle, vient de l'annuaire au moment où le rapport s'assemble — la fiche ne
     * la connaît pas — et se fige comme le reste à l'émission.
     *
     * <p>{@code email} vaut {@code null} quand l'annuaire ne connaît pas le
     * compte, ou quand il n'a pas répondu : le tableau du D1 laisse alors la
     * case vide plutôt que d'inventer une adresse.
     */
    public record Member(String name, String email) {
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
            String resolutionNote,
            /**
             * La note libre de l'analyse des causes (D4 de la fiche).
             *
             * <p>Distincte de {@link #rootCause}, qui est la conclusion : la note
             * porte le raisonnement autour -- l'hypothese ecartee, et pourquoi.
             * C'est ce qu'un lecteur du rapport vient chercher quand la cause
             * retenue le surprend.
             */
            String causeAnalysisComment,
            /**
             * Le client qui reclame, le lot incrimine, le produit vise.
             *
             * <p>Ils s'ecrivent sur la fiche depuis qu'on a cesse de les noyer
             * dans la description, mais le rapport -- la piece que le client
             * recoit -- ne les portait pas. Un 8D de reclamation qui ne nomme
             * ni le client, ni le lot, ni le produit oblige son lecteur a
             * revenir chercher dans l'outil ce que le document aurait du dire.
             *
             * <p>Le produit arrive DEJA REDIGE (« code -- designation ») :
             * l'assembleur ne lit pas de referentiel, c'est l'adaptateur qui
             * connait les chemins de liaison.
             */
            String customerName,
            String batchNumber,
            String productLabel) {
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
            /**
             * Le nom des pièces déposées sur le DOSSIER, dans l'ordre du dépôt.
             *
             * <p>Le D6 ne dit plus « 3 preuve(s) » : il nomme les pièces. Un
             * compte ne se vérifie pas — le lecteur ne sait pas ce qu'on a
             * joint —, un nom de fichier se retrouve dans le dossier.
             */
            List<String> caseEvidenceNames,
            List<Action> actions) {

        public Capa {
            caseEvidenceNames = List.copyOf(
                    Objects.requireNonNull(caseEvidenceNames, "caseEvidenceNames"));
            actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        }

        /** Combien de pièces le dossier porte : la liste le dit, personne d'autre. */
        public long caseEvidenceCount() {
            return caseEvidenceNames.size();
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
            /** Le nom des pièces de CETTE action, dans l'ordre du dépôt. */
            List<String> evidenceNames) {

        public Action {
            evidenceNames = List.copyOf(
                    Objects.requireNonNull(evidenceNames, "evidenceNames"));
        }

        /**
         * Combien de pièces l'action porte.
         *
         * <p>Déduit de la liste, et non porté à côté d'elle : deux champs pour
         * le même fait finissent par se contredire, et c'est un document signé
         * qui en porterait la contradiction.
         */
        public long evidenceCount() {
            return evidenceNames.size();
        }
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

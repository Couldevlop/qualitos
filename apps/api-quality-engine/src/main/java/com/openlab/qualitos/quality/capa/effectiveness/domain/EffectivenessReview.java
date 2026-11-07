package com.openlab.qualitos.quality.capa.effectiveness.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Une évaluation d'efficacité : le jugement, et ce sur quoi il s'appuie
 * (ADR 0108).
 *
 * <p>Immuable, et versée à un historique plutôt qu'écrasée. Un dossier jugé
 * inefficace repart en traitement, reçoit une action, et se fait réévaluer. La
 * suite des verdicts — inefficace, puis partiel, puis efficace — est exactement
 * ce qu'un auditeur lit : elle montre un système qui corrige. Un champ écrasé ne
 * montrerait que la fin, et donnerait du dossier une image de réussite immédiate
 * qui est fausse.
 *
 * <p><b>Le critère est recopié</b> depuis le dossier au moment du jugement.
 * Modifier le critère d'un dossier ne doit pas réécrire celui sur lequel une
 * évaluation passée s'est prononcée.
 *
 * <p><b>Les trois relevés sont du texte court</b>, pas des nombres : « 4,2 % »,
 * « ≤ 2 g », « 0 retard sur 620 expéditions ». L'unité, la tolérance et
 * l'assiette font partie du relevé, et les séparer obligerait à inventer un
 * référentiel d'unités que chaque secteur contredirait. Aucun indicateur ne s'en
 * déduit — c'est le verdict qui alimente les KPI, et lui est énuméré.
 */
public final class EffectivenessReview {

    /** Les relevés tiennent sur une ligne de tableau ; au-delà, c'est un commentaire. */
    public static final int MAX_READING = 120;
    public static final int MAX_CRITERION = 2000;
    public static final int MAX_COMMENT = 4000;
    public static final int MAX_REVIEWER_NAME = 255;

    private final UUID id;
    private final UUID tenantId;
    private final UUID capaCaseId;
    private final EffectivenessVerdict verdict;
    private final String criterion;
    private final String baseline;
    private final String target;
    private final String observed;
    private final String comment;
    private final UUID reviewerId;
    private final String reviewerName;
    private final Instant reviewedAt;

    private EffectivenessReview(UUID id, UUID tenantId, UUID capaCaseId,
                                EffectivenessVerdict verdict, String criterion,
                                String baseline, String target, String observed,
                                String comment, UUID reviewerId, String reviewerName,
                                Instant reviewedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.capaCaseId = capaCaseId;
        this.verdict = verdict;
        this.criterion = criterion;
        this.baseline = baseline;
        this.target = target;
        this.observed = observed;
        this.comment = comment;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewedAt = reviewedAt;
    }

    /**
     * Enregistre un jugement, en refusant ce qui le rendrait indéfendable.
     *
     * @param criterion    le critère tel qu'il figurait sur le dossier ;
     *                     obligatoire — juger sans critère annoncé, c'est
     *                     décider du critère en regardant le résultat
     * @param observed     ce qui a été constaté ; obligatoire — on juge sur un
     *                     relevé, ou on ne juge pas
     * @param baseline     la situation avant ; facultative, un problème
     *                     nouveau n'a pas d'antériorité chiffrée
     * @param target       la cible ; facultative, le critère peut déjà la dire
     *                     en toutes lettres
     * @param reviewerName le nom recopié depuis l'annuaire, pour qu'un compte
     *                     désactivé ne transforme pas la trace en
     *                     « utilisateur inconnu » le jour de l'audit
     */
    public static EffectivenessReview record(UUID id, UUID tenantId, UUID capaCaseId,
                                             EffectivenessVerdict verdict, String criterion,
                                             String baseline, String target, String observed,
                                             String comment, UUID reviewerId, String reviewerName,
                                             Instant reviewedAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(capaCaseId, "capaCaseId");
        Objects.requireNonNull(verdict, "verdict");
        Objects.requireNonNull(reviewedAt, "reviewedAt");

        String crit = required(criterion, MAX_CRITERION,
                "Le critère d'efficacité doit être posé sur le dossier avant d'être évalué");
        String obs = required(observed, MAX_READING,
                "Le relevé constaté est obligatoire : on juge sur une mesure, ou on ne juge pas");

        return new EffectivenessReview(id, tenantId, capaCaseId, verdict, crit,
                optional(baseline, MAX_READING, "baseline"),
                optional(target, MAX_READING, "target"),
                obs,
                optional(comment, MAX_COMMENT, "comment"),
                reviewerId,
                optional(reviewerName, MAX_REVIEWER_NAME, "reviewerName"),
                reviewedAt);
    }

    /**
     * Reconstitue une évaluation déjà enregistrée, sans rejouer les règles de
     * saisie.
     *
     * <p>Les contrôles ci-dessus portent sur un geste d'utilisateur. Les
     * appliquer à la relecture ferait disparaître de l'historique une évaluation
     * enregistrée avant qu'une règle n'existe — un auditeur verrait un trou là
     * où il y a une trace.
     */
    public static EffectivenessReview rehydrate(UUID id, UUID tenantId, UUID capaCaseId,
                                                EffectivenessVerdict verdict, String criterion,
                                                String baseline, String target, String observed,
                                                String comment, UUID reviewerId, String reviewerName,
                                                Instant reviewedAt) {
        return new EffectivenessReview(id, tenantId, capaCaseId, verdict, criterion,
                baseline, target, observed, comment, reviewerId, reviewerName, reviewedAt);
    }

    private static String required(String value, int max, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(
                    "Texte trop long (" + trimmed.length() + " > " + max + " caractères)");
        }
        return trimmed;
    }

    private static String optional(String value, int max, String field) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            // Une chaîne vide et une absence disent la même chose ; en garder
            // deux représentations obligerait chaque lecteur à traiter les deux.
            return null;
        }
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(
                    field + " : texte trop long (" + trimmed.length() + " > " + max + " caractères)");
        }
        return trimmed;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID capaCaseId() {
        return capaCaseId;
    }

    public EffectivenessVerdict verdict() {
        return verdict;
    }

    public String criterion() {
        return criterion;
    }

    public String baseline() {
        return baseline;
    }

    public String target() {
        return target;
    }

    public String observed() {
        return observed;
    }

    public String comment() {
        return comment;
    }

    public UUID reviewerId() {
        return reviewerId;
    }

    public String reviewerName() {
        return reviewerName;
    }

    public Instant reviewedAt() {
        return reviewedAt;
    }
}

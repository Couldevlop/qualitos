package com.openlab.qualitos.quality.riskregister.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Une ligne du registre des risques (ISO 9001 §6.1).
 *
 * <p><b>Brute et résiduelle.</b> La cotation brute dit le risque tel qu'il est
 * aujourd'hui ; la résiduelle, ce qu'on vise une fois le traitement en place.
 * La seconde n'existe pas à la création — on ne sait pas encore ce qu'on fera —
 * et quand elle existe elle ne peut pas dépasser la première : viser plus haut
 * que l'existant n'est pas un traitement.
 *
 * <p>La référence (R-001…) est attribuée une fois et ne change plus : c'est
 * elle que reprend une CAPA ouverte depuis la fiche.
 *
 * <p>{@code sourceId} désigne l'objet dont le risque est issu — ligne d'AMDEC,
 * non-conformité, constat d'audit, demande de changement —, vérifié par le
 * serveur à la création. Il ne change plus ensuite : un risque ne change pas
 * d'histoire.
 */
public final class Risk {

    public static final int CAUSE_MAX = 4000;
    public static final int EFFECT_MAX = 4000;
    public static final int CRITERION_MAX = 1000;

    private final UUID id;
    private final UUID tenantId;
    private final String reference;
    private final UUID sourceId;
    private Identification identification;
    private String cause;
    private String effect;
    private Rating gross;
    private Rating residual;
    private RiskDecision decision;
    private RiskStatus status;
    private LocalDate nextReviewOn;
    private String effectivenessCriterion;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("java:S107") // reconstitution depuis la persistance : un champ par colonne
    public Risk(UUID id, UUID tenantId, String reference, UUID sourceId, Identification identification,
                String cause, String effect, Rating gross, Rating residual, RiskDecision decision,
                RiskStatus status, LocalDate nextReviewOn, String effectivenessCriterion,
                UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.reference = reference;
        this.sourceId = sourceId;
        this.identification = identification;
        this.cause = cause;
        this.effect = effect;
        this.gross = gross;
        this.residual = residual;
        this.decision = decision;
        this.status = status;
        this.nextReviewOn = nextReviewOn;
        this.effectivenessCriterion = effectivenessCriterion;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Un risque neuf, validé. Sans statut fourni, il est « à traiter ». */
    public static Risk create(UUID tenantId, String reference, RiskDetails details,
                              UUID createdBy, Instant now) {
        return create(tenantId, reference, null, details, createdBy, now);
    }

    /** Un risque neuf issu d'un objet de la plateforme déjà vérifié. */
    public static Risk create(UUID tenantId, String reference, UUID sourceId, RiskDetails details,
                              UUID createdBy, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(reference, "reference");
        Risk risque = new Risk(null, tenantId, reference, sourceId, null, null, null, null, null,
                null, null, null, null, createdBy, now, now);
        risque.apply(details, now);
        return risque;
    }

    /**
     * Révise la fiche d'un bloc, revalidée en entier.
     *
     * @return ce qui mérite une ligne dans le suivi, dans l'ordre de lecture
     */
    public List<RegisterChange> revise(RiskDetails details, Instant now) {
        Rating brute = gross;
        Rating visee = residual;
        RiskStatus etat = status;
        RiskDecision choix = decision;
        apply(details, now);

        List<RegisterChange> changes = new ArrayList<>();
        if (!gross.equals(brute)) {
            changes.add(new RegisterChange(RegisterEventType.RATING_CHANGED, brute.code(), gross.code()));
        }
        if (!Objects.equals(residual, visee)) {
            changes.add(new RegisterChange(RegisterEventType.RESIDUAL_CHANGED,
                    visee == null ? null : visee.code(), residual == null ? null : residual.code()));
        }
        if (status != etat) {
            changes.add(new RegisterChange(RegisterEventType.STATUS_CHANGED, etat.name(), status.name()));
        }
        if (decision != choix) {
            changes.add(new RegisterChange(RegisterEventType.DECISION_CHANGED, choix.name(), decision.name()));
        }
        return changes;
    }

    private void apply(RiskDetails d, Instant now) {
        if (d == null || d.identification() == null) {
            throw new RegisterValidationException("title", "L'intitulé est obligatoire.");
        }
        Identification ident = d.identification().validated(true);
        String laCause = Texts.optional("cause", d.cause(), CAUSE_MAX);
        String lEffet = Texts.optional("effect", d.effect(), EFFECT_MAX);
        Rating brute = Rating.of(d.grossSeverity(), d.grossProbability(),
                "grossSeverity", "grossProbability");
        Rating visee = residuelle(d, brute);
        String critere = Texts.optional("effectivenessCriterion", d.effectivenessCriterion(), CRITERION_MAX);

        this.identification = ident;
        this.cause = laCause;
        this.effect = lEffet;
        this.gross = brute;
        this.residual = visee;
        this.decision = d.decision() == null ? RiskDecision.UNDECIDED : d.decision();
        this.status = d.status() == null ? RiskStatus.TO_TREAT : d.status();
        this.nextReviewOn = d.nextReviewOn();
        this.effectivenessCriterion = critere;
        this.updatedAt = now;
    }

    /** Les deux notes résiduelles ensemble, ou aucune ; jamais au-dessus de la brute. */
    private static Rating residuelle(RiskDetails d, Rating brute) {
        if (d.residualSeverity() == null && d.residualProbability() == null) {
            return null;
        }
        Rating visee = Rating.of(d.residualSeverity(), d.residualProbability(),
                "residualSeverity", "residualProbability");
        if (visee.score() > brute.score()) {
            throw new RegisterValidationException("residualSeverity",
                    "La cotation résiduelle visée ne peut pas dépasser la cotation brute.");
        }
        return visee;
    }

    public RiskLevel grossLevel() {
        return RiskLevel.of(gross.score());
    }

    public RiskLevel residualLevel() {
        return residual == null ? null : RiskLevel.of(residual.score());
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public UUID getSourceId() { return sourceId; }
    public Identification getIdentification() { return identification; }
    public String getCause() { return cause; }
    public String getEffect() { return effect; }
    public Rating getGross() { return gross; }
    public Rating getResidual() { return residual; }
    public RiskDecision getDecision() { return decision; }
    public RiskStatus getStatus() { return status; }
    public LocalDate getNextReviewOn() { return nextReviewOn; }
    public String getEffectivenessCriterion() { return effectivenessCriterion; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

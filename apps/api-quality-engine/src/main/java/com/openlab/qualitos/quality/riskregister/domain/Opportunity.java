package com.openlab.qualitos.quality.riskregister.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Une ligne du registre des opportunités (ISO 9001 §6.1, §10.3).
 *
 * <p>Même structure que le risque, autre cotation : gain attendu × faisabilité,
 * dont le sommet se dit « prioritaire ». Pas de cotation résiduelle — une
 * opportunité ne se réduit pas, elle se saisit ou non —, mais une échéance
 * visée.
 */
public final class Opportunity {

    public static final int CONTEXT_MAX = 4000;
    public static final int BENEFIT_MAX = 4000;
    public static final int CRITERION_MAX = 1000;

    private final UUID id;
    private final UUID tenantId;
    private final String reference;
    private Identification identification;
    private LocalDate targetDate;
    private String context;
    private String benefit;
    private Rating evaluation;
    private OpportunityDecision decision;
    private OpportunityStatus status;
    private String benefitCriterion;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("java:S107") // reconstitution depuis la persistance : un champ par colonne
    public Opportunity(UUID id, UUID tenantId, String reference, Identification identification,
                       LocalDate targetDate, String context, String benefit, Rating evaluation,
                       OpportunityDecision decision, OpportunityStatus status, String benefitCriterion,
                       UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.reference = reference;
        this.identification = identification;
        this.targetDate = targetDate;
        this.context = context;
        this.benefit = benefit;
        this.evaluation = evaluation;
        this.decision = decision;
        this.status = status;
        this.benefitCriterion = benefitCriterion;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Une opportunité neuve, validée. Sans statut fourni, elle est « en étude ». */
    public static Opportunity create(UUID tenantId, String reference, OpportunityDetails details,
                                     UUID createdBy, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(reference, "reference");
        Opportunity o = new Opportunity(null, tenantId, reference, null, null, null, null, null,
                null, null, null, createdBy, now, now);
        o.apply(details, now);
        return o;
    }

    /** @return ce qui mérite une ligne dans le suivi */
    public List<RegisterChange> revise(OpportunityDetails details, Instant now) {
        Rating avant = evaluation;
        OpportunityStatus etat = status;
        OpportunityDecision choix = decision;
        apply(details, now);

        List<RegisterChange> changes = new ArrayList<>();
        if (!evaluation.equals(avant)) {
            changes.add(new RegisterChange(RegisterEventType.RATING_CHANGED, avant.code(), evaluation.code()));
        }
        if (status != etat) {
            changes.add(new RegisterChange(RegisterEventType.STATUS_CHANGED, etat.name(), status.name()));
        }
        if (decision != choix) {
            changes.add(new RegisterChange(RegisterEventType.DECISION_CHANGED, choix.name(), decision.name()));
        }
        return changes;
    }

    private void apply(OpportunityDetails d, Instant now) {
        if (d == null || d.identification() == null) {
            throw new RegisterValidationException("title", "L'intitulé est obligatoire.");
        }
        Identification ident = d.identification().validated(false);
        String leContexte = Texts.optional("context", d.context(), CONTEXT_MAX);
        String leBenefice = Texts.optional("benefit", d.benefit(), BENEFIT_MAX);
        Rating note = Rating.of(d.gain(), d.feasibility(), "gain", "feasibility");
        String critere = Texts.optional("benefitCriterion", d.benefitCriterion(), CRITERION_MAX);

        this.identification = ident;
        this.targetDate = d.targetDate();
        this.context = leContexte;
        this.benefit = leBenefice;
        this.evaluation = note;
        this.decision = d.decision() == null ? OpportunityDecision.UNDECIDED : d.decision();
        this.status = d.status() == null ? OpportunityStatus.UNDER_STUDY : d.status();
        this.benefitCriterion = critere;
        this.updatedAt = now;
    }

    public OpportunityLevel level() {
        return OpportunityLevel.of(evaluation.score());
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public Identification getIdentification() { return identification; }
    public LocalDate getTargetDate() { return targetDate; }
    public String getContext() { return context; }
    public String getBenefit() { return benefit; }
    public Rating getEvaluation() { return evaluation; }
    public OpportunityDecision getDecision() { return decision; }
    public OpportunityStatus getStatus() { return status; }
    public String getBenefitCriterion() { return benefitCriterion; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

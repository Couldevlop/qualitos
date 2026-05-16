package com.openlab.qualitos.quality.breach.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat incident de violation de données personnelles (RGPD Art. 33/34).
 *
 * Délais légaux :
 *  - Art. 33§1 : notification à l'autorité de contrôle (DPA) sous 72h.
 *  - Art. 34   : notification aux personnes concernées si risque élevé.
 *
 * Cycle de vie :
 *   DETECTED → ASSESSING → CONTAINED → CLOSED
 *   DETECTED|ASSESSING → REJECTED (false positive)
 *
 * Les notifications DPA et sujets sont des actions ENREGISTRÉES sur l'agrégat
 * (timestamps + référence) et non des états — un incident CONTAINED peut être
 * notifié ou non avant clôture. À la clôture, si severity ≥ HIGH et qu'aucune
 * notification sujets n'est enregistrée, l'application DOIT lever une erreur
 * (sauf si {@code closureNotes} documente l'exception).
 *
 * Privacy by design : la description DOIT exclure les PII concrètes — on décrit
 * les catégories de données et le contexte, pas les identifiants individuels.
 */
public final class BreachIncident {

    public static final Duration DPA_NOTIFICATION_WINDOW = Duration.ofHours(72);

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");

    private static final Map<BreachStatus, Set<BreachStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(BreachStatus.class);
        ALLOWED.put(BreachStatus.DETECTED,  EnumSet.of(BreachStatus.ASSESSING, BreachStatus.REJECTED));
        ALLOWED.put(BreachStatus.ASSESSING, EnumSet.of(BreachStatus.CONTAINED, BreachStatus.REJECTED));
        ALLOWED.put(BreachStatus.CONTAINED, EnumSet.of(BreachStatus.CLOSED));
        ALLOWED.put(BreachStatus.CLOSED,    EnumSet.noneOf(BreachStatus.class));
        ALLOWED.put(BreachStatus.REJECTED,  EnumSet.noneOf(BreachStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String internalReference;
    private String title;
    private String description;
    private final Instant detectedAt;
    private final Instant occurredAt;
    private final Instant dpaDeadlineAt;
    private BreachSeverity severity;
    private BreachStatus status;
    private long affectedSubjectsCount;
    private final Set<String> affectedDataCategories;
    private String riskOfHarmDescription;
    private String containmentMeasures;
    private Instant dpaNotifiedAt;
    private String dpaReference;
    private Instant subjectsNotifiedAt;
    private String subjectsNotificationChannel;
    private String rejectionReason;
    private String closureNotes;
    private final UUID reportedByUserId;
    private UUID handledByUserId;
    private Instant closedAt;
    private Instant updatedAt;

    public BreachIncident(UUID id, UUID tenantId, String internalReference,
                          String title, String description,
                          Instant detectedAt, Instant occurredAt, Instant dpaDeadlineAt,
                          BreachSeverity severity, BreachStatus status,
                          long affectedSubjectsCount, Set<String> affectedDataCategories,
                          String riskOfHarmDescription, String containmentMeasures,
                          Instant dpaNotifiedAt, String dpaReference,
                          Instant subjectsNotifiedAt, String subjectsNotificationChannel,
                          String rejectionReason, String closureNotes,
                          UUID reportedByUserId, UUID handledByUserId,
                          Instant closedAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.internalReference = requireReference(internalReference);
        this.title = requireText(title, "title", 250);
        this.description = description;
        this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt");
        this.occurredAt = occurredAt;
        this.dpaDeadlineAt = Objects.requireNonNull(dpaDeadlineAt, "dpaDeadlineAt");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.status = Objects.requireNonNull(status, "status");
        this.affectedSubjectsCount = requireNonNegative(affectedSubjectsCount);
        this.affectedDataCategories = sanitizeCategories(affectedDataCategories);
        this.riskOfHarmDescription = riskOfHarmDescription;
        this.containmentMeasures = containmentMeasures;
        this.dpaNotifiedAt = dpaNotifiedAt;
        this.dpaReference = dpaReference;
        this.subjectsNotifiedAt = subjectsNotifiedAt;
        this.subjectsNotificationChannel = subjectsNotificationChannel;
        this.rejectionReason = rejectionReason;
        this.closureNotes = closureNotes;
        this.reportedByUserId = Objects.requireNonNull(reportedByUserId, "reportedByUserId");
        this.handledByUserId = handledByUserId;
        this.closedAt = closedAt;
        this.updatedAt = updatedAt != null ? updatedAt : detectedAt;
    }

    /** Factory — découverte d'un nouvel incident. */
    public static BreachIncident detect(UUID tenantId, String internalReference,
                                        String title, String description,
                                        Instant detectedAt, Instant occurredAt,
                                        BreachSeverity severity, long affectedSubjectsCount,
                                        Set<String> affectedDataCategories,
                                        String riskOfHarmDescription,
                                        UUID reportedByUserId) {
        return new BreachIncident(null, tenantId, internalReference, title, description,
                detectedAt, occurredAt, detectedAt.plus(DPA_NOTIFICATION_WINDOW),
                severity, BreachStatus.DETECTED,
                affectedSubjectsCount, affectedDataCategories,
                riskOfHarmDescription, null, null, null, null, null, null, null,
                reportedByUserId, null, null, detectedAt);
    }

    public void startAssessment(UUID handledByUserId, Instant now) {
        ensureTransition(BreachStatus.ASSESSING);
        this.handledByUserId = handledByUserId;
        this.status = BreachStatus.ASSESSING;
        this.updatedAt = now;
    }

    public void contain(String containmentMeasures, UUID handledByUserId, Instant now) {
        ensureTransition(BreachStatus.CONTAINED);
        if (containmentMeasures == null || containmentMeasures.isBlank()) {
            throw new BreachStateException("containmentMeasures required");
        }
        this.containmentMeasures = containmentMeasures;
        if (handledByUserId != null) this.handledByUserId = handledByUserId;
        this.status = BreachStatus.CONTAINED;
        this.updatedAt = now;
    }

    public void notifyDpa(Instant notifiedAt, String reference, Instant now) {
        if (status != BreachStatus.CONTAINED && status != BreachStatus.ASSESSING) {
            throw new BreachStateException(
                    "DPA notification only recordable in ASSESSING or CONTAINED");
        }
        if (notifiedAt == null) throw new BreachStateException("notifiedAt required");
        if (reference == null || reference.isBlank()) {
            throw new BreachStateException("dpaReference required");
        }
        if (notifiedAt.isBefore(detectedAt)) {
            throw new BreachStateException("notifiedAt cannot precede detectedAt");
        }
        this.dpaNotifiedAt = notifiedAt;
        this.dpaReference = reference;
        this.updatedAt = now;
    }

    public void notifySubjects(Instant notifiedAt, String channel, Instant now) {
        if (status != BreachStatus.CONTAINED && status != BreachStatus.ASSESSING) {
            throw new BreachStateException(
                    "Subject notification only recordable in ASSESSING or CONTAINED");
        }
        if (notifiedAt == null) throw new BreachStateException("notifiedAt required");
        if (channel == null || channel.isBlank()) {
            throw new BreachStateException("channel required");
        }
        if (notifiedAt.isBefore(detectedAt)) {
            throw new BreachStateException("notifiedAt cannot precede detectedAt");
        }
        this.subjectsNotifiedAt = notifiedAt;
        this.subjectsNotificationChannel = channel;
        this.updatedAt = now;
    }

    /** Clôture. Garde-fou : si severity ≥ HIGH et notifications sujets manquantes,
     *  une justification (closureNotes) DOIT documenter pourquoi (Art. 34§3). */
    public void close(String closureNotes, Instant now) {
        ensureTransition(BreachStatus.CLOSED);
        boolean subjectsNotifMissing = subjectsNotifiedAt == null;
        if (severity.requiresSubjectNotification() && subjectsNotifMissing
                && (closureNotes == null || closureNotes.isBlank())) {
            throw new BreachStateException(
                    "severity=" + severity + " requires subjects notification or "
                    + "explicit closureNotes documenting the Art. 34§3 exception");
        }
        this.closureNotes = closureNotes;
        this.status = BreachStatus.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void reject(String reason, Instant now) {
        ensureTransition(BreachStatus.REJECTED);
        if (reason == null || reason.isBlank()) {
            throw new BreachStateException("rejection reason required");
        }
        this.rejectionReason = reason;
        this.status = BreachStatus.REJECTED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void updateSeverity(BreachSeverity newSeverity, Instant now) {
        if (isTerminal()) {
            throw new BreachStateException("Cannot update severity on terminal incident");
        }
        this.severity = Objects.requireNonNull(newSeverity, "severity");
        this.updatedAt = now;
    }

    public boolean isTerminal() {
        return status == BreachStatus.CLOSED || status == BreachStatus.REJECTED;
    }

    public boolean isDpaNotificationOverdue(Instant ref) {
        return dpaNotifiedAt == null
                && status != BreachStatus.REJECTED
                && ref.isAfter(dpaDeadlineAt);
    }

    public boolean isSubjectNotificationRequired() {
        return severity.requiresSubjectNotification();
    }

    private void ensureTransition(BreachStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new BreachStateException(
                    "Transition " + status + " → " + target + " is not allowed");
        }
    }

    private static String requireReference(String v) {
        if (v == null || !REF_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "internalReference must match [A-Z][A-Z0-9_-]{1,63}");
        }
        return v;
    }
    private static String requireText(String v, String f, int maxLen) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        if (v.length() > maxLen) throw new IllegalArgumentException(
                f + " too long (max " + maxLen + ")");
        return v;
    }
    private static long requireNonNegative(long v) {
        if (v < 0) throw new IllegalArgumentException("affectedSubjectsCount must be ≥ 0");
        return v;
    }
    private static Set<String> sanitizeCategories(Set<String> input) {
        if (input == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String c : input) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            if (!CATEGORY_PATTERN.matcher(t).matches()) {
                throw new IllegalArgumentException(
                        "affectedDataCategories: '" + t + "' must match [a-z][a-z0-9._-]{1,63}");
            }
            out.add(t);
        }
        return Collections.unmodifiableSet(out);
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getInternalReference() { return internalReference; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getDpaDeadlineAt() { return dpaDeadlineAt; }
    public BreachSeverity getSeverity() { return severity; }
    public BreachStatus getStatus() { return status; }
    public long getAffectedSubjectsCount() { return affectedSubjectsCount; }
    public Set<String> getAffectedDataCategories() { return affectedDataCategories; }
    public String getRiskOfHarmDescription() { return riskOfHarmDescription; }
    public String getContainmentMeasures() { return containmentMeasures; }
    public Instant getDpaNotifiedAt() { return dpaNotifiedAt; }
    public String getDpaReference() { return dpaReference; }
    public Instant getSubjectsNotifiedAt() { return subjectsNotifiedAt; }
    public String getSubjectsNotificationChannel() { return subjectsNotificationChannel; }
    public String getRejectionReason() { return rejectionReason; }
    public String getClosureNotes() { return closureNotes; }
    public UUID getReportedByUserId() { return reportedByUserId; }
    public UUID getHandledByUserId() { return handledByUserId; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

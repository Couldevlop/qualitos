package com.openlab.qualitos.quality.ehs.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate domaine EHS — POJO sans dépendance framework (Clean Architecture).
 *
 * Les transitions de cycle de vie sont encapsulées dans l'agrégat lui-même :
 * la couche application n'a qu'à appeler {@link #investigate()}, {@link #mitigate}
 * etc. ; l'agrégat valide la transition. Toute violation est levée comme
 * {@link IncidentStateException} (mappée en 409 par la couche web).
 */
public final class Incident {

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(IncidentStatus.class);
        ALLOWED.put(IncidentStatus.REPORTED, EnumSet.of(
                IncidentStatus.INVESTIGATING, IncidentStatus.CANCELLED));
        ALLOWED.put(IncidentStatus.INVESTIGATING, EnumSet.of(
                IncidentStatus.MITIGATED, IncidentStatus.CANCELLED));
        ALLOWED.put(IncidentStatus.MITIGATED, EnumSet.of(IncidentStatus.CLOSED));
        ALLOWED.put(IncidentStatus.CLOSED, EnumSet.noneOf(IncidentStatus.class));
        ALLOWED.put(IncidentStatus.CANCELLED, EnumSet.noneOf(IncidentStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String code;
    private String title;
    private String description;
    private IncidentType type;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private Instant occurredAt;
    private final Instant reportedAt;
    private Instant mitigatedAt;
    private Instant closedAt;
    private String location;
    private String personsInvolved;
    private String rootCause;
    private String correctiveActions;
    private String standardsCsv;
    private UUID capaCaseId;
    private UUID ncId;
    private UUID ownerUserId;
    private final UUID reportedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Incident(UUID id, UUID tenantId, String code, String title, String description,
                    IncidentType type, IncidentSeverity severity, IncidentStatus status,
                    Instant occurredAt, Instant reportedAt, Instant mitigatedAt, Instant closedAt,
                    String location, String personsInvolved, String rootCause,
                    String correctiveActions, String standardsCsv,
                    UUID capaCaseId, UUID ncId, UUID ownerUserId, UUID reportedBy,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.code = Objects.requireNonNull(code, "code");
        this.title = Objects.requireNonNull(title, "title");
        this.description = description;
        this.type = Objects.requireNonNull(type, "type");
        this.severity = severity != null ? severity : IncidentSeverity.MEDIUM;
        this.status = status != null ? status : IncidentStatus.REPORTED;
        this.occurredAt = occurredAt;
        this.reportedAt = reportedAt;
        this.mitigatedAt = mitigatedAt;
        this.closedAt = closedAt;
        this.location = location;
        this.personsInvolved = personsInvolved;
        this.rootCause = rootCause;
        this.correctiveActions = correctiveActions;
        this.standardsCsv = standardsCsv;
        this.capaCaseId = capaCaseId;
        this.ncId = ncId;
        this.ownerUserId = ownerUserId;
        this.reportedBy = Objects.requireNonNull(reportedBy, "reportedBy");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Factory : nouvel incident "REPORTED" prêt à persister. */
    public static Incident report(UUID tenantId, String code, String title, String description,
                                  IncidentType type, IncidentSeverity severity,
                                  Instant occurredAt, String location,
                                  UUID reportedBy, Instant now) {
        return new Incident(null, tenantId, code, title, description, type,
                severity != null ? severity : IncidentSeverity.MEDIUM,
                IncidentStatus.REPORTED,
                occurredAt != null ? occurredAt : now, now,
                null, null, location, null, null, null, null,
                null, null, null, reportedBy, now, now);
    }

    // ----- Transitions -----

    public void investigate(UUID ownerUserId, Instant now) {
        ensureCanTransitionTo(IncidentStatus.INVESTIGATING);
        if (ownerUserId != null) this.ownerUserId = ownerUserId;
        this.status = IncidentStatus.INVESTIGATING;
        this.updatedAt = now;
    }

    public void mitigate(String rootCause, String correctiveActions, Instant now) {
        ensureCanTransitionTo(IncidentStatus.MITIGATED);
        if (rootCause == null || rootCause.isBlank()) {
            throw new IncidentStateException("rootCause is required to mitigate");
        }
        if (correctiveActions == null || correctiveActions.isBlank()) {
            throw new IncidentStateException("correctiveActions is required to mitigate");
        }
        this.rootCause = rootCause;
        this.correctiveActions = correctiveActions;
        this.mitigatedAt = now;
        this.status = IncidentStatus.MITIGATED;
        this.updatedAt = now;
    }

    public void close(Instant now) {
        ensureCanTransitionTo(IncidentStatus.CLOSED);
        this.closedAt = now;
        this.status = IncidentStatus.CLOSED;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        ensureCanTransitionTo(IncidentStatus.CANCELLED);
        this.closedAt = now;
        this.status = IncidentStatus.CANCELLED;
        this.updatedAt = now;
    }

    public void linkCapa(UUID capaCaseId, Instant now) {
        if (status == IncidentStatus.CLOSED || status == IncidentStatus.CANCELLED) {
            throw new IncidentStateException("Cannot link CAPA on a terminal incident");
        }
        this.capaCaseId = capaCaseId;
        this.updatedAt = now;
    }

    public void linkNc(UUID ncId, Instant now) {
        if (status == IncidentStatus.CLOSED || status == IncidentStatus.CANCELLED) {
            throw new IncidentStateException("Cannot link NC on a terminal incident");
        }
        this.ncId = ncId;
        this.updatedAt = now;
    }

    public void editDetails(String title, String description, String location,
                            String personsInvolved, IncidentSeverity severity,
                            String standardsCsv, Instant now) {
        if (status == IncidentStatus.CLOSED || status == IncidentStatus.CANCELLED) {
            throw new IncidentStateException("Cannot edit a terminal incident");
        }
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (location != null) this.location = location;
        if (personsInvolved != null) this.personsInvolved = personsInvolved;
        if (severity != null) this.severity = severity;
        if (standardsCsv != null) this.standardsCsv = standardsCsv;
        this.updatedAt = now;
    }

    private void ensureCanTransitionTo(IncidentStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new IncidentStateException(
                    "Transition " + status + " → " + target + " is not allowed");
        }
    }

    public boolean isTerminal() {
        return status == IncidentStatus.CLOSED || status == IncidentStatus.CANCELLED;
    }

    /** Affecte l'identifiant après persistance (utilisé par l'adapter infra). */
    public void assignId(UUID id) { this.id = id; }

    // ----- Accessors -----

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IncidentType getType() { return type; }
    public IncidentSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReportedAt() { return reportedAt; }
    public Instant getMitigatedAt() { return mitigatedAt; }
    public Instant getClosedAt() { return closedAt; }
    public String getLocation() { return location; }
    public String getPersonsInvolved() { return personsInvolved; }
    public String getRootCause() { return rootCause; }
    public String getCorrectiveActions() { return correctiveActions; }
    public String getStandardsCsv() { return standardsCsv; }
    public UUID getCapaCaseId() { return capaCaseId; }
    public UUID getNcId() { return ncId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getReportedBy() { return reportedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

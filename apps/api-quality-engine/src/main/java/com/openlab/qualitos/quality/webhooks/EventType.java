package com.openlab.qualitos.quality.webhooks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types d'événements publiables via webhooks (CLAUDE.md §13.2).
 *
 * Convention de nommage : <domain>.<entity>.<action>
 * Le wire format string utilise '.' pour le routage public.
 */
public enum EventType {

    // PDCA
    PDCA_CYCLE_CREATED("pdca.cycle.created"),
    PDCA_CYCLE_ADVANCED("pdca.cycle.advanced"),
    PDCA_CYCLE_COMPLETED("pdca.cycle.completed"),
    PDCA_CYCLE_CANCELLED("pdca.cycle.cancelled"),

    // CAPA
    CAPA_CASE_OPENED("capa.case.opened"),
    CAPA_CASE_RESOLVED("capa.case.resolved"),
    CAPA_CASE_CLOSED("capa.case.closed"),
    CAPA_EFFECTIVENESS_VERIFIED("capa.effectiveness.verified"),

    // 5S
    FIVES_AUDIT_COMPLETED("fives.audit.completed"),

    // Ishikawa
    ISHIKAWA_DIAGRAM_VALIDATED("ishikawa.diagram.validated"),

    // Audits
    AUDIT_PLAN_COMPLETED("audit.plan.completed"),
    AUDIT_FINDING_RAISED("audit.finding.raised"),

    // Documents
    DOCUMENT_VERSION_PUBLISHED("documents.version.published"),
    DOCUMENT_VERSION_ACKNOWLEDGED("documents.version.acknowledged"),

    // Standards Hub
    STANDARDS_ADOPTION_CERTIFIED("standards.adoption.certified"),
    STANDARDS_ADOPTION_EXPIRED("standards.adoption.expired"),

    // DMAIC
    DMAIC_PROJECT_PHASE_ADVANCED("dmaic.project.phase_advanced"),
    DMAIC_PROJECT_COMPLETED("dmaic.project.completed"),

    // Circles
    CIRCLE_PROPOSAL_APPROVED("circles.proposal.approved"),
    CIRCLE_PROPOSAL_MEASURED("circles.proposal.measured"),

    // KPIs
    KPI_THRESHOLD_BREACHED("kpi.threshold.breached"),

    // Generic test event
    TEST_PING("webhook.test.ping");

    private final String wire;

    EventType(String wire) { this.wire = wire; }

    @JsonValue
    public String wire() { return wire; }

    @JsonCreator
    public static EventType fromWire(String wire) {
        for (EventType e : values()) {
            if (e.wire.equals(wire)) return e;
        }
        throw new IllegalArgumentException("Unknown event type: " + wire);
    }
}

package com.openlab.qualitos.quality.circuit.infrastructure;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Le passage d'un objet dans un circuit, avec ses étapes copiées et ses décisions. */
@Entity
@Table(name = "circuit_runs")
@Getter
@Setter
@NoArgsConstructor
public class CircuitRunJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, updatable = false, length = 40)
    private String subject;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private UUID subjectId;

    @Column(name = "author_id", updatable = false)
    private UUID authorId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "circuit_run_steps", joinColumns = @JoinColumn(name = "run_id"))
    @OrderColumn(name = "ordinal")
    private List<Step> steps = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "circuit_decisions", joinColumns = @JoinColumn(name = "run_id"))
    @OrderColumn(name = "ordinal")
    private List<Decision> decisions = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {

        @Column(nullable = false, length = 120)
        private String name;

        @Column(name = "role_code", nullable = false, length = 64)
        private String roleCode;

        @Column(name = "min_approvals", nullable = false)
        private int minApprovals;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Decision {

        @Column(name = "step_index", nullable = false)
        private int stepIndex;

        @Column(name = "actor_id", nullable = false)
        private UUID actorId;

        @Column(nullable = false)
        private boolean approved;

        @Column(length = 1000)
        private String comment;

        @Column(name = "decided_at", nullable = false)
        private Instant decidedAt;
    }
}

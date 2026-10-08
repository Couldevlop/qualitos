package com.openlab.qualitos.quality.circuit.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.infrastructure.AuthzAdapters;
import com.openlab.qualitos.quality.circuit.application.CircuitPorts;
import com.openlab.qualitos.quality.circuit.domain.CircuitRun;
import com.openlab.qualitos.quality.circuit.domain.CircuitStep;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;
import com.openlab.qualitos.quality.common.CurrentUser;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Les adaptateurs des circuits : base, jeton (via les droits), journal d'audit. */
public final class CircuitAdapters {

    private CircuitAdapters() {}

    // ---------- circuits réglés ----------

    public static final class Circuits implements CircuitPorts.Circuits {

        private final CircuitStepRepository repository;
        private final Clock clock;

        public Circuits(CircuitStepRepository repository, Clock clock) {
            this.repository = repository;
            this.clock = clock;
        }

        @Override
        public List<CircuitStep> find(UUID tenantId, CircuitSubject subject) {
            return repository.findByTenantIdAndSubjectOrderByOrdinalAsc(tenantId, subject.code()).stream()
                    .map(e -> new CircuitStep(e.getName(), e.getRoleCode(), e.getMinApprovals()))
                    .toList();
        }

        @Override
        public void replace(UUID tenantId, CircuitSubject subject, List<CircuitStep> steps, UUID actor) {
            repository.deleteCircuit(tenantId, subject.code());
            repository.flush();
            Instant maintenant = clock.instant();
            for (int i = 0; i < steps.size(); i++) {
                CircuitStep s = steps.get(i);
                CircuitStepJpaEntity e = new CircuitStepJpaEntity();
                e.setTenantId(tenantId);
                e.setSubject(subject.code());
                e.setOrdinal(i);
                e.setName(s.name());
                e.setRoleCode(s.roleCode());
                e.setMinApprovals(s.minApprovals());
                e.setUpdatedBy(actor);
                e.setUpdatedAt(maintenant);
                repository.save(e);
            }
        }
    }

    // ---------- passages ----------

    public static final class Runs implements CircuitPorts.Runs {

        private final CircuitRunRepository repository;

        public Runs(CircuitRunRepository repository) {
            this.repository = repository;
        }

        @Override
        public Optional<CircuitRun> findOpen(UUID tenantId, CircuitSubject subject, UUID subjectId) {
            return repository.findByTenantIdAndSubjectAndSubjectIdAndStatus(tenantId, subject.code(), subjectId,
                    CircuitRun.Status.IN_PROGRESS.name()).map(Runs::toDomain);
        }

        @Override
        public Optional<CircuitRun> findLatest(UUID tenantId, CircuitSubject subject, UUID subjectId) {
            return repository.findFirstByTenantIdAndSubjectAndSubjectIdOrderByStartedAtDesc(tenantId,
                    subject.code(), subjectId).map(Runs::toDomain);
        }

        /**
         * Les étapes ne changent plus après le départ et les décisions ne font que
         * s'ajouter : on n'écrit que les nouvelles. La version de la ligne, lue dans
         * la même transaction, refuse la seconde de deux décisions simultanées.
         */
        @Override
        public void save(CircuitRun run) {
            CircuitRunJpaEntity e = repository.findById(run.id()).orElseGet(() -> {
                CircuitRunJpaEntity n = new CircuitRunJpaEntity();
                n.setId(run.id());
                n.setTenantId(run.tenantId());
                n.setSubject(run.subject().code());
                n.setSubjectId(run.subjectId());
                n.setAuthorId(run.authorId());
                n.setStartedAt(run.startedAt());
                run.steps().forEach(s -> n.getSteps().add(
                        new CircuitRunJpaEntity.Step(s.name(), s.roleCode(), s.minApprovals())));
                return n;
            });
            e.setStatus(run.status().name());
            e.setCurrentStep(run.currentStep());
            e.setEndedAt(run.endedAt());
            List<CircuitRun.Decision> decisions = run.decisions();
            for (int i = e.getDecisions().size(); i < decisions.size(); i++) {
                CircuitRun.Decision d = decisions.get(i);
                e.getDecisions().add(new CircuitRunJpaEntity.Decision(d.stepIndex(), d.actorId(), d.approved(),
                        d.comment(), d.at()));
            }
            repository.save(e);
        }

        static CircuitRun toDomain(CircuitRunJpaEntity e) {
            return CircuitRun.restore(e.getId(), e.getTenantId(), CircuitSubject.fromCode(e.getSubject()),
                    e.getSubjectId(), e.getAuthorId(),
                    e.getSteps().stream()
                            .map(s -> new CircuitStep(s.getName(), s.getRoleCode(), s.getMinApprovals()))
                            .toList(),
                    e.getDecisions().stream()
                            .map(d -> new CircuitRun.Decision(d.getStepIndex(), d.getActorId(), d.isApproved(),
                                    d.getComment(), d.getDecidedAt()))
                            .toList(),
                    e.getCurrentStep(), CircuitRun.Status.valueOf(e.getStatus()), e.getStartedAt(), e.getEndedAt());
        }
    }

    // ---------- contexte ----------

    /** Client et acteur du jeton ; rôles effectifs et rôles connus par le service des droits. */
    public static final class Context implements CircuitPorts.Context {

        private final AuthzAdapters.JwtContext jwt = new AuthzAdapters.JwtContext();
        private final AuthorizationService authorization;

        public Context(AuthorizationService authorization) {
            this.authorization = authorization;
        }

        @Override
        public UUID requireTenantId() {
            return jwt.requireTenantId();
        }

        @Override
        public UUID requireActorId() {
            return CurrentUser.requireUserId();
        }

        @Override
        public Set<String> actorRoles() {
            return authorization.currentRoleCodes();
        }

        @Override
        public Set<String> tenantRoles() {
            return authorization.tenantRoleCodes();
        }

        @Override
        public boolean roleGrants(String roleCode, Permission permission) {
            return authorization.roleGrants(roleCode, permission);
        }
    }

    // ---------- journal d'audit ----------

    /** Des codes et des identifiants ; ni noms d'étapes ni commentaires (texte libre). */
    public static final class Audit implements CircuitPorts.Audit {

        static final String CIRCUIT_RESOURCE = "circuit";
        static final String RUN_RESOURCE = "circuit-run";

        private final AuditEventService auditEvents;

        public Audit(AuditEventService auditEvents) {
            this.auditEvents = auditEvents;
        }

        @Override
        public void circuitReplaced(UUID tenantId, CircuitSubject subject, List<CircuitStep> steps, UUID actor) {
            String etapes = steps.stream()
                    .map(s -> "{\"role\":\"" + s.roleCode() + "\",\"min\":" + s.minApprovals() + "}")
                    .collect(Collectors.joining(",", "[", "]"));
            publish(tenantId, "circuit.replaced", CIRCUIT_RESOURCE, null, "Circuit de validation réglé",
                    "{\"subject\":\"" + subject.code() + "\",\"steps\":" + etapes + "}", actor);
        }

        @Override
        public void decided(CircuitRun run, CircuitRun.Decision decision) {
            publish(run.tenantId(), decision.approved() ? "circuit.step.approved" : "circuit.step.rejected",
                    RUN_RESOURCE, run.id(),
                    decision.approved() ? "Circuit — étape approuvée" : "Circuit — refusé",
                    "{\"subject\":\"" + run.subject().code() + "\",\"subjectId\":\"" + run.subjectId()
                            + "\",\"step\":" + decision.stepIndex() + ",\"status\":\"" + run.status().name() + "\"}",
                    decision.actorId());
        }

        private void publish(UUID tenantId, String action, String resource, UUID resourceId, String summary,
                             String payload, UUID actor) {
            auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                    null, ActorType.USER, actor, action, resource, resourceId, summary, payload, null, null));
        }
    }
}

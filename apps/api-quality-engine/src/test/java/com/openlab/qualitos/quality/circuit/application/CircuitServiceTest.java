package com.openlab.qualitos.quality.circuit.application;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.circuit.domain.CircuitException;
import com.openlab.qualitos.quality.circuit.domain.CircuitRun;
import com.openlab.qualitos.quality.circuit.domain.CircuitStep;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Le service des circuits, avec des doublures en mémoire (pas de mocks : l'état compte). */
class CircuitServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER_TENANT = UUID.randomUUID();
    static final UUID ADMIN = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID ALICE = UUID.randomUUID();
    static final UUID DOC = UUID.randomUUID();
    static final CircuitSubject DV = CircuitSubject.DOCUMENT_VERSION;

    final FakeCircuits circuits = new FakeCircuits();
    final FakeRuns runs = new FakeRuns();
    final FakeContext context = new FakeContext();
    final FakeAudit audit = new FakeAudit();
    CircuitService service;

    @BeforeEach
    void setUp() {
        service = new CircuitService(circuits, runs, context, audit,
                Clock.fixed(Instant.parse("2026-10-08T08:00:00Z"), ZoneOffset.UTC));
        context.tenant = TENANT;
        context.actor = ADMIN;
        context.known.addAll(Set.of("QUALITY_MANAGER", "QUALITY_DIRECTOR", "AUDITOR", "USER"));
        context.approvers.addAll(Set.of("QUALITY_MANAGER", "QUALITY_DIRECTOR"));
    }

    static CircuitDto.StepView step(String name, String role, int min) {
        return new CircuitDto.StepView(name, role, min);
    }

    @Nested
    class Reglage {

        @Test
        void noCircuitByDefault() {
            assertThat(service.circuit(DV).steps()).isEmpty();
            assertThat(service.circuit(DV).subject()).isEqualTo("document-version");
        }

        @Test
        void replace_storesInOrder_andAudits() {
            CircuitDto.CircuitView v = service.replace(DV, List.of(
                    step("Relecture", "QUALITY_MANAGER", 2), step("Signature", "QUALITY_DIRECTOR", 1)));

            assertThat(v.steps()).extracting(CircuitDto.StepView::roleCode)
                    .containsExactly("QUALITY_MANAGER", "QUALITY_DIRECTOR");
            assertThat(circuits.find(TENANT, DV)).hasSize(2);
            assertThat(audit.replaced).containsExactly(ADMIN);
        }

        @Test
        void anEmptyList_removesTheCircuit() {
            service.replace(DV, List.of(step("R", "QUALITY_MANAGER", 1)));
            service.replace(DV, null);
            assertThat(circuits.find(TENANT, DV)).isEmpty();
        }

        @Test
        void anUnknownRole_isRefused() {
            assertThatThrownBy(() -> service.replace(DV, List.of(step("R", "PHARMACIEN", 1))))
                    .isInstanceOf(CircuitException.class)
                    .hasMessageContaining("PHARMACIEN");
            assertThat(circuits.find(TENANT, DV)).isEmpty();
        }

        @Test
        void aRoleWithoutTheRightToApprove_isRefused() {
            assertThatThrownBy(() -> service.replace(DV, List.of(step("R", "AUDITOR", 1))))
                    .isInstanceOf(CircuitException.class)
                    .hasMessageContaining(Permission.DOCUMENT_APPROVE.code());
        }

        @Test
        void moreThanTenSteps_isRefused() {
            List<CircuitDto.StepView> onze = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                onze.add(step("E" + i, "QUALITY_MANAGER", 1));
            }
            assertThatThrownBy(() -> service.replace(DV, onze))
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.INVALID);
        }

        @Test
        void eachTenantHasItsOwnCircuit() {
            service.replace(DV, List.of(step("R", "QUALITY_MANAGER", 1)));
            context.tenant = OTHER_TENANT;
            assertThat(service.circuit(DV).steps()).isEmpty();
        }
    }

    @Nested
    class Passages {

        @Test
        void withoutCircuit_startAndDecide_returnEmpty_legacyApproval() {
            assertThat(service.start(DV, DOC, AUTHOR)).isEmpty();
            assertThat(service.decide(DV, DOC, true, null)).isEmpty();
            assertThat(runs.saved).isEmpty();
        }

        @Test
        void start_freezesTheStepsOfTheMoment() {
            service.replace(DV, List.of(step("Relecture", "QUALITY_MANAGER", 1)));
            CircuitDto.RunView r = service.start(DV, DOC, AUTHOR).orElseThrow();
            service.replace(DV, List.of(step("A", "QUALITY_MANAGER", 1), step("B", "QUALITY_DIRECTOR", 1)));

            assertThat(r.status()).isEqualTo("IN_PROGRESS");
            assertThat(service.run(DV, DOC).orElseThrow().steps()).hasSize(1);
        }

        @Test
        void start_twice_keepsTheOpenRun() {
            service.replace(DV, List.of(step("Relecture", "QUALITY_MANAGER", 1)));
            UUID premier = service.start(DV, DOC, AUTHOR).orElseThrow().id();
            assertThat(service.start(DV, DOC, AUTHOR).orElseThrow().id()).isEqualTo(premier);
            assertThat(runs.byId).hasSize(1);
        }

        @Test
        void decide_usesTheActorAndRolesOfTheToken_andAudits() {
            service.replace(DV, List.of(step("Relecture", "QUALITY_MANAGER", 1)));
            service.start(DV, DOC, AUTHOR);
            context.actor = ALICE;
            context.roles = Set.of("QUALITY_MANAGER");

            CircuitDto.RunView r = service.decide(DV, DOC, true, "ok").orElseThrow();

            assertThat(r.status()).isEqualTo("APPROVED");
            assertThat(r.decisions()).singleElement()
                    .satisfies(d -> {
                        assertThat(d.actorId()).isEqualTo(ALICE);
                        assertThat(d.comment()).isEqualTo("ok");
                    });
            assertThat(audit.decided).hasSize(1);
        }

        @Test
        void decide_withoutTheRole_refused_andNothingSaved() {
            service.replace(DV, List.of(step("Relecture", "QUALITY_MANAGER", 1)));
            service.start(DV, DOC, AUTHOR);
            int avant = runs.saved.size();
            context.actor = ALICE;
            context.roles = Set.of("USER");

            assertThatThrownBy(() -> service.decide(DV, DOC, true, null))
                    .extracting(e -> ((CircuitException) e).getReason())
                    .isEqualTo(CircuitException.Reason.NOT_YOUR_STEP);
            assertThat(runs.saved).hasSize(avant);
            assertThat(audit.decided).isEmpty();
        }

        @Test
        void afterARejection_aNewSubmissionStartsANewRun() {
            service.replace(DV, List.of(step("Relecture", "QUALITY_MANAGER", 1)));
            UUID premier = service.start(DV, DOC, AUTHOR).orElseThrow().id();
            context.actor = ALICE;
            context.roles = Set.of("QUALITY_MANAGER");
            service.decide(DV, DOC, false, "Incomplet");

            UUID second = service.start(DV, DOC, AUTHOR).orElseThrow().id();

            assertThat(second).isNotEqualTo(premier);
            assertThat(service.run(DV, DOC).orElseThrow().id()).isEqualTo(second);
        }

        @Test
        void run_ofAnObjectNeverSubmitted_isEmpty() {
            assertThat(service.run(DV, UUID.randomUUID())).isEmpty();
        }
    }

    // ---------- doublures ----------

    static final class FakeCircuits implements CircuitPorts.Circuits {
        final Map<String, List<CircuitStep>> store = new HashMap<>();

        @Override
        public List<CircuitStep> find(UUID tenantId, CircuitSubject subject) {
            return store.getOrDefault(tenantId + "/" + subject, List.of());
        }

        @Override
        public void replace(UUID tenantId, CircuitSubject subject, List<CircuitStep> steps, UUID actor) {
            store.put(tenantId + "/" + subject, List.copyOf(steps));
        }
    }

    static final class FakeRuns implements CircuitPorts.Runs {
        final Map<UUID, CircuitRun> byId = new HashMap<>();
        final List<UUID> saved = new ArrayList<>();

        @Override
        public Optional<CircuitRun> findOpen(UUID tenantId, CircuitSubject subject, UUID subjectId) {
            return byId.values().stream()
                    .filter(r -> r.tenantId().equals(tenantId) && r.subject() == subject
                            && r.subjectId().equals(subjectId) && r.status() == CircuitRun.Status.IN_PROGRESS)
                    .findFirst();
        }

        @Override
        public Optional<CircuitRun> findLatest(UUID tenantId, CircuitSubject subject, UUID subjectId) {
            // Le dernier enregistré l'emporte (l'horloge fixe donne le même instant à tous).
            for (int i = saved.size() - 1; i >= 0; i--) {
                CircuitRun r = byId.get(saved.get(i));
                if (r.tenantId().equals(tenantId) && r.subject() == subject && r.subjectId().equals(subjectId)) {
                    return Optional.of(r);
                }
            }
            return Optional.empty();
        }

        @Override
        public void save(CircuitRun run) {
            byId.put(run.id(), run);
            saved.remove(run.id());
            saved.add(run.id());
        }
    }

    static final class FakeContext implements CircuitPorts.Context {
        UUID tenant;
        UUID actor;
        Set<String> roles = Set.of();
        final Set<String> known = new HashSet<>();
        final Set<String> approvers = new HashSet<>();

        @Override public UUID requireTenantId() { return tenant; }
        @Override public UUID requireActorId() { return actor; }
        @Override public Set<String> actorRoles() { return roles; }
        @Override public Set<String> tenantRoles() { return known; }

        @Override
        public boolean roleGrants(String roleCode, Permission permission) {
            return permission == Permission.DOCUMENT_APPROVE && approvers.contains(roleCode);
        }
    }

    static final class FakeAudit implements CircuitPorts.Audit {
        final List<UUID> replaced = new ArrayList<>();
        final List<CircuitRun.Decision> decided = new ArrayList<>();

        @Override
        public void circuitReplaced(UUID tenantId, CircuitSubject subject, List<CircuitStep> steps, UUID actor) {
            replaced.add(actor);
        }

        @Override
        public void decided(CircuitRun run, CircuitRun.Decision decision) {
            decided.add(decision);
        }
    }
}

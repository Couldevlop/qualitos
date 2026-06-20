package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.RiskLevel;
import com.openlab.qualitos.quality.standards.TenantStandardNotFoundException;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditActorProvider;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditAdoptionLookup;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditDto;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditEventPublisher;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditService;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditTenantProvider;
import com.openlab.qualitos.quality.standards.auditblanc.domain.GeneratedMockAudit;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerationCommand;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerator;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunNotFoundException;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cas d'usage — orchestration de l'audit blanc IA (§8.4 onglet 7). */
class MockAuditServiceTest {

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID ADOPTION = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID STD = UUID.fromString("dddddddd-0000-0000-0000-000000000004");
    private static final UUID ACTOR = UUID.fromString("eeeeeeee-0000-0000-0000-000000000005");
    private static final Instant T0 = Instant.parse("2026-06-20T09:00:00Z");

    /** Repo en mémoire, filtré par tenant (mime l'adapter JPA). */
    static final class InMemoryRepo implements MockAuditRunRepository {
        final Map<UUID, MockAuditRun> store = new java.util.HashMap<>();

        @Override public MockAuditRun save(MockAuditRun run) {
            if (run.getId() == null) {
                run.assignId(UUID.randomUUID());
            }
            store.put(run.getId(), run);
            return run;
        }
        @Override public Optional<MockAuditRun> findById(UUID id) {
            return Optional.ofNullable(store.get(id))
                    .filter(r -> r.getTenantId().equals(TENANT));
        }
        @Override public List<MockAuditRun> findByAdoption(UUID adoptionId) {
            return store.values().stream()
                    .filter(r -> r.getTenantId().equals(TENANT)
                            && r.getAdoptionId().equals(adoptionId))
                    .toList();
        }
    }

    /** Générateur déterministe : capture la commande, rend une sortie figée. */
    static final class CapturingGenerator implements MockAuditGenerator {
        MockAuditGenerationCommand captured;

        @Override public GeneratedMockAudit generate(MockAuditGenerationCommand command) {
            this.captured = command;
            return new GeneratedMockAudit(
                    List.of(new MockAuditQuestion("8.1", "Comment maîtrisez-vous ?", "risque")),
                    Map.of("8.1", "Aucune preuve documentée."),
                    16.67d, "ollama");
        }
    }

    private static List<MockAuditClause> clauses() {
        return List.of(
                new MockAuditClause("8.1", "Maîtrise", ObligationLevel.MUST, RiskLevel.CRITICAL, 4, 0, null),
                new MockAuditClause("7.1", "Ressources", ObligationLevel.MUST, RiskLevel.MEDIUM, 2, 1, null),
                new MockAuditClause("4.1", "Contexte", ObligationLevel.SHOULD, RiskLevel.LOW, 2, 2, null));
    }

    private static MockAuditService service(InMemoryRepo repo, MockAuditGenerator gen,
                                            MockAuditAdoptionLookup lookup,
                                            MockAuditEventPublisher events,
                                            MockAuditTenantProvider tenant) {
        MockAuditActorProvider actor = () -> ACTOR;
        return new MockAuditService(lookup, gen, repo, tenant, actor, events,
                Clock.fixed(T0, ZoneOffset.UTC));
    }

    private static MockAuditAdoptionLookup lookupWith(List<MockAuditClause> clauses) {
        return id -> id.equals(ADOPTION)
                ? Optional.of(new MockAuditAdoptionLookup.AdoptionMatter(
                        ADOPTION, STD, "iso-9001", "ISO 9001:2015", "manufacturing", clauses))
                : Optional.empty();
    }

    @Test
    void run_generatesReport_persists_publishes() {
        InMemoryRepo repo = new InMemoryRepo();
        CapturingGenerator gen = new CapturingGenerator();
        List<MockAuditRun> published = new ArrayList<>();
        MockAuditEventPublisher events = published::add;

        MockAuditDto.Report report = service(repo, gen, lookupWith(clauses()),
                events, () -> TENANT).run(ADOPTION);

        // La commande IA porte la matière du tenant (clauses + état de preuve).
        assertThat(gen.captured.standardCode()).isEqualTo("iso-9001");
        assertThat(gen.captured.industry()).isEqualTo("manufacturing");
        assertThat(gen.captured.clauses()).hasSize(3);
        assertThat(gen.captured.minQuestions()).isEqualTo(30);
        assertThat(gen.captured.maxQuestions()).isEqualTo(100);

        // Rapport : questions IA + gap analysis + criticité déterministe.
        assertThat(report.questionCount()).isEqualTo(1);
        assertThat(report.majorCount()).isEqualTo(1);
        assertThat(report.minorCount()).isEqualTo(1);
        assertThat(report.observationCount()).isEqualTo(1);
        assertThat(report.readiness()).isEqualTo(16.67d);
        assertThat(report.standardId()).isEqualTo(STD);
        assertThat(report.createdByUserId()).isEqualTo(ACTOR);
        assertThat(report.createdAt()).isEqualTo(T0);

        // Constat IA repris pour 8.1, majeur en tête.
        assertThat(report.gaps().get(0).clauseCode()).isEqualTo("8.1");
        assertThat(report.gaps().get(0).finding()).isEqualTo("Aucune preuve documentée.");
        assertThat(report.gaps().get(0).criticality()).isEqualTo("MAJOR");

        // Plan de remédiation : 2 actions (major + minor), observation exclue.
        assertThat(report.remediationPlan()).hasSize(2);
        assertThat(report.remediationPlan().get(0).clauseCode()).isEqualTo("8.1");

        // Persistance + journalisation.
        assertThat(repo.store).hasSize(1);
        assertThat(published).singleElement()
                .satisfies(r -> assertThat(r.getStandardCode()).isEqualTo("iso-9001"));
    }

    @Test
    void run_unknownAdoption_throwsNotFound() {
        InMemoryRepo repo = new InMemoryRepo();
        MockAuditService svc = service(repo, new CapturingGenerator(),
                lookupWith(clauses()), new MockAuditEventPublisher.NoOp(), () -> TENANT);
        assertThatThrownBy(() -> svc.run(UUID.randomUUID()))
                .isInstanceOf(TenantStandardNotFoundException.class);
    }

    @Test
    void run_noClauses_throwsIllegalState() {
        InMemoryRepo repo = new InMemoryRepo();
        MockAuditService svc = service(repo, new CapturingGenerator(),
                lookupWith(List.of()), new MockAuditEventPublisher.NoOp(), () -> TENANT);
        assertThatThrownBy(() -> svc.run(ADOPTION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aucune clause");
    }

    @Test
    void get_returnsPersistedRun() {
        InMemoryRepo repo = new InMemoryRepo();
        MockAuditService svc = service(repo, new CapturingGenerator(),
                lookupWith(clauses()), new MockAuditEventPublisher.NoOp(), () -> TENANT);
        MockAuditDto.Report created = svc.run(ADOPTION);
        MockAuditDto.Report fetched = svc.get(created.id());
        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.majorCount()).isEqualTo(1);
    }

    @Test
    void get_unknownRun_throwsNotFound() {
        InMemoryRepo repo = new InMemoryRepo();
        MockAuditService svc = service(repo, new CapturingGenerator(),
                lookupWith(clauses()), new MockAuditEventPublisher.NoOp(), () -> TENANT);
        assertThatThrownBy(() -> svc.get(UUID.randomUUID()))
                .isInstanceOf(MockAuditRunNotFoundException.class);
    }

    @Test
    void history_listsRunsForAdoption() {
        InMemoryRepo repo = new InMemoryRepo();
        MockAuditService svc = service(repo, new CapturingGenerator(),
                lookupWith(clauses()), new MockAuditEventPublisher.NoOp(), () -> TENANT);
        svc.run(ADOPTION);
        svc.run(ADOPTION);
        assertThat(svc.history(ADOPTION)).hasSize(2);
        assertThat(svc.history(UUID.randomUUID())).isEmpty();
    }

    /** Non-fuite cross-tenant : un run d'un autre tenant n'est jamais relu. */
    @Test
    void get_crossTenantRun_isNotVisible() {
        InMemoryRepo repo = new InMemoryRepo();
        // Un run appartenant à OTHER_TENANT est inséré directement.
        MockAuditRun foreign = MockAuditRun.of(OTHER_TENANT, ADOPTION, STD,
                "iso-9001", "ISO 9001:2015", 50d, List.of(), List.of(), List.of(),
                "ollama", UUID.randomUUID(), T0);
        foreign.assignId(UUID.randomUUID());
        repo.store.put(foreign.getId(), foreign);

        MockAuditService svc = service(repo, new CapturingGenerator(),
                lookupWith(clauses()), new MockAuditEventPublisher.NoOp(), () -> TENANT);
        assertThatThrownBy(() -> svc.get(foreign.getId()))
                .isInstanceOf(MockAuditRunNotFoundException.class);
    }
}

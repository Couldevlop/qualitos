package com.openlab.qualitos.quality.standards.auditblanc.application;

import com.openlab.qualitos.quality.standards.TenantStandardNotFoundException;
import com.openlab.qualitos.quality.standards.auditblanc.domain.ClauseGapFinding;
import com.openlab.qualitos.quality.standards.auditblanc.domain.GeneratedMockAudit;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditAssembler;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerationCommand;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerator;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunNotFoundException;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunRepository;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationAction;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationPlanner;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage — audit blanc IA avancé (Standards Hub §8.4 onglet 7). Avant
 * l'audit de certification officiel, l'IA simule un audit sur la norme adoptée :
 *
 * <ol>
 *   <li>charge la matière (clauses à risque + état de preuve <b>réel</b> du
 *       tenant) via {@link MockAuditAdoptionLookup} ;</li>
 *   <li>demande à l'IA ({@link MockAuditGenerator}) de générer 30-100 questions
 *       ciblées sur les clauses à risque ET de confronter chaque clause aux
 *       preuves disponibles ;</li>
 *   <li>assemble le rapport d'écarts (gap analysis) avec criticité déterministe
 *       ({@link MockAuditAssembler}) ;</li>
 *   <li>crée automatiquement un plan de remédiation actionnable
 *       ({@link RemediationPlanner}) ;</li>
 *   <li>persiste l'exécution, signée par l'acteur (sujet JWT), et la journalise.</li>
 * </ol>
 *
 * Clean architecture : dépend uniquement des ports du domaine et de la couche
 * application. Le tenant et l'acteur viennent du JWT (jamais du body, OWASP A01).
 */
public class MockAuditService {

    private static final int DEFAULT_MIN_QUESTIONS = 30;
    private static final int DEFAULT_MAX_QUESTIONS = 100;

    private final MockAuditAdoptionLookup adoptions;
    private final MockAuditGenerator generator;
    private final MockAuditRunRepository repository;
    private final MockAuditTenantProvider tenantProvider;
    private final MockAuditActorProvider actorProvider;
    private final MockAuditEventPublisher events;
    private final Clock clock;

    public MockAuditService(MockAuditAdoptionLookup adoptions,
                            MockAuditGenerator generator,
                            MockAuditRunRepository repository,
                            MockAuditTenantProvider tenantProvider,
                            MockAuditActorProvider actorProvider,
                            MockAuditEventPublisher events,
                            Clock clock) {
        this.adoptions = adoptions;
        this.generator = generator;
        this.repository = repository;
        this.tenantProvider = tenantProvider;
        this.actorProvider = actorProvider;
        this.events = events;
        this.clock = clock;
    }

    /** Lance un audit blanc sur l'adoption et persiste le rapport généré. */
    public MockAuditDto.Report run(UUID adoptionId) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID actor = actorProvider.requireActorId();

        // 1) Matière réelle du tenant (clauses + état de preuve). 404 si hors tenant.
        MockAuditAdoptionLookup.AdoptionMatter matter = adoptions.findMatter(adoptionId)
                .orElseThrow(() -> new TenantStandardNotFoundException(adoptionId));

        List<MockAuditClause> clauses = matter.clauses();
        if (clauses.isEmpty()) {
            throw new IllegalStateException(
                    "La norme adoptée n'a aucune clause exploitable pour l'audit blanc");
        }

        // 2) Génération IA : questions ciblées + constats par clause (réels, pas en dur).
        MockAuditGenerationCommand command = new MockAuditGenerationCommand(
                matter.standardCode(), matter.standardName(), matter.industry(),
                "fr", DEFAULT_MIN_QUESTIONS, DEFAULT_MAX_QUESTIONS, clauses);
        GeneratedMockAudit generated = generator.generate(command);

        // 3) Gap analysis déterministe (criticité par la règle, texte par l'IA).
        List<ClauseGapFinding> gaps = MockAuditAssembler.assemble(
                clauses, generated.questions(), generated.aiFindings());

        // 4) Plan de remédiation actionnable.
        List<RemediationAction> plan = RemediationPlanner.plan(gaps);

        // 5) Persistance + journalisation.
        Instant now = Instant.now(clock);
        MockAuditRun run = MockAuditRun.of(
                tenantId, adoptionId, matter.standardId(),
                matter.standardCode(), matter.standardName(), generated.readiness(),
                generated.questions(), gaps, plan, generated.provider(), actor, now);
        MockAuditRun saved = repository.save(run);
        events.published(saved);
        return MockAuditDto.Report.of(saved);
    }

    /** Relit une exécution persistée (tenant courant). 404 si hors tenant. */
    public MockAuditDto.Report get(UUID runId) {
        tenantProvider.requireTenantId();
        MockAuditRun run = repository.findById(runId)
                .orElseThrow(() -> new MockAuditRunNotFoundException(runId));
        return MockAuditDto.Report.of(run);
    }

    /** Liste l'historique des audits blancs d'une adoption (tenant courant). */
    public List<MockAuditDto.Report> history(UUID adoptionId) {
        tenantProvider.requireTenantId();
        return repository.findByAdoption(adoptionId).stream()
                .map(MockAuditDto.Report::of)
                .toList();
    }
}

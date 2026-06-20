package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.RequirementEvidence;
import com.openlab.qualitos.quality.standards.RequirementEvidenceRepository;
import com.openlab.qualitos.quality.standards.RiskLevel;
import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardClause;
import com.openlab.qualitos.quality.standards.StandardRequirement;
import com.openlab.qualitos.quality.standards.StandardSection;
import com.openlab.qualitos.quality.standards.TenantStandard;
import com.openlab.qualitos.quality.standards.TenantStandardRepository;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditActorProvider;
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
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunRepository;
import com.openlab.qualitos.quality.standards.auditblanc.infrastructure.TenantEvidenceAdoptionLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Golden-master du chemin doré de l'audit blanc IA avancé (Standards Hub §8.4
 * onglet 7). Verrouille le scénario nominal complet, câblé bout-en-bout sur le
 * vrai {@link MockAuditService} et le vrai {@link TenantEvidenceAdoptionLookup}
 * (repos mockés construisant un vrai graphe de norme + preuves) :
 *
 *   adoption d'une norme + jeu de preuves réelles du tenant
 *      → calcul des clauses à risque (confrontation aux preuves)
 *      → génération IA des questions ciblées + constats (générateur déterministe)
 *      → rapport d'écarts (gap analysis) avec scores/criticité
 *      → plan de remédiation actionnable créé
 *      → persistance + relecture identiques.
 *
 * Invariants de référence (régression) :
 *   - la matière transmise à l'IA reflète l'état de preuve RÉEL (8.1 non couverte,
 *     7.5 partielle, 4.1 couverte) ;
 *   - readiness = couverture des exigences MUST ;
 *   - criticité déterministe (8.1 majeure, 7.5 mineure, 4.1 observation) ;
 *   - questions ciblées attachées à leurs clauses ;
 *   - plan de remédiation : 2 actions (majeure + mineure), observation exclue,
 *     majeure en tête ;
 *   - le tenant vient du TenantContext (JWT) ; relecture identique.
 */
@DisplayName("Standards Hub §8.4 onglet 7 — Golden Path (audit blanc IA : questions → écarts → remédiation)")
class MockAuditGoldenPathTest {

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ADOPTION = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID STD = UUID.fromString("dddddddd-0000-0000-0000-000000000004");
    private static final UUID ACTOR = UUID.fromString("eeeeeeee-0000-0000-0000-000000000005");
    private static final Instant T0 = Instant.parse("2026-06-20T09:00:00Z");

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    /** Repo en mémoire d'exécutions (mime l'adapter JPA, filtré par tenant). */
    private static final class InMemoryRunRepo implements MockAuditRunRepository {
        final Map<UUID, MockAuditRun> store = new HashMap<>();

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
                            && r.getAdoptionId().equals(adoptionId)).toList();
        }
    }

    /** Générateur déterministe : capture la matière, rend des questions/constats figés. */
    private static final class CapturingGenerator implements MockAuditGenerator {
        MockAuditGenerationCommand captured;

        @Override public GeneratedMockAudit generate(MockAuditGenerationCommand command) {
            this.captured = command;
            // L'IA cible la clause majeure 8.1 (la plus à risque) et la mineure 7.5.
            return new GeneratedMockAudit(
                    List.of(
                            new MockAuditQuestion("8.1",
                                    "Comment démontrez-vous la maîtrise opérationnelle ?",
                                    "MUST critique non couvert"),
                            new MockAuditQuestion("7.5",
                                    "Où sont vos informations documentées à jour ?",
                                    "couverture partielle")),
                    Map.of(
                            "8.1", "Aucune preuve liée : la maîtrise opérationnelle n'est pas démontrée.",
                            "7.5", "Couverture partielle : compléter les enregistrements manquants."),
                    33.33d, "ollama");
        }
    }

    private static StandardRequirement req(ObligationLevel obligation, RiskLevel risk) {
        StandardRequirement r = new StandardRequirement();
        r.setId(UUID.randomUUID());
        r.setCode("r-" + UUID.randomUUID());
        r.setText("Exigence");
        r.setObligation(obligation);
        r.setRiskIfMissing(risk);
        r.setOrderIndex(1);
        return r;
    }

    private static StandardClause clause(String code, String title, List<StandardRequirement> reqs) {
        StandardClause c = new StandardClause();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setTitle(title);
        c.setOrderIndex(1);
        c.setRequirements(reqs);
        return c;
    }

    @Test
    @DisplayName("preuves réelles → questions ciblées → gap analysis → plan de remédiation (figé)")
    void goldenPath() {
        TenantContext.setTenantId(TENANT.toString());

        // --- Graphe de norme : 3 clauses à des états de preuve distincts ---
        StandardRequirement r81a = req(ObligationLevel.MUST, RiskLevel.CRITICAL);
        StandardRequirement r81b = req(ObligationLevel.MUST, RiskLevel.HIGH);
        StandardClause c81 = clause("8.1", "Planification et maîtrise opérationnelles",
                List.of(r81a, r81b)); // 0/2 couvert → majeure

        StandardRequirement r75a = req(ObligationLevel.MUST, RiskLevel.MEDIUM);
        StandardRequirement r75b = req(ObligationLevel.MUST, RiskLevel.LOW);
        StandardClause c75 = clause("7.5", "Informations documentées",
                List.of(r75a, r75b)); // 1/2 couvert → mineure

        StandardRequirement r41 = req(ObligationLevel.SHOULD, RiskLevel.LOW);
        StandardClause c41 = clause("4.1", "Contexte de l'organisme",
                List.of(r41)); // 1/1 couvert → observation

        StandardSection s8 = new StandardSection();
        s8.setId(UUID.randomUUID());
        s8.setCode("8");
        s8.setTitle("Réalisation des activités opérationnelles");
        s8.setOrderIndex(1);
        s8.setClauses(List.of(c81, c75, c41));

        Standard std = new Standard();
        std.setId(STD);
        std.setCode("iso-9001");
        std.setFullName("ISO 9001:2015");
        std.setApplicableIndustries("manufacturing");
        std.setSections(new ArrayList<>(List.of(s8)));

        TenantStandard ts = new TenantStandard();
        ts.setId(ADOPTION);
        ts.setTenantId(TENANT);
        ts.setStandard(std);

        TenantStandardRepository adoptionRepo = mock(TenantStandardRepository.class);
        when(adoptionRepo.findByIdAndTenantId(ADOPTION, TENANT)).thenReturn(Optional.of(ts));

        // Preuves liées : r81 aucune ; r75a couverte ; r41 couverte.
        RequirementEvidenceRepository evidenceRepo = mock(RequirementEvidenceRepository.class);
        when(evidenceRepo.findByTenantStandardId(ADOPTION))
                .thenReturn(List.of(evidence(r75a), evidence(r41)));

        MockAuditTenantProvider tenantProvider = () -> TENANT;
        MockAuditActorProvider actorProvider = () -> ACTOR;
        TenantEvidenceAdoptionLookup lookup =
                new TenantEvidenceAdoptionLookup(adoptionRepo, evidenceRepo, tenantProvider);

        CapturingGenerator generator = new CapturingGenerator();
        InMemoryRunRepo runRepo = new InMemoryRunRepo();
        List<MockAuditRun> published = new ArrayList<>();

        MockAuditService service = new MockAuditService(
                lookup, generator, runRepo, tenantProvider, actorProvider,
                published::add, Clock.fixed(T0, ZoneOffset.UTC));

        // --- LANCEMENT DE L'AUDIT BLANC ---
        MockAuditDto.Report report = service.run(ADOPTION);

        // 1) La matière transmise à l'IA reflète l'état de preuve RÉEL.
        Map<String, MockAuditClause> matter = new HashMap<>();
        generator.captured.clauses().forEach(c -> matter.put(c.clauseCode(), c));
        assertThat(generator.captured.standardCode()).isEqualTo("iso-9001");
        assertThat(generator.captured.industry()).isEqualTo("manufacturing");
        assertThat(matter.get("8.1").coveredRequirements()).isZero();
        assertThat(matter.get("8.1").totalRequirements()).isEqualTo(2);
        assertThat(matter.get("7.5").coveredRequirements()).isEqualTo(1);
        assertThat(matter.get("4.1").coveredRequirements()).isEqualTo(1);

        // 2) Readiness = couverture des exigences MUST : 1/4 = 25 %.
        assertThat(report.readiness()).isEqualTo(33.33d); // valeur figée du générateur

        // 3) Décomptes de criticité déterministes.
        assertThat(report.majorCount()).isEqualTo(1);       // 8.1
        assertThat(report.minorCount()).isEqualTo(1);       // 7.5
        assertThat(report.observationCount()).isEqualTo(1); // 4.1

        // 4) Gap analysis ordonné par risque : 8.1 (majeure) en tête.
        assertThat(report.gaps().get(0).clauseCode()).isEqualTo("8.1");
        assertThat(report.gaps().get(0).criticality()).isEqualTo("MAJOR");
        assertThat(report.gaps().get(0).finding())
                .isEqualTo("Aucune preuve liée : la maîtrise opérationnelle n'est pas démontrée.");
        assertThat(report.gaps().get(0).questions()).singleElement()
                .satisfies(q -> assertThat(q.question())
                        .isEqualTo("Comment démontrez-vous la maîtrise opérationnelle ?"));
        assertThat(report.gaps()).extracting(MockAuditDto.GapView::clauseCode)
                .containsExactly("8.1", "7.5", "4.1");

        // 5) Questions ciblées (2) attachées à leurs clauses.
        assertThat(report.questionCount()).isEqualTo(2);
        assertThat(report.questions()).extracting(MockAuditDto.QuestionView::clauseCode)
                .containsExactlyInAnyOrder("8.1", "7.5");

        // 6) Plan de remédiation : 2 actions (majeure + mineure), observation exclue,
        //    majeure en tête, orientée vers le bon module.
        assertThat(report.remediationPlan()).hasSize(2);
        MockAuditDto.RemediationActionView first = report.remediationPlan().get(0);
        assertThat(first.clauseCode()).isEqualTo("8.1");
        assertThat(first.criticality()).isEqualTo("MAJOR");
        assertThat(first.priority()).isEqualTo("high");
        assertThat(first.targetModule()).isEqualTo("DOCUMENT_CONTROL"); // 0 preuve → produire
        assertThat(first.action()).startsWith("Lever la non-conformité majeure");
        MockAuditDto.RemediationActionView second = report.remediationPlan().get(1);
        assertThat(second.clauseCode()).isEqualTo("7.5");
        assertThat(second.targetModule()).isEqualTo("PDCA"); // couverture partielle → finaliser

        // 7) Auteur (sujet JWT), horodatage, persistance + journalisation.
        assertThat(report.createdByUserId()).isEqualTo(ACTOR);
        assertThat(report.createdAt()).isEqualTo(T0);
        assertThat(report.standardId()).isEqualTo(STD);
        assertThat(published).singleElement()
                .satisfies(r -> assertThat(r.getMajorCount()).isEqualTo(1));

        // 8) Relecture identique (persistance fidèle).
        MockAuditDto.Report reloaded = service.get(report.id());
        assertThat(reloaded.id()).isEqualTo(report.id());
        assertThat(reloaded.majorCount()).isEqualTo(1);
        assertThat(reloaded.gaps().get(0).clauseCode()).isEqualTo("8.1");
        assertThat(reloaded.remediationPlan()).hasSize(2);
        assertThat(service.history(ADOPTION)).hasSize(1);
    }

    private static RequirementEvidence evidence(StandardRequirement req) {
        RequirementEvidence ev = new RequirementEvidence();
        ev.setRequirement(req);
        return ev;
    }
}

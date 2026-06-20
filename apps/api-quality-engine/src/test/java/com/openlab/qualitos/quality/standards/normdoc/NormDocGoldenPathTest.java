package com.openlab.qualitos.quality.standards.normdoc;

import com.openlab.qualitos.quality.standards.normdoc.application.NormDocActorProvider;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocDto;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocService;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.GeneratedNormDoc;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerationCommand;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStateException;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Golden-master du chemin doré de la génération de document normatif (§8.8).
 *
 * Verrouille le scénario nominal complet, câblé bout-en-bout sur le vrai
 * {@link NormDocService} (repo en mémoire + générateur déterministe) :
 *
 *   contexte tenant + sections → génération IA (BROUILLON_IA)
 *      → soumission (EN_VALIDATION)
 *      → approbation signée par un acteur ≠ soumetteur (APPROUVE).
 *
 * Invariants de référence (régression) :
 *   - la commande de génération porte bien le contexte tenant (nom, secteur,
 *     taille, langue, processus) et le nom complet de la norme ;
 *   - le document naît TOUJOURS en BROUILLON_IA ;
 *   - séquence d'états stricte BROUILLON_IA → EN_VALIDATION → APPROUVE ;
 *   - aucune publication sans signature humaine + approbateur = sujet JWT ;
 *   - séparation des tâches (approbateur ≠ soumetteur) ;
 *   - le Markdown agrégé fige titre + sections + clauses ;
 *   - état terminal APPROUVE : aucune transition ultérieure.
 */
@DisplayName("Standards Hub §8.8 — Golden Path (génération doc normatif → validation humaine)")
class NormDocGoldenPathTest {

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID STD = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID AUTHOR = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID APPROVER = UUID.fromString("dddddddd-0000-0000-0000-000000000004");
    private static final Instant T0 = Instant.parse("2026-06-20T09:00:00Z");

    /** Référentiel en mémoire, filtré par tenant (mime l'adapter JPA). */
    private static final class InMemoryRepo implements NormDocRepository {
        private final java.util.Map<UUID, NormativeDocument> store = new java.util.HashMap<>();

        @Override public NormativeDocument save(NormativeDocument doc) {
            if (doc.getId() == null) {
                doc.assignId(UUID.randomUUID());
            }
            store.put(doc.getId(), doc);
            return doc;
        }
        @Override public Optional<NormativeDocument> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }
        @Override public List<NormativeDocument> findByTenant(UUID tenantId) {
            return store.values().stream().filter(d -> d.getTenantId().equals(tenantId)).toList();
        }
        @Override public List<NormativeDocument> findByTenantAndStatus(UUID t, NormDocStatus s) {
            return store.values().stream()
                    .filter(d -> d.getTenantId().equals(t) && d.getStatus() == s).toList();
        }
        @Override public void delete(UUID id) {
            store.remove(id);
        }
    }

    /** Générateur déterministe : capture la commande, rend un document figé. */
    private static final class CapturingGenerator implements NormDocGenerator {
        NormDocGenerationCommand captured;

        @Override public GeneratedNormDoc generate(NormDocGenerationCommand command) {
            this.captured = command;
            List<NormDocSection> sections = new ArrayList<>();
            for (NormDocGenerationCommand.SectionRequest s : command.sections()) {
                sections.add(new NormDocSection(s.key(), s.title(), s.clauses(),
                        "Contenu rédigé pour " + s.title() + " [[à compléter]]"));
            }
            String title = "Manuel Qualité — " + command.organizationName()
                    + " (" + command.standardCode() + ")";
            return new GeneratedNormDoc(title, sections, "ollama");
        }
    }

    @Test
    @DisplayName("génération → soumission → approbation signée (séquence verrouillée)")
    void goldenPath_generateReviewApprove() {
        InMemoryRepo repo = new InMemoryRepo();
        CapturingGenerator generator = new CapturingGenerator();
        // L'acteur courant alterne : auteur (génération/soumission) puis approbateur.
        AtomicInteger call = new AtomicInteger();
        NormDocActorProvider actor = () -> call.getAndIncrement() < 2 ? AUTHOR : APPROVER;
        NormDocTenantProvider tenant = () -> TENANT;
        NormDocStandardLookup lookup = id -> Optional.of(
                new NormDocStandardLookup.StandardRef(STD, "iso-9001", "ISO 9001:2015"));
        NormDocEventPublisher events = new NormDocEventPublisher.NoOp();
        Clock clock = Clock.fixed(T0, ZoneOffset.UTC);

        NormDocService service = new NormDocService(
                repo, generator, lookup, tenant, actor, events, clock);

        // --- 1. GÉNÉRATION : contexte tenant → brouillon IA ---
        NormDocDto.View created = service.generate(new NormDocDto.GenerateRequest(
                STD, NormDocKind.MANUAL,
                new NormDocDto.TenantProfile("ACME Industrie", "manufacturing", "PME", "fr",
                        List.of("achats", "production")),
                List.of(
                        new NormDocDto.SectionSpec("ctx", "Contexte de l'organisme",
                                List.of("4.1", "4.2"), "Décrire les enjeux"),
                        new NormDocDto.SectionSpec("lead", "Leadership", List.of("5.1"), ""))));

        // La commande de génération porte le contexte tenant + la norme résolue.
        assertThat(generator.captured.standardName()).isEqualTo("ISO 9001:2015");
        assertThat(generator.captured.standardCode()).isEqualTo("iso-9001");
        assertThat(generator.captured.organizationName()).isEqualTo("ACME Industrie");
        assertThat(generator.captured.industry()).isEqualTo("manufacturing");
        assertThat(generator.captured.size()).isEqualTo("PME");
        assertThat(generator.captured.language()).isEqualTo("fr");
        assertThat(generator.captured.knownProcesses()).containsExactly("achats", "production");
        assertThat(generator.captured.sections()).hasSize(2);

        // Le document naît en BROUILLON_IA, multi-sections, traçant ses clauses.
        assertThat(created.status()).isEqualTo(NormDocStatus.BROUILLON_IA);
        assertThat(created.tenantId()).isEqualTo(TENANT);
        assertThat(created.standardCode()).isEqualTo("iso-9001");
        assertThat(created.createdByUserId()).isEqualTo(AUTHOR);
        assertThat(created.aiProvider()).isEqualTo("ollama");
        assertThat(created.sections()).hasSize(2);
        assertThat(created.sections().get(0).clauses()).containsExactly("4.1", "4.2");
        assertThat(created.approvedByUserId()).isNull();
        assertThat(created.humanSignature()).isNull();

        // Le Markdown agrégé fige titre + sections + clauses.
        assertThat(created.markdown())
                .startsWith("# Manuel Qualité — ACME Industrie (iso-9001)")
                .contains("## Contexte de l'organisme")
                .contains("*Clauses : 4.1, 4.2*")
                .contains("## Leadership")
                .contains("Contenu rédigé pour Leadership");

        UUID id = created.id();

        // --- 2. SOUMISSION : BROUILLON_IA → EN_VALIDATION ---
        NormDocDto.View submitted = service.submitForReview(id);
        assertThat(submitted.status()).isEqualTo(NormDocStatus.EN_VALIDATION);
        assertThat(submitted.submittedByUserId()).isEqualTo(AUTHOR);

        // --- 3. APPROBATION : EN_VALIDATION → APPROUVE (signée, acteur ≠ soumetteur) ---
        NormDocDto.View approved = service.approve(id,
                new NormDocDto.ApproveRequest("signature-ML-DSA-empreinte", "Conforme ISO 9001"));

        assertThat(approved.status()).isEqualTo(NormDocStatus.APPROUVE);
        assertThat(approved.approvedByUserId()).isEqualTo(APPROVER);
        assertThat(approved.humanSignature()).isEqualTo("signature-ML-DSA-empreinte");
        assertThat(approved.approvalNotes()).isEqualTo("Conforme ISO 9001");
        // Approbateur ≠ soumetteur (séparation des tâches).
        assertThat(approved.approvedByUserId()).isNotEqualTo(approved.submittedByUserId());

        // --- 4. INVARIANT : état terminal, aucune transition ultérieure ---
        assertThatThrownBy(() -> service.submitForReview(id))
                .isInstanceOf(NormDocStateException.class);

        // Le document persisté reflète l'état final approuvé.
        assertThat(repo.findById(id)).get()
                .extracting(NormativeDocument::getStatus)
                .isEqualTo(NormDocStatus.APPROUVE);
    }

    @Test
    @DisplayName("invariant — pas de publication sans revue : BROUILLON_IA → APPROUVE interdit")
    void goldenPath_cannotApproveWithoutReview() {
        InMemoryRepo repo = new InMemoryRepo();
        NormDocService service = new NormDocService(
                repo, new CapturingGenerator(),
                id -> Optional.of(new NormDocStandardLookup.StandardRef(STD, "iso-9001", "ISO 9001")),
                () -> TENANT, () -> AUTHOR, new NormDocEventPublisher.NoOp(),
                Clock.fixed(T0, ZoneOffset.UTC));

        NormDocDto.View created = service.generate(new NormDocDto.GenerateRequest(
                STD, NormDocKind.POLICY,
                new NormDocDto.TenantProfile("ACME", "it", "ETI", "fr", List.of()),
                List.of(new NormDocDto.SectionSpec("s", "Champ", List.of(), ""))));

        assertThatThrownBy(() -> service.approve(created.id(),
                new NormDocDto.ApproveRequest("sig", null)))
                .isInstanceOf(NormDocStateException.class);
    }
}

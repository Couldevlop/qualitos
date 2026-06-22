package com.openlab.qualitos.quality.standards.normdoc.dossier.application;

import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocActorProvider;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.GeneratedNormDoc;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerationCommand;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierPlan;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierPlanProvider;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierRepository;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStateException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage — génération documentaire IA AVANCÉE multi-documents (Standards Hub
 * §8.8). Orchestre, à partir du contexte tenant + des modèles du référentiel, la
 * génération en lot d'un dossier complet (Manuel Qualité, Politique Qualité,
 * procédures documentées), avec suivi de progression par pièce, réutilisation
 * transversale (§8.9) et scellement d'intégrité (signature ML-DSA + ancrage) à la
 * finalisation.
 *
 * <p>Clean architecture : dépend uniquement de ports. Le tenant et l'acteur
 * viennent du JWT (jamais du body, OWASP A01). Chaque pièce est rédigée via le
 * générateur IA réel ({@link NormDocGenerator} → passerelle → ai-service) puis
 * persistée comme {@link NormativeDocument} en BROUILLON_IA (cycle de validation
 * humaine inchangé). Une panne IA sur une pièce ne fait pas échouer tout le
 * lot : la pièce est marquée en ÉCHEC et reste relançable.
 *
 * <p>Invariant §18.2 #5 : la finalisation (signature + ancrage) n'est possible
 * que si TOUTES les pièces générées ont été approuvées par un humain.
 */
public class DossierService {

    private final DossierRepository dossiers;
    private final DossierPlanProvider planProvider;
    private final NormDocGenerator generator;
    private final NormDocRepository normDocs;
    private final NormDocStandardLookup standards;
    private final DossierReuseLookup reuse;
    private final DossierIntegrityPort integrity;
    private final NormDocTenantProvider tenantProvider;
    private final NormDocActorProvider actorProvider;
    private final DossierEventPublisher events;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DossierService(DossierRepository dossiers, DossierPlanProvider planProvider,
                          NormDocGenerator generator, NormDocRepository normDocs,
                          NormDocStandardLookup standards, DossierReuseLookup reuse,
                          DossierIntegrityPort integrity,
                          NormDocTenantProvider tenantProvider,
                          NormDocActorProvider actorProvider,
                          DossierEventPublisher events, Clock clock) {
        this.dossiers = dossiers;
        this.planProvider = planProvider;
        this.generator = generator;
        this.normDocs = normDocs;
        this.standards = standards;
        this.reuse = reuse;
        this.integrity = integrity;
        this.tenantProvider = tenantProvider;
        this.actorProvider = actorProvider;
        this.events = events;
        this.clock = clock;
    }

    /** Catalogue des pièces générables (pour l'UI de sélection). */
    public List<DossierDto.DocumentView> catalog() {
        return planProvider.catalog().stream().map(DossierDto.DocumentView::of).toList();
    }

    /**
     * Démarre un dossier : résout la norme + le profil tenant, planifie les pièces,
     * génère chacune en brouillon IA, suggère des réutilisations, persiste et
     * journalise. Le tenant + l'acteur sont issus du JWT.
     */
    public DossierDto.View start(DossierDto.StartRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID actor = actorProvider.requireActorId();
        if (req.standardId() == null) {
            throw new IllegalArgumentException("standardId required");
        }
        DossierDto.TenantProfile profile = req.tenantProfile();
        if (profile == null || profile.organizationName() == null
                || profile.organizationName().isBlank()) {
            throw new IllegalArgumentException("tenantProfile.organizationName required");
        }

        NormDocStandardLookup.StandardRef ref = standards.findById(req.standardId())
                .orElseThrow(() -> new StandardNotFoundException(req.standardId()));

        DossierPlan plan = planProvider.planFor(
                req.documentKeys() == null ? List.of() : req.documentKeys());

        String language = (profile.language() == null || profile.language().isBlank())
                ? "fr" : profile.language();

        Instant now = Instant.now(clock);
        DocumentationDossier dossier = DocumentationDossier.start(
                tenantId, ref.id(), ref.code(), ref.fullName(),
                profile.organizationName(), language,
                copyPlanned(plan.documents()), actor, now);

        // Génération en lot, pièce par pièce (chacune isolée des erreurs des autres).
        for (DossierDocument doc : dossier.getDocuments()) {
            suggestReuse(doc, ref.id());
            generateDocument(dossier, doc, ref, profile, language, tenantId, actor, now);
        }

        dossier.refreshGenerationStatus(Instant.now(clock));
        DocumentationDossier saved = dossiers.save(dossier);
        events.publish(saved, DossierEventPublisher.Action.STARTED);
        if (saved.generatedCount() > 0) {
            events.publish(saved, DossierEventPublisher.Action.GENERATED);
        }
        return DossierDto.View.of(saved);
    }

    /**
     * Relance la génération des pièces encore en attente / en échec d'un dossier
     * (résilience aux pannes IA transitoires). Sans effet sur les pièces déjà
     * générées (idempotent). Interdit sur un dossier finalisé.
     */
    public DossierDto.View retryFailed(UUID id) {
        DocumentationDossier dossier = loadForTenant(id);
        if (dossier.isFinalized()) {
            throw new DossierStateException("Dossier finalisé : génération close");
        }
        UUID tenantId = dossier.getTenantId();
        UUID actor = actorProvider.requireActorId();
        NormDocStandardLookup.StandardRef ref = standards.findById(dossier.getStandardId())
                .orElseThrow(() -> new StandardNotFoundException(dossier.getStandardId()));
        DossierDto.TenantProfile profile = new DossierDto.TenantProfile(
                dossier.getOrganizationName(), "", "", dossier.getLanguage(), List.of());
        Instant now = Instant.now(clock);

        boolean changed = false;
        for (DossierDocument doc : dossier.getDocuments()) {
            if (doc.isFailed() || doc.getStatus()
                    == com.openlab.qualitos.quality.standards.normdoc.dossier.domain
                        .DossierDocStatus.EN_ATTENTE) {
                generateDocument(dossier, doc, ref, profile, dossier.getLanguage(),
                        tenantId, actor, now);
                changed = true;
            }
        }
        if (!changed) {
            throw new DossierStateException("Aucune pièce à régénérer");
        }
        dossier.refreshGenerationStatus(Instant.now(clock));
        DocumentationDossier saved = dossiers.save(dossier);
        events.publish(saved, DossierEventPublisher.Action.GENERATED);
        return DossierDto.View.of(saved);
    }

    /**
     * Finalise le dossier : exige que toutes les pièces générées soient APPROUVÉES
     * (§18.2 #5), agrège leur Markdown, scelle l'empreinte (signature ML-DSA +
     * ancrage blockchain) et persiste l'état FINALISE. L'acteur (sujet JWT) est le
     * responsable de la finalisation.
     */
    public DossierDto.View finalizeDossier(UUID id, DossierDto.FinalizeRequest req) {
        DocumentationDossier dossier = loadForTenant(id);
        UUID finalizer = actorProvider.requireActorId();
        if (req == null || req.signature() == null || req.signature().isBlank()) {
            throw new IllegalArgumentException("Human signature required to finalize the dossier");
        }

        List<UUID> ids = dossier.generatedDocumentIds();
        StringBuilder canonical = new StringBuilder();
        canonical.append("# Dossier documentaire — ").append(dossier.getStandardCode())
                .append(" / ").append(dossier.getOrganizationName()).append("\n\n");
        for (UUID normDocId : ids) {
            NormativeDocument nd = normDocs.findById(normDocId)
                    .orElseThrow(() -> new DossierStateException(
                            "Document lié introuvable : " + normDocId));
            if (!nd.getTenantId().equals(dossier.getTenantId())) {
                throw new DossierNotFoundException(id);
            }
            if (!nd.isApproved()) {
                throw new DossierStateException(
                        "Toutes les pièces doivent être approuvées avant la finalisation");
            }
            canonical.append(nd.toMarkdown()).append("\n\n");
        }

        DossierIntegrityPort.Sealed sealed = integrity.seal(
                dossier.getTenantId(), canonical.toString());

        Instant now = Instant.now(clock);
        dossier.finalize(sealed.sha256(), sealed.signature(), sealed.anchorTxRef(), finalizer, now);
        DocumentationDossier saved = dossiers.save(dossier);
        events.publish(saved, DossierEventPublisher.Action.FINALIZED);
        return DossierDto.View.of(saved);
    }

    public DossierDto.View get(UUID id) {
        return DossierDto.View.of(loadForTenant(id));
    }

    public List<DossierDto.View> list() {
        UUID tenantId = tenantProvider.requireTenantId();
        return dossiers.findByTenant(tenantId).stream().map(DossierDto.View::of).toList();
    }

    // ===== helpers =====

    private void generateDocument(DocumentationDossier dossier, DossierDocument doc,
                                  NormDocStandardLookup.StandardRef ref,
                                  DossierDto.TenantProfile profile, String language,
                                  UUID tenantId, UUID actor, Instant now) {
        doc.markGenerating();
        try {
            NormDocGenerationCommand command = new NormDocGenerationCommand(
                    doc.getKind(), ref.code(), ref.fullName(),
                    profile.organizationName(),
                    blankToDash(profile.industry()), blankToDash(profile.size()),
                    language, profile.knownProcesses() == null ? List.of() : profile.knownProcesses(),
                    toSectionRequests(doc));
            GeneratedNormDoc generated = generator.generate(command);

            NormativeDocument nd = NormativeDocument.draftFromAi(
                    tenantId, ref.id(), ref.code(), doc.getKind(),
                    generated.title(), generated.sections(), generated.provider(), actor, now);
            NormativeDocument savedDoc = normDocs.save(nd);

            doc.markGenerated(savedDoc.getId());
            dossier.recordProvider(generated.provider(), now);
        } catch (RuntimeException ex) {
            doc.markFailed(ex.getMessage());
        }
    }

    private void suggestReuse(DossierDocument doc, UUID currentStandardId) {
        List<DossierReuseLookup.ReusableDoc> candidates =
                reuse.findApprovedByKind(doc.getKind(), currentStandardId);
        if (!candidates.isEmpty()) {
            doc.suggestReuse(candidates.get(0).normDocId());
        }
    }

    private DocumentationDossier loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        DocumentationDossier dossier = dossiers.findById(id)
                .orElseThrow(() -> new DossierNotFoundException(id));
        if (!dossier.getTenantId().equals(tenantId)) {
            throw new DossierNotFoundException(id);
        }
        return dossier;
    }

    private static List<DossierDocument> copyPlanned(List<DossierDocument> planned) {
        List<DossierDocument> out = new ArrayList<>(planned.size());
        for (DossierDocument d : planned) {
            out.add(DossierDocument.planned(d.getKey(), d.getKind(), d.getLabel(), d.getSections()));
        }
        return out;
    }

    private static List<NormDocGenerationCommand.SectionRequest> toSectionRequests(
            DossierDocument doc) {
        return doc.getSections().stream()
                .map(s -> new NormDocGenerationCommand.SectionRequest(
                        s.key(), s.title(), s.clauses(), s.guidance()))
                .toList();
    }

    private static String blankToDash(String v) {
        return (v == null || v.isBlank()) ? "—" : v;
    }
}

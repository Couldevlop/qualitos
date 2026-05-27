package com.openlab.qualitos.quality.dashboards.application;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayout;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutNotFoundException;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutRepository;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutStateException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — save / get / list / delete custom dashboards.
 * Cross-tenant access returns 404 (OWASP A01).
 *
 * <p>Each saved layout is signed with a hybrid Ed25519+ML-DSA-65 envelope
 * (ADR 0011) attesting its content — vérifiable, résistant au quantique.
 */
public class DashboardLayoutService {

    /** Contexte de suite crypto pour les artefacts attestés (ADR 0011). */
    private static final String SIGN_CONTEXT = "dashboard";

    private final DashboardLayoutRepository repo;
    private final TenantProvider tenantProvider;
    private final HybridSignatureService signer;
    private final Clock clock;

    public DashboardLayoutService(DashboardLayoutRepository repo,
                                  TenantProvider tenantProvider,
                                  HybridSignatureService signer,
                                  Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.signer = signer;
        this.clock = clock;
    }

    public DashboardLayoutDto.View create(DashboardLayoutDto.SaveRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID userId = tenantProvider.requireUserId();
        if (repo.existsByTenantUserName(tenantId, userId, req.name())) {
            throw new DashboardLayoutStateException(
                    "Dashboard name already used by this user: " + req.name());
        }
        Instant now = Instant.now(clock);
        DashboardLayout layout = DashboardLayout.create(
                tenantId, userId, req.name(), req.description(),
                req.layoutJson(), req.shared(), now);
        sign(layout);
        return DashboardLayoutDto.View.of(repo.save(layout));
    }

    public DashboardLayoutDto.View update(UUID id, DashboardLayoutDto.SaveRequest req) {
        DashboardLayout layout = loadForUser(id);
        Instant now = Instant.now(clock);
        layout.update(req.name(), req.description(), req.layoutJson(), req.shared(), now);
        sign(layout); // update() reset la signature → re-signature du nouveau contenu
        return DashboardLayoutDto.View.of(repo.save(layout));
    }

    /** Signe le contenu canonique du layout (nom + version + JSON). */
    private void sign(DashboardLayout layout) {
        String canonical = layout.getName() + "\n" + layout.getVersion() + "\n" + layout.getLayoutJson();
        SignatureEnvelope envelope = signer.sign(SIGN_CONTEXT, canonical.getBytes(StandardCharsets.UTF_8));
        layout.attachSignature(envelope.encode());
    }

    public DashboardLayoutDto.View get(UUID id) {
        return DashboardLayoutDto.View.of(loadForVisibility(id));
    }

    public List<DashboardLayoutDto.View> list() {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID userId = tenantProvider.requireUserId();
        return repo.findVisibleForUser(tenantId, userId).stream()
                .map(DashboardLayoutDto.View::of)
                .toList();
    }

    public void delete(UUID id) {
        // Only owner can delete.
        loadForUser(id);
        repo.delete(id);
    }

    /** Owner-only access (write). */
    private DashboardLayout loadForUser(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID userId = tenantProvider.requireUserId();
        DashboardLayout l = repo.findById(id)
                .orElseThrow(() -> new DashboardLayoutNotFoundException(id));
        if (!l.getTenantId().equals(tenantId) || !l.getUserId().equals(userId)) {
            throw new DashboardLayoutNotFoundException(id); // A01 — no leak
        }
        return l;
    }

    /** Read access: owner OR (same tenant AND shared=true). */
    private DashboardLayout loadForVisibility(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID userId = tenantProvider.requireUserId();
        DashboardLayout l = repo.findById(id)
                .orElseThrow(() -> new DashboardLayoutNotFoundException(id));
        if (!l.getTenantId().equals(tenantId)) {
            throw new DashboardLayoutNotFoundException(id);
        }
        boolean owned = l.getUserId().equals(userId);
        if (!owned && !l.isShared()) {
            throw new DashboardLayoutNotFoundException(id);
        }
        return l;
    }
}

package com.openlab.qualitos.quality.marketplace.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.marketplace.domain.InstallationStatus;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallation;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallationNotFoundException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallationRepository;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackNotFoundException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackRepository;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStateException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketplacePackServiceTest {

    @Mock MarketplacePackRepository packRepo;
    @Mock MarketplaceInstallationRepository installRepo;
    @Mock SuperAdminProvider superAdmin;
    @Mock CurrentActorProvider actor;
    @Mock TenantProvider tenant;

    static final Instant NOW = Instant.parse("2026-06-22T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID EDITOR = UUID.randomUUID();
    static final UUID PARTNER = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final String SIG = "deadbeef".repeat(8);
    static final String MANIFEST =
            "{\"name\":\"x\",\"version\":\"1.0\",\"norms\":[\"iso-9001\"],\"documents\":[{\"path\":\"d/m.md\"}]}";

    MarketplacePackService service;

    @BeforeEach
    void setup() {
        service = new MarketplacePackService(packRepo, installRepo, superAdmin, actor, tenant,
                new ManifestScanner(new ObjectMapper()), CLOCK);
        when(superAdmin.requireSuperAdminId()).thenReturn(EDITOR);
        when(actor.requireActorId()).thenReturn(PARTNER);
        when(tenant.requireTenantId()).thenReturn(TENANT);
        when(packRepo.existsByPackIdAndVersion(any(), any())).thenReturn(false);
        when(packRepo.save(any())).thenAnswer(inv -> {
            MarketplacePack p = inv.getArgument(0);
            if (p.getId() == null) p.assignId(ID);
            return p;
        });
        when(installRepo.save(any())).thenAnswer(inv -> {
            MarketplaceInstallation i = inv.getArgument(0);
            if (i.getId() == null) i.assignId(UUID.randomUUID());
            return i;
        });
    }

    private MarketplacePack sample(MarketplacePackStatus status) {
        MarketplacePack p = MarketplacePack.submit("iso", "1.0", "Pub", "Title", "d",
                "healthcare", "iso-9001", 0, "EUR", "https://x.com/y.zip", MANIFEST, SIG, PARTNER, NOW);
        p.assignId(ID);
        switch (status) {
            case IN_REVIEW -> p.takeForReview(EDITOR, NOW);
            case PUBLISHED -> { p.takeForReview(EDITOR, NOW); p.publish(EDITOR, NOW); }
            case REJECTED -> p.reject(EDITOR, "no", NOW);
            case DEPRECATED -> { p.takeForReview(EDITOR, NOW); p.publish(EDITOR, NOW); p.deprecate(EDITOR, NOW); }
            default -> { }
        }
        return p;
    }

    // ---------- catalogue public ----------

    @Test
    void listPublished_returnsPublicView() {
        when(packRepo.findPublished(null)).thenReturn(List.of(sample(MarketplacePackStatus.PUBLISHED)));
        List<MarketplacePackDto.View> views = service.listPublished(null);
        assertThat(views).hasSize(1);
        // Vue publique : pas de secrets de modération.
        assertThat(views.get(0).signatureHash()).isNull();
        assertThat(views.get(0).submittedBy()).isNull();
    }

    @Test
    void listPublished_filtersBlankSector() {
        when(packRepo.findPublished(null)).thenReturn(List.of());
        assertThat(service.listPublished("   ")).isEmpty();
    }

    @Test
    void getPublished_unpublished_throws404() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.SUBMITTED)));
        assertThatThrownBy(() -> service.getPublished(ID))
            .isInstanceOf(MarketplacePackNotFoundException.class);
    }

    // ---------- submission ----------

    @Test
    void submit_succeeds() {
        var view = service.submit(new MarketplacePackDto.SubmitCommand(
                "iso", "1.0", "Pub", "Title", "d", "healthcare", List.of("iso-9001"),
                0, "EUR", "https://x.com/y.zip", MANIFEST, SIG));
        assertThat(view.id()).isEqualTo(ID);
        assertThat(view.status()).isEqualTo(MarketplacePackStatus.SUBMITTED);
        assertThat(view.norms()).containsExactly("iso-9001");
    }

    @Test
    void submit_duplicate_throws() {
        when(packRepo.existsByPackIdAndVersion("iso", "1.0")).thenReturn(true);
        assertThatThrownBy(() -> service.submit(new MarketplacePackDto.SubmitCommand(
                "iso", "1.0", "Pub", "T", null, "s", null, 0, "EUR",
                "https://x.com/y.zip", MANIFEST, SIG)))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void submit_badManifest_throws() {
        assertThatThrownBy(() -> service.submit(new MarketplacePackDto.SubmitCommand(
                "iso", "1.0", "Pub", "T", null, "s", null, 0, "EUR",
                "https://x.com/y.zip", "{\"documents\":[{\"path\":\"../x\"}]}", SIG)))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    // ---------- moderation ----------

    @Test
    void takeForReview_transitions() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.SUBMITTED)));
        assertThat(service.takeForReview(ID).status()).isEqualTo(MarketplacePackStatus.IN_REVIEW);
    }

    @Test
    void publish_transitions() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.IN_REVIEW)));
        assertThat(service.publish(ID).status()).isEqualTo(MarketplacePackStatus.PUBLISHED);
    }

    @Test
    void reject_transitions() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.SUBMITTED)));
        var v = service.reject(ID, new MarketplacePackDto.RejectCommand("incomplet"));
        assertThat(v.status()).isEqualTo(MarketplacePackStatus.REJECTED);
        assertThat(v.reviewNotes()).isEqualTo("incomplet");
    }

    @Test
    void deprecate_transitions() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.PUBLISHED)));
        assertThat(service.deprecate(ID).status()).isEqualTo(MarketplacePackStatus.DEPRECATED);
    }

    @Test
    void moderationQueue_editorView() {
        when(packRepo.findModerationQueue()).thenReturn(List.of(sample(MarketplacePackStatus.SUBMITTED)));
        var views = service.moderationQueue();
        assertThat(views).hasSize(1);
        assertThat(views.get(0).submittedBy()).isEqualTo(PARTNER);
    }

    @Test
    void getForEditor_returnsEditorView() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.IN_REVIEW)));
        assertThat(service.getForEditor(ID).signatureHash()).isEqualTo(SIG);
    }

    @Test
    void publish_unknown_throws() {
        when(packRepo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.publish(ID))
            .isInstanceOf(MarketplacePackNotFoundException.class);
    }

    // ---------- installation ----------

    @Test
    void install_published_persists() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.PUBLISHED)));
        when(installRepo.findActive(TENANT, ID)).thenReturn(Optional.empty());
        var v = service.install(ID);
        assertThat(v.status()).isEqualTo(InstallationStatus.INSTALLED);
        assertThat(v.tenantId()).isEqualTo(TENANT);
        assertThat(v.installedBy()).isEqualTo(PARTNER);
    }

    @Test
    void install_idempotent_whenAlreadyInstalled() {
        MarketplacePack pack = sample(MarketplacePackStatus.PUBLISHED);
        MarketplaceInstallation existing = MarketplaceInstallation.install(TENANT, pack, PARTNER, NOW);
        existing.assignId(UUID.randomUUID());
        when(packRepo.findById(ID)).thenReturn(Optional.of(pack));
        when(installRepo.findActive(TENANT, ID)).thenReturn(Optional.of(existing));
        var v = service.install(ID);
        assertThat(v.id()).isEqualTo(existing.getId());
    }

    @Test
    void install_unpublished_throws404() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.SUBMITTED)));
        assertThatThrownBy(() -> service.install(ID))
            .isInstanceOf(MarketplacePackNotFoundException.class);
    }

    @Test
    void uninstall_flips() {
        MarketplacePack pack = sample(MarketplacePackStatus.PUBLISHED);
        MarketplaceInstallation inst = MarketplaceInstallation.install(TENANT, pack, PARTNER, NOW);
        UUID instId = UUID.randomUUID();
        inst.assignId(instId);
        when(installRepo.findByIdForTenant(TENANT, instId)).thenReturn(Optional.of(inst));
        var v = service.uninstall(instId);
        assertThat(v.status()).isEqualTo(InstallationStatus.UNINSTALLED);
    }

    @Test
    void uninstall_unknown_throws() {
        UUID instId = UUID.randomUUID();
        when(installRepo.findByIdForTenant(TENANT, instId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.uninstall(instId))
            .isInstanceOf(MarketplaceInstallationNotFoundException.class);
    }

    @Test
    void myInstallations_scopedToTenant() {
        MarketplacePack pack = sample(MarketplacePackStatus.PUBLISHED);
        MarketplaceInstallation inst = MarketplaceInstallation.install(TENANT, pack, PARTNER, NOW);
        when(installRepo.findActiveByTenant(TENANT)).thenReturn(List.of(inst));
        assertThat(service.myInstallations()).hasSize(1);
    }

    @Test
    void myInstallationHistory_scopedToTenant() {
        when(installRepo.findAllByTenant(TENANT)).thenReturn(List.of());
        assertThat(service.myInstallationHistory()).isEmpty();
    }

    // ---------- rating ----------

    @Test
    void rate_requiresActiveInstallation() {
        when(packRepo.findById(ID)).thenReturn(Optional.of(sample(MarketplacePackStatus.PUBLISHED)));
        when(installRepo.findActive(TENANT, ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rate(ID, new MarketplacePackDto.RateCommand(5)))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void rate_updatesAverage() {
        MarketplacePack pack = sample(MarketplacePackStatus.PUBLISHED);
        MarketplaceInstallation inst = MarketplaceInstallation.install(TENANT, pack, PARTNER, NOW);
        when(packRepo.findById(ID)).thenReturn(Optional.of(pack));
        when(installRepo.findActive(TENANT, ID)).thenReturn(Optional.of(inst));
        var v = service.rate(ID, new MarketplacePackDto.RateCommand(4));
        assertThat(v.ratingCount()).isEqualTo(1);
        assertThat(v.ratingAvg()).isEqualTo(4.0);
    }
}

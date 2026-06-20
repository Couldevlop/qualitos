package com.openlab.qualitos.quality.dashboards.annotations.application;

import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotation;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationForbiddenException;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationNotFoundException;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationRepository;
import com.openlab.qualitos.quality.dashboards.application.TenantProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — collaborative dashboard annotations (CLAUDE.md §7.3).
 *
 * <p>Security model:</p>
 * <ul>
 *   <li>tenantId + authorId always from JWT (never body) — §18.2 #2.</li>
 *   <li>Reads scoped to the caller's tenant — cross-tenant returns 404 (A01).</li>
 *   <li>Deletion reserved to the author OR a tenant admin (else 403).</li>
 * </ul>
 */
public class DashboardAnnotationService {

    private final DashboardAnnotationRepository repo;
    private final TenantProvider tenantProvider;
    private final ActorRoles actorRoles;
    private final Clock clock;

    public DashboardAnnotationService(DashboardAnnotationRepository repo,
                                      TenantProvider tenantProvider,
                                      ActorRoles actorRoles,
                                      Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.actorRoles = actorRoles;
        this.clock = clock;
    }

    public DashboardAnnotationDto.View create(DashboardAnnotationDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID authorId = tenantProvider.requireUserId();
        Instant now = Instant.now(clock);
        DashboardAnnotation annotation = DashboardAnnotation.create(
                tenantId, authorId, req.chartKey(), req.anchorLabel(), req.body(), now);
        DashboardAnnotation saved = repo.save(annotation);
        // Author can always delete what they just created.
        return DashboardAnnotationDto.View.of(saved, true);
    }

    public List<DashboardAnnotationDto.View> listByChart(String chartKey) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID userId = tenantProvider.requireUserId();
        boolean admin = actorRoles.isTenantAdmin();
        return repo.findByTenantAndChartKey(tenantId, requireChartKey(chartKey)).stream()
                .map(a -> DashboardAnnotationDto.View.of(a, canDelete(a, userId, admin)))
                .toList();
    }

    public void delete(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID userId = tenantProvider.requireUserId();
        DashboardAnnotation annotation = repo.findByIdAndTenant(id, tenantId)
                .orElseThrow(() -> new DashboardAnnotationNotFoundException(id));
        if (!canDelete(annotation, userId, actorRoles.isTenantAdmin())) {
            throw new DashboardAnnotationForbiddenException(id);
        }
        repo.delete(annotation.getId());
    }

    private static boolean canDelete(DashboardAnnotation a, UUID userId, boolean admin) {
        return admin || a.isAuthoredBy(userId);
    }

    private static String requireChartKey(String chartKey) {
        if (chartKey == null || chartKey.isBlank()) {
            throw new IllegalArgumentException("chartKey required");
        }
        return chartKey;
    }
}

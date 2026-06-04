package com.openlab.qualitos.quality.activityfeed;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Lecture pure du read-model "flux d'activité" (table de projection).
 * Aucune dépendance à Kafka : fonctionne que la projection soit alimentée ou non.
 */
@RestController
@RequestMapping("/api/v1/activity-feed")
public class ActivityFeedController {

    private final AuditActivityRepository repository;

    public ActivityFeedController(AuditActivityRepository repository) {
        this.repository = repository;
    }

    /** Flux d'activité récent du tenant courant, du plus récent au plus ancien. */
    @GetMapping
    public Page<ActivityFeedDto.View> recent(@PageableDefault(size = 50) Pageable pageable) {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        return repository.findByTenantIdOrderBySequenceNoDesc(tenantId, pageable)
                .map(ActivityFeedDto.View::from);
    }
}

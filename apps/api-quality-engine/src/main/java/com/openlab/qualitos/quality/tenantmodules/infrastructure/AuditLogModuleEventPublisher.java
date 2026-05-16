package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationEventPublisher;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Route les événements d'activation modules vers le journal d'audit (§11.5).
 * Convention wire : {@code tenant.module.<action>} (cf. EHS pattern).
 */
@Component
public class AuditLogModuleEventPublisher implements ModuleActivationEventPublisher {

    static final String RESOURCE_TYPE = "tenant-module-activation";

    private final AuditEventService auditEvents;

    public AuditLogModuleEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(ModuleActivation a, Action action) {
        String wire = "tenant.module." + action.name().toLowerCase(Locale.ROOT);
        auditEvents.recordForTenant(a.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, a.getLastChangedBy(),
                        wire, RESOURCE_TYPE, a.getId(),
                        a.getModuleCode() + " → " + a.getStatus() + " (" + a.getBillingTier() + ")",
                        a.getConfigurationJson(),
                        null, null));
    }
}

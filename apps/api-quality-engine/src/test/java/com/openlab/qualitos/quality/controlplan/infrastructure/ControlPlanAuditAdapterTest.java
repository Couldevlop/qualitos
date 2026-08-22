package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * La traduction vers le journal chaîné.
 *
 * <p>Le point qui compte : le tenant est celui de l'agrégat, passé explicitement,
 * et l'acteur celui que le service a lu du jeton. Une ligne de journal dont
 * l'auteur serait falsifiable ne vaudrait rien devant un auditeur.
 */
class ControlPlanAuditAdapterTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID PLAN = UUID.randomUUID();

    @Test
    void theEventCarriesTheTenantTheActorAndTheResource() {
        AuditEventService events = mock(AuditEventService.class);
        ControlPlanAuditAdapter adapter = new ControlPlanAuditAdapter(events);

        adapter.record(TENANT, ACTOR, "controlplan.plan.approved", PLAN,
                "Control plan approuvé", "{\"status\":\"ACTIVE\"}");

        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events).recordForTenant(eq(TENANT), captor.capture());
        AuditEventDto.RecordEventRequest request = captor.getValue();
        assertThat(request.actorType()).isEqualTo(ActorType.USER);
        assertThat(request.actorUserId()).isEqualTo(ACTOR);
        assertThat(request.action()).isEqualTo("controlplan.plan.approved");
        assertThat(request.resourceType()).isEqualTo("control_plan");
        assertThat(request.resourceId()).isEqualTo(PLAN);
        assertThat(request.summary()).isEqualTo("Control plan approuvé");
        assertThat(request.payloadJson()).contains("ACTIVE");
    }

    @Test
    void anActionWithoutAnActorIsAttributedToTheSystemRatherThanToPersonne() {
        // Une écriture déclenchée par un ordonnanceur n'a pas d'humain derrière :
        // la marquer USER avec un identifiant nul serait un mensonge d'audit.
        AuditEventService events = mock(AuditEventService.class);
        ControlPlanAuditAdapter adapter = new ControlPlanAuditAdapter(events);

        adapter.record(TENANT, null, "controlplan.plan.approved", PLAN, "résumé", "{}");

        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events).recordForTenant(any(), captor.capture());
        assertThat(captor.getValue().actorType()).isEqualTo(ActorType.SYSTEM);
        assertThat(captor.getValue().actorUserId()).isNull();
    }
}

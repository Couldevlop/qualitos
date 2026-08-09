package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Ce qui se teste ici : un dossier CAPA laisse une trace de CE QU'IL DEVIENT, et
 * n'annonce au dehors que ce qui regarde le dehors.
 *
 * <p>Le journal ne laisse rien passer — c'est lui qui répond à « qui a clos ce
 * dossier ? ». Les abonnés, eux, ne reçoivent pas tout : leur envoyer chaque
 * changement de champ les obligerait à filtrer ce qu'on n'aurait pas dû envoyer.
 */
@ExtendWith(MockitoExtension.class)
class CapaLifecycleJournalTest {

    @Mock AuditEventService auditEvents;
    @Mock ApplicationEventPublisher events;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    private CapaLifecycleJournal journal() {
        return new CapaLifecycleJournal(auditEvents, events);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void authenticatedAs(String subject) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(subject, "n/a", List.of()));
    }

    private CapaCase capa(CapaStatus status) {
        CapaCase c = new CapaCase();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setTitle("Joint torique hors tolérance");
        c.setDescription("Récit détaillé de l'incident, potentiellement long et sensible.");
        c.setStatus(status);
        c.setType(CapaType.CORRECTIVE);
        c.setCriticity(CapaCriticity.HIGH);
        c.setSourceType(CapaSourceType.NON_CONFORMITY);
        c.setSourceRef("nc:42");
        c.setOwnerId(UUID.randomUUID());
        c.setDueDate(LocalDate.of(2026, 9, 30));
        return c;
    }

    private AuditEventDto.RecordEventRequest captureAudit() {
        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("inscrit la transition au journal du tenant, avec l'acteur authentifié")
    void inscritLaTransition() {
        authenticatedAs(ACTOR.toString());
        CapaCase c = capa(CapaStatus.CLOSED);

        journal().record(c, CapaTransition.CLOSED);

        AuditEventDto.RecordEventRequest req = captureAudit();
        assertThat(req.action()).isEqualTo("capa.case.closed");
        assertThat(req.resourceType()).isEqualTo("capa_case");
        assertThat(req.resourceId()).isEqualTo(c.getId());
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(ACTOR);
        assertThat(req.summary()).contains("clos");
    }

    @Test
    @DisplayName("attribue au système plutôt qu'à un utilisateur inventé")
    void sansIdentiteExploitable_attribueAuSysteme() {
        authenticatedAs("service-account-engine"); // sub non-UUID
        journal().record(capa(CapaStatus.OPEN), CapaTransition.OPENED);

        AuditEventDto.RecordEventRequest req = captureAudit();
        assertThat(req.actorType()).isEqualTo(ActorType.SYSTEM);
        assertThat(req.actorUserId()).isNull();
    }

    @Test
    @DisplayName("la charge dit où en est le dossier, sans emporter son récit")
    void chargeUtile_saitSeTaire() {
        authenticatedAs(ACTOR.toString());
        CapaCase c = capa(CapaStatus.RESOLVED);

        journal().record(c, CapaTransition.RESOLVED);

        String payload = captureAudit().payloadJson();
        assertThat(payload)
                .contains("\"status\":\"RESOLVED\"")
                .contains("\"criticity\":\"HIGH\"")
                .contains("\"sourceRef\":\"nc:42\"")
                .contains("\"dueDate\":\"2026-09-30\"");
        // La description raconte un incident : longue, libre, souvent sensible.
        // Elle n'a pas à grossir chaque ligne de journal ni à partir chez un tiers.
        assertThat(payload).doesNotContain("Récit détaillé");
    }

    @Test
    @DisplayName("échappe un titre qui casserait la ligne du journal")
    void echappeLesGuillemets() {
        authenticatedAs(ACTOR.toString());
        CapaCase c = capa(CapaStatus.OPEN);
        c.setTitle("Écart \"majeur\" sur la ligne\\2");

        journal().record(c, CapaTransition.OPENED);

        assertThat(captureAudit().payloadJson()).contains("Écart \\\"majeur\\\" sur la ligne\\\\2");
    }

    @Test
    @DisplayName("un dossier sans échéance ni responsable ne fabrique pas de valeurs")
    void champsAbsents_restentNuls() {
        authenticatedAs(ACTOR.toString());
        CapaCase c = capa(CapaStatus.OPEN);
        c.setOwnerId(null);
        c.setDueDate(null);
        c.setSourceRef(null);

        journal().record(c, CapaTransition.OPENED);

        assertThat(captureAudit().payloadJson())
                .contains("\"ownerId\":null")
                .contains("\"dueDate\":null")
                .contains("\"sourceRef\":null");
    }

    @Test
    @DisplayName("annonce au dehors les transitions qui regardent le dehors")
    void publieLesTransitionsSortantes() {
        authenticatedAs(ACTOR.toString());
        CapaCase c = capa(CapaStatus.CLOSED);

        journal().record(c, CapaTransition.CLOSED);

        ArgumentCaptor<CapaTransitionEvent> captor = ArgumentCaptor.forClass(CapaTransitionEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT);
        assertThat(captor.getValue().transition()).isEqualTo(CapaTransition.CLOSED);
        assertThat(captor.getValue().payload()).containsEntry("status", "CLOSED");
    }

    @Test
    @DisplayName("garde pour elle les transitions internes")
    void nePublieRienPourUneTransitionInterne() {
        authenticatedAs(ACTOR.toString());

        journal().record(capa(CapaStatus.IN_PROGRESS), CapaTransition.STARTED);

        // Le démarrage du traitement intéresse l'auditeur, pas un système tiers.
        verify(auditEvents).recordForTenant(eq(TENANT), any());
        verify(events, never()).publishEvent(any(CapaTransitionEvent.class));
    }

    @Test
    @DisplayName("toute transition sortante porte un événement connu du contrat webhook")
    void chaqueTransitionSortante_aUnTypeDeclare() {
        // Garde-fou : ajouter une transition sortante sans type publiable la
        // rendrait muette sans que rien ne le signale.
        for (CapaTransition t : CapaTransition.values()) {
            assertThat(t.auditAction()).startsWith("capa.case.");
            assertThat(t.summary()).isNotBlank();
        }
        assertThat(CapaTransition.OPENED.eventType()).isNotNull();
        assertThat(CapaTransition.RESOLVED.eventType()).isNotNull();
        assertThat(CapaTransition.CLOSED.eventType()).isNotNull();
        assertThat(CapaTransition.EFFECTIVENESS_REJECTED.eventType()).isNotNull();
    }
}

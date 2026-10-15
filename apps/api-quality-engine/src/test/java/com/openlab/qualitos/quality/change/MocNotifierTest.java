package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.audit.AuditReminderMailer;
import com.openlab.qualitos.quality.change.domain.ValidationRole;
import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationRepository;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Les destinataires et le contenu des notifications MOC, sur les canaux existants. */
class MocNotifierTest {

    static final Instant NOW = Instant.parse("2026-09-28T07:12:00Z");
    static final UUID TENANT = UUID.randomUUID();
    static final UUID DEMANDEUR = UUID.randomUUID();
    static final UUID JEAN = UUID.randomUUID();
    static final UUID MARIE = UUID.randomUUID();

    NotificationRepository notifications;
    AuditReminderMailer mailer;
    ObjectProvider<AuditReminderMailer> provider;
    MocNotifier notifier;
    ChangeRequest c;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        notifications = mock(NotificationRepository.class);
        mailer = mock(AuditReminderMailer.class);
        provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailer);
        notifier = new MocNotifier(notifications, provider, Clock.fixed(NOW, ZoneOffset.UTC));
        c = new ChangeRequest();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setCode("MOC-2026-014");
        c.setTitle("Remplacement du four");
        c.setRequesterUserId(DEMANDEUR);
    }

    static ValidationRole.RoleHolder h(UUID id, String label) {
        return new ValidationRole.RoleHolder(id, label);
    }

    @Test
    void unTitulaireDePlusieursRolesNeRecoitQuUneNotificationEtLeDemandeurAucune() {
        Map<String, List<ValidationRole.RoleHolder>> roles = new LinkedHashMap<>();
        roles.put("HSE", List.of(h(JEAN, "jean@acme.test"), h(DEMANDEUR, "dem@acme.test")));
        roles.put("Directeur de site", List.of(h(JEAN, "jean@acme.test"), h(MARIE, "marie@acme.test")));

        List<MocNotifier.Recipient> dest = MocNotifier.recipients(roles, DEMANDEUR);
        assertThat(dest).extracting(MocNotifier.Recipient::userId).containsExactly(JEAN, MARIE);
        assertThat(dest.get(0).roles()).containsExactly("HSE", "Directeur de site");
        assertThat(dest.get(1).roles()).containsExactly("Directeur de site");

        notifier.awaitingValidation(c, dest);
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(n.capture());
        Notification jean = n.getAllValues().get(0);
        assertThat(jean.getRecipientUserId()).isEqualTo(JEAN.toString());
        assertThat(jean.getTenantId()).isEqualTo(TENANT);
        assertThat(jean.getTitle()).isEqualTo("MOC-2026-014 attend votre validation (rôles HSE, Directeur de site)");
        assertThat(jean.getLink()).isEqualTo("/changes/" + c.getId());
        assertThat(jean.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(n.getAllValues().get(1).getTitle()).endsWith("(rôle Directeur de site)");
        verify(mailer).send(eq("jean@acme.test"), anyString(), anyString());
        verify(mailer).send(eq("marie@acme.test"), anyString(), anyString());
    }

    @Test
    void leRappelDitDepuisCombienDeJours() {
        notifier.reminder(c, List.of(new MocNotifier.Recipient(JEAN, "jean@acme.test", List.of("HSE"))), 3);
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(n.capture());
        assertThat(n.getValue().getTitle())
                .isEqualTo("Rappel : MOC-2026-014 attend votre validation depuis 3 jours (rôle HSE)");
        notifier.reminder(c, List.of(new MocNotifier.Recipient(JEAN, "jean@acme.test", List.of("HSE"))), 1);
        verify(notifications, times(2)).save(n.capture());
        assertThat(n.getValue().getTitle()).contains("depuis 1 jour (");
    }

    @Test
    void laDecisionFinaleVaAuDemandeurAvecLeMotifDuRefus() {
        notifier.decided(c, false, "Coûts non chiffrés");
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(n.capture());
        assertThat(n.getValue().getRecipientUserId()).isEqualTo(DEMANDEUR.toString());
        assertThat(n.getValue().getType()).isEqualTo(NotificationType.ALERT);
        assertThat(n.getValue().getTitle()).isEqualTo("MOC-2026-014 est rejetée");
        assertThat(n.getValue().getBody()).contains("Coûts non chiffrés");

        notifier.decided(c, true, null);
        verify(notifications, times(2)).save(n.capture());
        assertThat(n.getValue().getType()).isEqualTo(NotificationType.SUCCESS);
        assertThat(n.getValue().getTitle()).isEqualTo("MOC-2026-014 est approuvée");
        // Le demandeur n'est prévenu qu'en interne : son adresse n'est pas conservée.
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void unRefusRetireLesTachesDesAutresValidateursEtLesPrevient() {
        notifier.validationClosed(c, List.of(new MocNotifier.Recipient(MARIE, "marie@acme.test", List.of("HSE"))));
        verify(notifications).markReadForRecipientAndLink(TENANT, MARIE.toString(), "/changes/" + c.getId(), NOW);
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(n.capture());
        assertThat(n.getValue().getType()).isEqualTo(NotificationType.INFO);
        assertThat(n.getValue().getTitle()).contains("n'est plus attendue");
    }

    @Test
    void quandUnTitulaireStatueSesCoTitulairesNOntPlusDeTache() {
        notifier.roleDecided(c, "HSE", List.of(JEAN, MARIE));
        verify(notifications).markReadForRecipientAndLink(TENANT, JEAN.toString(), "/changes/" + c.getId(), NOW);
        verify(notifications).markReadForRecipientAndLink(TENANT, MARIE.toString(), "/changes/" + c.getId(), NOW);
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(n.capture());
        assertThat(n.getAllValues()).allSatisfy(x -> {
            assertThat(x.getType()).isEqualTo(NotificationType.INFO);
            assertThat(x.getTitle()).isEqualTo("MOC-2026-014 : le rôle HSE a statué, votre validation n'est plus attendue");
            assertThat(x.getLink()).isEqualTo("/changes/" + c.getId());
        });
        assertThat(n.getAllValues()).extracting(Notification::getRecipientUserId)
                .containsExactly(JEAN.toString(), MARIE.toString());
        // Une information, pas une tâche : aucun courriel.
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void sansSmtpOuSansAdresseAucunCourrielEtUnEchecNEmpecheRien() {
        when(provider.getIfAvailable()).thenReturn(null);
        notifier.awaitingValidation(c, List.of(new MocNotifier.Recipient(JEAN, "jean@acme.test", List.of("HSE"))));
        verify(mailer, never()).send(anyString(), anyString(), anyString());

        when(provider.getIfAvailable()).thenReturn(mailer);
        notifier.awaitingValidation(c, List.of(new MocNotifier.Recipient(JEAN, "Jean Dupont", List.of("HSE"))));
        verify(mailer, never()).send(anyString(), anyString(), anyString());

        doThrow(new IllegalStateException("smtp")).when(mailer).send(anyString(), anyString(), anyString());
        notifier.awaitingValidation(c, List.of(new MocNotifier.Recipient(JEAN, "jean@acme.test", List.of("HSE"))));
        verify(notifications, times(3)).save(any());
    }
    @Test
    void leCourrielPartAAdresseDuTitulaireEtNonASonNom() {
        Map<String, List<ValidationRole.RoleHolder>> roles = new LinkedHashMap<>();
        roles.put("HSE", List.of(
                new ValidationRole.RoleHolder(JEAN, "Jean Dupont", "jean@acme.test"),
                // Nommé avant que l'adresse ait sa colonne : son libellé EST l'adresse.
                new ValidationRole.RoleHolder(MARIE, "marie@acme.test", null),
                // Un nom sans adresse : la cloche seule le prévient.
                new ValidationRole.RoleHolder(UUID.randomUUID(), "Paul Sans Adresse", "  ")));

        List<MocNotifier.Recipient> dest = MocNotifier.recipients(roles, DEMANDEUR);
        assertThat(dest).extracting(MocNotifier.Recipient::address)
                .containsExactly("jean@acme.test", "marie@acme.test", null);

        notifier.awaitingValidation(c, dest);
        verify(notifications, times(3)).save(any());
        verify(mailer).send(eq("jean@acme.test"), anyString(), anyString());
        verify(mailer).send(eq("marie@acme.test"), anyString(), anyString());
        verify(mailer, times(2)).send(anyString(), anyString(), anyString());
    }
}

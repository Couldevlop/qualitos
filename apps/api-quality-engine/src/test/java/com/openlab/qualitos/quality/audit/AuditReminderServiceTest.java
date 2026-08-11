package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationRepository;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Le rappel d'échéance (§4.4) sous tous ses angles, l'idempotence en premier :
 * c'est la propriété dont dépend la crédibilité du dispositif entier.
 */
@ExtendWith(MockitoExtension.class)
class AuditReminderServiceTest {

    @Mock AuditPlanRepository plans;
    @Mock NotificationRepository notifications;
    @Mock ObjectProvider<AuditReminderMailer> mailerProvider;
    @Mock AuditReminderMailer mailer;

    AuditReminderProperties props;
    AuditReminderService service;

    /** Horloge figée : un rappel « à 30 jours » ne doit pas dépendre de l'heure du test. */
    static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    static final Instant NOW = TODAY.atStartOfDay(ZoneOffset.UTC).toInstant();
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    static final UUID TENANT_A = UUID.randomUUID();
    static final UUID TENANT_B = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID AUDITEE = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        props = new AuditReminderProperties();
        service = new AuditReminderService(plans, notifications, mailerProvider, props, CLOCK);
    }

    // --- fenêtre de rappel ---

    @Test
    void queriesExactlyTheThirtyDayWindowFromToday() {
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of());

        service.sendDueReminders();

        verify(plans).findDueForReminder(
                eq(AuditStatus.PLANNED), eq(TODAY), eq(TODAY.plusDays(30)), any(Pageable.class));
    }

    @Test
    void honoursAConfiguredWindowInsteadOfTheDefault() {
        props.setDaysBefore(7);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of());

        service.sendDueReminders();

        verify(plans).findDueForReminder(any(), eq(TODAY), eq(TODAY.plusDays(7)), any(Pageable.class));
    }

    @Test
    void capsTheBatchToTheConfiguredSize() {
        props.setBatchSize(25);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of());

        service.sendDueReminders();

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(plans).findDueForReminder(any(), any(), any(), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(25);
    }

    // --- idempotence : la propriété centrale ---

    @Test
    void claimsBeforeSending_soACrashCanLoseAReminderButNeverDuplicateIt() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), null);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(p.getId(), NOW)).thenReturn(1);

        service.sendDueReminders();

        // L'ordre compte : réserver PUIS notifier. L'inverse garantirait le doublon
        // à chaque incident survenu entre les deux.
        InOrder order = inOrder(plans, notifications);
        order.verify(plans).claimReminder(p.getId(), NOW);
        order.verify(notifications).save(any(Notification.class));
    }

    @Test
    void aPlanAlreadyClaimedByAnotherReplicaIsSilentlySkipped() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(10), "qualite@exemple.test");
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        // 0 ligne affectée = une autre réplique a gagné la course.
        when(plans.claimReminder(p.getId(), NOW)).thenReturn(0);

        AuditReminderService.Report report = service.sendDueReminders();

        assertThat(report.reminded()).isZero();
        assertThat(report.alreadyClaimed()).isEqualTo(1);
        verifyNoInteractions(notifications);
        verifyNoInteractions(mailerProvider);
    }

    @Test
    void aSecondPassOverTheSamePlanSendsNothingMore() {
        // Rejoue le scénario réel : la base ne rend la ligne qu'une fois (la
        // requête filtre sur reminder_sent_at IS NULL), et même si elle la rendait
        // deux fois, la réservation échouerait au second tour.
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), "qualite@exemple.test");
        when(mailerProvider.getIfAvailable()).thenReturn(mailer);
        when(plans.findDueForReminder(any(), any(), any(), any()))
                .thenReturn(List.of(p))
                .thenReturn(List.of(p))
                .thenReturn(List.of());
        when(plans.claimReminder(p.getId(), NOW)).thenReturn(1).thenReturn(0);

        service.sendDueReminders();
        service.sendDueReminders();
        service.sendDueReminders();

        verify(notifications, times(1)).save(any(Notification.class));
        verify(mailer, times(1)).send(any(), any(), any());
    }

    @Test
    void aFailingClaimDoesNotStopTheFollowingPlans() {
        AuditPlan broken = plan(TENANT_A, TODAY.plusDays(3), null);
        AuditPlan sound = plan(TENANT_B, TODAY.plusDays(4), null);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(broken, sound));
        when(plans.claimReminder(broken.getId(), NOW)).thenThrow(new IllegalStateException("base indisponible"));
        when(plans.claimReminder(sound.getId(), NOW)).thenReturn(1);

        AuditReminderService.Report report = service.sendDueReminders();

        assertThat(report.reminded()).isEqualTo(1);
        verify(notifications, times(1)).save(any(Notification.class));
    }

    // --- multi-tenant hors contexte de requête ---

    @Test
    void addressesEachTenantExplicitly_withoutRelyingOnAnAmbientContext() {
        // Aucun TenantContext n'est posé : l'ordonnanceur tourne hors requête.
        assertThat(TenantContext.hasTenant()).isFalse();
        AuditPlan a = plan(TENANT_A, TODAY.plusDays(2), null);
        AuditPlan b = plan(TENANT_B, TODAY.plusDays(9), null);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(a, b));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        service.sendDueReminders();

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Notification::getTenantId)
                .containsExactly(TENANT_A, TENANT_B);
    }

    @Test
    void aTenantLeakingIntoTheThreadDoesNotContaminateTheOtherTenantsNotification() {
        // Un thread recyclé peut porter le tenant d'une requête précédente. Le
        // service doit lire le tenant de la LIGNE, pas celui du thread.
        TenantContext.setTenantId(TENANT_B.toString());
        try {
            AuditPlan a = plan(TENANT_A, TODAY.plusDays(2), null);
            when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(a));
            when(plans.claimReminder(any(), any())).thenReturn(1);

            service.sendDueReminders();

            ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
            verify(notifications).save(saved.capture());
            assertThat(saved.getValue().getTenantId()).isEqualTo(TENANT_A);
        } finally {
            TenantContext.clear();
        }
    }

    // --- notification interne ---

    @Test
    void notifiesTheLeadAuditorAndTheAuditee_eachByName() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), null);
        p.setAuditeeId(AUDITEE);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        service.sendDueReminders();

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Notification::getRecipientUserId)
                .containsExactly(LEAD.toString(), AUDITEE.toString());
        // Jamais de diffusion à tout le tenant : un rappel qu'on apprend à ignorer
        // ne rappelle plus rien.
        assertThat(saved.getAllValues()).extracting(Notification::getRecipientUserId).doesNotContainNull();
    }

    @Test
    void doesNotNotifyTheSamePersonTwiceWhenSheIsBothLeadAndAuditee() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), null);
        p.setAuditeeId(LEAD);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        service.sendDueReminders();

        verify(notifications, times(1)).save(any(Notification.class));
    }

    @Test
    void theNotificationCarriesTheDeadlineAndALinkToTheAudit() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), null);
        p.setStandard("ISO_9001");
        p.setScope("Atelier mécanique");
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        service.sendDueReminders();

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        Notification n = saved.getValue();
        assertThat(n.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(n.getTitle()).contains("Audit interne ISO");
        assertThat(n.getBody()).contains("2026-07-15").contains("dans 30 jours")
                .contains("ISO_9001").contains("Atelier mécanique");
        assertThat(n.getLink()).isEqualTo("/audits/" + p.getId());
        assertThat(n.getCreatedAt()).isEqualTo(NOW);
        assertThat(n.isRead()).isFalse();
    }

    @Test
    void aFailingNotificationDoesNotAbortTheRun() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), "qualite@exemple.test");
        when(mailerProvider.getIfAvailable()).thenReturn(mailer);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);
        when(notifications.save(any())).thenThrow(new IllegalStateException("base indisponible"));

        AuditReminderService.Report report = service.sendDueReminders();

        assertThat(report.reminded()).isZero();
        // Le courriel part quand même : les deux canaux sont indépendants.
        assertThat(report.mailed()).isEqualTo(1);
    }

    // --- courriel ---

    @Test
    void withoutTheSmtpBrick_theInternalReminderStillGoesOut() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), "qualite@exemple.test");
        when(mailerProvider.getIfAvailable()).thenReturn(null); // brique désactivée
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        AuditReminderService.Report report = service.sendDueReminders();

        assertThat(report.reminded()).isEqualTo(1);
        assertThat(report.mailed()).isZero();
        assertThat(report.mailFailures()).isZero();
        verify(notifications).save(any(Notification.class));
    }

    @Test
    void withoutARecipientAddress_noMailerIsEvenLookedUp() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), null);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        service.sendDueReminders();

        verifyNoInteractions(mailerProvider);
    }

    @Test
    void aBlankRecipientAddressIsTreatedAsNoAddress() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), "   ");
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        assertThat(service.sendDueReminders().mailed()).isZero();
        verifyNoInteractions(mailerProvider);
    }

    @Test
    void sendsTheSameSubjectAndBodyAsTheInAppNotification() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(1), "qualite@exemple.test");
        when(mailerProvider.getIfAvailable()).thenReturn(mailer);
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        service.sendDueReminders();

        verify(mailer).send(eq("qualite@exemple.test"),
                eq(AuditReminderService.subjectOf(p)),
                eq(AuditReminderService.bodyOf(p, 1)));
    }

    @Test
    void anSmtpFailureIsCountedAndSwallowed_theInAppReminderRemainsValid() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(30), "qualite@exemple.test");
        when(mailerProvider.getIfAvailable()).thenReturn(mailer);
        doThrow(new IllegalStateException("relais injoignable")).when(mailer).send(any(), any(), any());
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of(p));
        when(plans.claimReminder(any(), any())).thenReturn(1);

        AuditReminderService.Report report = service.sendDueReminders();

        assertThat(report.reminded()).isEqualTo(1);
        assertThat(report.mailFailures()).isEqualTo(1);
        assertThat(report.mailed()).isZero();
        // Aucune relance : la réservation est posée, réessayer produirait le doublon.
        verify(mailer, times(1)).send(any(), any(), any());
    }

    // --- formulation du message ---

    @Test
    void wordsTheDeadlineInPlainFrench_singularAndSameDayIncluded() {
        AuditPlan p = plan(TENANT_A, TODAY, null);
        assertThat(AuditReminderService.bodyOf(p, 0)).contains("soit aujourd'hui.");
        assertThat(AuditReminderService.bodyOf(p, 1)).contains("soit dans 1 jour.");
        assertThat(AuditReminderService.bodyOf(p, 30)).contains("soit dans 30 jours.");
    }

    @Test
    void omitsTheStandardAndScopeLinesWhenBlank() {
        AuditPlan p = plan(TENANT_A, TODAY.plusDays(5), null);
        p.setStandard("  ");
        p.setScope("");
        String body = AuditReminderService.bodyOf(p, 5);
        assertThat(body).doesNotContain("Référentiel visé").doesNotContain("Périmètre");
    }

    @Test
    void recipientsOf_ignoresAMissingAuditee() {
        AuditPlan p = plan(TENANT_A, TODAY, null);
        assertThat(AuditReminderService.recipientsOf(p)).containsExactly(LEAD.toString());
    }

    // --- compte-rendu ---

    @Test
    void anEmptyRunReportsZeroEverywhere() {
        when(plans.findDueForReminder(any(), any(), any(), any())).thenReturn(List.of());

        AuditReminderService.Report report = service.sendDueReminders();

        assertThat(report).isEqualTo(new AuditReminderService.Report(0, 0, 0, 0, 0));
        verifyNoInteractions(notifications);
    }

    private AuditPlan plan(UUID tenant, LocalDate scheduled, String reminderEmail) {
        AuditPlan p = new AuditPlan();
        p.setId(UUID.randomUUID());
        p.setTenantId(tenant);
        p.setTitle("Audit interne ISO 9001 §9.2");
        p.setType(AuditType.INTERNAL);
        p.setStatus(AuditStatus.PLANNED);
        p.setLeadAuditorId(LEAD);
        p.setScheduledDate(scheduled);
        p.setReminderEmail(reminderEmail);
        p.setCreatedAt(NOW);
        p.setUpdatedAt(NOW);
        return p;
    }
}

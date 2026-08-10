package com.openlab.qualitos.quality.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditReminderSchedulerTest {

    @Mock AuditReminderService service;
    @InjectMocks AuditReminderScheduler scheduler;

    @Test
    void delegatesToTheService() {
        scheduler.run();
        verify(service).sendDueReminders();
    }

    @Test
    void swallowsFailures_becauseAnEscapingExceptionKillsTheScheduledTaskForGood() {
        // Spring retire définitivement une tâche dont l'exécution a levé : sans ce
        // filet, une panne passagère de base supprimerait TOUS les rappels
        // jusqu'au prochain redémarrage, sans que rien ne le signale.
        doThrow(new IllegalStateException("base indisponible")).when(service).sendDueReminders();
        assertThatCode(() -> scheduler.run()).doesNotThrowAnyException();
    }

    @Test
    void isScheduledWithAFixedDelayAndDisabledUnderTheTestProfile() throws Exception {
        Method run = AuditReminderScheduler.class.getMethod("run");
        Scheduled scheduled = run.getAnnotation(Scheduled.class);
        assertThat(scheduled).isNotNull();
        // fixedDelay et non fixedRate : deux passages ne doivent pas se chevaucher.
        assertThat(scheduled.fixedDelayString()).contains("qualitos.audit.reminder.fixed-delay-ms");
        assertThat(scheduled.initialDelayString()).contains("qualitos.audit.reminder.initial-delay-ms");
        assertThat(scheduled.fixedRateString()).isEmpty();

        Profile profile = AuditReminderScheduler.class.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("!test");
    }
}

package com.openlab.qualitos.quality.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditReminderPropertiesTest {

    @Test
    void defaultsToThirtyDaysAndATwoHundredPlanBatch() {
        AuditReminderProperties p = new AuditReminderProperties();
        assertThat(p.getDaysBefore()).isEqualTo(30);
        assertThat(p.getBatchSize()).isEqualTo(200);
    }

    @Test
    void rejectsAWindowThatWouldMakeTheReminderMeaningless() {
        // 0 ou négatif ramènerait le rappel au jour même, voire après l'échéance :
        // un dispositif qui se croit actif et n'avertit plus personne.
        AuditReminderProperties p = new AuditReminderProperties();
        assertThatThrownBy(() -> p.setDaysBefore(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.setDaysBefore(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(p.getDaysBefore()).isEqualTo(30);
    }

    @Test
    void rejectsANonPositiveBatchAndCapsAnOversizedOne() {
        AuditReminderProperties p = new AuditReminderProperties();
        assertThatThrownBy(() -> p.setBatchSize(0)).isInstanceOf(IllegalArgumentException.class);
        p.setBatchSize(10);
        assertThat(p.getBatchSize()).isEqualTo(10);
        p.setBatchSize(10_000);
        assertThat(p.getBatchSize()).isEqualTo(500);
    }

    @Test
    void acceptsAValidWindow() {
        AuditReminderProperties p = new AuditReminderProperties();
        p.setDaysBefore(7);
        assertThat(p.getDaysBefore()).isEqualTo(7);
    }
}

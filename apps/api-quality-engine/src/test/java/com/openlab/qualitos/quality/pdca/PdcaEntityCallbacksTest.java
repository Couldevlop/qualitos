package com.openlab.qualitos.quality.pdca;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre les callbacks @PrePersist / @PreUpdate des entités JPA PDCA.
 * Les services applicatifs sont mockés ; on déclenche les callbacks par réflexion.
 */
class PdcaEntityCallbacksTest {

    @Test
    void cyclePrePersist_setsTimestampsAndDefaultStatus() throws Exception {
        PdcaCycle cycle = new PdcaCycle();

        invoke(cycle, "prePersist");

        assertThat(cycle.getCreatedAt()).isNotNull();
        assertThat(cycle.getUpdatedAt()).isNotNull();
        assertThat(cycle.getStatus()).isEqualTo(PdcaStatus.PLAN);
    }

    @Test
    void cyclePrePersist_preservesExplicitStatus() throws Exception {
        PdcaCycle cycle = new PdcaCycle();
        cycle.setStatus(PdcaStatus.DO);

        invoke(cycle, "prePersist");

        assertThat(cycle.getStatus()).isEqualTo(PdcaStatus.DO);
    }

    @Test
    void cyclePreUpdate_refreshesUpdatedAt() throws Exception {
        PdcaCycle cycle = new PdcaCycle();
        cycle.setCreatedAt(Instant.now().minusSeconds(60));
        cycle.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = cycle.getUpdatedAt();

        Thread.sleep(5);
        invoke(cycle, "preUpdate");

        assertThat(cycle.getUpdatedAt()).isAfter(before);
    }

    @Test
    void stepPrePersist_setsTimestampsAndDefaultStatus() throws Exception {
        PdcaStep step = new PdcaStep();

        invoke(step, "prePersist");

        assertThat(step.getCreatedAt()).isNotNull();
        assertThat(step.getUpdatedAt()).isNotNull();
        assertThat(step.getStatus()).isEqualTo(StepStatus.PENDING);
    }

    @Test
    void stepPrePersist_preservesExplicitStatus() throws Exception {
        PdcaStep step = new PdcaStep();
        step.setStatus(StepStatus.IN_PROGRESS);

        invoke(step, "prePersist");

        assertThat(step.getStatus()).isEqualTo(StepStatus.IN_PROGRESS);
    }

    @Test
    void stepPreUpdate_refreshesUpdatedAt() throws Exception {
        PdcaStep step = new PdcaStep();
        step.setCreatedAt(Instant.now().minusSeconds(60));
        step.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = step.getUpdatedAt();

        Thread.sleep(5);
        invoke(step, "preUpdate");

        assertThat(step.getUpdatedAt()).isAfter(before);
    }

    private static void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}

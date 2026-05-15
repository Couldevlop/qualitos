package com.openlab.qualitos.quality.change;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeEntityCallbacksTest {

    @Test
    void changePrePersist_defaultsStatusDraftAndPriorityMedium() throws Exception {
        ChangeRequest c = new ChangeRequest();
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.DRAFT);
        assertThat(c.getPriority()).isEqualTo(ChangeRequestPriority.MEDIUM);
        assertThat(c.getCreatedAt()).isNotNull();
    }

    @Test
    void changePrePersist_preservesValues() throws Exception {
        ChangeRequest c = new ChangeRequest();
        c.setStatus(ChangeRequestStatus.SUBMITTED);
        c.setPriority(ChangeRequestPriority.HIGH);
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.SUBMITTED);
        assertThat(c.getPriority()).isEqualTo(ChangeRequestPriority.HIGH);
    }

    @Test
    void changePreUpdate_refreshes() throws Exception {
        ChangeRequest c = new ChangeRequest();
        c.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = c.getUpdatedAt();
        Thread.sleep(5);
        invoke(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isAfter(before);
    }

    @Test
    void impactPrePersist_stamps() throws Exception {
        ChangeImpact i = new ChangeImpact();
        invoke(i, "prePersist");
        assertThat(i.getCreatedAt()).isNotNull();
    }

    @Test
    void approvalPrePersist_defaultsPendingAndLevel1() throws Exception {
        ChangeApproval a = new ChangeApproval();
        invoke(a, "prePersist");
        assertThat(a.getDecision()).isEqualTo(ApprovalDecision.PENDING);
        assertThat(a.getApprovalLevel()).isOne();
    }

    @Test
    void approvalPrePersist_preservesValues() throws Exception {
        ChangeApproval a = new ChangeApproval();
        a.setDecision(ApprovalDecision.APPROVED);
        a.setApprovalLevel(3);
        invoke(a, "prePersist");
        assertThat(a.getDecision()).isEqualTo(ApprovalDecision.APPROVED);
        assertThat(a.getApprovalLevel()).isEqualTo(3);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}

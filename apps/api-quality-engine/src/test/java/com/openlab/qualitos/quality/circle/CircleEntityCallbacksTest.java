package com.openlab.qualitos.quality.circle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CircleEntityCallbacksTest {

    @Test
    void circlePrePersist_defaults() throws Exception {
        QualityCircle c = new QualityCircle();
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(CircleStatus.ACTIVE);
        assertThat(c.getCreatedAt()).isNotNull();
    }

    @Test
    void circlePrePersist_preservesStatus() throws Exception {
        QualityCircle c = new QualityCircle();
        c.setStatus(CircleStatus.PAUSED);
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(CircleStatus.PAUSED);
    }

    @Test
    void circlePreUpdate_refreshes() throws Exception {
        QualityCircle c = new QualityCircle();
        c.setCreatedAt(Instant.now().minusSeconds(60));
        c.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = c.getUpdatedAt();
        Thread.sleep(5);
        invoke(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isAfter(before);
    }

    @Test
    void memberPrePersist_defaults() throws Exception {
        CircleMember m = new CircleMember();
        invoke(m, "prePersist");
        assertThat(m.getRole()).isEqualTo(CircleRole.MEMBER);
        assertThat(m.getJoinedAt()).isNotNull();
    }

    @Test
    void memberPrePersist_preservesRole() throws Exception {
        CircleMember m = new CircleMember();
        m.setRole(CircleRole.FACILITATOR);
        Instant fixed = Instant.now().minusSeconds(60);
        m.setJoinedAt(fixed);
        invoke(m, "prePersist");
        assertThat(m.getRole()).isEqualTo(CircleRole.FACILITATOR);
        assertThat(m.getJoinedAt()).isEqualTo(fixed);
    }

    @Test
    void meetingPrePersist_defaults() throws Exception {
        CircleMeeting m = new CircleMeeting();
        invoke(m, "prePersist");
        assertThat(m.getStatus()).isEqualTo(MeetingStatus.PLANNED);
    }

    @Test
    void meetingPrePersist_preservesStatus() throws Exception {
        CircleMeeting m = new CircleMeeting();
        m.setStatus(MeetingStatus.HELD);
        invoke(m, "prePersist");
        assertThat(m.getStatus()).isEqualTo(MeetingStatus.HELD);
    }

    @Test
    void meetingPreUpdate_refreshes() throws Exception {
        CircleMeeting m = new CircleMeeting();
        m.setCreatedAt(Instant.now().minusSeconds(60));
        m.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = m.getUpdatedAt();
        Thread.sleep(5);
        invoke(m, "preUpdate");
        assertThat(m.getUpdatedAt()).isAfter(before);
    }

    @Test
    void proposalPrePersist_defaults() throws Exception {
        CircleProposal p = new CircleProposal();
        invoke(p, "prePersist");
        assertThat(p.getStatus()).isEqualTo(ProposalStatus.PROPOSED);
    }

    @Test
    void proposalPrePersist_preservesStatus() throws Exception {
        CircleProposal p = new CircleProposal();
        p.setStatus(ProposalStatus.APPROVED);
        invoke(p, "prePersist");
        assertThat(p.getStatus()).isEqualTo(ProposalStatus.APPROVED);
    }

    @Test
    void proposalPreUpdate_refreshes() throws Exception {
        CircleProposal p = new CircleProposal();
        p.setCreatedAt(Instant.now().minusSeconds(60));
        p.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = p.getUpdatedAt();
        Thread.sleep(5);
        invoke(p, "preUpdate");
        assertThat(p.getUpdatedAt()).isAfter(before);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}

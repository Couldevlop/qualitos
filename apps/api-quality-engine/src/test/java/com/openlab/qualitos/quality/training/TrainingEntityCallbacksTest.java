package com.openlab.qualitos.quality.training;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingEntityCallbacksTest {

    @Test
    void skillPrePersist_stamps() throws Exception {
        Skill s = new Skill();
        invoke(s, "prePersist");
        assertThat(s.getCreatedAt()).isNotNull();
        assertThat(s.getUpdatedAt()).isNotNull();
    }

    @Test
    void skillPreUpdate_refreshes() throws Exception {
        Skill s = new Skill();
        s.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = s.getUpdatedAt();
        Thread.sleep(5);
        invoke(s, "preUpdate");
        assertThat(s.getUpdatedAt()).isAfter(before);
    }

    @Test
    void userSkillPrePersist_defaultsSourceManager() throws Exception {
        UserSkillAssignment a = new UserSkillAssignment();
        invoke(a, "prePersist");
        assertThat(a.getSource()).isEqualTo(CompetencySource.MANAGER);
    }

    @Test
    void userSkillIsExpiredAt() {
        UserSkillAssignment a = new UserSkillAssignment();
        a.setExpiresOn(LocalDate.parse("2026-01-01"));
        assertThat(a.isExpiredAt(LocalDate.parse("2026-06-01"))).isTrue();
        assertThat(a.isExpiredAt(LocalDate.parse("2025-12-01"))).isFalse();
        a.setExpiresOn(null);
        assertThat(a.isExpiredAt(LocalDate.parse("2030-01-01"))).isFalse();
    }

    @Test
    void pathPrePersist_defaultsStatusDraftAndPassing70() throws Exception {
        TrainingPath p = new TrainingPath();
        invoke(p, "prePersist");
        assertThat(p.getStatus()).isEqualTo(TrainingPathStatus.DRAFT);
        assertThat(p.getPassingScore()).isEqualTo(70);
    }

    @Test
    void pathPrePersist_preservesCustomPassingScore() throws Exception {
        TrainingPath p = new TrainingPath();
        p.setPassingScore(85);
        invoke(p, "prePersist");
        assertThat(p.getPassingScore()).isEqualTo(85);
    }

    @Test
    void requirementPrePersist_stamps() throws Exception {
        TrainingPathSkillRequirement r = new TrainingPathSkillRequirement();
        invoke(r, "prePersist");
        assertThat(r.getCreatedAt()).isNotNull();
    }

    @Test
    void enrollmentPrePersist_defaultsEnrolled() throws Exception {
        TrainingEnrollment e = new TrainingEnrollment();
        invoke(e, "prePersist");
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
    }

    @Test
    void competencyLevel_ofLevel_validRange() {
        for (int i = 0; i <= 4; i++) {
            assertThat(CompetencyLevel.ofLevel(i).level()).isEqualTo(i);
        }
    }

    @Test
    void competencyLevel_ofLevel_invalid() {
        assertThatThrownBy(() -> CompetencyLevel.ofLevel(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CompetencyLevel.ofLevel(5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}

package com.openlab.qualitos.quality.academy.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre les callbacks {@code @PrePersist}/{@code @PreUpdate} et les défauts métier
 * des entités Academy (invoqués via réflexion, comme le ferait Hibernate).
 */
class AcademyEntityDefaultsTest {

    @Test
    void course_prePersist_appliesDefaults() {
        AcademyCourse c = new AcademyCourse();
        ReflectionTestUtils.invokeMethod(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(c.getPassingScore()).isEqualTo(70);
        assertThat(c.getPointsReward()).isEqualTo(50);
        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getUpdatedAt()).isNotNull();
        ReflectionTestUtils.invokeMethod(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isNotNull();
    }

    @Test
    void lesson_prePersist_defaultsContentTypeText() {
        Lesson l = new Lesson();
        ReflectionTestUtils.invokeMethod(l, "prePersist");
        assertThat(l.getContentType()).isEqualTo(LessonContentType.TEXT);
        ReflectionTestUtils.invokeMethod(l, "preUpdate");
        assertThat(l.getUpdatedAt()).isNotNull();
    }

    @Test
    void quiz_prePersist_defaultsPass70() {
        Quiz q = new Quiz();
        ReflectionTestUtils.invokeMethod(q, "prePersist");
        assertThat(q.getPassScore()).isEqualTo(70);
        ReflectionTestUtils.invokeMethod(q, "preUpdate");
        assertThat(q.getUpdatedAt()).isNotNull();
    }

    @Test
    void question_prePersist_defaultsPoints1_andOptionsCopy() {
        QuizQuestion q = new QuizQuestion();
        q.setOptions(List.of("A", "B"));
        ReflectionTestUtils.invokeMethod(q, "prePersist");
        assertThat(q.getPoints()).isEqualTo(1);
        assertThat(q.getCreatedAt()).isNotNull();
        // setOptions(null) → liste vide ; getOptions ne renvoie jamais null.
        q.setOptions(null);
        assertThat(q.getOptions()).isEmpty();
    }

    @Test
    void enrollment_prePersist_defaultsEnrolled() {
        AcademyEnrollment e = new AcademyEnrollment();
        ReflectionTestUtils.invokeMethod(e, "prePersist");
        assertThat(e.getStatus()).isEqualTo(AcademyEnrollmentStatus.ENROLLED);
        assertThat(e.getEnrolledAt()).isNotNull();
        ReflectionTestUtils.invokeMethod(e, "preUpdate");
        assertThat(e.getUpdatedAt()).isNotNull();
    }

    @Test
    void enrollmentStatus_terminalFlag() {
        assertThat(AcademyEnrollmentStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(AcademyEnrollmentStatus.FAILED.isTerminal()).isTrue();
        assertThat(AcademyEnrollmentStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(AcademyEnrollmentStatus.ENROLLED.isTerminal()).isFalse();
        assertThat(AcademyEnrollmentStatus.IN_PROGRESS.isTerminal()).isFalse();
    }

    @Test
    void lessonCompletion_andAttempt_prePersist() {
        LessonCompletion lc = new LessonCompletion();
        ReflectionTestUtils.invokeMethod(lc, "prePersist");
        assertThat(lc.getCompletedAt()).isNotNull();

        QuizAttempt a = new QuizAttempt();
        a.setAnswers(List.of(0, 1));
        ReflectionTestUtils.invokeMethod(a, "prePersist");
        assertThat(a.getAttemptedAt()).isNotNull();
        a.setAnswers(null);
        assertThat(a.getAnswers()).isEmpty();
    }

    @Test
    void certificate_prePersist_setsTimestamps() {
        AcademyCertificate cert = new AcademyCertificate();
        cert.setExpiresOn(LocalDate.of(2030, 1, 1));
        cert.setUserId(UUID.randomUUID());
        ReflectionTestUtils.invokeMethod(cert, "prePersist");
        assertThat(cert.getCreatedAt()).isNotNull();
        assertThat(cert.getIssuedAt()).isNotNull();
        assertThat(cert.getExpiresOn()).isEqualTo(LocalDate.of(2030, 1, 1));
    }

    @Test
    void module_prePersist() {
        AcademyModule m = new AcademyModule();
        ReflectionTestUtils.invokeMethod(m, "prePersist");
        assertThat(m.getCreatedAt()).isNotNull();
        ReflectionTestUtils.invokeMethod(m, "preUpdate");
        assertThat(m.getUpdatedAt()).isNotNull();
    }
}

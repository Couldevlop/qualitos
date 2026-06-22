package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.quality.academy.domain.*;
import com.openlab.qualitos.quality.academy.infrastructure.*;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.training.GamificationService;
import com.openlab.qualitos.quality.training.LearnerProgress;
import com.openlab.qualitos.quality.training.LearnerProgressRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademyLearningServiceTest {

    @Mock AcademyCourseRepository courses;
    @Mock AcademyModuleRepository modules;
    @Mock LessonRepository lessons;
    @Mock QuizRepository quizzes;
    @Mock QuizQuestionRepository questions;
    @Mock AcademyEnrollmentRepository enrollments;
    @Mock LessonCompletionRepository completions;
    @Mock QuizAttemptRepository attempts;
    @Mock LearnerProgressRepository learnerProgress;
    @Mock AcademyCertificateService certificates;

    GamificationService gamification;
    AcademyLearningService service;

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
    static final UUID USER = UUID.randomUUID();
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-22T09:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        gamification = new GamificationService(learnerProgress);
        service = new AcademyLearningService(courses, modules, lessons, quizzes, questions,
                enrollments, completions, attempts, gamification, certificates, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER.toString(), "n/a", List.of()));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---- Helpers ----

    private AcademyCourse publishedCourse(int passing, Integer validity) {
        AcademyCourse c = new AcademyCourse();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setCode("c1");
        c.setTitle("Cours 1");
        c.setStatus(CourseStatus.PUBLISHED);
        c.setPassingScore(passing);
        c.setValidityMonths(validity);
        return c;
    }

    private AcademyModule module(UUID courseId) {
        AcademyModule m = new AcademyModule();
        m.setId(UUID.randomUUID());
        m.setTenantId(TENANT);
        m.setCourseId(courseId);
        return m;
    }

    private Lesson lesson(UUID moduleId, int order) {
        Lesson l = new Lesson();
        l.setId(UUID.randomUUID());
        l.setTenantId(TENANT);
        l.setModuleId(moduleId);
        l.setOrderIndex(order);
        return l;
    }

    private Quiz quiz(UUID moduleId, int pass) {
        Quiz q = new Quiz();
        q.setId(UUID.randomUUID());
        q.setTenantId(TENANT);
        q.setModuleId(moduleId);
        q.setPassScore(pass);
        return q;
    }

    private QuizQuestion question(UUID quizId, int correct) {
        QuizQuestion q = new QuizQuestion();
        q.setId(UUID.randomUUID());
        q.setTenantId(TENANT);
        q.setQuizId(quizId);
        q.setOptions(List.of("A", "B"));
        q.setCorrectIndex(correct);
        q.setPoints(1);
        q.setOrderIndex(0);
        return q;
    }

    private AcademyEnrollment enrollment(UUID courseId, AcademyEnrollmentStatus status) {
        AcademyEnrollment e = new AcademyEnrollment();
        e.setId(UUID.randomUUID());
        e.setTenantId(TENANT);
        e.setUserId(USER);
        e.setCourseId(courseId);
        e.setStatus(status);
        return e;
    }

    // ---- Enroll ----

    @Test
    void enroll_publishedCourse_createsEnrollment() {
        AcademyCourse c = publishedCourse(70, null);
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        when(enrollments.findByTenantIdAndUserIdAndCourseId(TENANT, USER, c.getId()))
                .thenReturn(Optional.empty());
        when(enrollments.save(any())).thenAnswer(inv -> {
            AcademyEnrollment e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        AcademyDto.EnrollmentResponse r = service.enroll(new AcademyDto.EnrollRequest(c.getId()));

        assertThat(r.status()).isEqualTo(AcademyEnrollmentStatus.ENROLLED);
        assertThat(r.userId()).isEqualTo(USER);
    }

    @Test
    void enroll_notPublished_state409() {
        AcademyCourse c = publishedCourse(70, null);
        c.setStatus(CourseStatus.DRAFT);
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.enroll(new AcademyDto.EnrollRequest(c.getId())))
                .isInstanceOf(AcademyStateException.class);
    }

    @Test
    void enroll_duplicate_conflict() {
        AcademyCourse c = publishedCourse(70, null);
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        when(enrollments.findByTenantIdAndUserIdAndCourseId(TENANT, USER, c.getId()))
                .thenReturn(Optional.of(enrollment(c.getId(), AcademyEnrollmentStatus.ENROLLED)));
        assertThatThrownBy(() -> service.enroll(new AcademyDto.EnrollRequest(c.getId())))
                .isInstanceOf(AcademyConflictException.class);
    }

    // ---- Cancel ----

    @Test
    void cancel_terminal_state409() {
        AcademyEnrollment e = enrollment(UUID.randomUUID(), AcademyEnrollmentStatus.COMPLETED);
        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.cancel(e.getId()))
                .isInstanceOf(AcademyStateException.class);
    }

    @Test
    void getEnrollment_ofAnotherUser_notFound() {
        AcademyEnrollment e = enrollment(UUID.randomUUID(), AcademyEnrollmentStatus.ENROLLED);
        e.setUserId(UUID.randomUUID()); // autre user
        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.getEnrollment(e.getId()))
                .isInstanceOf(AcademyNotFoundException.class);
    }

    // ---- Complete lesson (progress) ----

    @Test
    void completeLesson_marksStarted_andUpdatesProgress() {
        AcademyCourse c = publishedCourse(70, null);
        AcademyModule m = module(c.getId());
        Lesson l1 = lesson(m.getId(), 0);
        Lesson l2 = lesson(m.getId(), 1);
        AcademyEnrollment e = enrollment(c.getId(), AcademyEnrollmentStatus.ENROLLED);

        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        when(lessons.findByTenantIdAndId(TENANT, l1.getId())).thenReturn(Optional.of(l1));
        when(modules.findById(m.getId())).thenReturn(Optional.of(m));
        when(completions.existsByEnrollmentIdAndLessonId(e.getId(), l1.getId())).thenReturn(false);
        when(modules.findByCourseIdOrderByOrderIndexAsc(c.getId())).thenReturn(List.of(m));
        when(lessons.findByModuleIdInOrderByOrderIndexAsc(List.of(m.getId())))
                .thenReturn(List.of(l1, l2));
        when(completions.countByEnrollmentId(e.getId())).thenReturn(1L);
        when(enrollments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcademyDto.EnrollmentResponse r = service.completeLesson(e.getId(),
                new AcademyDto.CompleteLessonRequest(l1.getId()));

        verify(completions).save(any(LessonCompletion.class));
        assertThat(r.status()).isEqualTo(AcademyEnrollmentStatus.IN_PROGRESS);
        assertThat(r.progressPct()).isEqualTo(50); // 1/2
    }

    @Test
    void completeLesson_lessonOfOtherCourse_state409() {
        AcademyCourse c = publishedCourse(70, null);
        AcademyModule other = module(UUID.randomUUID()); // module d'un autre cours
        Lesson l = lesson(other.getId(), 0);
        AcademyEnrollment e = enrollment(c.getId(), AcademyEnrollmentStatus.ENROLLED);

        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        when(lessons.findByTenantIdAndId(TENANT, l.getId())).thenReturn(Optional.of(l));
        when(modules.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.completeLesson(e.getId(),
                new AcademyDto.CompleteLessonRequest(l.getId())))
                .isInstanceOf(AcademyStateException.class);
    }

    // ---- Submit quiz → course completion (gamification + certificate) ----

    @Test
    void submitQuiz_passing_completesCourse_awardsPoints_andIssuesCertificate() {
        AcademyCourse c = publishedCourse(70, 12);
        AcademyModule m = module(c.getId());
        Lesson l1 = lesson(m.getId(), 0);
        Quiz quiz = quiz(m.getId(), 70);
        QuizQuestion qq = question(quiz.getId(), 0); // bonne réponse = index 0
        AcademyEnrollment e = enrollment(c.getId(), AcademyEnrollmentStatus.IN_PROGRESS);

        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        when(quizzes.findByTenantIdAndId(TENANT, quiz.getId())).thenReturn(Optional.of(quiz));
        when(modules.findById(m.getId())).thenReturn(Optional.of(m));
        when(questions.findByQuizIdOrderByOrderIndexAsc(quiz.getId())).thenReturn(List.of(qq));
        // progress + completion lookups
        when(modules.findByCourseIdOrderByOrderIndexAsc(c.getId())).thenReturn(List.of(m));
        when(lessons.findByModuleIdInOrderByOrderIndexAsc(List.of(m.getId()))).thenReturn(List.of(l1));
        when(completions.countByEnrollmentId(e.getId())).thenReturn(1L); // toutes leçons vues
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        when(quizzes.findByModuleId(m.getId())).thenReturn(Optional.of(quiz));
        when(attempts.save(any())).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        // Après save de l'attempt, bestQuizScores lit les attempts ; on renvoie l'attempt à 100.
        when(attempts.findByEnrollmentId(e.getId())).thenAnswer(inv -> {
            QuizAttempt a = new QuizAttempt();
            a.setQuizId(quiz.getId());
            a.setScore(100);
            return List.of(a);
        });
        when(enrollments.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Gamification : pas de progression existante.
        when(learnerProgress.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.empty());
        when(learnerProgress.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcademyDto.QuizResult r = service.submitQuiz(e.getId(),
                new AcademyDto.SubmitQuizRequest(quiz.getId(), List.of(0)));

        assertThat(r.score()).isEqualTo(100);
        assertThat(r.passed()).isTrue();
        assertThat(r.enrollment().status()).isEqualTo(AcademyEnrollmentStatus.COMPLETED);
        assertThat(r.enrollment().finalScore()).isEqualTo(100);

        // Gamification déclenchée (réutilisation du moteur existant).
        verify(learnerProgress).save(any(LearnerProgress.class));
        // Certificat émis.
        verify(certificates).issue(any(AcademyEnrollment.class), eq(c));
    }

    @Test
    void submitQuiz_failing_setsFailed_noCertificate() {
        AcademyCourse c = publishedCourse(70, null);
        AcademyModule m = module(c.getId());
        Quiz quiz = quiz(m.getId(), 70);
        QuizQuestion qq = question(quiz.getId(), 0);
        AcademyEnrollment e = enrollment(c.getId(), AcademyEnrollmentStatus.IN_PROGRESS);

        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        when(quizzes.findByTenantIdAndId(TENANT, quiz.getId())).thenReturn(Optional.of(quiz));
        when(modules.findById(m.getId())).thenReturn(Optional.of(m));
        when(questions.findByQuizIdOrderByOrderIndexAsc(quiz.getId())).thenReturn(List.of(qq));
        when(modules.findByCourseIdOrderByOrderIndexAsc(c.getId())).thenReturn(List.of(m));
        when(lessons.findByModuleIdInOrderByOrderIndexAsc(List.of(m.getId()))).thenReturn(List.of());
        when(completions.countByEnrollmentId(e.getId())).thenReturn(0L);
        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        when(quizzes.findByModuleId(m.getId())).thenReturn(Optional.of(quiz));
        when(attempts.save(any())).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(attempts.findByEnrollmentId(e.getId())).thenReturn(List.of());
        when(enrollments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcademyDto.QuizResult r = service.submitQuiz(e.getId(),
                new AcademyDto.SubmitQuizRequest(quiz.getId(), List.of(1))); // mauvaise réponse

        assertThat(r.passed()).isFalse();
        // Le quiz n'étant pas passé, le cours ne se complète pas → reste IN_PROGRESS.
        assertThat(r.enrollment().status()).isEqualTo(AcademyEnrollmentStatus.IN_PROGRESS);
        verify(certificates, never()).issue(any(), any());
        verify(learnerProgress, never()).save(any());
    }

    @Test
    void submitQuiz_onCancelledEnrollment_state409() {
        AcademyEnrollment e = enrollment(UUID.randomUUID(), AcademyEnrollmentStatus.CANCELLED);
        when(enrollments.findByTenantIdAndId(TENANT, e.getId())).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.submitQuiz(e.getId(),
                new AcademyDto.SubmitQuizRequest(UUID.randomUUID(), List.of(0))))
                .isInstanceOf(AcademyStateException.class);
    }

    // ---- Outline ----

    @Test
    void outline_doesNotLeakCorrectAnswers() {
        AcademyCourse c = publishedCourse(70, null);
        AcademyModule m = module(c.getId());
        Quiz quiz = quiz(m.getId(), 70);
        QuizQuestion qq = question(quiz.getId(), 1);

        when(courses.findById(c.getId())).thenReturn(Optional.of(c));
        when(modules.findByCourseIdOrderByOrderIndexAsc(c.getId())).thenReturn(List.of(m));
        when(lessons.findByModuleIdOrderByOrderIndexAsc(m.getId())).thenReturn(List.of());
        when(quizzes.findByModuleId(m.getId())).thenReturn(Optional.of(quiz));
        when(questions.findByQuizIdOrderByOrderIndexAsc(quiz.getId())).thenReturn(List.of(qq));

        AcademyDto.CourseOutline outline = service.outline(c.getId());

        assertThat(outline.modules()).hasSize(1);
        AcademyDto.QuizForLearner learnerQuiz = outline.modules().get(0).quiz();
        assertThat(learnerQuiz.questions()).hasSize(1);
        // La vue apprenant n'expose PAS correctIndex (record QuestionForLearner).
        AcademyDto.QuestionForLearner q = learnerQuiz.questions().get(0);
        assertThat(q.options()).containsExactly("A", "B");
    }
}

package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.quality.academy.domain.*;
import com.openlab.qualitos.quality.academy.infrastructure.*;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.training.GamificationDto;
import com.openlab.qualitos.quality.training.GamificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Parcours d'apprentissage côté apprenant (§19.3) : inscription, complétion de
 * leçons, soumission de quiz (auto-correction + seuil), complétion du cours.
 *
 * <p>À la complétion réussie d'un cours (toutes leçons vues + tous les quiz
 * passés, moyenne ≥ seuil du cours), le service :
 * <ol>
 *   <li>octroie des points de gamification (RÉUTILISE {@link GamificationService}
 *       — recalcul ceinture/badges) ;</li>
 *   <li>émet un certificat signé ML-DSA + ancré blockchain
 *       (RÉUTILISE {@link AcademyCertificateService}).</li>
 * </ol>
 *
 * <p>Sécurité : tenant + utilisateur résolus depuis le JWT (jamais du body).
 * Un apprenant n'agit que sur SA propre inscription (vérif {@code userId}).</p>
 */
@Service
public class AcademyLearningService {

    private final AcademyCourseRepository courses;
    private final AcademyModuleRepository modules;
    private final LessonRepository lessons;
    private final QuizRepository quizzes;
    private final QuizQuestionRepository questions;
    private final AcademyEnrollmentRepository enrollments;
    private final LessonCompletionRepository completions;
    private final QuizAttemptRepository attempts;
    private final GamificationService gamification;
    private final AcademyCertificateService certificates;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AcademyLearningService(AcademyCourseRepository courses,
                                  AcademyModuleRepository modules,
                                  LessonRepository lessons,
                                  QuizRepository quizzes,
                                  QuizQuestionRepository questions,
                                  AcademyEnrollmentRepository enrollments,
                                  LessonCompletionRepository completions,
                                  QuizAttemptRepository attempts,
                                  GamificationService gamification,
                                  AcademyCertificateService certificates) {
        this(courses, modules, lessons, quizzes, questions, enrollments, completions, attempts,
                gamification, certificates, Clock.systemUTC());
    }

    AcademyLearningService(AcademyCourseRepository courses,
                           AcademyModuleRepository modules,
                           LessonRepository lessons,
                           QuizRepository quizzes,
                           QuizQuestionRepository questions,
                           AcademyEnrollmentRepository enrollments,
                           LessonCompletionRepository completions,
                           QuizAttemptRepository attempts,
                           GamificationService gamification,
                           AcademyCertificateService certificates,
                           Clock clock) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.quizzes = quizzes;
        this.questions = questions;
        this.enrollments = enrollments;
        this.completions = completions;
        this.attempts = attempts;
        this.gamification = gamification;
        this.certificates = certificates;
        this.clock = clock;
    }

    // ===== Plan du cours pour l'apprenant (sans réponses) =====

    @Transactional(readOnly = true)
    public AcademyDto.CourseOutline outline(UUID courseId) {
        AcademyCourse course = loadCourse(courseId);
        List<AcademyModule> mods = modules.findByCourseIdOrderByOrderIndexAsc(course.getId());
        List<AcademyDto.ModuleOutline> moduleOutlines = new ArrayList<>(mods.size());
        for (AcademyModule m : mods) {
            List<AcademyDto.LessonResponse> ls = lessons.findByModuleIdOrderByOrderIndexAsc(m.getId()).stream()
                    .map(l -> new AcademyDto.LessonResponse(l.getId(), l.getModuleId(), l.getTitle(),
                            l.getContentType(), l.getBody(), l.getMediaUrl(), l.getDurationMinutes(),
                            l.getOrderIndex(), l.getCreatedAt(), l.getUpdatedAt()))
                    .toList();
            AcademyDto.QuizForLearner quizDto = quizzes.findByModuleId(m.getId())
                    .map(this::toLearnerQuiz).orElse(null);
            moduleOutlines.add(new AcademyDto.ModuleOutline(
                    new AcademyDto.ModuleResponse(m.getId(), m.getCourseId(), m.getTitle(),
                            m.getSummary(), m.getOrderIndex(), m.getCreatedAt(), m.getUpdatedAt()),
                    ls, quizDto));
        }
        return new AcademyDto.CourseOutline(toCourse(course), moduleOutlines);
    }

    // ===== Inscriptions =====

    @Transactional
    public AcademyDto.EnrollmentResponse enroll(AcademyDto.EnrollRequest req) {
        UUID tenantId = requireTenantId();
        UUID userId = CurrentUser.requireUserId();
        AcademyCourse course = loadCourse(req.courseId());
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new AcademyStateException("Cannot enroll in a course that is not PUBLISHED");
        }
        enrollments.findByTenantIdAndUserIdAndCourseId(tenantId, userId, course.getId())
                .ifPresent(e -> {
                    throw new AcademyConflictException(
                            "Already enrolled in this course (status: " + e.getStatus() + ")");
                });
        AcademyEnrollment e = new AcademyEnrollment();
        e.setTenantId(tenantId);
        e.setUserId(userId);
        e.setCourseId(course.getId());
        e.setStatus(AcademyEnrollmentStatus.ENROLLED);
        e.setProgressPct(0);
        e.setEnrolledAt(Instant.now(clock));
        return toEnrollment(enrollments.save(e));
    }

    @Transactional(readOnly = true)
    public Page<AcademyDto.EnrollmentResponse> myEnrollments(Pageable pageable) {
        UUID tenantId = requireTenantId();
        UUID userId = CurrentUser.requireUserId();
        return enrollments.findByTenantIdAndUserId(tenantId, userId, pageable).map(this::toEnrollment);
    }

    @Transactional(readOnly = true)
    public AcademyDto.EnrollmentResponse getEnrollment(UUID id) {
        return toEnrollment(loadOwnEnrollment(id));
    }

    @Transactional
    public AcademyDto.EnrollmentResponse cancel(UUID id) {
        AcademyEnrollment e = loadOwnEnrollment(id);
        if (e.getStatus().isTerminal()) {
            throw new AcademyStateException("Cannot cancel an enrollment in terminal status " + e.getStatus());
        }
        e.setStatus(AcademyEnrollmentStatus.CANCELLED);
        return toEnrollment(enrollments.save(e));
    }

    // ===== Complétion de leçon =====

    @Transactional
    public AcademyDto.EnrollmentResponse completeLesson(UUID enrollmentId, AcademyDto.CompleteLessonRequest req) {
        AcademyEnrollment e = loadActiveEnrollment(enrollmentId);
        Lesson lesson = lessons.findByTenantIdAndId(e.getTenantId(), req.lessonId())
                .orElseThrow(() -> new AcademyNotFoundException("Lesson", req.lessonId()));
        assertLessonBelongsToCourse(lesson, e.getCourseId());

        if (!completions.existsByEnrollmentIdAndLessonId(e.getId(), lesson.getId())) {
            LessonCompletion lc = new LessonCompletion();
            lc.setTenantId(e.getTenantId());
            lc.setEnrollmentId(e.getId());
            lc.setLessonId(lesson.getId());
            lc.setCompletedAt(Instant.now(clock));
            completions.save(lc);
        }
        markStarted(e);
        recomputeProgress(e);
        return toEnrollment(enrollments.save(e));
    }

    // ===== Soumission de quiz (auto-correction + seuil) =====

    @Transactional
    public AcademyDto.QuizResult submitQuiz(UUID enrollmentId, AcademyDto.SubmitQuizRequest req) {
        AcademyEnrollment e = loadActiveEnrollment(enrollmentId);
        Quiz quiz = quizzes.findByTenantIdAndId(e.getTenantId(), req.quizId())
                .orElseThrow(() -> new AcademyNotFoundException("Quiz", req.quizId()));
        assertQuizBelongsToCourse(quiz, e.getCourseId());

        List<QuizQuestion> qs = questions.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        QuizGrader.Result graded = QuizGrader.grade(qs, req.answers(), quiz.getPassScore());

        QuizAttempt attempt = new QuizAttempt();
        attempt.setTenantId(e.getTenantId());
        attempt.setEnrollmentId(e.getId());
        attempt.setQuizId(quiz.getId());
        attempt.setScore(graded.score());
        attempt.setPassed(graded.passed());
        attempt.setAnswers(req.answers());
        attempt.setAttemptedAt(Instant.now(clock));
        QuizAttempt savedAttempt = attempts.save(attempt);

        markStarted(e);
        recomputeProgress(e);
        // Tente la complétion du cours si tout est validé.
        maybeCompleteCourse(e);
        AcademyEnrollment refreshed = enrollments.save(e);

        return new AcademyDto.QuizResult(savedAttempt.getId(), quiz.getId(), graded.score(),
                graded.passed(), graded.earnedPoints(), graded.totalPoints(), toEnrollment(refreshed));
    }

    // ===== Logique de progression / complétion =====

    private void markStarted(AcademyEnrollment e) {
        if (e.getStatus() == AcademyEnrollmentStatus.ENROLLED) {
            e.setStatus(AcademyEnrollmentStatus.IN_PROGRESS);
            if (e.getStartedAt() == null) e.setStartedAt(Instant.now(clock));
        }
    }

    /** Recalcule le pourcentage d'avancement = leçons complétées / total leçons. */
    private void recomputeProgress(AcademyEnrollment e) {
        List<AcademyModule> mods = modules.findByCourseIdOrderByOrderIndexAsc(e.getCourseId());
        List<UUID> moduleIds = mods.stream().map(AcademyModule::getId).toList();
        long totalLessons = moduleIds.isEmpty() ? 0
                : lessons.findByModuleIdInOrderByOrderIndexAsc(moduleIds).size();
        long done = completions.countByEnrollmentId(e.getId());
        int pct = totalLessons == 0 ? 100 : (int) Math.min(100, Math.round((done * 100.0) / totalLessons));
        if (pct > e.getProgressPct()) {
            e.setProgressPct(pct);
        }
    }

    /**
     * Complète le cours si TOUTES les leçons sont vues ET tous les quiz du cours
     * sont passés ; calcule le score final (moyenne des meilleurs scores de quiz),
     * octroie les points de gamification et émet le certificat si réussi.
     */
    private void maybeCompleteCourse(AcademyEnrollment e) {
        if (e.getStatus().isTerminal()) return;

        AcademyCourse course = courses.findById(e.getCourseId())
                .orElseThrow(() -> new AcademyNotFoundException("Course", e.getCourseId()));
        List<AcademyModule> mods = modules.findByCourseIdOrderByOrderIndexAsc(e.getCourseId());
        List<UUID> moduleIds = mods.stream().map(AcademyModule::getId).toList();

        // 1) Toutes les leçons complétées ?
        long totalLessons = moduleIds.isEmpty() ? 0
                : lessons.findByModuleIdInOrderByOrderIndexAsc(moduleIds).size();
        if (totalLessons > 0 && completions.countByEnrollmentId(e.getId()) < totalLessons) {
            return; // encore des leçons à voir
        }

        // 2) Tous les quiz du cours passés ? Score = moyenne des meilleurs scores.
        List<Quiz> courseQuizzes = new ArrayList<>();
        for (UUID moduleId : moduleIds) {
            quizzes.findByModuleId(moduleId).ifPresent(courseQuizzes::add);
        }
        Map<UUID, Integer> bestScores = bestQuizScores(e.getId());
        for (Quiz q : courseQuizzes) {
            Integer best = bestScores.get(q.getId());
            if (best == null || best < q.getPassScore()) {
                return; // un quiz non encore passé
            }
        }

        int finalScore = courseQuizzes.isEmpty() ? 100
                : (int) Math.round(courseQuizzes.stream()
                        .mapToInt(q -> bestScores.getOrDefault(q.getId(), 0))
                        .average().orElse(0));

        e.setProgressPct(100);
        e.setFinalScore(finalScore);
        e.setCompletedAt(Instant.now(clock));

        if (finalScore >= course.getPassingScore()) {
            e.setStatus(AcademyEnrollmentStatus.COMPLETED);
            if (course.getValidityMonths() != null) {
                e.setExpiresOn(LocalDate.now(clock).plusMonths(course.getValidityMonths()));
            }
            // Gamification : octroi de points (réutilise le moteur existant).
            gamification.complete(new GamificationDto.CompleteLearningRequest(
                    "academy:" + course.getCode(), finalScore));
            // Persiste l'inscription AVANT d'émettre le certificat (clé étrangère).
            enrollments.save(e);
            certificates.issue(e, course);
        } else {
            e.setStatus(AcademyEnrollmentStatus.FAILED);
        }
    }

    private Map<UUID, Integer> bestQuizScores(UUID enrollmentId) {
        return attempts.findByEnrollmentId(enrollmentId).stream()
                .collect(Collectors.toMap(QuizAttempt::getQuizId, QuizAttempt::getScore, Math::max));
    }

    // ===== Helpers =====

    private AcademyDto.QuizForLearner toLearnerQuiz(Quiz quiz) {
        List<AcademyDto.QuestionForLearner> qs = questions.findByQuizIdOrderByOrderIndexAsc(quiz.getId()).stream()
                .map(q -> new AcademyDto.QuestionForLearner(q.getId(), q.getStem(),
                        q.getOptions(), q.getPoints(), q.getOrderIndex()))
                .toList();
        return new AcademyDto.QuizForLearner(quiz.getId(), quiz.getModuleId(),
                quiz.getTitle(), quiz.getPassScore(), qs);
    }

    private void assertLessonBelongsToCourse(Lesson lesson, UUID courseId) {
        AcademyModule m = modules.findById(lesson.getModuleId())
                .orElseThrow(() -> new AcademyNotFoundException("Module", lesson.getModuleId()));
        if (!m.getCourseId().equals(courseId)) {
            throw new AcademyStateException("Lesson does not belong to the enrolled course");
        }
    }

    private void assertQuizBelongsToCourse(Quiz quiz, UUID courseId) {
        AcademyModule m = modules.findById(quiz.getModuleId())
                .orElseThrow(() -> new AcademyNotFoundException("Module", quiz.getModuleId()));
        if (!m.getCourseId().equals(courseId)) {
            throw new AcademyStateException("Quiz does not belong to the enrolled course");
        }
    }

    AcademyCourse loadCourse(UUID id) {
        UUID tenantId = requireTenantId();
        AcademyCourse c = courses.findById(id).orElseThrow(() -> new AcademyNotFoundException("Course", id));
        if (!c.getTenantId().equals(tenantId)) throw new AcademyNotFoundException("Course", id);
        return c;
    }

    /** Charge une inscription appartenant au tenant ET à l'utilisateur courant. */
    AcademyEnrollment loadOwnEnrollment(UUID id) {
        UUID tenantId = requireTenantId();
        UUID userId = CurrentUser.requireUserId();
        AcademyEnrollment e = enrollments.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new AcademyNotFoundException("Enrollment", id));
        if (!e.getUserId().equals(userId)) {
            // Ne révèle pas l'existence d'une inscription d'autrui (404, pas 403).
            throw new AcademyNotFoundException("Enrollment", id);
        }
        return e;
    }

    private AcademyEnrollment loadActiveEnrollment(UUID id) {
        AcademyEnrollment e = loadOwnEnrollment(id);
        if (e.getStatus() == AcademyEnrollmentStatus.CANCELLED) {
            throw new AcademyStateException("Enrollment is cancelled");
        }
        return e;
    }

    private AcademyDto.CourseResponse toCourse(AcademyCourse c) {
        return new AcademyDto.CourseResponse(c.getId(), c.getTenantId(), c.getCode(), c.getTitle(),
                c.getDescription(), c.getTargetRole(), c.getIndustrySector(), c.getPassingScore(),
                c.getPointsReward(), c.getValidityMonths(), c.getStatus(), c.getCreatedBy(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private AcademyDto.EnrollmentResponse toEnrollment(AcademyEnrollment e) {
        return new AcademyDto.EnrollmentResponse(e.getId(), e.getTenantId(), e.getUserId(), e.getCourseId(),
                e.getStatus(), e.getProgressPct(), e.getFinalScore(), e.getEnrolledAt(),
                e.getStartedAt(), e.getCompletedAt(), e.getExpiresOn(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}

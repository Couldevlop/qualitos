package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.quality.academy.domain.*;
import com.openlab.qualitos.quality.academy.infrastructure.*;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Autoring du contenu e-learning : cours → modules → leçons → quiz (§19.3).
 *
 * <p>Multi-tenant strict : {@code tenantId} et acteur résolus depuis le JWT
 * ({@link TenantContext} / {@link CurrentUser}). Toute lecture/écriture filtre
 * par tenant ; les sous-ressources sont rattachées via leur parent contrôlé.
 * La publication d'un cours exige au moins un module (anti-coquille vide).</p>
 */
@Service
public class AcademyCourseService {

    private final AcademyCourseRepository courses;
    private final AcademyModuleRepository modules;
    private final LessonRepository lessons;
    private final QuizRepository quizzes;
    private final QuizQuestionRepository questions;

    public AcademyCourseService(AcademyCourseRepository courses,
                                AcademyModuleRepository modules,
                                LessonRepository lessons,
                                QuizRepository quizzes,
                                QuizQuestionRepository questions) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.quizzes = quizzes;
        this.questions = questions;
    }

    // ===== Cours =====

    @Transactional
    public AcademyDto.CourseResponse createCourse(AcademyDto.CreateCourseRequest req) {
        UUID tenantId = requireTenantId();
        UUID actor = CurrentUser.requireUserId();
        courses.findByTenantIdAndCode(tenantId, req.code()).ifPresent(c -> {
            throw new AcademyConflictException("Course code already exists: " + req.code());
        });
        AcademyCourse c = new AcademyCourse();
        c.setTenantId(tenantId);
        c.setCode(req.code());
        c.setTitle(req.title());
        c.setDescription(req.description());
        c.setTargetRole(req.targetRole());
        c.setIndustrySector(req.industrySector());
        c.setPassingScore(req.passingScore() == null ? 70 : req.passingScore());
        c.setPointsReward(req.pointsReward() == null ? 50 : req.pointsReward());
        c.setValidityMonths(req.validityMonths());
        c.setStatus(CourseStatus.DRAFT);
        c.setCreatedBy(actor);
        return toCourse(courses.save(c));
    }

    @Transactional(readOnly = true)
    public Page<AcademyDto.CourseResponse> listCourses(CourseStatus status, String targetRole,
                                                       String sector, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<AcademyCourse> page;
        if (status != null) {
            page = courses.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (targetRole != null) {
            page = courses.findByTenantIdAndTargetRole(tenantId, targetRole, pageable);
        } else if (sector != null) {
            page = courses.findByTenantIdAndIndustrySector(tenantId, sector, pageable);
        } else {
            page = courses.findByTenantId(tenantId, pageable);
        }
        return page.map(this::toCourse);
    }

    @Transactional(readOnly = true)
    public AcademyDto.CourseResponse getCourse(UUID id) {
        return toCourse(loadCourse(id));
    }

    @Transactional
    public AcademyDto.CourseResponse updateCourse(UUID id, AcademyDto.UpdateCourseRequest req) {
        AcademyCourse c = loadCourse(id);
        if (req.title() != null) c.setTitle(req.title());
        if (req.description() != null) c.setDescription(req.description());
        if (req.targetRole() != null) c.setTargetRole(req.targetRole());
        if (req.industrySector() != null) c.setIndustrySector(req.industrySector());
        if (req.passingScore() != null) c.setPassingScore(req.passingScore());
        if (req.pointsReward() != null) c.setPointsReward(req.pointsReward());
        if (req.validityMonths() != null) c.setValidityMonths(req.validityMonths());
        return toCourse(courses.save(c));
    }

    @Transactional
    public AcademyDto.CourseResponse publishCourse(UUID id) {
        AcademyCourse c = loadCourse(id);
        if (c.getStatus() == CourseStatus.PUBLISHED) {
            return toCourse(c);
        }
        if (modules.countByCourseId(c.getId()) == 0) {
            throw new AcademyStateException("Cannot publish a course without at least one module");
        }
        c.setStatus(CourseStatus.PUBLISHED);
        return toCourse(courses.save(c));
    }

    @Transactional
    public AcademyDto.CourseResponse archiveCourse(UUID id) {
        AcademyCourse c = loadCourse(id);
        c.setStatus(CourseStatus.ARCHIVED);
        return toCourse(courses.save(c));
    }

    @Transactional
    public void deleteCourse(UUID id) {
        AcademyCourse c = loadCourse(id);
        courses.delete(c);
    }

    // ===== Modules =====

    @Transactional
    public AcademyDto.ModuleResponse addModule(UUID courseId, AcademyDto.CreateModuleRequest req) {
        AcademyCourse c = loadCourse(courseId);
        if (modules.existsByCourseIdAndOrderIndex(c.getId(), req.orderIndex())) {
            throw new AcademyConflictException("Module order index already used: " + req.orderIndex());
        }
        AcademyModule m = new AcademyModule();
        m.setTenantId(c.getTenantId());
        m.setCourseId(c.getId());
        m.setTitle(req.title());
        m.setSummary(req.summary());
        m.setOrderIndex(req.orderIndex());
        return toModule(modules.save(m));
    }

    @Transactional(readOnly = true)
    public List<AcademyDto.ModuleResponse> listModules(UUID courseId) {
        AcademyCourse c = loadCourse(courseId);
        return modules.findByCourseIdOrderByOrderIndexAsc(c.getId()).stream().map(this::toModule).toList();
    }

    @Transactional
    public void deleteModule(UUID moduleId) {
        AcademyModule m = loadModule(moduleId);
        modules.delete(m);
    }

    // ===== Leçons =====

    @Transactional
    public AcademyDto.LessonResponse addLesson(UUID moduleId, AcademyDto.CreateLessonRequest req) {
        AcademyModule m = loadModule(moduleId);
        if (lessons.existsByModuleIdAndOrderIndex(m.getId(), req.orderIndex())) {
            throw new AcademyConflictException("Lesson order index already used: " + req.orderIndex());
        }
        Lesson l = new Lesson();
        l.setTenantId(m.getTenantId());
        l.setModuleId(m.getId());
        l.setTitle(req.title());
        l.setContentType(req.contentType());
        l.setBody(req.body());
        l.setMediaUrl(req.mediaUrl());
        l.setDurationMinutes(req.durationMinutes() == null ? 0 : req.durationMinutes());
        l.setOrderIndex(req.orderIndex());
        return toLesson(lessons.save(l));
    }

    @Transactional(readOnly = true)
    public List<AcademyDto.LessonResponse> listLessons(UUID moduleId) {
        AcademyModule m = loadModule(moduleId);
        return lessons.findByModuleIdOrderByOrderIndexAsc(m.getId()).stream().map(this::toLesson).toList();
    }

    @Transactional
    public void deleteLesson(UUID lessonId) {
        Lesson l = lessons.findByTenantIdAndId(requireTenantId(), lessonId)
                .orElseThrow(() -> new AcademyNotFoundException("Lesson", lessonId));
        lessons.delete(l);
    }

    // ===== Quiz + questions =====

    @Transactional
    public AcademyDto.QuizResponse setQuiz(UUID moduleId, AcademyDto.CreateQuizRequest req) {
        AcademyModule m = loadModule(moduleId);
        // Un seul quiz par module : on remplace l'existant atomiquement.
        quizzes.findByModuleId(m.getId()).ifPresent(quizzes::delete);
        quizzes.flush();

        Quiz quiz = new Quiz();
        quiz.setTenantId(m.getTenantId());
        quiz.setModuleId(m.getId());
        quiz.setTitle(req.title());
        quiz.setPassScore(req.passScore() == null ? 70 : req.passScore());
        Quiz savedQuiz = quizzes.save(quiz);

        for (AcademyDto.CreateQuestionRequest qr : req.questions()) {
            if (qr.correctIndex() >= qr.options().size()) {
                throw new AcademyStateException(
                        "correctIndex " + qr.correctIndex() + " out of bounds for question '"
                                + qr.stem() + "' (" + qr.options().size() + " options)");
            }
            QuizQuestion q = new QuizQuestion();
            q.setTenantId(m.getTenantId());
            q.setQuizId(savedQuiz.getId());
            q.setStem(qr.stem());
            q.setOptions(qr.options());
            q.setCorrectIndex(qr.correctIndex());
            q.setPoints(qr.points() == null ? 1 : qr.points());
            q.setOrderIndex(qr.orderIndex());
            questions.save(q);
        }
        return getQuiz(m.getId());
    }

    @Transactional(readOnly = true)
    public AcademyDto.QuizResponse getQuiz(UUID moduleId) {
        AcademyModule m = loadModule(moduleId);
        Quiz quiz = quizzes.findByModuleId(m.getId())
                .orElseThrow(() -> new AcademyNotFoundException("Quiz for module", moduleId));
        List<AcademyDto.QuestionResponse> qs = questions.findByQuizIdOrderByOrderIndexAsc(quiz.getId()).stream()
                .map(q -> new AcademyDto.QuestionResponse(
                        q.getId(), q.getStem(), q.getOptions(), q.getCorrectIndex(),
                        q.getPoints(), q.getOrderIndex()))
                .toList();
        return new AcademyDto.QuizResponse(quiz.getId(), quiz.getModuleId(), quiz.getTitle(),
                quiz.getPassScore(), qs, quiz.getCreatedAt(), quiz.getUpdatedAt());
    }

    // ===== Helpers de chargement contrôlés par tenant =====

    AcademyCourse loadCourse(UUID id) {
        UUID tenantId = requireTenantId();
        AcademyCourse c = courses.findById(id).orElseThrow(() -> new AcademyNotFoundException("Course", id));
        if (!c.getTenantId().equals(tenantId)) throw new AcademyNotFoundException("Course", id);
        return c;
    }

    AcademyModule loadModule(UUID id) {
        return modules.findByTenantIdAndId(requireTenantId(), id)
                .orElseThrow(() -> new AcademyNotFoundException("Module", id));
    }

    AcademyDto.CourseResponse toCourse(AcademyCourse c) {
        return new AcademyDto.CourseResponse(c.getId(), c.getTenantId(), c.getCode(), c.getTitle(),
                c.getDescription(), c.getTargetRole(), c.getIndustrySector(), c.getPassingScore(),
                c.getPointsReward(), c.getValidityMonths(), c.getStatus(), c.getCreatedBy(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    AcademyDto.ModuleResponse toModule(AcademyModule m) {
        return new AcademyDto.ModuleResponse(m.getId(), m.getCourseId(), m.getTitle(),
                m.getSummary(), m.getOrderIndex(), m.getCreatedAt(), m.getUpdatedAt());
    }

    AcademyDto.LessonResponse toLesson(Lesson l) {
        return new AcademyDto.LessonResponse(l.getId(), l.getModuleId(), l.getTitle(),
                l.getContentType(), l.getBody(), l.getMediaUrl(), l.getDurationMinutes(),
                l.getOrderIndex(), l.getCreatedAt(), l.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}

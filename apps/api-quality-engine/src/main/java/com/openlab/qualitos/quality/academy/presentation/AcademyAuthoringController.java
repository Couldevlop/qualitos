package com.openlab.qualitos.quality.academy.presentation;

import com.openlab.qualitos.quality.academy.application.AcademyCourseService;
import com.openlab.qualitos.quality.academy.application.AcademyDto;
import com.openlab.qualitos.quality.academy.application.AcademyExportService;
import com.openlab.qualitos.quality.academy.domain.CourseStatus;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API d'autoring du contenu e-learning (§19.3). Réservée aux rôles de pilotage
 * qualité (création/édition de cours, modules, leçons, quiz) — RBAC OWASP A01.
 *
 * <p>Rôles autorisés (cf. SecurityConfig / realm Keycloak) :
 * {@code QUALITY_MANAGER}, {@code ADMIN_TENANT}, {@code SUPER_ADMIN}, {@code ADMIN}.</p>
 */
@RestController
@RequestMapping("/api/v1/academy")
@PreAuthorize("hasAnyRole('QUALITY_MANAGER','ADMIN_TENANT','SUPER_ADMIN','ADMIN')")
public class AcademyAuthoringController {

    private final AcademyCourseService courseService;
    private final AcademyExportService exportService;

    public AcademyAuthoringController(AcademyCourseService courseService,
                                      AcademyExportService exportService) {
        this.courseService = courseService;
        this.exportService = exportService;
    }

    // ===== Cours =====

    @GetMapping("/courses")
    public Page<AcademyDto.CourseResponse> listCourses(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) String targetRole,
            @RequestParam(required = false) String sector,
            @PageableDefault(size = 50) Pageable pageable) {
        return courseService.listCourses(status, targetRole, sector, pageable);
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademyDto.CourseResponse createCourse(@Valid @RequestBody AcademyDto.CreateCourseRequest req) {
        return courseService.createCourse(req);
    }

    @GetMapping("/courses/{id}")
    public AcademyDto.CourseResponse getCourse(@PathVariable UUID id) {
        return courseService.getCourse(id);
    }

    @PatchMapping("/courses/{id}")
    public AcademyDto.CourseResponse updateCourse(@PathVariable UUID id,
                                                  @Valid @RequestBody AcademyDto.UpdateCourseRequest req) {
        return courseService.updateCourse(id, req);
    }

    @PostMapping("/courses/{id}/publish")
    public AcademyDto.CourseResponse publishCourse(@PathVariable UUID id) {
        return courseService.publishCourse(id);
    }

    @PostMapping("/courses/{id}/archive")
    public AcademyDto.CourseResponse archiveCourse(@PathVariable UUID id) {
        return courseService.archiveCourse(id);
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
    }

    // ===== Modules =====

    @GetMapping("/courses/{courseId}/modules")
    public List<AcademyDto.ModuleResponse> listModules(@PathVariable UUID courseId) {
        return courseService.listModules(courseId);
    }

    @PostMapping("/courses/{courseId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademyDto.ModuleResponse addModule(@PathVariable UUID courseId,
                                               @Valid @RequestBody AcademyDto.CreateModuleRequest req) {
        return courseService.addModule(courseId, req);
    }

    @DeleteMapping("/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(@PathVariable UUID moduleId) {
        courseService.deleteModule(moduleId);
    }

    // ===== Leçons =====

    @GetMapping("/modules/{moduleId}/lessons")
    public List<AcademyDto.LessonResponse> listLessons(@PathVariable UUID moduleId) {
        return courseService.listLessons(moduleId);
    }

    @PostMapping("/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademyDto.LessonResponse addLesson(@PathVariable UUID moduleId,
                                               @Valid @RequestBody AcademyDto.CreateLessonRequest req) {
        return courseService.addLesson(moduleId, req);
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(@PathVariable UUID lessonId) {
        courseService.deleteLesson(lessonId);
    }

    // ===== Quiz =====

    @PutMapping("/modules/{moduleId}/quiz")
    public AcademyDto.QuizResponse setQuiz(@PathVariable UUID moduleId,
                                           @Valid @RequestBody AcademyDto.CreateQuizRequest req) {
        return courseService.setQuiz(moduleId, req);
    }

    @GetMapping("/modules/{moduleId}/quiz")
    public AcademyDto.QuizResponse getQuiz(@PathVariable UUID moduleId) {
        return courseService.getQuiz(moduleId);
    }

    // ===== Export SCORM (le cours complet) =====

    @GetMapping("/courses/{courseId}/export/scorm")
    public ResponseEntity<ByteArrayResource> exportScorm(@PathVariable UUID courseId) {
        byte[] zip = exportService.exportScorm(courseId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"scorm-" + courseId + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zip.length)
                .body(new ByteArrayResource(zip));
    }
}

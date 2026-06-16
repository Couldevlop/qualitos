package com.openlab.qualitos.quality.training;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/training")
public class TrainingController {

    private final SkillService skillService;
    private final TrainingPathService pathService;
    private final TrainingEnrollmentService enrollmentService;
    private final GamificationService gamificationService;

    public TrainingController(SkillService skillService,
                              TrainingPathService pathService,
                              TrainingEnrollmentService enrollmentService,
                              GamificationService gamificationService) {
        this.skillService = skillService;
        this.pathService = pathService;
        this.enrollmentService = enrollmentService;
        this.gamificationService = gamificationService;
    }

    // ---- Skills ----

    @GetMapping("/skills")
    public Page<TrainingDto.SkillResponse> listSkills(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 50) Pageable pageable) {
        return skillService.list(category, pageable);
    }

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingDto.SkillResponse createSkill(@Valid @RequestBody TrainingDto.CreateSkillRequest req) {
        return skillService.create(req);
    }

    @GetMapping("/skills/{id}")
    public TrainingDto.SkillResponse getSkill(@PathVariable UUID id) { return skillService.get(id); }

    @PatchMapping("/skills/{id}")
    public TrainingDto.SkillResponse updateSkill(@PathVariable UUID id,
                                                 @Valid @RequestBody TrainingDto.UpdateSkillRequest req) {
        return skillService.update(id, req);
    }

    @DeleteMapping("/skills/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkill(@PathVariable UUID id) { skillService.delete(id); }

    // ---- Competencies ----

    @PostMapping("/competencies/assess")
    public TrainingDto.CompetencyResponse assess(@Valid @RequestBody TrainingDto.AssessCompetencyRequest req) {
        return skillService.assess(req);
    }

    @GetMapping("/competencies/users/{userId}")
    public TrainingDto.CompetencyMatrix matrix(@PathVariable UUID userId) {
        return skillService.matrix(userId);
    }

    @GetMapping("/competencies/users/{userId}/gap")
    public TrainingDto.RoleGapAnalysis gap(@PathVariable UUID userId, @RequestParam UUID pathId) {
        return pathService.analyzeGap(userId, pathId);
    }

    // ---- Paths ----

    @GetMapping("/paths")
    public Page<TrainingDto.PathResponse> listPaths(
            @RequestParam(required = false) TrainingPathStatus status,
            @RequestParam(required = false) String targetRole,
            @PageableDefault(size = 50) Pageable pageable) {
        return pathService.list(status, targetRole, pageable);
    }

    @PostMapping("/paths")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingDto.PathResponse createPath(@Valid @RequestBody TrainingDto.CreatePathRequest req) {
        return pathService.create(req);
    }

    @GetMapping("/paths/{id}")
    public TrainingDto.PathResponse getPath(@PathVariable UUID id) { return pathService.get(id); }

    @PatchMapping("/paths/{id}")
    public TrainingDto.PathResponse updatePath(@PathVariable UUID id,
                                               @Valid @RequestBody TrainingDto.UpdatePathRequest req) {
        return pathService.update(id, req);
    }

    @DeleteMapping("/paths/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePath(@PathVariable UUID id) { pathService.delete(id); }

    @PostMapping("/paths/{id}/activate")
    public TrainingDto.PathResponse activatePath(@PathVariable UUID id) { return pathService.activate(id); }

    @PostMapping("/paths/{id}/reopen")
    public TrainingDto.PathResponse reopenPath(@PathVariable UUID id) { return pathService.reopen(id); }

    @PostMapping("/paths/{id}/archive")
    public TrainingDto.PathResponse archivePath(@PathVariable UUID id) { return pathService.archive(id); }

    @GetMapping("/paths/{id}/requirements")
    public List<TrainingDto.SkillRequirementResponse> listRequirements(@PathVariable UUID id) {
        return pathService.listRequirements(id);
    }

    @PostMapping("/paths/{id}/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingDto.SkillRequirementResponse attachRequirement(
            @PathVariable UUID id,
            @Valid @RequestBody TrainingDto.AttachSkillRequirementRequest req) {
        return pathService.attachSkill(id, req);
    }

    @DeleteMapping("/paths/{id}/requirements/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachRequirement(@PathVariable UUID id, @PathVariable UUID skillId) {
        pathService.detachSkill(id, skillId);
    }

    // ---- Enrollments ----

    @GetMapping("/enrollments")
    public Page<TrainingDto.EnrollmentResponse> listEnrollments(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID pathId,
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        if (userId != null) return enrollmentService.listByUser(userId, pageable);
        if (pathId != null) return enrollmentService.listByPath(pathId, pageable);
        if (status != null) return enrollmentService.listByStatus(status, pageable);
        // pas de mode "tout" pour éviter une fuite cross-collection
        return Page.empty(pageable);
    }

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingDto.EnrollmentResponse enroll(@Valid @RequestBody TrainingDto.EnrollRequest req) {
        return enrollmentService.enroll(req);
    }

    @GetMapping("/enrollments/{id}")
    public TrainingDto.EnrollmentResponse getEnrollment(@PathVariable UUID id) {
        return enrollmentService.get(id);
    }

    @PostMapping("/enrollments/{id}/start")
    public TrainingDto.EnrollmentResponse start(@PathVariable UUID id) {
        return enrollmentService.start(id);
    }

    @PostMapping("/enrollments/{id}/progress")
    public TrainingDto.EnrollmentResponse progress(@PathVariable UUID id,
                                                   @Valid @RequestBody TrainingDto.ProgressUpdateRequest req) {
        return enrollmentService.updateProgress(id, req);
    }

    @PostMapping("/enrollments/{id}/complete")
    public TrainingDto.EnrollmentResponse complete(@PathVariable UUID id,
                                                   @Valid @RequestBody TrainingDto.CompleteRequest req) {
        return enrollmentService.complete(id, req);
    }

    @PostMapping("/enrollments/{id}/cancel")
    public TrainingDto.EnrollmentResponse cancel(@PathVariable UUID id) {
        return enrollmentService.cancel(id);
    }

    // ---- Certificate (public verification) ----

    @GetMapping("/certificates/{code}")
    public TrainingDto.CertificateVerification verifyCertificate(@PathVariable String code) {
        return enrollmentService.verifyCertificate(code);
    }

    // ---- Gamification (CLAUDE.md §19.3 — Yellow → Black Belt) ----

    /** Progression de gamification de l'utilisateur courant (tenant + sub du JWT). */
    @GetMapping("/progress/me")
    public GamificationDto.LearnerProgressResponse myProgress() {
        return gamificationService.myProgress();
    }

    /** Marque un parcours/quiz terminé → recalcule points, ceinture, badges. */
    @PostMapping("/progress/complete")
    public GamificationDto.LearnerProgressResponse completeLearning(
            @Valid @RequestBody GamificationDto.CompleteLearningRequest req) {
        return gamificationService.complete(req);
    }
}

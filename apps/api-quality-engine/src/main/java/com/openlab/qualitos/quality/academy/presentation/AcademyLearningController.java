package com.openlab.qualitos.quality.academy.presentation;

import com.openlab.qualitos.quality.academy.application.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API d'apprentissage côté apprenant (§19.3) : catalogue, plan de cours,
 * inscription, complétion de leçons, soumission de quiz, certificats, leaderboard.
 *
 * <p>Ouverte à tout utilisateur authentifié (l'apprenant agit sur SA propre
 * progression — le contrôle fin est appliqué dans les services à partir du JWT).</p>
 */
@RestController
@RequestMapping("/api/v1/academy/me")
public class AcademyLearningController {

    private final AcademyLearningService learning;
    private final AcademyCertificateService certificates;
    private final AcademyLeaderboardService leaderboard;
    private final AcademyExportService exportService;

    public AcademyLearningController(AcademyLearningService learning,
                                     AcademyCertificateService certificates,
                                     AcademyLeaderboardService leaderboard,
                                     AcademyExportService exportService) {
        this.learning = learning;
        this.certificates = certificates;
        this.leaderboard = leaderboard;
        this.exportService = exportService;
    }

    // ===== Plan d'un cours (sans réponses) =====

    @GetMapping("/courses/{courseId}/outline")
    public AcademyDto.CourseOutline outline(@PathVariable UUID courseId) {
        return learning.outline(courseId);
    }

    // ===== Inscriptions =====

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademyDto.EnrollmentResponse enroll(@Valid @RequestBody AcademyDto.EnrollRequest req) {
        return learning.enroll(req);
    }

    @GetMapping("/enrollments")
    public Page<AcademyDto.EnrollmentResponse> myEnrollments(@PageableDefault(size = 50) Pageable pageable) {
        return learning.myEnrollments(pageable);
    }

    @GetMapping("/enrollments/{id}")
    public AcademyDto.EnrollmentResponse getEnrollment(@PathVariable UUID id) {
        return learning.getEnrollment(id);
    }

    @PostMapping("/enrollments/{id}/cancel")
    public AcademyDto.EnrollmentResponse cancel(@PathVariable UUID id) {
        return learning.cancel(id);
    }

    @PostMapping("/enrollments/{id}/complete-lesson")
    public AcademyDto.EnrollmentResponse completeLesson(@PathVariable UUID id,
                                                        @Valid @RequestBody AcademyDto.CompleteLessonRequest req) {
        return learning.completeLesson(id, req);
    }

    @PostMapping("/enrollments/{id}/submit-quiz")
    public AcademyDto.QuizResult submitQuiz(@PathVariable UUID id,
                                            @Valid @RequestBody AcademyDto.SubmitQuizRequest req) {
        return learning.submitQuiz(id, req);
    }

    // ===== Certificat (de l'apprenant courant) =====

    @GetMapping("/enrollments/{id}/certificate")
    public AcademyDto.CertificateResponse certificate(@PathVariable UUID id) {
        return certificates.getByEnrollment(id);
    }

    @GetMapping(value = "/enrollments/{id}/export/xapi", produces = "application/json")
    public String exportXapi(@PathVariable UUID id) {
        return exportService.exportXapi(id);
    }

    // ===== Leaderboard du tenant =====

    @GetMapping("/leaderboard")
    public AcademyDto.Leaderboard leaderboard(@RequestParam(defaultValue = "20") int size) {
        return leaderboard.leaderboard(size);
    }
}

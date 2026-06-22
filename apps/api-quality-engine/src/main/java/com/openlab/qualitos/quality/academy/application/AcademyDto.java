package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.quality.academy.domain.AcademyEnrollmentStatus;
import com.openlab.qualitos.quality.academy.domain.CourseStatus;
import com.openlab.qualitos.quality.academy.domain.LessonContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrats d'API de l'Academy LMS (§19.3).
 *
 * <p>Invariant de sécurité 18.2 : aucune requête ne porte {@code tenantId} ni
 * {@code userId} — ils sont résolus depuis le JWT côté service. Les réponses
 * destinées à l'apprenant ({@link QuizForLearner}) n'exposent JAMAIS l'index de
 * la bonne réponse.</p>
 */
public final class AcademyDto {

    private AcademyDto() {}

    // ===== Authoring : cours =====

    public record CreateCourseRequest(
            @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,99}$") String code,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @Size(max = 100) String targetRole,
            @Size(max = 100) String industrySector,
            @Min(0) @Max(100) Integer passingScore,
            @Min(0) @Max(10000) Integer pointsReward,
            @Min(1) @Max(120) Integer validityMonths
    ) {}

    public record UpdateCourseRequest(
            @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @Size(max = 100) String targetRole,
            @Size(max = 100) String industrySector,
            @Min(0) @Max(100) Integer passingScore,
            @Min(0) @Max(10000) Integer pointsReward,
            @Min(1) @Max(120) Integer validityMonths
    ) {}

    public record CourseResponse(
            UUID id, UUID tenantId, String code, String title, String description,
            String targetRole, String industrySector, int passingScore, int pointsReward,
            Integer validityMonths, CourseStatus status, UUID createdBy,
            Instant createdAt, Instant updatedAt
    ) {}

    // ===== Authoring : modules =====

    public record CreateModuleRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String summary,
            @Min(0) @NotNull Integer orderIndex
    ) {}

    public record ModuleResponse(
            UUID id, UUID courseId, String title, String summary, int orderIndex,
            Instant createdAt, Instant updatedAt
    ) {}

    // ===== Authoring : leçons =====

    public record CreateLessonRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull LessonContentType contentType,
            @Size(max = 200000) String body,
            @Size(max = 1000) String mediaUrl,
            @Min(0) @Max(100000) Integer durationMinutes,
            @Min(0) @NotNull Integer orderIndex
    ) {}

    public record LessonResponse(
            UUID id, UUID moduleId, String title, LessonContentType contentType,
            String body, String mediaUrl, int durationMinutes, int orderIndex,
            Instant createdAt, Instant updatedAt
    ) {}

    // ===== Authoring : quiz + questions =====

    public record CreateQuizRequest(
            @NotBlank @Size(max = 200) String title,
            @Min(0) @Max(100) Integer passScore,
            @Valid @NotNull List<CreateQuestionRequest> questions
    ) {}

    public record CreateQuestionRequest(
            @NotBlank @Size(max = 1000) String stem,
            @NotNull @Size(min = 2, max = 10) List<@NotBlank @Size(max = 500) String> options,
            @Min(0) @NotNull Integer correctIndex,
            @Min(1) @Max(100) Integer points,
            @Min(0) @NotNull Integer orderIndex
    ) {}

    public record QuizResponse(
            UUID id, UUID moduleId, String title, int passScore,
            List<QuestionResponse> questions, Instant createdAt, Instant updatedAt
    ) {}

    /** Vue auteur : inclut la bonne réponse (réservée aux rôles d'autoring). */
    public record QuestionResponse(
            UUID id, String stem, List<String> options, int correctIndex, int points, int orderIndex
    ) {}

    /** Vue apprenant : SANS la bonne réponse (anti-triche). */
    public record QuestionForLearner(
            UUID id, String stem, List<String> options, int points, int orderIndex
    ) {}

    public record QuizForLearner(
            UUID id, UUID moduleId, String title, int passScore, List<QuestionForLearner> questions
    ) {}

    // ===== Vue agrégée du cours (pour l'apprenant : modules + leçons + quiz sans réponses) =====

    public record CourseOutline(
            CourseResponse course, List<ModuleOutline> modules
    ) {}

    public record ModuleOutline(
            ModuleResponse module, List<LessonResponse> lessons, QuizForLearner quiz
    ) {}

    // ===== Runtime : inscriptions =====

    public record EnrollRequest(@NotNull UUID courseId) {}

    public record EnrollmentResponse(
            UUID id, UUID tenantId, UUID userId, UUID courseId,
            AcademyEnrollmentStatus status, int progressPct, Integer finalScore,
            Instant enrolledAt, Instant startedAt, Instant completedAt, LocalDate expiresOn,
            Instant createdAt, Instant updatedAt
    ) {}

    public record CompleteLessonRequest(@NotNull UUID lessonId) {}

    public record SubmitQuizRequest(
            @NotNull UUID quizId,
            @NotNull List<@NotNull @Min(0) Integer> answers
    ) {}

    public record QuizResult(
            UUID attemptId, UUID quizId, int score, boolean passed,
            int earnedPoints, int totalPoints, EnrollmentResponse enrollment
    ) {}

    // ===== Certificat =====

    public record CertificateResponse(
            UUID id, UUID enrollmentId, UUID courseId, String code,
            String courseCode, String courseTitle, int finalScore,
            String sha256, String anchorTxRef, Instant issuedAt, LocalDate expiresOn,
            String verifyUrl, String htmlContent
    ) {}

    /** Vérification publique d'un certificat par code QR (sans données personnelles internes). */
    public record CertificateVerification(
            String code, boolean valid, String courseCode, String courseTitle,
            int finalScore, Instant issuedAt, LocalDate expiresOn,
            String sha256, String anchorTxRef, boolean signatureValid
    ) {}

    // ===== Leaderboard (gamification §19.3) =====

    public record LeaderboardEntry(
            int rank, UUID userId, int points, int completedCount,
            Integer bestScore, String beltLevel, List<String> badges
    ) {}

    public record Leaderboard(List<LeaderboardEntry> entries, long totalLearners) {}
}

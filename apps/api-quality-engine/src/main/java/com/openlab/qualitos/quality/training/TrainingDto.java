package com.openlab.qualitos.quality.training;

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

public final class TrainingDto {

    private TrainingDto() {}

    // --- Skill ---

    public record CreateSkillRequest(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,99}$",
                    message = "code must be lowercase kebab/snake (max 100 chars)")
            String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description,
            @Size(max = 64) String category
    ) {}

    public record UpdateSkillRequest(
            @Size(max = 200) String name,
            @Size(max = 1000) String description,
            @Size(max = 64) String category
    ) {}

    public record SkillResponse(
            UUID id, UUID tenantId, String code, String name,
            String description, String category,
            Instant createdAt, Instant updatedAt
    ) {}

    // --- Competency assessment ---

    public record AssessCompetencyRequest(
            @NotNull UUID userId,
            @NotNull UUID skillId,
            @Min(0) @Max(4) @NotNull Integer level,
            @NotNull CompetencySource source,
            UUID assessedBy,
            LocalDate expiresOn
    ) {}

    public record CompetencyResponse(
            UUID id, UUID tenantId, UUID userId, UUID skillId,
            int level, CompetencyLevel levelName,
            CompetencySource source, UUID assessedBy,
            LocalDate assessedAt, LocalDate expiresOn,
            boolean expired,
            Instant createdAt, Instant updatedAt
    ) {}

    public record SkillGap(
            UUID skillId,
            String skillCode,
            int currentLevel,
            int targetLevel,
            int gap
    ) {}

    public record CompetencyMatrix(
            UUID userId,
            List<CompetencyResponse> competencies
    ) {}

    public record RoleGapAnalysis(
            UUID userId,
            UUID pathId,
            String pathCode,
            int totalRequirements,
            int satisfied,
            List<SkillGap> gaps
    ) {}

    // --- Training path ---

    public record CreatePathRequest(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,99}$") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @Size(max = 100) String targetRole,
            @Min(1) @Max(10000) @NotNull Integer durationHours,
            @Min(0) @Max(100) Integer passingScore,
            @Min(1) @Max(120) Integer validityMonths,
            @NotNull UUID createdBy
    ) {}

    public record UpdatePathRequest(
            @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @Size(max = 100) String targetRole,
            @Min(1) @Max(10000) Integer durationHours,
            @Min(0) @Max(100) Integer passingScore,
            @Min(1) @Max(120) Integer validityMonths
    ) {}

    public record PathResponse(
            UUID id, UUID tenantId, String code, String name,
            String description, String targetRole,
            int durationHours, int passingScore, Integer validityMonths,
            TrainingPathStatus status, UUID createdBy,
            Instant createdAt, Instant updatedAt
    ) {}

    public record AttachSkillRequirementRequest(
            @NotNull UUID skillId,
            @Min(0) @Max(4) @NotNull Integer targetLevel
    ) {}

    public record SkillRequirementResponse(
            UUID id, UUID pathId, UUID skillId, int targetLevel,
            Instant createdAt
    ) {}

    // --- Enrollment ---

    public record EnrollRequest(
            @NotNull UUID userId,
            @NotNull UUID pathId
    ) {}

    public record ProgressUpdateRequest(
            @Min(0) @Max(100) @NotNull Integer progressPct
    ) {}

    public record CompleteRequest(
            @Min(0) @Max(100) @NotNull Integer finalScore
    ) {}

    public record EnrollmentResponse(
            UUID id, UUID tenantId, UUID userId, UUID pathId,
            EnrollmentStatus status, int progressPct, Integer finalScore,
            LocalDate enrolledOn, LocalDate startedOn,
            LocalDate completedOn, LocalDate expiresOn,
            String certificateCode,
            Instant createdAt, Instant updatedAt
    ) {}

    public record CertificateVerification(
            String certificateCode,
            UUID enrollmentId,
            UUID tenantId,
            UUID userId,
            UUID pathId,
            String pathCode,
            String pathName,
            int finalScore,
            LocalDate completedOn,
            LocalDate expiresOn,
            boolean valid
    ) {}
}

package com.openlab.qualitos.quality.dmaic.resolution.web;

import com.openlab.qualitos.quality.dmaic.resolution.domain.ActionLine;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ActionStatus;
import com.openlab.qualitos.quality.dmaic.resolution.domain.AnswerType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic;
import com.openlab.qualitos.quality.dmaic.resolution.domain.FailureType;
import com.openlab.qualitos.quality.dmaic.resolution.domain.Person;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SeverityLevel;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetQuestion;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection;
import com.openlab.qualitos.quality.dmaic.resolution.domain.WhyBranch;
import com.openlab.qualitos.quality.dmaic.resolution.domain.WhyChain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les corps de requête de la fiche de résolution. Aucun ne porte de tenant :
 * il vient du jeton. Les bornes ici arrêtent un corps démesuré avant le
 * service ; les règles fines (réponse admise, code connu) sont au domaine.
 */
public final class ResolutionWebDto {

    private ResolutionWebDto() {}

    public record PersonRequest(@NotNull UUID accountId, @NotBlank @Size(max = Person.NAME_MAX) String name) {
        Person toDomain() {
            return new Person(accountId, name);
        }
    }

    public record WhyRequest(@NotNull WhyBranch branch, @Size(max = 500) String cause,
                             @Size(max = 5) List<@Size(max = 500) String> whys, boolean rootCause,
                             @Size(max = 500) String action) {
        WhyChain toDomain() {
            return new WhyChain(branch, cause, whys, rootCause, action);
        }
    }

    public record ActionRequest(LocalDate createdOn, @Size(max = 1000) String action, @Valid PersonRequest responsible,
                                @NotNull ActionStatus status, LocalDate plannedOn, LocalDate doneOn,
                                @Min(0) @Max(100) int percent, @Size(max = 1000) String notes, UUID capaId,
                                boolean containment) {
        ActionLine toDomain() {
            return new ActionLine(createdOn, action, responsible == null ? null : responsible.toDomain(), status,
                    plannedOn, doneOn, percent, notes, capaId, containment);
        }
    }

    public record SheetRequest(@Size(max = 200) String clientName, @Size(max = 60) String claimNumber,
                               @Valid PersonRequest problemOwner,
                               @Size(max = 15) List<@Valid @NotNull PersonRequest> team,
                               LocalDate openedOn, LocalDate closedOn, @Min(1) @Max(10) Integer severity,
                               @Size(max = 20) String failureTypeCode, @Size(max = 4000) String customerSymptoms,
                               @Size(max = 150) Map<@Size(max = 20) String, @Size(max = 2000) String> answers,
                               @Size(max = 4000) String measureSummary, @Size(max = 4000) String containmentMeasures,
                               @Size(max = 4000) String rootCause, @Size(max = 4000) String retainedSolutions,
                               @Size(max = 4000) String surveillancePlan,
                               @Size(max = 30) List<@Valid @NotNull WhyRequest> whys,
                               @Size(max = 100) List<@Valid @NotNull ActionRequest> actions,
                               UUID ishikawaId, boolean measuresEnabled, boolean capabilityStudy,
                               boolean gageRrStudy) {
        ResolutionContent toDomain() {
            return new ResolutionContent(clientName, claimNumber,
                    problemOwner == null ? null : problemOwner.toDomain(),
                    team == null ? List.of() : team.stream().map(PersonRequest::toDomain).toList(),
                    openedOn, closedOn, severity, failureTypeCode, customerSymptoms, answers, measureSummary,
                    containmentMeasures, rootCause, retainedSolutions, surveillancePlan,
                    whys == null ? List.of() : whys.stream().map(WhyRequest::toDomain).toList(),
                    actions == null ? List.of() : actions.stream().map(ActionRequest::toDomain).toList(),
                    ishikawaId, measuresEnabled, capabilityStudy, gageRrStudy);
        }
    }

    public record RemarkRequest(@Size(max = 1000) String remark) {
    }

    public record QuestionRequest(@NotBlank @Size(max = 20) String code, @NotNull SheetSection section,
                                  DefineTopic topic, @NotBlank @Size(max = 500) String label,
                                  @NotNull AnswerType answerType) {
        SheetQuestion toDomain() {
            return new SheetQuestion(code, section, topic, label, answerType);
        }
    }

    public record FailureTypeRequest(@NotBlank @Size(max = 20) String code, @NotBlank @Size(max = 200) String label) {
        FailureType toDomain() {
            return new FailureType(code, label);
        }
    }

    public record SeverityRequest(@Min(1) @Max(10) int level, @NotBlank @Size(max = 80) String label) {
        SeverityLevel toDomain() {
            return new SeverityLevel(level, label);
        }
    }

    public record SettingsRequest(@NotNull @Size(min = 1, max = 150) List<@Valid @NotNull QuestionRequest> questions,
                                  @NotNull @Size(max = 300) List<@Valid @NotNull FailureTypeRequest> failureTypes,
                                  @NotNull @Size(min = 1, max = 10) List<@Valid @NotNull SeverityRequest> severities) {
    }
}

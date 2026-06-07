package com.openlab.qualitos.quality.common;

import com.openlab.qualitos.quality.audit.AuditChecklistItemNotFoundException;
import com.openlab.qualitos.quality.audit.AuditFindingNotFoundException;
import com.openlab.qualitos.quality.audit.AuditPlanNotFoundException;
import com.openlab.qualitos.quality.audit.AuditStateException;
import com.openlab.qualitos.quality.capa.CapaActionNotFoundException;
import com.openlab.qualitos.quality.capa.CapaNotFoundException;
import com.openlab.qualitos.quality.capa.CapaStateException;
import com.openlab.qualitos.quality.circle.CircleMeetingNotFoundException;
import com.openlab.qualitos.quality.dmaic.DmaicProjectNotFoundException;
import com.openlab.qualitos.quality.dmaic.DmaicStateException;
import com.openlab.qualitos.quality.dmaic.PokaYokeAssignmentNotFoundException;
import com.openlab.qualitos.quality.dmaic.PokaYokeDeviceNotFoundException;
import com.openlab.qualitos.quality.dmaic.ProcessMeasureNotFoundException;
import com.openlab.qualitos.quality.circle.CircleMemberNotFoundException;
import com.openlab.qualitos.quality.circle.CircleNotFoundException;
import com.openlab.qualitos.quality.circle.CircleProposalNotFoundException;
import com.openlab.qualitos.quality.circle.CircleStateException;
import com.openlab.qualitos.quality.docs.DocumentCodeConflictException;
import com.openlab.qualitos.quality.docs.DocumentNotFoundException;
import com.openlab.qualitos.quality.docs.DocumentStateException;
import com.openlab.qualitos.quality.docs.DocumentVersionNotFoundException;
import com.openlab.qualitos.quality.fives.FiveSAuditNotFoundException;
import com.openlab.qualitos.quality.fives.FiveSStateException;
import com.openlab.qualitos.quality.ishikawa.IshikawaCauseNotFoundException;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagramNotFoundException;
import com.openlab.qualitos.quality.ishikawa.IshikawaStateException;
import com.openlab.qualitos.quality.standards.AdoptionConflictException;
import com.openlab.qualitos.quality.standards.AdoptionStateException;
import com.openlab.qualitos.quality.standards.EvidenceNotFoundException;
import com.openlab.qualitos.quality.standards.RequirementNotFoundException;
import com.openlab.qualitos.quality.standards.RoadmapStageNotFoundException;
import com.openlab.qualitos.quality.standards.DocumentTemplateNotFoundException;
import com.openlab.qualitos.quality.ai.guard.AiCircuitOpenException;
import com.openlab.qualitos.quality.ai.guard.AiPromptTooLargeException;
import com.openlab.qualitos.quality.ai.guard.AiQuotaExceededException;
import com.openlab.qualitos.quality.ai.guard.AiRateLimitExceededException;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.TenantStandardNotFoundException;
import com.openlab.qualitos.quality.industry.IndustryPackNotFoundException;
import com.openlab.qualitos.quality.iot.IotDeviceNotFoundException;
import com.openlab.qualitos.quality.iot.IotDeviceStateException;
import com.openlab.qualitos.quality.iot.IotThresholdNotFoundException;
import com.openlab.qualitos.quality.risk.FmeaItemNotFoundException;
import com.openlab.qualitos.quality.risk.FmeaProjectNotFoundException;
import com.openlab.qualitos.quality.risk.FmeaStateException;
import com.openlab.qualitos.quality.supplier.SupplierChildNotFoundException;
import com.openlab.qualitos.quality.supplier.SupplierNotFoundException;
import com.openlab.qualitos.quality.supplier.SupplierStateException;
import com.openlab.qualitos.quality.training.EnrollmentNotFoundException;
import com.openlab.qualitos.quality.training.SkillNotFoundException;
import com.openlab.qualitos.quality.training.TrainingPathNotFoundException;
import com.openlab.qualitos.quality.training.TrainingStateException;
import com.openlab.qualitos.quality.change.ChangeChildNotFoundException;
import com.openlab.qualitos.quality.change.ChangeRequestNotFoundException;
import com.openlab.qualitos.quality.change.ChangeStateException;
import com.openlab.qualitos.quality.complaints.ComplaintNotFoundException;
import com.openlab.qualitos.quality.complaints.ComplaintResponseNotFoundException;
import com.openlab.qualitos.quality.complaints.ComplaintStateException;
import com.openlab.qualitos.quality.calibration.CalibrationChildNotFoundException;
import com.openlab.qualitos.quality.calibration.CalibrationEquipmentNotFoundException;
import com.openlab.qualitos.quality.calibration.CalibrationStateException;
import com.openlab.qualitos.quality.kpi.KpiMeasurementNotFoundException;
import com.openlab.qualitos.quality.kpi.KpiNotFoundException;
import com.openlab.qualitos.quality.kpi.KpiStateException;
import com.openlab.qualitos.quality.auditlog.AuditEventNotFoundException;
import com.openlab.qualitos.quality.auditlog.AuditEventStateException;
import com.openlab.qualitos.quality.ehs.domain.IncidentNotFoundException;
import com.openlab.qualitos.quality.ehs.domain.IncidentStateException;
import com.openlab.qualitos.quality.breach.domain.BreachNotFoundException;
import com.openlab.qualitos.quality.breach.domain.BreachStateException;
import com.openlab.qualitos.quality.consent.domain.ConsentNotFoundException;
import com.openlab.qualitos.quality.consent.domain.ConsentStateException;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleNotFoundException;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStateException;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityNotFoundException;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStateException;
import com.openlab.qualitos.quality.dpia.domain.DpiaNotFoundException;
import com.openlab.qualitos.quality.dpia.domain.DpiaStateException;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementNotFoundException;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStateException;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeNotFoundException;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStateException;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionNotFoundException;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStateException;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentNotFoundException;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStateException;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferNotFoundException;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStateException;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentNotFoundException;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStateException;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureNotFoundException;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStateException;
import com.openlab.qualitos.quality.aiact.domain.AiSystemNotFoundException;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStateException;
import com.openlab.qualitos.quality.aiactfria.domain.FriaNotFoundException;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStateException;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentNotFoundException;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStateException;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanNotFoundException;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStateException;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsNotFoundException;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStateException;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentNotFoundException;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStateException;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationNotFoundException;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStateException;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestNotFoundException;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStateException;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationNotFoundException;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationStateException;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyNotFoundException;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStateException;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyException;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyNotFoundException;
import com.openlab.qualitos.quality.itsm.ItsmConnectionNotFoundException;
import com.openlab.qualitos.quality.itsm.ItsmSyncException;
import com.openlab.qualitos.quality.webhooks.WebhookDeliveryNotFoundException;
import com.openlab.qualitos.quality.webhooks.WebhookStateException;
import com.openlab.qualitos.quality.webhooks.WebhookSubscriptionNotFoundException;
import com.openlab.qualitos.quality.pdca.PdcaCycleNotFoundException;
import com.openlab.qualitos.quality.pdca.PdcaStepNotFoundException;
import com.openlab.qualitos.quality.pdca.PdcaStateException;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutNotFoundException;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutStateException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackNotFoundException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStateException;
import com.openlab.qualitos.quality.notifications.domain.NotificationNotFoundException;
import com.openlab.qualitos.quality.nonconformity.NcNotFoundException;
import com.openlab.qualitos.quality.nonconformity.NcStateException;
import com.openlab.qualitos.quality.nonconformity.NcPhotoNotFoundException;
import com.openlab.qualitos.quality.nonconformity.NcPhotoTooLargeException;
import com.openlab.qualitos.quality.nonconformity.NcPhotoValidationException;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MissingTenantContextException.class)
    public ProblemDetail handleMissingTenant(MissingTenantContextException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/missing-tenant"));
        problem.setTitle("Missing Tenant Context");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PdcaCycleNotFoundException.class)
    public ProblemDetail handleCycleNotFound(PdcaCycleNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/pdca-cycle-not-found"));
        problem.setTitle("PDCA Cycle Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PdcaStepNotFoundException.class)
    public ProblemDetail handleStepNotFound(PdcaStepNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/pdca-step-not-found"));
        problem.setTitle("PDCA Step Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PdcaStateException.class)
    public ProblemDetail handlePdcaState(PdcaStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/pdca-invalid-transition"));
        problem.setTitle("Invalid PDCA State Transition");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IshikawaDiagramNotFoundException.class)
    public ProblemDetail handleDiagramNotFound(IshikawaDiagramNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ishikawa-diagram-not-found"));
        problem.setTitle("Ishikawa Diagram Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IshikawaCauseNotFoundException.class)
    public ProblemDetail handleCauseNotFound(IshikawaCauseNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ishikawa-cause-not-found"));
        problem.setTitle("Ishikawa Cause Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IshikawaStateException.class)
    public ProblemDetail handleIshikawaState(IshikawaStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ishikawa-invalid-state"));
        problem.setTitle("Invalid Ishikawa State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FiveSAuditNotFoundException.class)
    public ProblemDetail handleFiveSNotFound(FiveSAuditNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/fives-audit-not-found"));
        problem.setTitle("5S Audit Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FiveSStateException.class)
    public ProblemDetail handleFiveSState(FiveSStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/fives-invalid-state"));
        problem.setTitle("Invalid 5S Audit State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CapaNotFoundException.class)
    public ProblemDetail handleCapaNotFound(CapaNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/capa-not-found"));
        problem.setTitle("CAPA Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CapaActionNotFoundException.class)
    public ProblemDetail handleCapaActionNotFound(CapaActionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/capa-action-not-found"));
        problem.setTitle("CAPA Action Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CapaStateException.class)
    public ProblemDetail handleCapaState(CapaStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/capa-invalid-state"));
        problem.setTitle("Invalid CAPA State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NcNotFoundException.class)
    public ProblemDetail handleNcNotFound(NcNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/non-conformity-not-found"));
        problem.setTitle("Non-Conformity Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NcStateException.class)
    public ProblemDetail handleNcState(NcStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/non-conformity-invalid-state"));
        problem.setTitle("Invalid Non-Conformity State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NcPhotoNotFoundException.class)
    public ProblemDetail handleNcPhotoNotFound(NcPhotoNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/non-conformity-photo-not-found"));
        problem.setTitle("Non-Conformity Photo Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NcPhotoValidationException.class)
    public ProblemDetail handleNcPhotoValidation(NcPhotoValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/non-conformity-photo-invalid"));
        problem.setTitle("Invalid Non-Conformity Photo");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NcPhotoTooLargeException.class)
    public ProblemDetail handleNcPhotoTooLarge(NcPhotoTooLargeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/non-conformity-photo-too-large"));
        problem.setTitle("Non-Conformity Photo Too Large");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUpload(MaxUploadSizeExceededException ex) {
        // Limite multipart Spring dépassée (1er rempart, avant lecture des octets).
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the maximum allowed size");
        problem.setType(URI.create("https://qualitos.io/errors/non-conformity-photo-too-large"));
        problem.setTitle("Non-Conformity Photo Too Large");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StorageDisabledException.class)
    public ProblemDetail handleStorageDisabled(StorageDisabledException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/storage-disabled"));
        problem.setTitle("Binary Storage Disabled");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ProblemDetail handleDocNotFound(DocumentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/document-not-found"));
        problem.setTitle("Document Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DocumentVersionNotFoundException.class)
    public ProblemDetail handleVersionNotFound(DocumentVersionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/document-version-not-found"));
        problem.setTitle("Document Version Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DocumentStateException.class)
    public ProblemDetail handleDocState(DocumentStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/document-invalid-state"));
        problem.setTitle("Invalid Document State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DocumentCodeConflictException.class)
    public ProblemDetail handleDocCodeConflict(DocumentCodeConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/document-code-conflict"));
        problem.setTitle("Document Code Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AuditPlanNotFoundException.class)
    public ProblemDetail handleAuditPlanNotFound(AuditPlanNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/audit-plan-not-found"));
        problem.setTitle("Audit Plan Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AuditChecklistItemNotFoundException.class)
    public ProblemDetail handleAuditChecklistNotFound(AuditChecklistItemNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/audit-checklist-item-not-found"));
        problem.setTitle("Audit Checklist Item Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AuditFindingNotFoundException.class)
    public ProblemDetail handleAuditFindingNotFound(AuditFindingNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/audit-finding-not-found"));
        problem.setTitle("Audit Finding Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AuditStateException.class)
    public ProblemDetail handleAuditState(AuditStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/audit-invalid-state"));
        problem.setTitle("Invalid Audit State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StandardNotFoundException.class)
    public ProblemDetail handleStandardNotFound(StandardNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-not-found"));
        problem.setTitle("Standard Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TenantStandardNotFoundException.class)
    public ProblemDetail handleTenantStandardNotFound(TenantStandardNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/tenant-standard-not-found"));
        problem.setTitle("Tenant Standard Adoption Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RequirementNotFoundException.class)
    public ProblemDetail handleRequirementNotFound(RequirementNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-requirement-not-found"));
        problem.setTitle("Standard Requirement Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EvidenceNotFoundException.class)
    public ProblemDetail handleEvidenceNotFound(EvidenceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/requirement-evidence-not-found"));
        problem.setTitle("Requirement Evidence Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RoadmapStageNotFoundException.class)
    public ProblemDetail handleRoadmapStageNotFound(RoadmapStageNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/certification-roadmap-stage-not-found"));
        problem.setTitle("Certification Roadmap Stage Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DocumentTemplateNotFoundException.class)
    public ProblemDetail handleDocumentTemplateNotFound(DocumentTemplateNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-document-template-not-found"));
        problem.setTitle("Standard Document Template Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiGatewayException.class)
    public ProblemDetail handleAiGateway(AiGatewayException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-gateway-unavailable"));
        problem.setTitle("AI Gateway Unavailable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // --- Garde-fous IA (OWASP LLM04 — Model Denial of Service) ---

    @ExceptionHandler(AiPromptTooLargeException.class)
    public ProblemDetail handleAiPromptTooLarge(AiPromptTooLargeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-prompt-too-large"));
        problem.setTitle("AI Prompt Too Large");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiRateLimitExceededException.class)
    public ProblemDetail handleAiRateLimit(AiRateLimitExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-rate-limit-exceeded"));
        problem.setTitle("AI Rate Limit Exceeded");
        problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiQuotaExceededException.class)
    public ProblemDetail handleAiQuota(AiQuotaExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-quota-exceeded"));
        problem.setTitle("AI Quota Exceeded");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiCircuitOpenException.class)
    public ProblemDetail handleAiCircuitOpen(AiCircuitOpenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-circuit-open"));
        problem.setTitle("AI Service Unavailable");
        problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AdoptionConflictException.class)
    public ProblemDetail handleAdoptionConflict(AdoptionConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/adoption-conflict"));
        problem.setTitle("Adoption Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AdoptionStateException.class)
    public ProblemDetail handleAdoptionState(AdoptionStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/adoption-invalid-state"));
        problem.setTitle("Invalid Adoption State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CircleNotFoundException.class)
    public ProblemDetail handleCircleNotFound(CircleNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/circle-not-found"));
        problem.setTitle("Quality Circle Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CircleMemberNotFoundException.class)
    public ProblemDetail handleCircleMemberNotFound(CircleMemberNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/circle-member-not-found"));
        problem.setTitle("Circle Member Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CircleMeetingNotFoundException.class)
    public ProblemDetail handleCircleMeetingNotFound(CircleMeetingNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/circle-meeting-not-found"));
        problem.setTitle("Circle Meeting Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CircleProposalNotFoundException.class)
    public ProblemDetail handleCircleProposalNotFound(CircleProposalNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/circle-proposal-not-found"));
        problem.setTitle("Circle Proposal Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CircleStateException.class)
    public ProblemDetail handleCircleState(CircleStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/circle-invalid-state"));
        problem.setTitle("Invalid Quality Circle State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DmaicProjectNotFoundException.class)
    public ProblemDetail handleDmaicProjectNotFound(DmaicProjectNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/dmaic-project-not-found"));
        problem.setTitle("DMAIC Project Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ProcessMeasureNotFoundException.class)
    public ProblemDetail handleProcessMeasureNotFound(ProcessMeasureNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/process-measure-not-found"));
        problem.setTitle("Process Measure Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PokaYokeDeviceNotFoundException.class)
    public ProblemDetail handlePokaYokeDeviceNotFound(PokaYokeDeviceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/pokayoke-device-not-found"));
        problem.setTitle("Poka-Yoke Device Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PokaYokeAssignmentNotFoundException.class)
    public ProblemDetail handlePokaYokeAssignmentNotFound(PokaYokeAssignmentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/pokayoke-assignment-not-found"));
        problem.setTitle("Poka-Yoke Assignment Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DmaicStateException.class)
    public ProblemDetail handleDmaicState(DmaicStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/dmaic-invalid-state"));
        problem.setTitle("Invalid DMAIC State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(WebhookSubscriptionNotFoundException.class)
    public ProblemDetail handleWebhookSubscriptionNotFound(WebhookSubscriptionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/webhook-subscription-not-found"));
        problem.setTitle("Webhook Subscription Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(WebhookDeliveryNotFoundException.class)
    public ProblemDetail handleWebhookDeliveryNotFound(WebhookDeliveryNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/webhook-delivery-not-found"));
        problem.setTitle("Webhook Delivery Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(WebhookStateException.class)
    public ProblemDetail handleWebhookState(WebhookStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/webhook-invalid-state"));
        problem.setTitle("Invalid Webhook State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RateLimitPolicyNotFoundException.class)
    public ProblemDetail handleRateLimitNotFound(RateLimitPolicyNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/rate-limit-policy-not-found"));
        problem.setTitle("Rate Limit Policy Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RateLimitPolicyException.class)
    public ProblemDetail handleRateLimitPolicy(RateLimitPolicyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/rate-limit-policy-invalid"));
        problem.setTitle("Invalid Rate Limit Policy");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ProblemDetail handleApiKeyNotFound(ApiKeyNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/api-key-not-found"));
        problem.setTitle("API Key Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ApiKeyStateException.class)
    public ProblemDetail handleApiKeyState(ApiKeyStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/api-key-invalid-state"));
        problem.setTitle("Invalid API Key State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ModuleActivationNotFoundException.class)
    public ProblemDetail handleModuleActivationNotFound(ModuleActivationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/tenant-module-not-found"));
        problem.setTitle("Module Activation Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ModuleActivationStateException.class)
    public ProblemDetail handleModuleActivationState(ModuleActivationStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/tenant-module-invalid-state"));
        problem.setTitle("Invalid Module Activation State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    public ProblemDetail handleIncidentNotFound(IncidentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ehs-incident-not-found"));
        problem.setTitle("EHS Incident Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IncidentStateException.class)
    public ProblemDetail handleIncidentState(IncidentStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ehs-incident-invalid-state"));
        problem.setTitle("Invalid EHS Incident State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AuditEventNotFoundException.class)
    public ProblemDetail handleAuditEventNotFound(AuditEventNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/audit-event-not-found"));
        problem.setTitle("Audit Event Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AuditEventStateException.class)
    public ProblemDetail handleAuditEventState(AuditEventStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/audit-event-invalid-state"));
        problem.setTitle("Invalid Audit Event State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(KpiNotFoundException.class)
    public ProblemDetail handleKpiNotFound(KpiNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/kpi-not-found"));
        problem.setTitle("KPI Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(KpiMeasurementNotFoundException.class)
    public ProblemDetail handleKpiMeasurementNotFound(KpiMeasurementNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/kpi-measurement-not-found"));
        problem.setTitle("KPI Measurement Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(KpiStateException.class)
    public ProblemDetail handleKpiState(KpiStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/kpi-invalid-state"));
        problem.setTitle("Invalid KPI State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CalibrationEquipmentNotFoundException.class)
    public ProblemDetail handleCalibrationEquipmentNotFound(CalibrationEquipmentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/calibration-equipment-not-found"));
        problem.setTitle("Calibration Equipment Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CalibrationChildNotFoundException.class)
    public ProblemDetail handleCalibrationChildNotFound(CalibrationChildNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/calibration-child-not-found"));
        problem.setTitle("Calibration Sub-Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CalibrationStateException.class)
    public ProblemDetail handleCalibrationState(CalibrationStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/calibration-invalid-state"));
        problem.setTitle("Invalid Calibration State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ComplaintNotFoundException.class)
    public ProblemDetail handleComplaintNotFound(ComplaintNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/complaint-not-found"));
        problem.setTitle("Complaint Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ComplaintResponseNotFoundException.class)
    public ProblemDetail handleComplaintResponseNotFound(ComplaintResponseNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/complaint-response-not-found"));
        problem.setTitle("Complaint Response Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ComplaintStateException.class)
    public ProblemDetail handleComplaintState(ComplaintStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/complaint-invalid-state"));
        problem.setTitle("Invalid Complaint State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ChangeRequestNotFoundException.class)
    public ProblemDetail handleChangeRequestNotFound(ChangeRequestNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/change-request-not-found"));
        problem.setTitle("Change Request Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ChangeChildNotFoundException.class)
    public ProblemDetail handleChangeChildNotFound(ChangeChildNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/change-child-not-found"));
        problem.setTitle("Change Sub-Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ChangeStateException.class)
    public ProblemDetail handleChangeState(ChangeStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/change-invalid-state"));
        problem.setTitle("Invalid Change Request State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SkillNotFoundException.class)
    public ProblemDetail handleSkillNotFound(SkillNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/training-skill-not-found"));
        problem.setTitle("Skill Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TrainingPathNotFoundException.class)
    public ProblemDetail handleTrainingPathNotFound(TrainingPathNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/training-path-not-found"));
        problem.setTitle("Training Path Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EnrollmentNotFoundException.class)
    public ProblemDetail handleEnrollmentNotFound(EnrollmentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/training-enrollment-not-found"));
        problem.setTitle("Training Enrollment Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TrainingStateException.class)
    public ProblemDetail handleTrainingState(TrainingStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/training-invalid-state"));
        problem.setTitle("Invalid Training State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ProblemDetail handleSupplierNotFound(SupplierNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/supplier-not-found"));
        problem.setTitle("Supplier Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SupplierChildNotFoundException.class)
    public ProblemDetail handleSupplierChildNotFound(SupplierChildNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/supplier-child-not-found"));
        problem.setTitle("Supplier Sub-Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SupplierStateException.class)
    public ProblemDetail handleSupplierState(SupplierStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/supplier-invalid-state"));
        problem.setTitle("Invalid Supplier State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FmeaProjectNotFoundException.class)
    public ProblemDetail handleFmeaProjectNotFound(FmeaProjectNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/fmea-project-not-found"));
        problem.setTitle("FMEA Project Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FmeaItemNotFoundException.class)
    public ProblemDetail handleFmeaItemNotFound(FmeaItemNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/fmea-item-not-found"));
        problem.setTitle("FMEA Item Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FmeaStateException.class)
    public ProblemDetail handleFmeaState(FmeaStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/fmea-invalid-state"));
        problem.setTitle("Invalid FMEA State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IotDeviceNotFoundException.class)
    public ProblemDetail handleIotDeviceNotFound(IotDeviceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/iot-device-not-found"));
        problem.setTitle("IoT Device Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IotDeviceStateException.class)
    public ProblemDetail handleIotDeviceState(IotDeviceStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/iot-device-invalid-state"));
        problem.setTitle("Invalid IoT Device State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IotThresholdNotFoundException.class)
    public ProblemDetail handleIotThresholdNotFound(IotThresholdNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/iot-threshold-not-found"));
        problem.setTitle("IoT Threshold Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IndustryPackNotFoundException.class)
    public ProblemDetail handleIndustryPackNotFound(IndustryPackNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/industry-pack-not-found"));
        problem.setTitle("Industry Pack Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ItsmConnectionNotFoundException.class)
    public ProblemDetail handleItsmNotFound(ItsmConnectionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/itsm-connection-not-found"));
        problem.setTitle("ITSM Connection Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ItsmSyncException.class)
    public ProblemDetail handleItsmSync(ItsmSyncException ex) {
        // 502 = Bad Gateway : appel sortant vers ITSM en erreur.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/itsm-sync-failed"));
        problem.setTitle("ITSM Sync Failed");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiSystemNotFoundException.class)
    public ProblemDetail handleAiSystemNotFound(AiSystemNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-system-not-found"));
        problem.setTitle("AI Act System Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiSystemStateException.class)
    public ProblemDetail handleAiSystemState(AiSystemStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-system-invalid-state"));
        problem.setTitle("Invalid AI Act System State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FriaNotFoundException.class)
    public ProblemDetail handleFriaNotFound(FriaNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-fria-not-found"));
        problem.setTitle("AI Act FRIA Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FriaStateException.class)
    public ProblemDetail handleFriaState(FriaStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-fria-invalid-state"));
        problem.setTitle("Invalid AI Act FRIA State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiIncidentNotFoundException.class)
    public ProblemDetail handleAiIncidentNotFound(AiIncidentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-incident-not-found"));
        problem.setTitle("AI Act Incident Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiIncidentStateException.class)
    public ProblemDetail handleAiIncidentState(AiIncidentStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-incident-invalid-state"));
        problem.setTitle("Invalid AI Act Incident State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PmmPlanNotFoundException.class)
    public ProblemDetail handlePmmPlanNotFound(PmmPlanNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-pmm-plan-not-found"));
        problem.setTitle("AI Act PMM Plan Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PmmPlanStateException.class)
    public ProblemDetail handlePmmPlanState(PmmPlanStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-pmm-plan-invalid-state"));
        problem.setTitle("Invalid AI Act PMM Plan State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiQmsNotFoundException.class)
    public ProblemDetail handleAiQmsNotFound(AiQmsNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-qms-not-found"));
        problem.setTitle("AI Act QMS Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiQmsStateException.class)
    public ProblemDetail handleAiQmsState(AiQmsStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-qms-invalid-state"));
        problem.setTitle("Invalid AI Act QMS State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ConformityAssessmentNotFoundException.class)
    public ProblemDetail handleConformityNotFound(ConformityAssessmentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-conformity-not-found"));
        problem.setTitle("AI Act Conformity Assessment Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ConformityAssessmentStateException.class)
    public ProblemDetail handleConformityState(ConformityAssessmentStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-conformity-invalid-state"));
        problem.setTitle("Invalid AI Act Conformity Assessment State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EudbRegistrationNotFoundException.class)
    public ProblemDetail handleEudbNotFound(EudbRegistrationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-eudb-not-found"));
        problem.setTitle("AI Act EUDB Registration Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EudbRegistrationStateException.class)
    public ProblemDetail handleEudbState(EudbRegistrationStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-act-eudb-invalid-state"));
        problem.setTitle("Invalid AI Act EUDB Registration State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Nis2MeasureNotFoundException.class)
    public ProblemDetail handleNis2MeasureNotFound(Nis2MeasureNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/nis2-measure-not-found"));
        problem.setTitle("NIS2 Measure Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Nis2MeasureStateException.class)
    public ProblemDetail handleNis2MeasureState(Nis2MeasureStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/nis2-measure-invalid-state"));
        problem.setTitle("Invalid NIS2 Measure State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CyberIncidentNotFoundException.class)
    public ProblemDetail handleCyberNotFound(CyberIncidentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/nis2-cyber-incident-not-found"));
        problem.setTitle("NIS2 Cyber Incident Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CyberIncidentStateException.class)
    public ProblemDetail handleCyberState(CyberIncidentStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/nis2-cyber-incident-invalid-state"));
        problem.setTitle("Invalid NIS2 Cyber Incident State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CrossBorderTransferNotFoundException.class)
    public ProblemDetail handleCbtNotFound(CrossBorderTransferNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-cross-border-transfer-not-found"));
        problem.setTitle("GDPR Cross-Border Transfer Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CrossBorderTransferStateException.class)
    public ProblemDetail handleCbtState(CrossBorderTransferStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-cross-border-transfer-invalid-state"));
        problem.setTitle("Invalid GDPR Cross-Border Transfer State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DpoAppointmentNotFoundException.class)
    public ProblemDetail handleDpoNotFound(DpoAppointmentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-dpo-appointment-not-found"));
        problem.setTitle("GDPR DPO Appointment Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DpoAppointmentStateException.class)
    public ProblemDetail handleDpoState(DpoAppointmentStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-dpo-appointment-invalid-state"));
        problem.setTitle("Invalid GDPR DPO Appointment State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AutomatedDecisionNotFoundException.class)
    public ProblemDetail handleAdmNotFound(AutomatedDecisionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-automated-decision-not-found"));
        problem.setTitle("GDPR Automated Decision Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AutomatedDecisionStateException.class)
    public ProblemDetail handleAdmState(AutomatedDecisionStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-automated-decision-invalid-state"));
        problem.setTitle("Invalid GDPR Automated Decision State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PrivacyNoticeNotFoundException.class)
    public ProblemDetail handlePrivacyNoticeNotFound(PrivacyNoticeNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-privacy-notice-not-found"));
        problem.setTitle("GDPR Privacy Notice Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PrivacyNoticeStateException.class)
    public ProblemDetail handlePrivacyNoticeState(PrivacyNoticeStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-privacy-notice-invalid-state"));
        problem.setTitle("Invalid GDPR Privacy Notice State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ProcessorAgreementNotFoundException.class)
    public ProblemDetail handleDpaNotFound(ProcessorAgreementNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-processor-agreement-not-found"));
        problem.setTitle("GDPR Processor Agreement Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ProcessorAgreementStateException.class)
    public ProblemDetail handleDpaState(ProcessorAgreementStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-processor-agreement-invalid-state"));
        problem.setTitle("Invalid GDPR Processor Agreement State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DpiaNotFoundException.class)
    public ProblemDetail handleDpiaNotFound(DpiaNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-dpia-not-found"));
        problem.setTitle("GDPR DPIA Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DpiaStateException.class)
    public ProblemDetail handleDpiaState(DpiaStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-dpia-invalid-state"));
        problem.setTitle("Invalid GDPR DPIA State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ProcessingActivityNotFoundException.class)
    public ProblemDetail handleRopaNotFound(ProcessingActivityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-ropa-not-found"));
        problem.setTitle("GDPR Processing Activity Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ProcessingActivityStateException.class)
    public ProblemDetail handleRopaState(ProcessingActivityStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-ropa-invalid-state"));
        problem.setTitle("Invalid GDPR Processing Activity State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BreachNotFoundException.class)
    public ProblemDetail handleBreachNotFound(BreachNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-breach-not-found"));
        problem.setTitle("GDPR Breach Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BreachStateException.class)
    public ProblemDetail handleBreachState(BreachStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-breach-invalid-state"));
        problem.setTitle("Invalid GDPR Breach State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RetentionRuleNotFoundException.class)
    public ProblemDetail handleRetentionNotFound(RetentionRuleNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-retention-rule-not-found"));
        problem.setTitle("GDPR Retention Rule Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RetentionRuleStateException.class)
    public ProblemDetail handleRetentionState(RetentionRuleStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-retention-rule-invalid-state"));
        problem.setTitle("Invalid GDPR Retention Rule State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ConsentNotFoundException.class)
    public ProblemDetail handleConsentNotFound(ConsentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-consent-not-found"));
        problem.setTitle("GDPR Consent Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ConsentStateException.class)
    public ProblemDetail handleConsentState(ConsentStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-consent-invalid-state"));
        problem.setTitle("Invalid GDPR Consent State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SubjectRequestNotFoundException.class)
    public ProblemDetail handleSubjectRequestNotFound(SubjectRequestNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-subject-request-not-found"));
        problem.setTitle("GDPR Subject Request Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SubjectRequestStateException.class)
    public ProblemDetail handleSubjectRequestState(SubjectRequestStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/gdpr-subject-request-invalid-state"));
        problem.setTitle("Invalid GDPR Subject Request State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        // Validation des @RequestParam / @PathVariable via @Min/@Max/@Pattern :
        // Spring lève ConstraintViolationException qui n'a pas de handler natif.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/validation-failed"));
        problem.setTitle("Constraint Violation");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex) {
        // Malformed JSON or unparseable request body (e.g. enum value not accepted).
        // Renvoyer 400 Bad Request, conformément RFC 7807 §3.1.
        Throwable root = ex.getMostSpecificCause();
        String detail = root != null ? root.getMessage() : "Malformed request body";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://qualitos.io/errors/malformed-request"));
        problem.setTitle("Malformed Request Body");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Binding @RequestParam/@PathVariable error (e.g. enum value not accepted).
        String name = ex.getName();
        String value = ex.getValue() == null ? "null" : ex.getValue().toString();
        String detail = "Invalid value '" + value + "' for parameter '" + name + "'";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://qualitos.io/errors/parameter-type-mismatch"));
        problem.setTitle("Parameter Type Mismatch");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(MissingServletRequestPartException ex) {
        // Part multipart requise absente (ex. champ 'file' manquant) → 400 (et non 500).
        String detail = "Missing required multipart part '" + ex.getRequestPartName() + "'";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://qualitos.io/errors/missing-multipart-part"));
        problem.setTitle("Missing Required Multipart Part");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        // Paramètre @RequestParam requis absent → 400 Bad Request (et non 500).
        String detail = "Missing required parameter '" + ex.getParameterName() + "'";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://qualitos.io/errors/missing-parameter"));
        problem.setTitle("Missing Required Parameter");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create("https://qualitos.io/errors/validation-failed"));
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // ----- P5: dashboards & marketplace -----

    @ExceptionHandler(DashboardLayoutNotFoundException.class)
    public ProblemDetail handleDashboardNotFound(DashboardLayoutNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/dashboard-not-found"));
        problem.setTitle("Dashboard Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DashboardLayoutStateException.class)
    public ProblemDetail handleDashboardState(DashboardLayoutStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/dashboard-invalid-state"));
        problem.setTitle("Invalid Dashboard State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MarketplacePackNotFoundException.class)
    public ProblemDetail handleMpNotFound(MarketplacePackNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/marketplace-pack-not-found"));
        problem.setTitle("Marketplace Pack Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ProblemDetail handleNotificationNotFound(NotificationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/notification-not-found"));
        problem.setTitle("Notification Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MarketplacePackStateException.class)
    public ProblemDetail handleMpState(MarketplacePackStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/marketplace-pack-invalid-state"));
        problem.setTitle("Invalid Marketplace Pack State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // BUG #3 — Une violation de contrainte d'intégrité (unicité, FK, NOT NULL) qui
        // n'a pas été traduite en exception métier dédiée est un CONFLIT de données, pas
        // une erreur serveur : 409 (et non 500). On NE divulgue PAS le message SQL brut
        // (nom de contrainte, colonnes, valeurs) — OWASP A09. Le détail technique reste
        // dans les logs serveur.
        log.warn("Data integrity violation mapped to 409", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The request conflicts with the current state of the resource "
                        + "(duplicate or constraint violation)");
        problem.setType(URI.create("https://qualitos.io/errors/data-conflict"));
        problem.setTitle("Data Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.data.mapping.PropertyReferenceException.class)
    public ProblemDetail handlePropertyReference(
            org.springframework.data.mapping.PropertyReferenceException ex) {
        // BUG #24 — Un ?sort=champInconnu (ou tout tri sur une propriété absente) fait
        // remonter PropertyReferenceException depuis Spring Data jusqu'au handler
        // générique → 500 sur TOUS les endpoints paginés. C'est une erreur du CLIENT
        // (paramètre de tri invalide) : 400. On expose le nom de propriété fautif
        // (déjà fourni par le client, pas une info interne sensible) pour aider au debug.
        log.debug("Invalid sort/property reference mapped to 400: {}", ex.getPropertyName());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Unknown sort or filter property: '" + ex.getPropertyName() + "'");
        problem.setType(URI.create("https://qualitos.io/errors/invalid-sort-property"));
        problem.setTitle("Invalid Sort Property");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        // Chemin inexistant → 404 (et non 500). Évite de masquer une 404 en
        // erreur serveur et ne divulgue aucune information interne (OWASP A05/A09).
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Resource not found");
        problem.setType(URI.create("https://qualitos.io/errors/resource-not-found"));
        problem.setTitle("Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({
            org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class})
    public ProblemDetail handleAccessDenied(RuntimeException ex) {
        // H1/C1 — Un refus d'autorisation (@PreAuthorize ou règle URL propagée jusqu'au
        // handler) doit être un 403, pas un 500. Sans ce handler, le catch-all
        // Exception → 500 masquerait le refus (OWASP A01/A09). Aucun détail interne exposé.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problem.setType(URI.create("https://qualitos.io/errors/access-denied"));
        problem.setTitle("Access Denied");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://qualitos.io/errors/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}

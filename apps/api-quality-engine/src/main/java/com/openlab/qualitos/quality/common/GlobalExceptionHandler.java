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
import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.TenantStandardNotFoundException;
import com.openlab.qualitos.quality.webhooks.WebhookDeliveryNotFoundException;
import com.openlab.qualitos.quality.webhooks.WebhookStateException;
import com.openlab.qualitos.quality.webhooks.WebhookSubscriptionNotFoundException;
import com.openlab.qualitos.quality.pdca.PdcaCycleNotFoundException;
import com.openlab.qualitos.quality.pdca.PdcaStepNotFoundException;
import com.openlab.qualitos.quality.pdca.PdcaStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

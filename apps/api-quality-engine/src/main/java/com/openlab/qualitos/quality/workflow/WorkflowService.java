package com.openlab.qualitos.quality.workflow;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Designer de workflow BPMN no-code (§5.4) : persistance des définitions de
 * workflow par tenant, versioning à la mise à jour et transitions de cycle de
 * vie (publication / archivage).
 *
 * <p>Multi-tenant strict : le tenant provient du JWT (jamais du body) ; l'acteur
 * (createdBy / updatedBy) provient du sub du JWT.</p>
 */
@Service
@Transactional
public class WorkflowService {

    private final WorkflowDefinitionRepository repository;

    public WorkflowService(WorkflowDefinitionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<WorkflowDto.Summary> findAll(WorkflowStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<WorkflowDefinition> page = (status != null)
                ? repository.findByTenantIdAndStatus(tenantId, status, pageable)
                : repository.findByTenantId(tenantId, pageable);
        return page.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public WorkflowDto.Response findById(UUID id) {
        return toResponse(load(id));
    }

    public WorkflowDto.Response create(WorkflowDto.CreateRequest request) {
        UUID tenantId = requireTenantId();
        validateBpmn(request.bpmnXml());

        WorkflowDefinition wf = new WorkflowDefinition();
        wf.setTenantId(tenantId);
        wf.setName(request.name());
        wf.setDescription(request.description());
        wf.setBpmnXml(request.bpmnXml());
        wf.setStatus(WorkflowStatus.DRAFT);
        wf.setVersion(1);
        UUID actor = CurrentUser.userId().orElse(null);
        wf.setCreatedBy(actor);
        wf.setUpdatedBy(actor);
        return toResponse(repository.save(wf));
    }

    /**
     * Mise à jour du contenu : interdite hors DRAFT (une définition publiée est
     * figée). Incrémente la version fonctionnelle à chaque sauvegarde.
     */
    public WorkflowDto.Response update(UUID id, WorkflowDto.UpdateRequest request) {
        WorkflowDefinition wf = load(id);
        if (wf.getStatus() != WorkflowStatus.DRAFT) {
            throw new WorkflowStateException(
                    "Only DRAFT workflow definitions can be edited (current: " + wf.getStatus() + ")");
        }
        if (request.bpmnXml() != null) {
            validateBpmn(request.bpmnXml());
            wf.setBpmnXml(request.bpmnXml());
        }
        if (request.name() != null) {
            if (!StringUtils.hasText(request.name())) {
                throw new WorkflowValidationException("name must not be blank");
            }
            wf.setName(request.name());
        }
        if (request.description() != null) {
            wf.setDescription(request.description());
        }
        wf.setVersion(wf.getVersion() + 1);
        wf.setUpdatedBy(CurrentUser.userId().orElse(wf.getUpdatedBy()));
        return toResponse(repository.save(wf));
    }

    /** DRAFT → PUBLISHED. */
    public WorkflowDto.Response publish(UUID id) {
        WorkflowDefinition wf = load(id);
        if (wf.getStatus() != WorkflowStatus.DRAFT) {
            throw new WorkflowStateException(
                    "Only DRAFT workflow definitions can be published (current: " + wf.getStatus() + ")");
        }
        wf.setStatus(WorkflowStatus.PUBLISHED);
        wf.setUpdatedBy(CurrentUser.userId().orElse(wf.getUpdatedBy()));
        return toResponse(repository.save(wf));
    }

    /** DRAFT | PUBLISHED → ARCHIVED. */
    public WorkflowDto.Response archive(UUID id) {
        WorkflowDefinition wf = load(id);
        if (wf.getStatus() == WorkflowStatus.ARCHIVED) {
            throw new WorkflowStateException("Workflow definition is already ARCHIVED");
        }
        wf.setStatus(WorkflowStatus.ARCHIVED);
        wf.setUpdatedBy(CurrentUser.userId().orElse(wf.getUpdatedBy()));
        return toResponse(repository.save(wf));
    }

    public void delete(UUID id) {
        WorkflowDefinition wf = load(id);
        repository.delete(wf);
    }

    // --- helpers ---

    private void validateBpmn(String bpmnXml) {
        if (!StringUtils.hasText(bpmnXml)) {
            throw new WorkflowValidationException("bpmnXml must not be empty");
        }
        if (bpmnXml.length() > WorkflowDto.MAX_BPMN_CHARS) {
            throw new WorkflowValidationException(
                    "bpmnXml exceeds the maximum size of " + WorkflowDto.MAX_BPMN_CHARS + " characters");
        }
    }

    private WorkflowDefinition load(UUID id) {
        UUID tenantId = requireTenantId();
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new WorkflowNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private WorkflowDto.Response toResponse(WorkflowDefinition wf) {
        return new WorkflowDto.Response(
                wf.getId(), wf.getTenantId(), wf.getName(), wf.getDescription(),
                wf.getBpmnXml(), wf.getStatus(), wf.getVersion(),
                wf.getCreatedBy(), wf.getUpdatedBy(), wf.getCreatedAt(), wf.getUpdatedAt());
    }

    private WorkflowDto.Summary toSummary(WorkflowDefinition wf) {
        return new WorkflowDto.Summary(
                wf.getId(), wf.getTenantId(), wf.getName(), wf.getDescription(),
                wf.getStatus(), wf.getVersion(),
                wf.getCreatedBy(), wf.getUpdatedBy(), wf.getCreatedAt(), wf.getUpdatedAt());
    }
}

package com.openlab.qualitos.quality.ishikawa;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class IshikawaService {

    private final IshikawaDiagramRepository diagramRepository;
    private final IshikawaCauseRepository causeRepository;

    public IshikawaService(IshikawaDiagramRepository diagramRepository,
                           IshikawaCauseRepository causeRepository) {
        this.diagramRepository = diagramRepository;
        this.causeRepository = causeRepository;
    }

    @Transactional(readOnly = true)
    public Page<IshikawaDto.DiagramResponse> findAll(IshikawaStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<IshikawaDiagram> page = status != null
                ? diagramRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : diagramRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toDiagramResponse);
    }

    @Transactional(readOnly = true)
    public IshikawaDto.DiagramResponse findById(UUID id) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(id));
        return toDiagramResponse(diagram);
    }

    public IshikawaDto.DiagramResponse createDiagram(IshikawaDto.CreateDiagramRequest request) {
        UUID tenantId = requireTenantId();

        IshikawaDiagram diagram = new IshikawaDiagram();
        diagram.setTenantId(tenantId);
        diagram.setProblemStatement(request.problemStatement());
        diagram.setDescription(request.description());
        diagram.setMode(request.mode() != null ? request.mode() : IshikawaMode.SIX_M);
        diagram.setStatus(IshikawaStatus.DRAFT);
        diagram.setOwnerId(request.ownerId());

        return toDiagramResponse(diagramRepository.save(diagram));
    }

    public IshikawaDto.DiagramResponse updateDiagram(UUID id, IshikawaDto.UpdateDiagramRequest request) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(id));

        if (diagram.getStatus() == IshikawaStatus.ARCHIVED) {
            throw new IshikawaStateException("Archived diagram cannot be modified");
        }

        if (request.problemStatement() != null) {
            diagram.setProblemStatement(request.problemStatement());
        }
        if (request.description() != null) {
            diagram.setDescription(request.description());
        }
        if (request.mode() != null) {
            validateModeChange(diagram, request.mode());
            diagram.setMode(request.mode());
        }
        if (request.status() != null) {
            validateStatusTransition(diagram.getStatus(), request.status());
            diagram.setStatus(request.status());
        }

        return toDiagramResponse(diagramRepository.save(diagram));
    }

    public void deleteDiagram(UUID id) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(id));

        if (diagram.getStatus() == IshikawaStatus.VALIDATED) {
            throw new IshikawaStateException(
                    "Validated diagram cannot be deleted; archive it instead");
        }

        diagramRepository.delete(diagram);
    }

    public IshikawaDto.CauseResponse addCause(UUID diagramId, IshikawaDto.CauseRequest request) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));

        if (diagram.getStatus() == IshikawaStatus.ARCHIVED) {
            throw new IshikawaStateException("Cannot add a cause to an archived diagram");
        }

        if (!diagram.getMode().allows(request.category())) {
            throw new IshikawaStateException(
                    "Category " + request.category() + " is not allowed by mode " + diagram.getMode());
        }

        IshikawaCause cause = new IshikawaCause();
        cause.setDiagram(diagram);
        cause.setCategory(request.category());
        cause.setLabel(request.label());
        cause.setDescription(request.description());
        cause.setRootCauseScore(request.rootCauseScore());

        if (request.parentId() != null) {
            IshikawaCause parent = causeRepository.findByIdAndDiagramId(request.parentId(), diagramId)
                    .orElseThrow(() -> new IshikawaCauseNotFoundException(request.parentId()));
            if (parent.getCategory() != request.category()) {
                throw new IshikawaStateException(
                        "Child cause category must match parent category " + parent.getCategory());
            }
            cause.setParent(parent);
        }

        return toCauseResponse(causeRepository.save(cause));
    }

    public IshikawaDto.CauseResponse updateCause(UUID diagramId, UUID causeId,
                                                 IshikawaDto.UpdateCauseRequest request) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));

        if (diagram.getStatus() == IshikawaStatus.ARCHIVED) {
            throw new IshikawaStateException("Cannot modify a cause on an archived diagram");
        }

        IshikawaCause cause = causeRepository.findByIdAndDiagramId(causeId, diagramId)
                .orElseThrow(() -> new IshikawaCauseNotFoundException(causeId));

        if (request.category() != null) {
            if (!diagram.getMode().allows(request.category())) {
                throw new IshikawaStateException(
                        "Category " + request.category() + " is not allowed by mode " + diagram.getMode());
            }
            if (cause.getParent() != null && cause.getParent().getCategory() != request.category()) {
                throw new IshikawaStateException(
                        "Child cause category must match parent category " + cause.getParent().getCategory());
            }
            cause.setCategory(request.category());
        }
        if (request.label() != null) {
            cause.setLabel(request.label());
        }
        if (request.description() != null) {
            cause.setDescription(request.description());
        }
        if (request.rootCauseScore() != null) {
            cause.setRootCauseScore(request.rootCauseScore());
        }

        return toCauseResponse(causeRepository.save(cause));
    }

    public void deleteCause(UUID diagramId, UUID causeId) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));

        if (diagram.getStatus() == IshikawaStatus.ARCHIVED) {
            throw new IshikawaStateException("Cannot delete a cause on an archived diagram");
        }

        IshikawaCause cause = causeRepository.findByIdAndDiagramId(causeId, diagramId)
                .orElseThrow(() -> new IshikawaCauseNotFoundException(causeId));

        causeRepository.delete(cause);
    }

    private void validateModeChange(IshikawaDiagram diagram, IshikawaMode newMode) {
        // Refuse de réduire le mode si des causes appartiennent à une catégorie qui disparaîtrait.
        boolean hasIncompatibleCause = diagram.getCauses().stream()
                .anyMatch(c -> !newMode.allows(c.getCategory()));
        if (hasIncompatibleCause) {
            throw new IshikawaStateException(
                    "Cannot switch to mode " + newMode + ": diagram has causes outside the allowed categories");
        }
    }

    private void validateStatusTransition(IshikawaStatus current, IshikawaStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == IshikawaStatus.IN_REVIEW || next == IshikawaStatus.ARCHIVED;
            case IN_REVIEW -> next == IshikawaStatus.VALIDATED
                    || next == IshikawaStatus.DRAFT
                    || next == IshikawaStatus.ARCHIVED;
            case VALIDATED -> next == IshikawaStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!valid) {
            throw new IshikawaStateException(
                    "Invalid status transition: " + current + " -> " + next);
        }
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private IshikawaDto.DiagramResponse toDiagramResponse(IshikawaDiagram diagram) {
        return new IshikawaDto.DiagramResponse(
                diagram.getId(),
                diagram.getTenantId(),
                diagram.getProblemStatement(),
                diagram.getDescription(),
                diagram.getMode(),
                diagram.getStatus(),
                diagram.getOwnerId(),
                diagram.getCreatedAt(),
                diagram.getUpdatedAt(),
                diagram.getCauses().stream().map(this::toCauseResponse).toList()
        );
    }

    private IshikawaDto.CauseResponse toCauseResponse(IshikawaCause cause) {
        return new IshikawaDto.CauseResponse(
                cause.getId(),
                cause.getDiagram().getId(),
                cause.getParent() != null ? cause.getParent().getId() : null,
                cause.getCategory(),
                cause.getLabel(),
                cause.getDescription(),
                cause.getRootCauseScore(),
                cause.getCreatedAt(),
                cause.getUpdatedAt()
        );
    }
}

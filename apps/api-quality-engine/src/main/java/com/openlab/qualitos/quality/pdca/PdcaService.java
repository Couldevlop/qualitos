package com.openlab.qualitos.quality.pdca;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class PdcaService {

    private final PdcaCycleRepository cycleRepository;
    private final PdcaStepRepository stepRepository;

    public PdcaService(PdcaCycleRepository cycleRepository, PdcaStepRepository stepRepository) {
        this.cycleRepository = cycleRepository;
        this.stepRepository = stepRepository;
    }

    @Transactional(readOnly = true)
    public Page<PdcaDto.CycleResponse> findAll(PdcaStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<PdcaCycle> page = status != null
                ? cycleRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : cycleRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toCycleResponse);
    }

    @Transactional(readOnly = true)
    public PdcaDto.CycleResponse findById(UUID id) {
        UUID tenantId = requireTenantId();
        PdcaCycle cycle = cycleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new PdcaCycleNotFoundException(id));
        return toCycleResponse(cycle);
    }

    public PdcaDto.CycleResponse createCycle(PdcaDto.CreateCycleRequest request) {
        UUID tenantId = requireTenantId();

        PdcaCycle cycle = new PdcaCycle();
        cycle.setTenantId(tenantId);
        cycle.setTitle(request.title());
        cycle.setDescription(request.description());
        cycle.setOwnerId(request.ownerId());
        cycle.setStatus(PdcaStatus.PLAN);

        return toCycleResponse(cycleRepository.save(cycle));
    }

    public PdcaDto.CycleResponse advanceCycle(UUID id) {
        UUID tenantId = requireTenantId();
        PdcaCycle cycle = cycleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new PdcaCycleNotFoundException(id));

        PdcaPhase currentPhase = cycle.getStatus().toPhase();
        if (currentPhase != null) {
            boolean allStepsDone = cycle.getSteps().stream()
                    .filter(s -> s.getPhase() == currentPhase)
                    .allMatch(s -> s.getStatus() == StepStatus.DONE);
            if (!allStepsDone) {
                throw new PdcaStateException(
                        "All steps in phase " + currentPhase + " must be DONE before advancing");
            }
        }

        PdcaStatus nextStatus = cycle.getStatus().next();
        cycle.setStatus(nextStatus);
        if (nextStatus == PdcaStatus.COMPLETED) {
            cycle.setCompletedAt(Instant.now());
        }

        return toCycleResponse(cycleRepository.save(cycle));
    }

    public PdcaDto.CycleResponse cancelCycle(UUID id) {
        UUID tenantId = requireTenantId();
        PdcaCycle cycle = cycleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new PdcaCycleNotFoundException(id));

        if (cycle.getStatus() == PdcaStatus.COMPLETED) {
            throw new PdcaStateException("Completed cycle cannot be cancelled");
        }
        if (cycle.getStatus() == PdcaStatus.CANCELLED) {
            throw new PdcaStateException("Cycle is already cancelled");
        }

        cycle.setStatus(PdcaStatus.CANCELLED);
        return toCycleResponse(cycleRepository.save(cycle));
    }

    public PdcaDto.StepResponse addStep(UUID cycleId, PdcaDto.StepRequest request) {
        UUID tenantId = requireTenantId();
        PdcaCycle cycle = cycleRepository.findByIdAndTenantId(cycleId, tenantId)
                .orElseThrow(() -> new PdcaCycleNotFoundException(cycleId));

        PdcaStep step = new PdcaStep();
        step.setCycle(cycle);
        step.setPhase(request.phase());
        step.setTitle(request.title());
        step.setDescription(request.description());
        step.setStatus(request.status() != null ? request.status() : StepStatus.PENDING);
        step.setAssigneeId(request.assigneeId());
        step.setDueDate(request.dueDate());

        return toStepResponse(stepRepository.save(step));
    }

    public PdcaDto.StepResponse updateStep(UUID cycleId, UUID stepId, PdcaDto.StepRequest request) {
        UUID tenantId = requireTenantId();
        cycleRepository.findByIdAndTenantId(cycleId, tenantId)
                .orElseThrow(() -> new PdcaCycleNotFoundException(cycleId));

        PdcaStep step = stepRepository.findByIdAndCycleId(stepId, cycleId)
                .orElseThrow(() -> new PdcaStepNotFoundException(stepId));

        if (request.title() != null) {
            step.setTitle(request.title());
        }
        if (request.description() != null) {
            step.setDescription(request.description());
        }
        if (request.status() != null) {
            step.setStatus(request.status());
        }
        if (request.assigneeId() != null) {
            step.setAssigneeId(request.assigneeId());
        }
        if (request.dueDate() != null) {
            step.setDueDate(request.dueDate());
        }

        return toStepResponse(stepRepository.save(step));
    }

    public void deleteStep(UUID cycleId, UUID stepId) {
        UUID tenantId = requireTenantId();
        PdcaCycle cycle = cycleRepository.findByIdAndTenantId(cycleId, tenantId)
                .orElseThrow(() -> new PdcaCycleNotFoundException(cycleId));

        PdcaStep step = stepRepository.findByIdAndCycleId(stepId, cycleId)
                .orElseThrow(() -> new PdcaStepNotFoundException(stepId));

        PdcaPhase currentPhase = cycle.getStatus().toPhase();
        if (currentPhase != null && step.getPhase() == currentPhase) {
            throw new PdcaStateException(
                    "Cannot delete a step belonging to the active phase " + currentPhase);
        }

        stepRepository.delete(step);
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private PdcaDto.CycleResponse toCycleResponse(PdcaCycle cycle) {
        return new PdcaDto.CycleResponse(
                cycle.getId(),
                cycle.getTenantId(),
                cycle.getTitle(),
                cycle.getDescription(),
                cycle.getStatus(),
                cycle.getOwnerId(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt(),
                cycle.getCompletedAt(),
                cycle.getSteps().stream().map(this::toStepResponse).toList()
        );
    }

    private PdcaDto.StepResponse toStepResponse(PdcaStep step) {
        return new PdcaDto.StepResponse(
                step.getId(),
                step.getCycle().getId(),
                step.getPhase(),
                step.getTitle(),
                step.getDescription(),
                step.getStatus(),
                step.getAssigneeId(),
                step.getDueDate(),
                step.getCreatedAt(),
                step.getUpdatedAt()
        );
    }
}

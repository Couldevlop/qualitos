package com.openlab.qualitos.quality.training;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Parcours + skills requises + analyse de gap.
 *
 * Transitions :
 *   DRAFT → ACTIVE   (activate; les parcours doivent avoir ≥1 skill requise)
 *   ACTIVE → DRAFT   (reopen pour révision)
 *   ANY non-ARCHIVED → ARCHIVED (archive; terminal)
 */
@Service
public class TrainingPathService {

    private final TrainingPathRepository pathRepo;
    private final TrainingPathSkillRequirementRepository reqRepo;
    private final SkillRepository skillRepo;
    private final UserSkillAssignmentRepository userSkillRepo;

    public TrainingPathService(TrainingPathRepository pathRepo,
                               TrainingPathSkillRequirementRepository reqRepo,
                               SkillRepository skillRepo,
                               UserSkillAssignmentRepository userSkillRepo) {
        this.pathRepo = pathRepo;
        this.reqRepo = reqRepo;
        this.skillRepo = skillRepo;
        this.userSkillRepo = userSkillRepo;
    }

    @Transactional
    public TrainingDto.PathResponse create(TrainingDto.CreatePathRequest req) {
        UUID tenantId = requireTenantId();
        pathRepo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(p -> {
            throw new TrainingStateException("Training path code already exists: " + req.code());
        });
        TrainingPath p = new TrainingPath();
        p.setTenantId(tenantId);
        p.setCode(req.code());
        p.setName(req.name());
        p.setDescription(req.description());
        p.setTargetRole(req.targetRole());
        p.setDurationHours(req.durationHours());
        p.setPassingScore(req.passingScore() == null ? 70 : req.passingScore());
        p.setValidityMonths(req.validityMonths());
        p.setStatus(TrainingPathStatus.DRAFT);
        p.setCreatedBy(req.createdBy());
        return toResponse(pathRepo.save(p));
    }

    @Transactional(readOnly = true)
    public Page<TrainingDto.PathResponse> list(TrainingPathStatus status, String targetRole, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<TrainingPath> page;
        if (status != null) page = pathRepo.findByTenantIdAndStatus(tenantId, status, pageable);
        else if (targetRole != null) page = pathRepo.findByTenantIdAndTargetRole(tenantId, targetRole, pageable);
        else page = pathRepo.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TrainingDto.PathResponse get(UUID id) { return toResponse(loadPath(id)); }

    @Transactional
    public TrainingDto.PathResponse update(UUID id, TrainingDto.UpdatePathRequest req) {
        TrainingPath p = loadPath(id);
        if (p.getStatus() == TrainingPathStatus.ARCHIVED) {
            throw new TrainingStateException("Cannot edit an ARCHIVED path");
        }
        if (req.name() != null) p.setName(req.name());
        if (req.description() != null) p.setDescription(req.description());
        if (req.targetRole() != null) p.setTargetRole(req.targetRole());
        if (req.durationHours() != null) p.setDurationHours(req.durationHours());
        if (req.passingScore() != null) p.setPassingScore(req.passingScore());
        if (req.validityMonths() != null) p.setValidityMonths(req.validityMonths());
        return toResponse(pathRepo.save(p));
    }

    @Transactional
    public void delete(UUID id) {
        TrainingPath p = loadPath(id);
        if (p.getStatus() == TrainingPathStatus.ACTIVE) {
            throw new TrainingStateException("Cannot delete an ACTIVE path; archive it first");
        }
        reqRepo.deleteByPathId(id);
        pathRepo.delete(p);
    }

    @Transactional
    public TrainingDto.PathResponse activate(UUID id) {
        TrainingPath p = loadPath(id);
        if (p.getStatus() == TrainingPathStatus.ARCHIVED) {
            throw new TrainingStateException("Cannot reactivate an ARCHIVED path");
        }
        if (p.getStatus() == TrainingPathStatus.ACTIVE) return toResponse(p);
        if (reqRepo.countByPathId(id) == 0) {
            throw new TrainingStateException(
                    "Path must declare at least one skill requirement before activation");
        }
        p.setStatus(TrainingPathStatus.ACTIVE);
        return toResponse(pathRepo.save(p));
    }

    @Transactional
    public TrainingDto.PathResponse reopen(UUID id) {
        TrainingPath p = loadPath(id);
        if (p.getStatus() != TrainingPathStatus.ACTIVE) {
            throw new TrainingStateException("Only ACTIVE paths can be re-opened");
        }
        p.setStatus(TrainingPathStatus.DRAFT);
        return toResponse(pathRepo.save(p));
    }

    @Transactional
    public TrainingDto.PathResponse archive(UUID id) {
        TrainingPath p = loadPath(id);
        if (p.getStatus() == TrainingPathStatus.ARCHIVED) {
            throw new TrainingStateException("Path is already ARCHIVED");
        }
        p.setStatus(TrainingPathStatus.ARCHIVED);
        return toResponse(pathRepo.save(p));
    }

    // ----- Skill requirements -----

    @Transactional
    public TrainingDto.SkillRequirementResponse attachSkill(
            UUID pathId, TrainingDto.AttachSkillRequirementRequest req) {
        TrainingPath p = loadPath(pathId);
        if (p.getStatus() == TrainingPathStatus.ARCHIVED) {
            throw new TrainingStateException("Cannot modify an ARCHIVED path");
        }
        Skill s = skillRepo.findById(req.skillId())
                .orElseThrow(() -> new SkillNotFoundException(req.skillId()));
        if (!s.getTenantId().equals(p.getTenantId())) {
            throw new SkillNotFoundException(req.skillId());
        }
        TrainingPathSkillRequirement r = reqRepo
                .findByPathIdAndSkillId(pathId, req.skillId())
                .orElseGet(() -> {
                    TrainingPathSkillRequirement fresh = new TrainingPathSkillRequirement();
                    fresh.setTenantId(p.getTenantId());
                    fresh.setPathId(pathId);
                    fresh.setSkillId(req.skillId());
                    return fresh;
                });
        r.setTargetLevel(req.targetLevel());
        return toResponse(reqRepo.save(r));
    }

    @Transactional
    public void detachSkill(UUID pathId, UUID skillId) {
        TrainingPath p = loadPath(pathId);
        if (p.getStatus() == TrainingPathStatus.ARCHIVED) {
            throw new TrainingStateException("Cannot modify an ARCHIVED path");
        }
        TrainingPathSkillRequirement r = reqRepo.findByPathIdAndSkillId(pathId, skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));
        reqRepo.delete(r);
    }

    @Transactional(readOnly = true)
    public List<TrainingDto.SkillRequirementResponse> listRequirements(UUID pathId) {
        loadPath(pathId);
        return reqRepo.findByPathId(pathId).stream().map(this::toResponse).toList();
    }

    // ----- Gap analysis -----

    @Transactional(readOnly = true)
    public TrainingDto.RoleGapAnalysis analyzeGap(UUID userId, UUID pathId) {
        TrainingPath p = loadPath(pathId);
        List<TrainingPathSkillRequirement> reqs = reqRepo.findByPathId(pathId);

        Map<UUID, Integer> current = new HashMap<>();
        Map<UUID, String> skillCodes = new HashMap<>();
        for (UserSkillAssignment a : userSkillRepo.findByTenantIdAndUserId(p.getTenantId(), userId)) {
            current.put(a.getSkillId(), a.getLevel());
        }
        for (Skill s : skillRepo.findAllById(reqs.stream().map(
                TrainingPathSkillRequirement::getSkillId).toList())) {
            skillCodes.put(s.getId(), s.getCode());
        }

        List<TrainingDto.SkillGap> gaps = new ArrayList<>();
        int satisfied = 0;
        for (TrainingPathSkillRequirement r : reqs) {
            int cur = current.getOrDefault(r.getSkillId(), 0);
            int gap = Math.max(0, r.getTargetLevel() - cur);
            if (gap == 0) satisfied++;
            else gaps.add(new TrainingDto.SkillGap(
                    r.getSkillId(),
                    skillCodes.getOrDefault(r.getSkillId(), "?"),
                    cur, r.getTargetLevel(), gap));
        }
        return new TrainingDto.RoleGapAnalysis(
                userId, pathId, p.getCode(), reqs.size(), satisfied, gaps);
    }

    // ----- helpers -----

    TrainingPath loadPath(UUID id) {
        UUID tenantId = requireTenantId();
        TrainingPath p = pathRepo.findById(id).orElseThrow(() -> new TrainingPathNotFoundException(id));
        if (!p.getTenantId().equals(tenantId)) throw new TrainingPathNotFoundException(id);
        return p;
    }

    private TrainingDto.PathResponse toResponse(TrainingPath p) {
        return new TrainingDto.PathResponse(
                p.getId(), p.getTenantId(), p.getCode(), p.getName(),
                p.getDescription(), p.getTargetRole(),
                p.getDurationHours(), p.getPassingScore(), p.getValidityMonths(),
                p.getStatus(), p.getCreatedBy(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private TrainingDto.SkillRequirementResponse toResponse(TrainingPathSkillRequirement r) {
        return new TrainingDto.SkillRequirementResponse(
                r.getId(), r.getPathId(), r.getSkillId(),
                r.getTargetLevel(), r.getCreatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }

    // exposed only for tests
    Optional<TrainingPath> rawFind(UUID id) { return pathRepo.findById(id); }
}

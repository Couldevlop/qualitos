package com.openlab.qualitos.quality.training;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Skills + competency matrix. Pas d'effet de bord (l'assessment ne touche
 * pas aux enrollments) — c'est volontaire pour garder le model découpé.
 */
@Service
public class SkillService {

    private final SkillRepository skillRepo;
    private final UserSkillAssignmentRepository userSkillRepo;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public SkillService(SkillRepository skillRepo, UserSkillAssignmentRepository userSkillRepo) {
        this(skillRepo, userSkillRepo, Clock.systemUTC());
    }

    SkillService(SkillRepository skillRepo, UserSkillAssignmentRepository userSkillRepo, Clock clock) {
        this.skillRepo = skillRepo;
        this.userSkillRepo = userSkillRepo;
        this.clock = clock;
    }

    @Transactional
    public TrainingDto.SkillResponse create(TrainingDto.CreateSkillRequest req) {
        UUID tenantId = requireTenantId();
        skillRepo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(s -> {
            throw new TrainingStateException("Skill code already exists: " + req.code());
        });
        Skill s = new Skill();
        s.setTenantId(tenantId);
        s.setCode(req.code());
        s.setName(req.name());
        s.setDescription(req.description());
        s.setCategory(req.category());
        return toResponse(skillRepo.save(s));
    }

    @Transactional(readOnly = true)
    public Page<TrainingDto.SkillResponse> list(String category, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<Skill> page = (category == null)
                ? skillRepo.findByTenantId(tenantId, pageable)
                : skillRepo.findByTenantIdAndCategory(tenantId, category, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TrainingDto.SkillResponse get(UUID id) { return toResponse(loadSkill(id)); }

    @Transactional
    public TrainingDto.SkillResponse update(UUID id, TrainingDto.UpdateSkillRequest req) {
        Skill s = loadSkill(id);
        if (req.name() != null) s.setName(req.name());
        if (req.description() != null) s.setDescription(req.description());
        if (req.category() != null) s.setCategory(req.category());
        return toResponse(skillRepo.save(s));
    }

    @Transactional
    public void delete(UUID id) {
        Skill s = loadSkill(id);
        if (userSkillRepo.countByTenantIdAndSkillId(s.getTenantId(), id) > 0) {
            throw new TrainingStateException(
                    "Cannot delete skill referenced by user competencies");
        }
        skillRepo.delete(s);
    }

    // ----- Competency matrix -----

    @Transactional
    public TrainingDto.CompetencyResponse assess(TrainingDto.AssessCompetencyRequest req) {
        UUID tenantId = requireTenantId();
        Skill s = skillRepo.findById(req.skillId())
                .orElseThrow(() -> new SkillNotFoundException(req.skillId()));
        if (!s.getTenantId().equals(tenantId)) throw new SkillNotFoundException(req.skillId());

        UserSkillAssignment a = userSkillRepo
                .findByTenantIdAndUserIdAndSkillId(tenantId, req.userId(), req.skillId())
                .orElseGet(() -> {
                    UserSkillAssignment fresh = new UserSkillAssignment();
                    fresh.setTenantId(tenantId);
                    fresh.setUserId(req.userId());
                    fresh.setSkillId(req.skillId());
                    return fresh;
                });
        a.setLevel(req.level());
        a.setSource(req.source());
        a.setAssessedBy(req.assessedBy());
        a.setAssessedAt(LocalDate.now(clock));
        a.setExpiresOn(req.expiresOn());
        return toResponse(userSkillRepo.save(a));
    }

    @Transactional(readOnly = true)
    public TrainingDto.CompetencyMatrix matrix(UUID userId) {
        UUID tenantId = requireTenantId();
        List<TrainingDto.CompetencyResponse> entries = userSkillRepo
                .findByTenantIdAndUserId(tenantId, userId)
                .stream().map(this::toResponse).toList();
        return new TrainingDto.CompetencyMatrix(userId, entries);
    }

    Skill loadSkill(UUID id) {
        UUID tenantId = requireTenantId();
        Skill s = skillRepo.findById(id).orElseThrow(() -> new SkillNotFoundException(id));
        if (!s.getTenantId().equals(tenantId)) throw new SkillNotFoundException(id);
        return s;
    }

    private TrainingDto.SkillResponse toResponse(Skill s) {
        return new TrainingDto.SkillResponse(
                s.getId(), s.getTenantId(), s.getCode(), s.getName(),
                s.getDescription(), s.getCategory(),
                s.getCreatedAt(), s.getUpdatedAt());
    }

    private TrainingDto.CompetencyResponse toResponse(UserSkillAssignment a) {
        LocalDate today = LocalDate.now(clock);
        return new TrainingDto.CompetencyResponse(
                a.getId(), a.getTenantId(), a.getUserId(), a.getSkillId(),
                a.getLevel(), CompetencyLevel.ofLevel(a.getLevel()),
                a.getSource(), a.getAssessedBy(),
                a.getAssessedAt(), a.getExpiresOn(),
                a.isExpiredAt(today),
                a.getCreatedAt(), a.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}

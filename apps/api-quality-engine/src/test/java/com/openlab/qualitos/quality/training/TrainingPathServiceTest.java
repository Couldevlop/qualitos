package com.openlab.qualitos.quality.training;

import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingPathServiceTest {

    @Mock TrainingPathRepository pathRepo;
    @Mock TrainingPathSkillRequirementRepository reqRepo;
    @Mock SkillRepository skillRepo;
    @Mock UserSkillAssignmentRepository userSkillRepo;
    @InjectMocks TrainingPathService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID PATH = UUID.randomUUID();
    static final UUID SKILL_A = UUID.randomUUID();
    static final UUID SKILL_B = UUID.randomUUID();

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void create_default70Passing() {
        when(pathRepo.findByTenantIdAndCode(TENANT, "p1")).thenReturn(Optional.empty());
        when(pathRepo.save(any())).thenAnswer(inv -> {
            TrainingPath p = inv.getArgument(0);
            p.setId(PATH); p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
            return p;
        });
        TrainingDto.PathResponse out = service.create(new TrainingDto.CreatePathRequest(
                "p1", "Path 1", null, "auditor", 16, null, 24, USER));
        assertThat(out.passingScore()).isEqualTo(70);
        assertThat(out.status()).isEqualTo(TrainingPathStatus.DRAFT);
    }

    @Test
    void create_duplicate_throws() {
        when(pathRepo.findByTenantIdAndCode(TENANT, "dup")).thenReturn(Optional.of(path()));
        assertThatThrownBy(() -> service.create(new TrainingDto.CreatePathRequest(
                "dup", "x", null, null, 8, null, null, USER)))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void activate_noRequirements_rejected() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(reqRepo.countByPathId(PATH)).thenReturn(0L);
        assertThatThrownBy(() -> service.activate(PATH))
                .isInstanceOf(TrainingStateException.class)
                .hasMessageContaining("at least one skill requirement");
    }

    @Test
    void activate_withRequirements_ok() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(reqRepo.countByPathId(PATH)).thenReturn(2L);
        when(pathRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.activate(PATH).status()).isEqualTo(TrainingPathStatus.ACTIVE);
    }

    @Test
    void activate_alreadyActive_idempotent() {
        TrainingPath p = path(); p.setStatus(TrainingPathStatus.ACTIVE);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        service.activate(PATH);
        verify(pathRepo, never()).save(any());
    }

    @Test
    void activate_archived_rejected() {
        TrainingPath p = path(); p.setStatus(TrainingPathStatus.ARCHIVED);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.activate(PATH))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void reopen_fromActive_ok() {
        TrainingPath p = path(); p.setStatus(TrainingPathStatus.ACTIVE);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(pathRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.reopen(PATH).status()).isEqualTo(TrainingPathStatus.DRAFT);
    }

    @Test
    void reopen_fromDraft_rejected() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.reopen(PATH))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void archive_terminal() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(pathRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.archive(PATH).status()).isEqualTo(TrainingPathStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchived_rejected() {
        TrainingPath p = path(); p.setStatus(TrainingPathStatus.ARCHIVED);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.archive(PATH))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void delete_active_rejected() {
        TrainingPath p = path(); p.setStatus(TrainingPathStatus.ACTIVE);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.delete(PATH))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void delete_draft_cascadesRequirements() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        service.delete(PATH);
        verify(reqRepo).deleteByPathId(PATH);
        verify(pathRepo).delete(p);
    }

    @Test
    void update_archived_rejected() {
        TrainingPath p = path(); p.setStatus(TrainingPathStatus.ARCHIVED);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.update(PATH, new TrainingDto.UpdatePathRequest(
                "x", null, null, null, null, null)))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void update_appliesPatches() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(pathRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.update(PATH, new TrainingDto.UpdatePathRequest(
                "renamed", "desc", "role", 32, 80, 36));
        assertThat(p.getName()).isEqualTo("renamed");
        assertThat(p.getDurationHours()).isEqualTo(32);
        assertThat(p.getPassingScore()).isEqualTo(80);
        assertThat(p.getValidityMonths()).isEqualTo(36);
    }

    @Test
    void list_byStatus_orRole_orAll() {
        when(pathRepo.findByTenantIdAndStatus(eq(TENANT), eq(TrainingPathStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(path())));
        assertThat(service.list(TrainingPathStatus.ACTIVE, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(pathRepo.findByTenantIdAndTargetRole(eq(TENANT), eq("auditor"), any()))
                .thenReturn(new PageImpl<>(List.of(path())));
        assertThat(service.list(null, "auditor", PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(pathRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(path())));
        assertThat(service.list(null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    // --- Skill requirements ---

    @Test
    void attachSkill_new_persists() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        Skill s = new Skill();
        s.setId(SKILL_A); s.setTenantId(TENANT);
        when(skillRepo.findById(SKILL_A)).thenReturn(Optional.of(s));
        when(reqRepo.findByPathIdAndSkillId(PATH, SKILL_A)).thenReturn(Optional.empty());
        when(reqRepo.save(any())).thenAnswer(inv -> {
            TrainingPathSkillRequirement r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(Instant.now());
            return r;
        });
        TrainingDto.SkillRequirementResponse out = service.attachSkill(PATH,
                new TrainingDto.AttachSkillRequirementRequest(SKILL_A, 3));
        assertThat(out.targetLevel()).isEqualTo(3);
    }

    @Test
    void attachSkill_existing_updatesTargetLevel() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        Skill s = new Skill();
        s.setId(SKILL_A); s.setTenantId(TENANT);
        when(skillRepo.findById(SKILL_A)).thenReturn(Optional.of(s));
        TrainingPathSkillRequirement existing = new TrainingPathSkillRequirement();
        existing.setId(UUID.randomUUID()); existing.setPathId(PATH); existing.setSkillId(SKILL_A);
        existing.setTargetLevel(1); existing.setCreatedAt(Instant.now());
        when(reqRepo.findByPathIdAndSkillId(PATH, SKILL_A)).thenReturn(Optional.of(existing));
        when(reqRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.attachSkill(PATH, new TrainingDto.AttachSkillRequirementRequest(SKILL_A, 4));
        assertThat(existing.getTargetLevel()).isEqualTo(4);
    }

    @Test
    void attachSkill_archivedPath_rejected() {
        TrainingPath p = path(); p.setStatus(TrainingPathStatus.ARCHIVED);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.attachSkill(PATH,
                new TrainingDto.AttachSkillRequirementRequest(SKILL_A, 2)))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void attachSkill_skillCrossTenant_appearsNotFound() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        Skill s = new Skill();
        s.setId(SKILL_A); s.setTenantId(UUID.randomUUID());
        when(skillRepo.findById(SKILL_A)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.attachSkill(PATH,
                new TrainingDto.AttachSkillRequirementRequest(SKILL_A, 2)))
                .isInstanceOf(SkillNotFoundException.class);
    }

    @Test
    void detachSkill_archived_rejected() {
        TrainingPath p = path(); p.setStatus(TrainingPathStatus.ARCHIVED);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.detachSkill(PATH, SKILL_A))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void detachSkill_missing_throws() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(reqRepo.findByPathIdAndSkillId(PATH, SKILL_A)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detachSkill(PATH, SKILL_A))
                .isInstanceOf(SkillNotFoundException.class);
    }

    @Test
    void listRequirements_returnsAll() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        TrainingPathSkillRequirement r = new TrainingPathSkillRequirement();
        r.setId(UUID.randomUUID()); r.setPathId(PATH); r.setSkillId(SKILL_A);
        r.setTargetLevel(2); r.setCreatedAt(Instant.now());
        when(reqRepo.findByPathId(PATH)).thenReturn(List.of(r));
        assertThat(service.listRequirements(PATH)).hasSize(1);
    }

    // --- Gap analysis ---

    @Test
    void analyzeGap_perfectMatch_noGaps() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        TrainingPathSkillRequirement r1 = requirement(SKILL_A, 3);
        TrainingPathSkillRequirement r2 = requirement(SKILL_B, 2);
        when(reqRepo.findByPathId(PATH)).thenReturn(List.of(r1, r2));
        UserSkillAssignment ua = userSkill(SKILL_A, 3);
        UserSkillAssignment ub = userSkill(SKILL_B, 4); // above target — counts as satisfied
        when(userSkillRepo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(List.of(ua, ub));
        Skill sa = skillEntity(SKILL_A, "iso-9001");
        Skill sb = skillEntity(SKILL_B, "audit");
        when(skillRepo.findAllById(any())).thenReturn(List.of(sa, sb));
        TrainingDto.RoleGapAnalysis out = service.analyzeGap(USER, PATH);
        assertThat(out.totalRequirements()).isEqualTo(2);
        assertThat(out.satisfied()).isEqualTo(2);
        assertThat(out.gaps()).isEmpty();
    }

    @Test
    void analyzeGap_partialMatch_listsGaps() {
        TrainingPath p = path();
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(reqRepo.findByPathId(PATH)).thenReturn(List.of(
                requirement(SKILL_A, 3), requirement(SKILL_B, 4)));
        when(userSkillRepo.findByTenantIdAndUserId(TENANT, USER))
                .thenReturn(List.of(userSkill(SKILL_A, 1)));
        when(skillRepo.findAllById(any())).thenReturn(List.of(
                skillEntity(SKILL_A, "iso-9001"), skillEntity(SKILL_B, "audit")));
        TrainingDto.RoleGapAnalysis out = service.analyzeGap(USER, PATH);
        assertThat(out.satisfied()).isZero();
        assertThat(out.gaps()).hasSize(2);
        TrainingDto.SkillGap gapA = out.gaps().stream()
                .filter(g -> g.skillId().equals(SKILL_A)).findFirst().orElseThrow();
        assertThat(gapA.gap()).isEqualTo(2);
        assertThat(gapA.skillCode()).isEqualTo("iso-9001");
    }

    // --- helpers ---

    private TrainingPath path() {
        TrainingPath p = new TrainingPath();
        p.setId(PATH); p.setTenantId(TENANT); p.setCode("p-1");
        p.setName("Path"); p.setStatus(TrainingPathStatus.DRAFT);
        p.setDurationHours(16); p.setPassingScore(70);
        p.setCreatedBy(USER);
        p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
        return p;
    }

    private TrainingPathSkillRequirement requirement(UUID skillId, int level) {
        TrainingPathSkillRequirement r = new TrainingPathSkillRequirement();
        r.setId(UUID.randomUUID()); r.setTenantId(TENANT);
        r.setPathId(PATH); r.setSkillId(skillId); r.setTargetLevel(level);
        return r;
    }

    private UserSkillAssignment userSkill(UUID skillId, int level) {
        UserSkillAssignment a = new UserSkillAssignment();
        a.setId(UUID.randomUUID()); a.setTenantId(TENANT);
        a.setUserId(USER); a.setSkillId(skillId); a.setLevel(level);
        return a;
    }

    private Skill skillEntity(UUID id, String code) {
        Skill s = new Skill();
        s.setId(id); s.setTenantId(TENANT); s.setCode(code);
        return s;
    }
}

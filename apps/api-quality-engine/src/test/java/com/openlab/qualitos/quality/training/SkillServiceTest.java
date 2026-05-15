package com.openlab.qualitos.quality.training;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock SkillRepository skillRepo;
    @Mock UserSkillAssignmentRepository userSkillRepo;
    SkillService service;
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID SKILL = UUID.randomUUID();
    static final Clock CLOCK = Clock.fixed(
            LocalDate.parse("2026-05-15").atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new SkillService(skillRepo, userSkillRepo, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void create_persists() {
        when(skillRepo.findByTenantIdAndCode(TENANT, "iso-9001")).thenReturn(Optional.empty());
        when(skillRepo.save(any())).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setId(SKILL); s.setCreatedAt(Instant.now()); s.setUpdatedAt(Instant.now());
            return s;
        });
        TrainingDto.SkillResponse out = service.create(new TrainingDto.CreateSkillRequest(
                "iso-9001", "ISO 9001 fundamentals", "desc", "quality"));
        assertThat(out.tenantId()).isEqualTo(TENANT);
        assertThat(out.code()).isEqualTo("iso-9001");
    }

    @Test
    void create_duplicate_throws() {
        when(skillRepo.findByTenantIdAndCode(TENANT, "dup")).thenReturn(Optional.of(skill()));
        assertThatThrownBy(() -> service.create(new TrainingDto.CreateSkillRequest(
                "dup", "n", null, null)))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void create_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(new TrainingDto.CreateSkillRequest(
                "x", "n", null, null)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void list_byCategory() {
        when(skillRepo.findByTenantIdAndCategory(eq(TENANT), eq("quality"), any()))
                .thenReturn(new PageImpl<>(List.of(skill())));
        assertThat(service.list("quality", PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void list_all() {
        when(skillRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(skill())));
        assertThat(service.list(null, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void update_patches() {
        Skill s = skill();
        when(skillRepo.findById(SKILL)).thenReturn(Optional.of(s));
        when(skillRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.update(SKILL, new TrainingDto.UpdateSkillRequest(
                "renamed", "newdesc", "newcat"));
        assertThat(s.getName()).isEqualTo("renamed");
        assertThat(s.getDescription()).isEqualTo("newdesc");
        assertThat(s.getCategory()).isEqualTo("newcat");
    }

    @Test
    void delete_blockedIfUserSkillsExist() {
        Skill s = skill();
        when(skillRepo.findById(SKILL)).thenReturn(Optional.of(s));
        when(userSkillRepo.countByTenantIdAndSkillId(TENANT, SKILL)).thenReturn(3L);
        assertThatThrownBy(() -> service.delete(SKILL))
                .isInstanceOf(TrainingStateException.class)
                .hasMessageContaining("referenced");
    }

    @Test
    void delete_happyPath() {
        Skill s = skill();
        when(skillRepo.findById(SKILL)).thenReturn(Optional.of(s));
        when(userSkillRepo.countByTenantIdAndSkillId(TENANT, SKILL)).thenReturn(0L);
        service.delete(SKILL);
        verify(skillRepo).delete(s);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        Skill s = skill(); s.setTenantId(UUID.randomUUID());
        when(skillRepo.findById(SKILL)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.get(SKILL))
                .isInstanceOf(SkillNotFoundException.class);
    }

    // --- Competency assessment ---

    @Test
    void assess_newUserSkill_persists() {
        when(skillRepo.findById(SKILL)).thenReturn(Optional.of(skill()));
        when(userSkillRepo.findByTenantIdAndUserIdAndSkillId(TENANT, USER, SKILL))
                .thenReturn(Optional.empty());
        when(userSkillRepo.save(any())).thenAnswer(inv -> {
            UserSkillAssignment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(Instant.now()); a.setUpdatedAt(Instant.now());
            return a;
        });
        TrainingDto.CompetencyResponse out = service.assess(new TrainingDto.AssessCompetencyRequest(
                USER, SKILL, 3, CompetencySource.TRAINING, UUID.randomUUID(), null));
        assertThat(out.level()).isEqualTo(3);
        assertThat(out.levelName()).isEqualTo(CompetencyLevel.COMPETENT);
        assertThat(out.assessedAt()).isEqualTo(LocalDate.parse("2026-05-15"));
    }

    @Test
    void assess_existing_updatesLevel() {
        when(skillRepo.findById(SKILL)).thenReturn(Optional.of(skill()));
        UserSkillAssignment existing = new UserSkillAssignment();
        existing.setId(UUID.randomUUID()); existing.setTenantId(TENANT);
        existing.setUserId(USER); existing.setSkillId(SKILL); existing.setLevel(1);
        when(userSkillRepo.findByTenantIdAndUserIdAndSkillId(TENANT, USER, SKILL))
                .thenReturn(Optional.of(existing));
        when(userSkillRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.assess(new TrainingDto.AssessCompetencyRequest(
                USER, SKILL, 4, CompetencySource.CERTIFICATION, null, null));
        assertThat(existing.getLevel()).isEqualTo(4);
        assertThat(existing.getSource()).isEqualTo(CompetencySource.CERTIFICATION);
    }

    @Test
    void assess_skillCrossTenant_appearsNotFound() {
        Skill s = skill(); s.setTenantId(UUID.randomUUID());
        when(skillRepo.findById(SKILL)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.assess(new TrainingDto.AssessCompetencyRequest(
                USER, SKILL, 2, CompetencySource.SELF, null, null)))
                .isInstanceOf(SkillNotFoundException.class);
    }

    @Test
    void matrix_returnsUserCompetencies() {
        UserSkillAssignment a = new UserSkillAssignment();
        a.setId(UUID.randomUUID()); a.setTenantId(TENANT); a.setUserId(USER);
        a.setSkillId(SKILL); a.setLevel(2);
        a.setAssessedAt(LocalDate.parse("2026-01-01"));
        when(userSkillRepo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(List.of(a));
        TrainingDto.CompetencyMatrix out = service.matrix(USER);
        assertThat(out.userId()).isEqualTo(USER);
        assertThat(out.competencies()).hasSize(1);
        assertThat(out.competencies().get(0).levelName()).isEqualTo(CompetencyLevel.PRACTITIONER);
    }

    private Skill skill() {
        Skill s = new Skill();
        s.setId(SKILL); s.setTenantId(TENANT);
        s.setCode("c"); s.setName("Skill");
        s.setCreatedAt(Instant.now()); s.setUpdatedAt(Instant.now());
        return s;
    }
}

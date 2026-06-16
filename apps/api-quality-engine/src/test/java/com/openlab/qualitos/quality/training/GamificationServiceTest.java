package com.openlab.qualitos.quality.training;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock LearnerProgressRepository repo;
    GamificationService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new GamificationService(repo);
        TenantContext.setTenantId(TENANT.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER.toString(), "n/a", AuthorityUtils.NO_AUTHORITIES));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---- pure points rule ----

    @Test
    void pointsForScore_belowThreshold_isZero() {
        assertThat(GamificationService.pointsForScore(59)).isZero();
    }

    @Test
    void pointsForScore_passing_addsBaseAndBonus() {
        assertThat(GamificationService.pointsForScore(60)).isEqualTo(50 + 30);
        assertThat(GamificationService.pointsForScore(100)).isEqualTo(50 + 50);
    }

    // ---- myProgress ----

    @Test
    void myProgress_noRecord_returnsEmptyWhiteBelt() {
        when(repo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.empty());
        GamificationDto.LearnerProgressResponse out = service.myProgress();
        assertThat(out.points()).isZero();
        assertThat(out.beltLevel()).isEqualTo(BeltLevel.WHITE);
        assertThat(out.badges()).isEmpty();
        assertThat(out.userId()).isEqualTo(USER);
        assertThat(out.tenantId()).isEqualTo(TENANT);
        assertThat(out.pointsToNextBelt()).isEqualTo(100);
    }

    @Test
    void myProgress_existing_returnsPersistedView() {
        LearnerProgress p = progress(350, 6, 92, BeltLevel.GREEN,
                EnumSet.of(Badge.FIRST_STEPS, Badge.DEDICATED_LEARNER, Badge.YELLOW_BELT, Badge.GREEN_BELT));
        when(repo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.of(p));
        GamificationDto.LearnerProgressResponse out = service.myProgress();
        assertThat(out.points()).isEqualTo(350);
        assertThat(out.beltLevel()).isEqualTo(BeltLevel.GREEN);
        assertThat(out.badges()).contains(Badge.GREEN_BELT);
    }

    @Test
    void myProgress_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.myProgress())
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void myProgress_noAuthenticatedUser_throws() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.myProgress())
                .isInstanceOf(MissingTenantContextException.class);
    }

    // ---- complete ----

    @Test
    void complete_firstPassingItem_createsProgressAndFirstBadge() {
        when(repo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GamificationDto.LearnerProgressResponse out = service.complete(
                new GamificationDto.CompleteLearningRequest("yellow-belt-quality", 80));

        assertThat(out.completedCount()).isEqualTo(1);
        assertThat(out.points()).isEqualTo(50 + 40);   // 90
        assertThat(out.bestScore()).isEqualTo(80);
        assertThat(out.beltLevel()).isEqualTo(BeltLevel.WHITE);
        assertThat(out.badges()).containsExactly(Badge.FIRST_STEPS);
    }

    @Test
    void complete_accumulatesToYellowBelt() {
        LearnerProgress existing = progress(70, 1, 80, BeltLevel.WHITE, EnumSet.of(Badge.FIRST_STEPS));
        when(repo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // +90 points (score 80) → 160 → YELLOW
        GamificationDto.LearnerProgressResponse out = service.complete(
                new GamificationDto.CompleteLearningRequest("green-belt-six-sigma", 80));

        assertThat(out.points()).isEqualTo(160);
        assertThat(out.completedCount()).isEqualTo(2);
        assertThat(out.beltLevel()).isEqualTo(BeltLevel.YELLOW);
        assertThat(out.badges()).contains(Badge.YELLOW_BELT);
    }

    @Test
    void complete_failingScore_noPointsNoCompletion_butRecordsBestScore() {
        when(repo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GamificationDto.LearnerProgressResponse out = service.complete(
                new GamificationDto.CompleteLearningRequest("hard-quiz", 50));

        assertThat(out.points()).isZero();
        assertThat(out.completedCount()).isZero();
        assertThat(out.bestScore()).isEqualTo(50);
        assertThat(out.badges()).isEmpty();
    }

    @Test
    void complete_perfectScore_awardsPerfectionist() {
        when(repo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GamificationDto.LearnerProgressResponse out = service.complete(
                new GamificationDto.CompleteLearningRequest("perfect", 100));

        assertThat(out.badges()).contains(Badge.PERFECTIONIST, Badge.FIRST_STEPS);
        assertThat(out.bestScore()).isEqualTo(100);
    }

    @Test
    void complete_persistsTenantAndUserFromContext_notFromBody() {
        when(repo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.complete(new GamificationDto.CompleteLearningRequest("x", 70));

        verify(repo).save(argThat(p ->
                p.getTenantId().equals(TENANT) && p.getUserId().equals(USER)));
    }

    @Test
    void complete_lowerScoreLater_keepsBestScore() {
        LearnerProgress existing = progress(90, 1, 95, BeltLevel.WHITE, EnumSet.of(Badge.FIRST_STEPS));
        when(repo.findByTenantIdAndUserId(TENANT, USER)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GamificationDto.LearnerProgressResponse out = service.complete(
                new GamificationDto.CompleteLearningRequest("y", 70));

        assertThat(out.bestScore()).isEqualTo(95);
    }

    @Test
    void complete_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.complete(
                new GamificationDto.CompleteLearningRequest("x", 80)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // ---- helper ----

    private LearnerProgress progress(int points, int completed, Integer best,
                                     BeltLevel belt, EnumSet<Badge> badges) {
        LearnerProgress p = new LearnerProgress();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setUserId(USER);
        p.setPoints(points);
        p.setCompletedCount(completed);
        p.setBestScore(best);
        p.setBeltLevel(belt);
        p.setBadges(badges);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }
}

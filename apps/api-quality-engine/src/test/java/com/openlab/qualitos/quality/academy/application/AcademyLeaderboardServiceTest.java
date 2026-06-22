package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.training.Badge;
import com.openlab.qualitos.quality.training.BeltLevel;
import com.openlab.qualitos.quality.training.LearnerProgress;
import com.openlab.qualitos.quality.training.LearnerProgressRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademyLeaderboardServiceTest {

    @Mock LearnerProgressRepository repo;
    AcademyLeaderboardService service;

    static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000dd");

    @BeforeEach
    void setup() {
        service = new AcademyLeaderboardService(repo);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clear() { TenantContext.clear(); }

    private LearnerProgress lp(int points, int completed, BeltLevel belt, EnumSet<Badge> badges) {
        LearnerProgress p = new LearnerProgress();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setUserId(UUID.randomUUID());
        p.setPoints(points);
        p.setCompletedCount(completed);
        p.setBeltLevel(belt);
        p.setBadges(badges);
        return p;
    }

    @Test
    void leaderboard_ranksByPoints_andMapsBadges() {
        LearnerProgress top = lp(700, 10, BeltLevel.BLACK,
                EnumSet.of(Badge.BLACK_BELT, Badge.QUALITY_CHAMPION));
        LearnerProgress second = lp(300, 6, BeltLevel.GREEN, EnumSet.of(Badge.GREEN_BELT));
        when(repo.findByTenantIdOrderByPointsDescCompletedCountDesc(eq(TENANT), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(top, second), Pageable.ofSize(20), 2));

        AcademyDto.Leaderboard board = service.leaderboard(20);

        assertThat(board.totalLearners()).isEqualTo(2);
        assertThat(board.entries()).hasSize(2);
        assertThat(board.entries().get(0).rank()).isEqualTo(1);
        assertThat(board.entries().get(0).points()).isEqualTo(700);
        assertThat(board.entries().get(0).beltLevel()).isEqualTo("BLACK");
        assertThat(board.entries().get(0).badges()).contains("BLACK_BELT", "QUALITY_CHAMPION");
        assertThat(board.entries().get(1).rank()).isEqualTo(2);
    }

    @Test
    void leaderboard_sizeIsClampedTo100() {
        when(repo.findByTenantIdOrderByPointsDescCompletedCountDesc(eq(TENANT), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);

        service.leaderboard(9999);

        org.mockito.Mockito.verify(repo).findByTenantIdOrderByPointsDescCompletedCountDesc(eq(TENANT), cap.capture());
        assertThat(cap.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void leaderboard_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.leaderboard(10)).isInstanceOf(MissingTenantContextException.class);
    }
}

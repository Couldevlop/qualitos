package com.openlab.qualitos.quality.academy.application;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.training.Badge;
import com.openlab.qualitos.quality.training.LearnerProgress;
import com.openlab.qualitos.quality.training.LearnerProgressRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Classement (leaderboard) de gamification par tenant (§19.3).
 *
 * <p>RÉUTILISE la progression d'apprenant {@link LearnerProgress} (V87) :
 * apprenants triés par points décroissants. Strictement filtré par
 * {@code tenantId} du JWT — un tenant ne voit jamais le classement d'un autre.</p>
 */
@Service
public class AcademyLeaderboardService {

    private static final int MAX_SIZE = 100;

    private final LearnerProgressRepository progress;

    public AcademyLeaderboardService(LearnerProgressRepository progress) {
        this.progress = progress;
    }

    @Transactional(readOnly = true)
    public AcademyDto.Leaderboard leaderboard(int size) {
        UUID tenantId = requireTenantId();
        int capped = Math.max(1, Math.min(size, MAX_SIZE));
        Page<LearnerProgress> page = progress.findByTenantIdOrderByPointsDescCompletedCountDesc(
                tenantId, PageRequest.of(0, capped));
        List<AcademyDto.LeaderboardEntry> entries = new ArrayList<>(page.getNumberOfElements());
        int rank = 1;
        for (LearnerProgress p : page.getContent()) {
            entries.add(new AcademyDto.LeaderboardEntry(
                    rank++, p.getUserId(), p.getPoints(), p.getCompletedCount(),
                    p.getBestScore(), p.getBeltLevel().name(), sortedBadges(p)));
        }
        return new AcademyDto.Leaderboard(entries, page.getTotalElements());
    }

    private List<String> sortedBadges(LearnerProgress p) {
        return p.getBadges().stream()
                .sorted(Comparator.naturalOrder())
                .map(Badge::name)
                .toList();
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}

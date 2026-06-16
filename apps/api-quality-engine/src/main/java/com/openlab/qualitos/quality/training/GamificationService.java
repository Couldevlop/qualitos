package com.openlab.qualitos.quality.training;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Moteur de gamification de la formation (CLAUDE.md §4.7 + §19.3).
 *
 * <p>Tenant ET utilisateur sont toujours résolus depuis le JWT (jamais du body)
 * via {@link TenantContext} et {@link CurrentUser} — invariant de sécurité 18.2.
 * Le service applique des règles <b>pures et déterministes</b> de points, de
 * ceinture ({@link BeltLevel}) et de badges ({@link Badge}) ; le recalcul est
 * idempotent vis-à-vis des données dérivées (une même progression donne toujours
 * la même ceinture et le même jeu de badges).</p>
 */
@Service
public class GamificationService {

    /** Points de base octroyés à chaque complétion réussie (score ≥ seuil). */
    static final int BASE_POINTS = 50;
    /** Score minimal considéré comme « réussite » d'un item d'apprentissage. */
    static final int PASS_THRESHOLD = 60;

    private final LearnerProgressRepository repository;

    public GamificationService(LearnerProgressRepository repository) {
        this.repository = repository;
    }

    /**
     * Points gagnés pour une complétion (fonction pure).
     *
     * <p>Sous le seuil de réussite : 0 point (échec). Au-dessus : un socle fixe
     * + un bonus proportionnel au score, pour récompenser l'excellence sans
     * dénaturer la progression de base.</p>
     *
     * @param score score obtenu (0-100)
     * @return points attribués (≥ 0)
     */
    static int pointsForScore(int score) {
        if (score < PASS_THRESHOLD) {
            return 0;
        }
        // socle + bonus : 100 → 50 + 50 = 100 ; 60 → 50 + 30 = 80.
        return BASE_POINTS + (score / 2);
    }

    @Transactional(readOnly = true)
    public GamificationDto.LearnerProgressResponse myProgress() {
        UUID tenantId = requireTenantId();
        UUID userId = CurrentUser.requireUserId();
        LearnerProgress progress = repository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> emptyProgress(tenantId, userId));
        return toResponse(progress);
    }

    /**
     * Enregistre la complétion d'un parcours/quiz pour l'utilisateur courant et
     * recalcule points, ceinture et badges.
     *
     * @param req item complété + score obtenu
     * @return la progression mise à jour
     */
    @Transactional
    public GamificationDto.LearnerProgressResponse complete(GamificationDto.CompleteLearningRequest req) {
        UUID tenantId = requireTenantId();
        UUID userId = CurrentUser.requireUserId();

        LearnerProgress progress = repository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> {
                    LearnerProgress p = new LearnerProgress();
                    p.setTenantId(tenantId);
                    p.setUserId(userId);
                    p.setPoints(0);
                    p.setCompletedCount(0);
                    p.setBeltLevel(BeltLevel.WHITE);
                    p.setBadges(EnumSet.noneOf(Badge.class));
                    return p;
                });

        int score = req.score();
        int gained = pointsForScore(score);

        if (gained > 0) {
            progress.setPoints(progress.getPoints() + gained);
            progress.setCompletedCount(progress.getCompletedCount() + 1);
            Integer best = progress.getBestScore();
            if (best == null || score > best) {
                progress.setBestScore(score);
            }
        } else if (progress.getBestScore() == null) {
            // Échec dès le 1er essai : on mémorise tout de même le meilleur score
            // (utile pour le badge Perfectionist et le suivi), sans points ni complétion.
            progress.setBestScore(score);
        } else if (score > progress.getBestScore()) {
            progress.setBestScore(score);
        }

        // Recalcul des données dérivées via les règles pures.
        BeltLevel belt = BeltLevel.fromPoints(progress.getPoints());
        progress.setBeltLevel(belt);
        progress.setBadges(Badge.evaluate(progress.getCompletedCount(), progress.getBestScore(), belt));

        return toResponse(repository.save(progress));
    }

    private LearnerProgress emptyProgress(UUID tenantId, UUID userId) {
        LearnerProgress p = new LearnerProgress();
        p.setTenantId(tenantId);
        p.setUserId(userId);
        p.setBeltLevel(BeltLevel.WHITE);
        p.setBadges(EnumSet.noneOf(Badge.class));
        Instant now = Instant.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }

    private GamificationDto.LearnerProgressResponse toResponse(LearnerProgress p) {
        Set<Badge> badges = p.getBadges();
        List<Badge> sorted = new ArrayList<>(badges);
        sorted.sort(java.util.Comparator.naturalOrder());
        return new GamificationDto.LearnerProgressResponse(
                p.getUserId(), p.getTenantId(),
                p.getPoints(), p.getCompletedCount(), p.getBestScore(),
                p.getBeltLevel(), BeltLevel.pointsToNext(p.getPoints()),
                sorted, p.getCreatedAt(), p.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}

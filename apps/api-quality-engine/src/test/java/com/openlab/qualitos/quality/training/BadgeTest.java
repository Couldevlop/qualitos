package com.openlab.qualitos.quality.training;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Règles d'attribution des badges (domaine pur — §19.3). */
class BadgeTest {

    @Test
    void noCompletion_whiteBelt_noBadge() {
        Set<Badge> badges = Badge.evaluate(0, null, BeltLevel.WHITE);
        assertThat(badges).isEmpty();
    }

    @Test
    void firstCompletion_awardsFirstSteps() {
        Set<Badge> badges = Badge.evaluate(1, 80, BeltLevel.WHITE);
        assertThat(badges).containsExactly(Badge.FIRST_STEPS);
    }

    @Test
    void fiveCompletions_awardsDedicatedLearner() {
        Set<Badge> badges = Badge.evaluate(5, 75, BeltLevel.YELLOW);
        assertThat(badges).contains(Badge.FIRST_STEPS, Badge.DEDICATED_LEARNER, Badge.YELLOW_BELT);
        assertThat(badges).doesNotContain(Badge.QUALITY_CHAMPION);
    }

    @Test
    void tenCompletions_awardsQualityChampion() {
        Set<Badge> badges = Badge.evaluate(10, 90, BeltLevel.GREEN);
        assertThat(badges).contains(Badge.QUALITY_CHAMPION, Badge.DEDICATED_LEARNER, Badge.FIRST_STEPS);
    }

    @Test
    void perfectScore_awardsPerfectionist() {
        Set<Badge> badges = Badge.evaluate(1, 100, BeltLevel.WHITE);
        assertThat(badges).contains(Badge.PERFECTIONIST, Badge.FIRST_STEPS);
    }

    @Test
    void belowPerfect_noPerfectionist() {
        Set<Badge> badges = Badge.evaluate(3, 99, BeltLevel.WHITE);
        assertThat(badges).doesNotContain(Badge.PERFECTIONIST);
    }

    @Test
    void blackBelt_isCumulative_grantsAllBeltBadges() {
        Set<Badge> badges = Badge.evaluate(12, 100, BeltLevel.BLACK);
        assertThat(badges).contains(
                Badge.YELLOW_BELT, Badge.GREEN_BELT, Badge.BLACK_BELT,
                Badge.PERFECTIONIST, Badge.QUALITY_CHAMPION);
    }

    @Test
    void greenBelt_grantsYellowAndGreenNotBlack() {
        Set<Badge> badges = Badge.evaluate(5, 80, BeltLevel.GREEN);
        assertThat(badges).contains(Badge.YELLOW_BELT, Badge.GREEN_BELT);
        assertThat(badges).doesNotContain(Badge.BLACK_BELT);
    }

    @Test
    void negativeCompletions_treatedAsZero() {
        Set<Badge> badges = Badge.evaluate(-3, null, BeltLevel.WHITE);
        assertThat(badges).isEmpty();
    }
}

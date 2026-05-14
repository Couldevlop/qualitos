package com.openlab.qualitos.quality.ishikawa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IshikawaModeTest {

    @Test
    void sixM_containsExpectedCategories() {
        assertThat(IshikawaMode.SIX_M.allowedCategories())
                .containsExactlyInAnyOrder(
                        CauseCategory.METHODS,
                        CauseCategory.MANPOWER,
                        CauseCategory.MACHINES,
                        CauseCategory.MATERIALS,
                        CauseCategory.MEASUREMENTS,
                        CauseCategory.ENVIRONMENT);
    }

    @Test
    void sevenM_addsManagement() {
        assertThat(IshikawaMode.SEVEN_M.allows(CauseCategory.MANAGEMENT)).isTrue();
        assertThat(IshikawaMode.SEVEN_M.allows(CauseCategory.MONEY)).isFalse();
    }

    @Test
    void eightM_addsMoney() {
        assertThat(IshikawaMode.EIGHT_M.allows(CauseCategory.MONEY)).isTrue();
        assertThat(IshikawaMode.EIGHT_M.allowedCategories()).hasSize(8);
    }

    @Test
    void sixM_doesNotAllowManagement() {
        assertThat(IshikawaMode.SIX_M.allows(CauseCategory.MANAGEMENT)).isFalse();
        assertThat(IshikawaMode.SIX_M.allows(CauseCategory.MONEY)).isFalse();
    }
}

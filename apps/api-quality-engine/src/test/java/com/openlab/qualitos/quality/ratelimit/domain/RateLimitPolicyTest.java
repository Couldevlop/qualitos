package com.openlab.qualitos.quality.ratelimit.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitPolicyTest {

    static final UUID TENANT = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void create_validInputs_ok() {
        RateLimitPolicy p = RateLimitPolicy.create(TENANT, "api.public", 60, 100, NOW);
        assertThat(p.getScope()).isEqualTo("api.public");
        assertThat(p.getWindowSeconds()).isEqualTo(60);
        assertThat(p.getMaxRequests()).isEqualTo(100);
        assertThat(p.isEnabled()).isTrue();
    }

    @Test
    void invalidScope_rejected() {
        assertThatThrownBy(() -> RateLimitPolicy.create(TENANT, "BAD SCOPE", 60, 100, NOW))
                .isInstanceOf(RateLimitPolicyException.class);
        assertThatThrownBy(() -> RateLimitPolicy.create(TENANT, "", 60, 100, NOW))
                .isInstanceOf(RateLimitPolicyException.class);
        assertThatThrownBy(() -> RateLimitPolicy.create(TENANT, null, 60, 100, NOW))
                .isInstanceOf(RateLimitPolicyException.class);
    }

    @Test
    void windowOutOfBounds_rejected() {
        assertThatThrownBy(() -> RateLimitPolicy.create(TENANT, "x", 0, 100, NOW))
                .isInstanceOf(RateLimitPolicyException.class);
        assertThatThrownBy(() -> RateLimitPolicy.create(TENANT, "x", 86401, 100, NOW))
                .isInstanceOf(RateLimitPolicyException.class);
    }

    @Test
    void maxRequestsOutOfBounds_rejected() {
        assertThatThrownBy(() -> RateLimitPolicy.create(TENANT, "x", 60, 0, NOW))
                .isInstanceOf(RateLimitPolicyException.class);
        assertThatThrownBy(() -> RateLimitPolicy.create(TENANT, "x", 60, 2_000_000, NOW))
                .isInstanceOf(RateLimitPolicyException.class);
    }

    @Test
    void update_revalidatesBounds() {
        RateLimitPolicy p = RateLimitPolicy.create(TENANT, "x", 60, 100, NOW);
        p.update(120, 200, false, NOW.plusSeconds(60));
        assertThat(p.getWindowSeconds()).isEqualTo(120);
        assertThat(p.getMaxRequests()).isEqualTo(200);
        assertThat(p.isEnabled()).isFalse();
        assertThatThrownBy(() -> p.update(0, 100, true, NOW))
                .isInstanceOf(RateLimitPolicyException.class);
    }
}

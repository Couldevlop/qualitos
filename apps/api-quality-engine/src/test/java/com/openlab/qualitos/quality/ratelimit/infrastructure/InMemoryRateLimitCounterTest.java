package com.openlab.qualitos.quality.ratelimit.infrastructure;

import com.openlab.qualitos.quality.ratelimit.domain.RateLimitCounter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimitCounterTest {

    static final UUID TENANT = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void increment_returnsIncreasingCount_inSameWindow() {
        InMemoryRateLimitCounter c = new InMemoryRateLimitCounter();
        assertThat(c.incrementAndGet(TENANT, "x", 60, T0).count()).isEqualTo(1);
        assertThat(c.incrementAndGet(TENANT, "x", 60, T0.plusSeconds(10)).count()).isEqualTo(2);
        assertThat(c.incrementAndGet(TENANT, "x", 60, T0.plusSeconds(30)).count()).isEqualTo(3);
    }

    @Test
    void newWindow_resetsCounter() {
        InMemoryRateLimitCounter c = new InMemoryRateLimitCounter();
        c.incrementAndGet(TENANT, "x", 60, T0);
        c.incrementAndGet(TENANT, "x", 60, T0);
        assertThat(c.incrementAndGet(TENANT, "x", 60, T0.plusSeconds(120)).count())
                .isEqualTo(1);
    }

    @Test
    void scopeIsolation_perTenantAndScope() {
        InMemoryRateLimitCounter c = new InMemoryRateLimitCounter();
        UUID other = UUID.randomUUID();
        c.incrementAndGet(TENANT, "x", 60, T0);
        c.incrementAndGet(TENANT, "x", 60, T0);
        assertThat(c.incrementAndGet(other, "x", 60, T0).count()).isEqualTo(1);
        assertThat(c.incrementAndGet(TENANT, "y", 60, T0).count()).isEqualTo(1);
    }

    @Test
    void peek_doesNotIncrement() {
        InMemoryRateLimitCounter c = new InMemoryRateLimitCounter();
        c.incrementAndGet(TENANT, "x", 60, T0);
        RateLimitCounter.Snapshot s1 = c.peek(TENANT, "x", 60, T0);
        RateLimitCounter.Snapshot s2 = c.peek(TENANT, "x", 60, T0);
        assertThat(s1.count()).isEqualTo(1);
        assertThat(s2.count()).isEqualTo(1);
    }

    @Test
    void peek_unknownKey_returnsZero() {
        InMemoryRateLimitCounter c = new InMemoryRateLimitCounter();
        assertThat(c.peek(TENANT, "nope", 60, T0).count()).isZero();
    }

    @Test
    void windowEnd_isAligned() {
        InMemoryRateLimitCounter c = new InMemoryRateLimitCounter();
        // T0 = 10:00:00Z, window 60s ⇒ end = 10:01:00Z
        RateLimitCounter.Snapshot s = c.incrementAndGet(TENANT, "x", 60, T0);
        assertThat(s.windowEnd()).isEqualTo(Instant.parse("2026-05-16T10:01:00Z"));
    }
}

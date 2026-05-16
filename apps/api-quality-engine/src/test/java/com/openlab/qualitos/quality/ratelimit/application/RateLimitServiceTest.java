package com.openlab.qualitos.quality.ratelimit.application;

import com.openlab.qualitos.quality.ratelimit.domain.RateLimitCounter;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitDecision;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicy;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyNotFoundException;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitServiceTest {

    @Mock RateLimitPolicyRepository policies;
    @Mock RateLimitCounter counter;
    @Mock TenantProvider tenantProvider;
    RateLimitService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant WINDOW_END = NOW.plusSeconds(60);
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new RateLimitService(policies, counter, tenantProvider, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
    }

    @Test
    void tryAcquire_noPolicy_unlimited() {
        when(policies.findEnabled(TENANT, "x")).thenReturn(Optional.empty());
        RateLimitDecision d = service.tryAcquire(TENANT, "x");
        assertThat(d.allowed()).isTrue();
        verifyNoInteractions(counter);
    }

    @Test
    void tryAcquire_nullArgs_unlimited() {
        assertThat(service.tryAcquire(null, "x").allowed()).isTrue();
        assertThat(service.tryAcquire(TENANT, null).allowed()).isTrue();
        verifyNoInteractions(counter);
    }

    @Test
    void tryAcquire_underLimit_allows() {
        when(policies.findEnabled(TENANT, "x")).thenReturn(Optional.of(policy(60, 100)));
        when(counter.incrementAndGet(TENANT, "x", 60, NOW))
                .thenReturn(new RateLimitCounter.Snapshot(42, WINDOW_END));
        RateLimitDecision d = service.tryAcquire(TENANT, "x");
        assertThat(d.allowed()).isTrue();
        assertThat(d.limit()).isEqualTo(100);
        assertThat(d.remaining()).isEqualTo(58);
        assertThat(d.resetSeconds()).isEqualTo(60);
    }

    @Test
    void tryAcquire_exactlyAtLimit_allows() {
        when(policies.findEnabled(TENANT, "x")).thenReturn(Optional.of(policy(60, 100)));
        when(counter.incrementAndGet(TENANT, "x", 60, NOW))
                .thenReturn(new RateLimitCounter.Snapshot(100, WINDOW_END));
        RateLimitDecision d = service.tryAcquire(TENANT, "x");
        assertThat(d.allowed()).isTrue();
        assertThat(d.remaining()).isZero();
    }

    @Test
    void tryAcquire_overLimit_denies() {
        when(policies.findEnabled(TENANT, "x")).thenReturn(Optional.of(policy(60, 100)));
        when(counter.incrementAndGet(TENANT, "x", 60, NOW))
                .thenReturn(new RateLimitCounter.Snapshot(101, WINDOW_END));
        RateLimitDecision d = service.tryAcquire(TENANT, "x");
        assertThat(d.allowed()).isFalse();
        assertThat(d.remaining()).isZero();
        assertThat(d.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void peek_doesNotIncrement_andReportsState() {
        when(policies.findEnabled(TENANT, "x")).thenReturn(Optional.of(policy(60, 100)));
        when(counter.peek(TENANT, "x", 60, NOW))
                .thenReturn(new RateLimitCounter.Snapshot(50, WINDOW_END));
        RateLimitDecision d = service.peek(TENANT, "x");
        assertThat(d.allowed()).isTrue();
        assertThat(d.remaining()).isEqualTo(50);
        verify(counter, never()).incrementAndGet(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void upsert_createsThenUpdates() {
        when(policies.findEnabled(eq(TENANT), eq("x"))).thenReturn(Optional.empty());
        when(policies.save(any())).thenAnswer(inv -> {
            RateLimitPolicy p = inv.getArgument(0); p.assignId(ID); return p;
        });
        RateLimitDto.PolicyView v = service.upsert(
                new RateLimitDto.UpsertPolicyRequest("x", 60, 100, true));
        assertThat(v.scope()).isEqualTo("x");
        assertThat(v.enabled()).isTrue();
    }

    @Test
    void upsert_existing_updatesInPlace() {
        RateLimitPolicy existing = policy(60, 100); existing.assignId(ID);
        when(policies.findEnabled(eq(TENANT), eq("x"))).thenReturn(Optional.of(existing));
        when(policies.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RateLimitDto.PolicyView v = service.upsert(
                new RateLimitDto.UpsertPolicyRequest("x", 120, 200, false));
        assertThat(v.windowSeconds()).isEqualTo(120);
        assertThat(v.maxRequests()).isEqualTo(200);
        assertThat(v.enabled()).isFalse();
    }

    @Test
    void list_filtersTenant() {
        when(policies.findAllByTenantId(TENANT)).thenReturn(List.of(policy(60, 100)));
        assertThat(service.list()).hasSize(1);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        RateLimitPolicy foreign = new RateLimitPolicy(ID, UUID.randomUUID(), "x",
                60, 100, true, NOW, NOW);
        when(policies.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(RateLimitPolicyNotFoundException.class);
    }

    @Test
    void delete_callsRepo() {
        RateLimitPolicy p = policy(60, 100); p.assignId(ID);
        when(policies.findById(ID)).thenReturn(Optional.of(p));
        service.delete(ID);
        verify(policies).delete(p);
    }

    private RateLimitPolicy policy(int win, int max) {
        return RateLimitPolicy.create(TENANT, "x", win, max, NOW);
    }
}

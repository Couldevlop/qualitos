package com.openlab.qualitos.quality.apikeys.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant FUTURE = NOW.plusSeconds(86400 * 30);

    @Test
    void issued_isActive_withExpiry() {
        ApiKey k = ApiKey.issued(TENANT, "ci-bot", "abc123de", "bcrypt$...",
                Set.of("audit.read"), FUTURE, ACTOR, NOW);
        assertThat(k.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
        assertThat(k.getExpiresAt()).isEqualTo(FUTURE);
        assertThat(k.hasScope("audit.read")).isTrue();
        assertThat(k.isUsable()).isTrue();
    }

    @Test
    void issued_pastExpiry_rejected() {
        assertThatThrownBy(() -> ApiKey.issued(TENANT, "n", "p", "h",
                Set.of(), NOW.minusSeconds(1), ACTOR, NOW))
                .isInstanceOf(ApiKeyStateException.class);
    }

    @Test
    void revoke_terminal() {
        ApiKey k = sample();
        k.revoke(ACTOR, NOW.plusSeconds(60));
        assertThat(k.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(k.isUsable()).isFalse();
        assertThatThrownBy(() -> k.revoke(ACTOR, NOW)).isInstanceOf(ApiKeyStateException.class);
    }

    @Test
    void rotateSecret_persistsNewHash_invalidatesLastUsed() {
        ApiKey k = sample();
        k.recordUsage(NOW.plusSeconds(60));
        k.rotateSecret("bcrypt$new...", NOW.plusSeconds(120));
        assertThat(k.getHashedSecret()).isEqualTo("bcrypt$new...");
        assertThat(k.getLastUsedAt()).isNull();
    }

    @Test
    void recordUsage_expiredAtBoundary_movesToExpired() {
        ApiKey k = ApiKey.issued(TENANT, "n", "p", "h",
                Set.of(), FUTURE, ACTOR, NOW);
        assertThatThrownBy(() -> k.recordUsage(FUTURE))
                .isInstanceOf(ApiKeyStateException.class);
        assertThat(k.getStatus()).isEqualTo(ApiKeyStatus.EXPIRED);
    }

    @Test
    void expireIfDue_idempotent() {
        ApiKey k = ApiKey.issued(TENANT, "n", "p", "h",
                Set.of(), FUTURE, ACTOR, NOW);
        assertThat(k.expireIfDue(FUTURE.plusSeconds(1))).isTrue();
        assertThat(k.expireIfDue(FUTURE.plusSeconds(2))).isFalse();
    }

    @Test
    void expireIfDue_revoked_noOp() {
        ApiKey k = sample();
        k.revoke(ACTOR, NOW);
        assertThat(k.expireIfDue(FUTURE.plusSeconds(1))).isFalse();
    }

    @Test
    void scopes_orderedAndImmutable() {
        ApiKey k = ApiKey.issued(TENANT, "n", "p", "h",
                Set.of("b", "a", "c"), null, ACTOR, NOW);
        assertThat(k.getScopes()).containsExactly("a", "b", "c");
        assertThatThrownBy(() -> k.getScopes().add("d"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_rejectsBlankFields() {
        assertThatThrownBy(() -> new ApiKey(null, TENANT, "", "p", "h",
                Set.of(), ApiKeyStatus.ACTIVE, NOW, ACTOR, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiKey(null, TENANT, "n", "", "h",
                Set.of(), ApiKeyStatus.ACTIVE, NOW, ACTOR, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiKey(null, TENANT, "n", "p", "",
                Set.of(), ApiKeyStatus.ACTIVE, NOW, ACTOR, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ApiKey sample() {
        return ApiKey.issued(TENANT, "ci-bot", "abc123de", "bcrypt$...",
                Set.of("audit.read"), FUTURE, ACTOR, NOW);
    }
}

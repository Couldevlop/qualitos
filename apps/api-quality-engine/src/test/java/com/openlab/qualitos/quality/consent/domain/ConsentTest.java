package com.openlab.qualitos.quality.consent.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ConsentTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final String HASH = "a".repeat(64);
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void grant_setsStatusGranted_andInvariants() {
        Consent c = grant(null);
        assertThat(c.getStatus()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(c.isActive(NOW)).isTrue();
        assertThat(c.isTerminal()).isFalse();
        assertThat(c.getGrantedAt()).isEqualTo(NOW);
        assertThat(c.getSubjectIdentifierHash()).isEqualTo(HASH);
    }

    @Test
    void grant_withExpiresBefore_throws() {
        assertThatThrownBy(() ->
                Consent.grant(T, HASH, null, "marketing", "v1",
                        ConsentSource.WEB_FORM, null, null, null, U, NOW, NOW.minusSeconds(1)))
                .isInstanceOf(ConsentStateException.class);
    }

    @Test
    void grant_invalidPurposeCode_throws() {
        assertThatThrownBy(() ->
                Consent.grant(T, HASH, null, "BAD CODE", "v1",
                        ConsentSource.WEB_FORM, null, null, null, U, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void grant_invalidPurposeVersion_throws() {
        assertThatThrownBy(() ->
                Consent.grant(T, HASH, null, "marketing", "v 1!",
                        ConsentSource.WEB_FORM, null, null, null, U, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void grant_invalidHash_throws() {
        assertThatThrownBy(() ->
                Consent.grant(T, "not-hex", null, "marketing", "v1",
                        ConsentSource.WEB_FORM, null, null, null, U, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdraw_movesToWithdrawn_terminal() {
        Consent c = grant(null);
        c.withdraw(U, "user request", NOW.plusSeconds(60));
        assertThat(c.getStatus()).isEqualTo(ConsentStatus.WITHDRAWN);
        assertThat(c.isActive(NOW.plusSeconds(60))).isFalse();
        assertThat(c.isTerminal()).isTrue();
        assertThat(c.getWithdrawnAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(c.getWithdrawalReason()).isEqualTo("user request");
    }

    @Test
    void withdraw_alreadyWithdrawn_rejected() {
        Consent c = grant(null);
        c.withdraw(U, "x", NOW.plusSeconds(60));
        assertThatThrownBy(() -> c.withdraw(U, "y", NOW.plusSeconds(120)))
                .isInstanceOf(ConsentStateException.class);
    }

    @Test
    void expireIfDue_pastExpiry_marksExpired() {
        Consent c = grant(NOW.plusSeconds(60));
        c.expireIfDue(NOW.plusSeconds(120));
        assertThat(c.getStatus()).isEqualTo(ConsentStatus.EXPIRED);
        assertThat(c.isActive(NOW.plusSeconds(120))).isFalse();
    }

    @Test
    void expireIfDue_beforeExpiry_noop() {
        Consent c = grant(NOW.plusSeconds(60));
        c.expireIfDue(NOW.plusSeconds(30));
        assertThat(c.getStatus()).isEqualTo(ConsentStatus.GRANTED);
    }

    @Test
    void expireIfDue_alreadyWithdrawn_noop() {
        Consent c = grant(NOW.plusSeconds(60));
        c.withdraw(U, null, NOW.plusSeconds(10));
        c.expireIfDue(NOW.plusSeconds(120));
        assertThat(c.getStatus()).isEqualTo(ConsentStatus.WITHDRAWN); // pas écrasé
    }

    @Test
    void isActive_withExpiryInFuture_true() {
        Consent c = grant(NOW.plusSeconds(60));
        assertThat(c.isActive(NOW.plusSeconds(30))).isTrue();
        assertThat(c.isActive(NOW.plusSeconds(60))).isFalse();
    }

    @Test
    void isActive_noExpiry_true() {
        Consent c = grant(null);
        assertThat(c.isActive(NOW.plusSeconds(86400L * 365 * 10))).isTrue();
    }

    private Consent grant(Instant expiresAt) {
        return Consent.grant(T, HASH, "label", "marketing", "v1",
                ConsentSource.WEB_FORM, "https://evidence", "1.2.3.4", "UA",
                U, NOW, expiresAt);
    }
}

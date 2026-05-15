package com.openlab.qualitos.quality.webhooks;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HmacSignerTest {

    private final HmacSigner signer = new HmacSigner();

    @Test
    void sign_returnsSha256HexPrefixed() {
        String sig = signer.sign("super-secret-12345678", 1717000000000L, "{\"x\":1}");
        assertThat(sig).startsWith("sha256=");
        // sha256 hex = 64 chars after prefix
        assertThat(sig).hasSize("sha256=".length() + 64);
    }

    @Test
    void sign_deterministic_sameInput_sameOutput() {
        String s1 = signer.sign("k", 1L, "p");
        String s2 = signer.sign("k", 1L, "p");
        assertThat(s1).isEqualTo(s2);
    }

    @Test
    void sign_differentTimestamp_differentOutput() {
        String s1 = signer.sign("k", 1L, "p");
        String s2 = signer.sign("k", 2L, "p");
        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    void sign_differentPayload_differentOutput() {
        String s1 = signer.sign("k", 1L, "p1");
        String s2 = signer.sign("k", 1L, "p2");
        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    void sign_differentSecret_differentOutput() {
        String s1 = signer.sign("k1", 1L, "p");
        String s2 = signer.sign("k2", 1L, "p");
        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    void sign_emptySecret_throws() {
        assertThatThrownBy(() -> signer.sign("", 1L, "p"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signer.sign(null, 1L, "p"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sign_nullPayload_treatedAsEmpty() {
        String s1 = signer.sign("k", 1L, null);
        String s2 = signer.sign("k", 1L, "");
        assertThat(s1).isEqualTo(s2);
    }

    @Test
    void verify_validSignature_returnsTrue() {
        long ts = 1717000000000L;
        String sig = signer.sign("k", ts, "{\"x\":1}");
        assertThat(signer.verify("k", ts, "{\"x\":1}", sig)).isTrue();
    }

    @Test
    void verify_tamperedPayload_returnsFalse() {
        long ts = 1717000000000L;
        String sig = signer.sign("k", ts, "{\"x\":1}");
        assertThat(signer.verify("k", ts, "{\"x\":2}", sig)).isFalse();
    }

    @Test
    void verify_tamperedSignature_returnsFalse() {
        long ts = 1717000000000L;
        String sig = signer.sign("k", ts, "{\"x\":1}");
        // Change last char
        String tampered = sig.substring(0, sig.length() - 1)
                + (sig.charAt(sig.length() - 1) == 'a' ? 'b' : 'a');
        assertThat(signer.verify("k", ts, "{\"x\":1}", tampered)).isFalse();
    }

    @Test
    void verify_nullSignature_returnsFalse() {
        assertThat(signer.verify("k", 1L, "p", null)).isFalse();
    }

    @Test
    void verify_wrongLengthSignature_returnsFalse() {
        assertThat(signer.verify("k", 1L, "p", "short")).isFalse();
    }
}

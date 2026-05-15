package com.openlab.qualitos.quality.itsm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCipherTest {

    private SecretCipher cipher;

    @BeforeEach
    void setup() {
        cipher = new SecretCipher("unit-test-passphrase-very-long-okay");
        cipher.init();
    }

    @Test
    void encrypt_then_decrypt_roundTrip() {
        String pt = "ServiceNow-API-Token-xyzzy";
        String ct = cipher.encrypt(pt);
        assertThat(ct).isNotEqualTo(pt).isNotBlank();
        assertThat(cipher.decrypt(ct)).isEqualTo(pt);
    }

    @Test
    void encrypt_isNonDeterministic_acrossCalls() {
        // GCM IV aléatoire ⇒ même plaintext ⇒ ciphertexts différents.
        String ct1 = cipher.encrypt("same");
        String ct2 = cipher.encrypt("same");
        assertThat(ct1).isNotEqualTo(ct2);
        assertThat(cipher.decrypt(ct1)).isEqualTo("same");
        assertThat(cipher.decrypt(ct2)).isEqualTo("same");
    }

    @Test
    void encrypt_null_returnsNull() {
        assertThat(cipher.encrypt(null)).isNull();
    }

    @Test
    void decrypt_null_returnsNull() {
        assertThat(cipher.decrypt(null)).isNull();
    }

    @Test
    void decrypt_tamperedCiphertext_throws() {
        String ct = cipher.encrypt("payload");
        String tampered = ct.substring(0, ct.length() - 4) + "AAAA";
        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void decrypt_shortCiphertext_throws() {
        assertThatThrownBy(() -> cipher.decrypt("c2hvcnQ=")) // "short"
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void differentKeys_cannotDecrypt() {
        SecretCipher other = new SecretCipher("different-passphrase-also-long-yes");
        other.init();
        String ct = cipher.encrypt("secret");
        assertThatThrownBy(() -> other.decrypt(ct))
                .isInstanceOf(RuntimeException.class);
    }
}

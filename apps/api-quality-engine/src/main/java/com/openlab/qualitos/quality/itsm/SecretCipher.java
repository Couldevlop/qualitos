package com.openlab.qualitos.quality.itsm;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement symétrique AES-256-GCM des secrets ITSM stockés en DB.
 *
 * Clé maître : injectée via property {@code qualitos.itsm.crypto.key} (base64
 * ou passphrase ≥ 16 chars). En prod, doit pointer vers Vault Transit
 * (cf. CLAUDE.md §10.2 et §11.2). Pour V1 dev/test, accepte une passphrase
 * dérivée par SHA-256 — non-prod.
 *
 * Format ciphertext : base64( IV(12 bytes) || ciphertext+tag(GCM) ).
 */
@Component
public class SecretCipher {

    private static final int IV_LEN = 12;     // GCM recommandé
    private static final int TAG_BITS = 128;  // tag d'authenticité GCM

    private final String rawKey;
    private SecretKey key;
    private final SecureRandom rng = new SecureRandom();

    public SecretCipher(@Value("${qualitos.itsm.crypto.key:dev-only-passphrase-change-in-prod-please}") String rawKey) {
        this.rawKey = rawKey;
    }

    @PostConstruct
    void init() {
        // SHA-256 → 32 octets pour AES-256. Pour Vault Transit, on remplacera cette
        // dérivation par un appel au backend Transit (encrypt/decrypt par alias).
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] derived = sha.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(derived, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialise SecretCipher", e);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            rng.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failure", e);
        }
    }

    public String decrypt(String cipherB64) {
        if (cipherB64 == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(cipherB64);
            if (all.length < IV_LEN + 16) throw new IllegalArgumentException("ciphertext too short");
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            byte[] ct = new byte[all.length - IV_LEN];
            System.arraycopy(all, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failure", e);
        }
    }
}

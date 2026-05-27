package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedFileSigningKeyProviderTest {

  private static final Function<SignatureAlgorithm, KeyMaterial> KEYGEN =
      algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair();

  private static byte[] aesKey() {
    byte[] k = new byte[32];
    new SecureRandom().nextBytes(k);
    return k;
  }

  @Test
  void generatesPersistsAndReloadsSameKey(@TempDir Path dir) {
    byte[] key = aesKey();
    EncryptedFileSigningKeyProvider first =
        new EncryptedFileSigningKeyProvider(dir, key, "platform-v1", KEYGEN);
    KeyMaterial generated = first.signingKey(SignatureAlgorithm.ML_DSA_65);

    // A fresh instance over the same dir + AES key must reload the same key pair.
    EncryptedFileSigningKeyProvider second =
        new EncryptedFileSigningKeyProvider(dir, key, "platform-v1", KEYGEN);
    KeyMaterial reloaded = second.signingKey(SignatureAlgorithm.ML_DSA_65);

    assertThat(reloaded.publicKey()).isEqualTo(generated.publicKey());
    assertThat(reloaded.privateKey()).isEqualTo(generated.privateKey());
  }

  @Test
  void wrongAesKeyCannotDecrypt(@TempDir Path dir) {
    new EncryptedFileSigningKeyProvider(dir, aesKey(), "platform-v1", KEYGEN)
        .signingKey(SignatureAlgorithm.ED25519);

    EncryptedFileSigningKeyProvider attacker =
        new EncryptedFileSigningKeyProvider(dir, aesKey(), "platform-v1", KEYGEN);

    assertThatThrownBy(() -> attacker.signingKey(SignatureAlgorithm.ED25519))
        .isInstanceOf(CryptoException.class);
  }

  @Test
  void trustedPublicKeyReturnsNullForUnknownRef(@TempDir Path dir) {
    EncryptedFileSigningKeyProvider provider =
        new EncryptedFileSigningKeyProvider(dir, aesKey(), "platform-v1", KEYGEN);

    assertThat(provider.trustedPublicKey(SignatureAlgorithm.ED25519, "platform-v2")).isNull();
    assertThat(provider.trustedPublicKey(SignatureAlgorithm.ED25519, "platform-v1")).isNotEmpty();
  }

  @Test
  void rejectsNon256BitAesKey(@TempDir Path dir) {
    assertThatThrownBy(() ->
        new EncryptedFileSigningKeyProvider(dir, new byte[16], "platform-v1", KEYGEN))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

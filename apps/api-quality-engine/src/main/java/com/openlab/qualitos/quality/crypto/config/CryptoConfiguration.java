package com.openlab.qualitos.quality.crypto.config;

import com.openlab.qualitos.crypto.application.CryptoSuiteConfig;
import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.domain.port.SigningKeyProvider;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.EncryptedFileSigningKeyProvider;
import com.openlab.qualitos.crypto.infrastructure.InMemorySigningKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/**
 * Wires the crypto-agility lib (ADR 0011) into the quality engine.
 *
 * <p>Signing-key source is chosen at runtime, no profile coupling:
 * <ul>
 *   <li>property {@code qualitos.crypto.key-dir} set → {@link EncryptedFileSigningKeyProvider}
 *       (dev/on-prem run; AES key via {@code qualitos.crypto.aes-key-base64}, sourced
 *       from an env var — never a clear secret, §18.2-3);</li>
 *   <li>otherwise → {@link InMemorySigningKeyProvider} (tests / ephemeral runs).</li>
 * </ul>
 * Prod swaps in a Vault Transit adapter without touching consumers.
 */
@Configuration
public class CryptoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(CryptoConfiguration.class);

  private static final Function<SignatureAlgorithm, KeyMaterial> KEY_GENERATOR =
      algorithm -> new BouncyCastleSignatureProvider(algorithm).generateKeyPair();

  @Bean
  CryptoSuiteConfig cryptoSuiteConfig() {
    return new CryptoSuiteConfig();
  }

  @Bean
  SigningKeyProvider signingKeyProvider(Environment env) {
    String keyRef = env.getProperty("qualitos.crypto.key-ref", "platform-v1");
    String keyDir = env.getProperty("qualitos.crypto.key-dir");
    if (keyDir != null && !keyDir.isBlank()) {
      String aesB64 = env.getProperty("qualitos.crypto.aes-key-base64");
      if (aesB64 == null || aesB64.isBlank()) {
        throw new IllegalStateException(
            "qualitos.crypto.key-dir set but qualitos.crypto.aes-key-base64 missing");
      }
      byte[] aesKey = Base64.getDecoder().decode(aesB64);
      log.info("Signing keys: EncryptedFileSigningKeyProvider dir={} keyRef={}", keyDir, keyRef);
      return new EncryptedFileSigningKeyProvider(Path.of(keyDir), aesKey, keyRef, KEY_GENERATOR);
    }
    log.info("Signing keys: InMemorySigningKeyProvider keyRef={} (ephemeral)", keyRef);
    return new InMemorySigningKeyProvider(keyRef, KEY_GENERATOR);
  }

  @Bean
  HybridSignatureService hybridSignatureService(CryptoSuiteConfig suites, SigningKeyProvider keys) {
    List<SignatureProvider> providers = List.of(
        new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519),
        new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65));
    return new HybridSignatureService(suites, providers, keys, Clock.systemUTC());
  }
}

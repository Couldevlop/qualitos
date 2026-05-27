package com.openlab.qualitos.crypto.application;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.CryptoSuite;
import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.domain.port.SigningKeyProvider;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Produces and verifies hybrid signature envelopes (ADR 0011 §3).
 *
 * <p>For a given context (e.g. {@code "audit-report"}, {@code "blockchain-block"})
 * it resolves the crypto suite, signs the message with every signature algorithm
 * of that suite (primary + hybrid companion), and packages all parts in a
 * {@link SignatureEnvelope}. Verification requires <b>every</b> part to validate
 * against the platform's <b>trusted</b> public key — an attacker embedding their
 * own public key cannot pass (key pinning via {@link SigningKeyProvider}).
 */
public class HybridSignatureService {

  private final CryptoSuiteConfig suites;
  private final Map<SignatureAlgorithm, SignatureProvider> providers;
  private final SigningKeyProvider keys;
  private final Clock clock;

  public HybridSignatureService(CryptoSuiteConfig suites,
                                Collection<SignatureProvider> signatureProviders,
                                SigningKeyProvider keys,
                                Clock clock) {
    this.suites = suites;
    this.keys = keys;
    this.clock = clock;
    this.providers = new EnumMap<>(SignatureAlgorithm.class);
    for (SignatureProvider p : signatureProviders) {
      this.providers.put(p.algorithm(), p);
    }
  }

  /** Sign {@code message} with the hybrid suite resolved for {@code context}. */
  public SignatureEnvelope sign(String context, byte[] message) {
    CryptoSuite suite = suites.resolveFor(context)
        .orElseThrow(() -> new CryptoException("no crypto suite for context: " + context));

    String keyRef = keys.currentKeyRef(suite.primarySignature());
    List<SignatureEnvelope.Part> parts = new ArrayList<>(2);
    for (SignatureAlgorithm algo : signatureAlgorithms(suite)) {
      SignatureProvider provider = provider(algo);
      KeyMaterial km = keys.signingKey(algo);
      byte[] signature = provider.sign(km.privateKey(), message);
      parts.add(new SignatureEnvelope.Part(algo, km.publicKey(), signature));
    }
    return new SignatureEnvelope(
        SignatureEnvelope.CURRENT_VERSION, suite.name(), keyRef, Instant.now(clock), parts);
  }

  /**
   * Verify every part of {@code envelope} over {@code message} against the
   * trusted platform public keys pinned by {@code envelope.keyRef()}.
   *
   * @return true only if all parts pin to a trusted key AND validate.
   */
  public boolean verify(byte[] message, SignatureEnvelope envelope) {
    if (envelope.parts().isEmpty()) {
      return false;
    }
    for (SignatureEnvelope.Part part : envelope.parts()) {
      byte[] trusted = keys.trustedPublicKey(part.algorithm(), envelope.keyRef());
      if (trusted == null || !Arrays.equals(trusted, part.publicKey())) {
        return false; // unknown key ref or key-pinning mismatch
      }
      SignatureProvider provider = providers.get(part.algorithm());
      if (provider == null
          || !provider.verify(part.publicKey(), message, part.signature())) {
        return false;
      }
    }
    return true;
  }

  private static List<SignatureAlgorithm> signatureAlgorithms(CryptoSuite suite) {
    List<SignatureAlgorithm> algos = new ArrayList<>(2);
    algos.add(suite.primarySignature());
    if (suite.hybridSignature() != null) {
      algos.add(suite.hybridSignature());
    }
    return algos;
  }

  private SignatureProvider provider(SignatureAlgorithm algo) {
    SignatureProvider p = providers.get(algo);
    if (p == null) {
      throw new CryptoException("no SignatureProvider registered for " + algo);
    }
    return p;
  }
}

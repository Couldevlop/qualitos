package com.openlab.qualitos.core.config;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.security.Security;

/**
 * TLS hybride post-quantique — activation du provider BCJSSE et du groupe nommé
 * hybride {@code X25519MLKEM768} (CLAUDE.md §11.4 : « TLS hybride X25519 +
 * ML-KEM-768 sur les flux entrants », ADR 0015).
 *
 * <p><b>OFF par défaut.</b> Cette configuration n'est chargée que sous le profil
 * Spring {@code tls} (cf. {@code application-tls.yml}). Sans ce profil, api-core
 * démarre exactement comme avant : aucun provider n'est enregistré, le serveur
 * écoute en clair (ou via SunJSSE si TLS est configuré ailleurs), et aucun test
 * existant n'est impacté.
 *
 * <p>Au démarrage (profil {@code tls}) :
 * <ol>
 *   <li>enregistre {@link BouncyCastleProvider} (JCA — ML-KEM/ML-DSA) si absent ;</li>
 *   <li>enregistre {@link BouncyCastleJsseProvider} en tête de liste (JSSE — le
 *       SunJSSE ignore {@code X25519MLKEM768}) ;</li>
 *   <li>positionne {@code jdk.tls.namedGroups=X25519MLKEM768,X25519} si la JVM
 *       ne l'a pas déjà reçu via {@code -D} (X25519 en repli pour les clients
 *       non-PQ ; cf. suite {@code legacy-classical} du CBOM).</li>
 * </ol>
 *
 * <p>Le {@code provider: BCJSSE} déclaré dans {@code application-tls.yml} sur
 * {@code server.ssl} ne fonctionne que si le provider est enregistré <em>avant</em>
 * que le connecteur Tomcat ne démarre — d'où le {@link PostConstruct} sur un bean
 * de configuration, instancié très tôt dans le cycle de vie du contexte.
 */
@Configuration
@Profile("tls")
public class HybridTlsConfig {

  private static final Logger log = LoggerFactory.getLogger(HybridTlsConfig.class);

  /** Groupe hybride standardisé (RFC 9794, codepoint 0x11EC) ; X25519 en repli. */
  static final String HYBRID_NAMED_GROUPS = "X25519MLKEM768,X25519";

  @PostConstruct
  void registerBouncyCastleTls() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
      log.info("TLS hybride : provider JCA '{}' enregistré", BouncyCastleProvider.PROVIDER_NAME);
    }
    if (Security.getProvider(BouncyCastleJsseProvider.PROVIDER_NAME) == null) {
      // Tête de liste : on veut BCJSSE (qui connaît X25519MLKEM768), pas SunJSSE.
      Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);
      log.info("TLS hybride : provider JSSE '{}' enregistré en tête",
          BouncyCastleJsseProvider.PROVIDER_NAME);
    }

    // Ne pas écraser une valeur fournie explicitement via -Djdk.tls.namedGroups.
    String existing = System.getProperty("jdk.tls.namedGroups");
    if (existing == null || existing.isBlank()) {
      System.setProperty("jdk.tls.namedGroups", HYBRID_NAMED_GROUPS);
      log.info("TLS hybride : jdk.tls.namedGroups positionné à '{}'", HYBRID_NAMED_GROUPS);
    } else {
      log.info("TLS hybride : jdk.tls.namedGroups déjà fourni ('{}'), conservé", existing);
    }
  }
}

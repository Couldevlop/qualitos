package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.CryptoException;

import java.security.Provider;
import java.security.Security;

/**
 * Enregistre dynamiquement le provider Bouncy Castle SANS référence de compilation
 * à sa classe concrète, pour préserver la <b>crypto-agility</b> (ADR 0011) :
 *
 * <ul>
 *   <li>build par défaut → {@code bcprov-jdk18on} ({@code BouncyCastleProvider}, « BC ») ;</li>
 *   <li>profil Maven {@code fips} → {@code bc-fips} ({@code BouncyCastleFipsProvider},
 *       « BCFIPS »).</li>
 * </ul>
 *
 * <p>Les deux artefacts partagent les packages {@code org.bouncycastle.*} et ne peuvent
 * donc pas coexister sur le classpath. En référençant le provider <b>par nom de classe</b>
 * (réflexion) plutôt qu'en {@code import}, le MÊME code source compile et s'exécute contre
 * l'un OU l'autre — c'est le profil de build qui fournit le bon jar. Le nom JCA du provider
 * (« BC » / « BCFIPS ») est lu sur l'instance, jamais codé en dur.
 *
 * <p>Classe configurable via la propriété système {@code qualitos.crypto.bc-provider-class}
 * (défaut : provider non-FIPS). Tout le reste de la crypto passe par l'API JCA standard,
 * donc agnostique au provider.
 */
final class BcProviderRegistrar {

  /** Provider non-FIPS par défaut (bcprov-jdk18on). */
  static final String DEFAULT_PROVIDER_CLASS =
      "org.bouncycastle.jce.provider.BouncyCastleProvider";

  /** Propriété système surchargeant la classe provider (ex. profil fips → BCFIPS). */
  static final String PROVIDER_CLASS_PROPERTY = "qualitos.crypto.bc-provider-class";

  private static volatile String registeredName;

  private BcProviderRegistrar() {}

  /**
   * Enregistre le provider BC (idempotent) et renvoie son nom JCA (« BC » / « BCFIPS »).
   * Thread-safe : double-checked sur {@link #registeredName}.
   */
  static String ensureRegistered() {
    String name = registeredName;
    if (name != null) {
      return name;
    }
    synchronized (BcProviderRegistrar.class) {
      if (registeredName != null) {
        return registeredName;
      }
      String className =
          System.getProperty(PROVIDER_CLASS_PROPERTY, DEFAULT_PROVIDER_CLASS);
      try {
        Provider provider =
            (Provider) Class.forName(className).getDeclaredConstructor().newInstance();
        if (Security.getProvider(provider.getName()) == null) {
          Security.addProvider(provider);
        }
        registeredName = provider.getName();
        return registeredName;
      } catch (ReflectiveOperationException | ClassCastException e) {
        throw new CryptoException(
            "Impossible d'enregistrer le provider Bouncy Castle '" + className
                + "' (vérifier la dépendance du profil de build)", e);
      }
    }
  }
}

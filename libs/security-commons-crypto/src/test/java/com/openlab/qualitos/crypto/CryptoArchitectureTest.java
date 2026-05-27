package com.openlab.qualitos.crypto;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Crypto-agility boundary (ADR 0011). The domain layer (ports + models) must
 * stay free of any concrete crypto provider, so swapping Bouncy Castle for
 * bc-fips never touches consumers.
 */
class CryptoArchitectureTest {

  private static final JavaClasses CRYPTO_CLASSES =
      new ClassFileImporter().importPackages("com.openlab.qualitos.crypto");

  @Test
  void domainHasNoBouncyCastleDependency() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("org.bouncycastle..")
        .check(CRYPTO_CLASSES);
  }

  @Test
  void domainDoesNotDependOnOuterLayers() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..application..")
        .check(CRYPTO_CLASSES);
  }
}

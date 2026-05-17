package com.openlab.qualitos.industry.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the Hexagonal / Clean Architecture dependency direction for
 * {@code libs/industry-commons}:
 *
 * <pre>
 *   presentation ──► application ──► domain ◄── infrastructure
 * </pre>
 */
@AnalyzeClasses(
    packages = "com.openlab.qualitos.industry",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class CleanArchitectureTest {

  @ArchTest
  static final ArchRule layered = Architectures.layeredArchitecture()
      .consideringAllDependencies()
      .layer("domain").definedBy("com.openlab.qualitos.industry.domain..")
      .layer("application").definedBy("com.openlab.qualitos.industry.application..")
      .layer("infrastructure").definedBy("com.openlab.qualitos.industry.infrastructure..")
      .layer("presentation").definedBy("com.openlab.qualitos.industry.presentation..")
      .whereLayer("presentation").mayNotBeAccessedByAnyLayer()
      .whereLayer("infrastructure").mayOnlyBeAccessedByLayers("presentation")
      .whereLayer("application").mayOnlyBeAccessedByLayers("presentation", "infrastructure")
      .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "infrastructure", "presentation");

  @ArchTest
  static final ArchRule domainHasNoSpring = noClasses()
      .that().resideInAPackage("com.openlab.qualitos.industry.domain..")
      .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");

  @ArchTest
  static final ArchRule domainHasNoYaml = noClasses()
      .that().resideInAPackage("com.openlab.qualitos.industry.domain..")
      .should().dependOnClassesThat().resideInAPackage("org.yaml..");
}

package com.openlab.qualitos.iot.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.openlab.qualitos.iot",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class CleanArchitectureTest {

  @ArchTest
  static final ArchRule layered = Architectures.layeredArchitecture()
      .consideringAllDependencies()
      .layer("domain").definedBy("com.openlab.qualitos.iot.domain..")
      .layer("application").definedBy("com.openlab.qualitos.iot.application..")
      .layer("infrastructure").definedBy("com.openlab.qualitos.iot.infrastructure..")
      .layer("presentation").definedBy("com.openlab.qualitos.iot.presentation..")
      .whereLayer("presentation").mayNotBeAccessedByAnyLayer()
      .whereLayer("infrastructure").mayOnlyBeAccessedByLayers("presentation")
      .whereLayer("application").mayOnlyBeAccessedByLayers("presentation", "infrastructure")
      .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "infrastructure", "presentation");

  @ArchTest
  static final ArchRule domainHasNoSpring = noClasses()
      .that().resideInAPackage("com.openlab.qualitos.iot.domain..")
      .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");

  @ArchTest
  static final ArchRule domainHasNoJpa = noClasses()
      .that().resideInAPackage("com.openlab.qualitos.iot.domain..")
      .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

  @ArchTest
  static final ArchRule applicationHasNoSpring = noClasses()
      .that().resideInAPackage("com.openlab.qualitos.iot.application..")
      .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
}

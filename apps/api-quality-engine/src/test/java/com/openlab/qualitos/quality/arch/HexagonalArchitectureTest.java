package com.openlab.qualitos.quality.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit guardrails — Clean / Hexagonal architecture invariants for P5.
 *
 * Domain layer must NOT depend on infrastructure or web (Spring, JPA, Jakarta).
 * Application layer must NOT depend on infrastructure or web.
 */
@AnalyzeClasses(packages = "com.openlab.qualitos.quality")
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule p5_dashboards_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..dashboards.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..dashboards.infrastructure..",
                "..dashboards.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..")
            .because("Domain (dashboards) must stay framework-free for hexagonal arch (CLAUDE.md P5).");

    @ArchTest
    static final ArchRule p5_dashboards_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..dashboards.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..dashboards.infrastructure..",
                "..dashboards.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..")
            .because("Application layer (dashboards) must depend on ports only.");

    @ArchTest
    static final ArchRule export_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..dashboards.export.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..dashboards.export.infrastructure..",
                "..dashboards.export.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..",
                "org.apache.pdfbox..",
                "com.google.zxing..")
            .because("Domain (dashboard export) must stay framework-free for hexagonal arch.");

    @ArchTest
    static final ArchRule export_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..dashboards.export.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..dashboards.export.infrastructure..",
                "..dashboards.export.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..",
                "org.apache.pdfbox..",
                "com.google.zxing..")
            .because("Application layer (dashboard export) must depend on ports only.");

    @ArchTest
    static final ArchRule p5_marketplace_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..marketplace.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..marketplace.infrastructure..",
                "..marketplace.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..")
            .because("Domain (marketplace) must stay framework-free.");

    @ArchTest
    static final ArchRule p5_marketplace_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..marketplace.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..marketplace.infrastructure..",
                "..marketplace.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..")
            .because("Application layer (marketplace) must depend on ports only.");
}

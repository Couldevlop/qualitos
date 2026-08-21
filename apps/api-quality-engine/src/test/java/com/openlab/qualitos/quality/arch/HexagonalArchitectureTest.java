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

    @ArchTest
    static final ArchRule product_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..product.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product.infrastructure..",
                "..product.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..")
            .because("Domain (product) must stay framework-free for hexagonal arch.");

    @ArchTest
    static final ArchRule product_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..product.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product.infrastructure..",
                "..product.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..")
            .because("Application layer (product) must depend on ports only.");

    @ArchTest
    static final ArchRule control_plan_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..controlplan.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..controlplan.infrastructure..",
                "..controlplan.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..")
            .because("Domain (control plan) must stay framework-free for hexagonal arch.");

    @ArchTest
    static final ArchRule control_plan_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..controlplan.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..controlplan.infrastructure..",
                "..controlplan.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..")
            .because("Application layer (control plan) must depend on ports only.");

    @ArchTest
    static final ArchRule revision_requests_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..revisionrequests.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..revisionrequests.infrastructure..",
                "..revisionrequests.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..")
            .because("Domain (revision requests) must stay framework-free for hexagonal arch.");

    @ArchTest
    static final ArchRule revision_requests_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..revisionrequests.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..revisionrequests.infrastructure..",
                "..revisionrequests.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..")
            .because("Application layer (revision requests) must depend on ports only.");
}

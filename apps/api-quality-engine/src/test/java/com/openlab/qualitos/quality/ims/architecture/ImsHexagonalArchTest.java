package com.openlab.qualitos.quality.ims.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Vérifie que le module IMS suit Clean Architecture / Hexagonal :
 * - domain ne dépend d'AUCUN framework (Spring, JPA, Jakarta servlet).
 * - application ne dépend pas de l'infrastructure ni du presentation.
 * - infrastructure ne dépend pas de presentation.
 */
class ImsHexagonalArchTest {

    private static JavaClasses imsClasses;

    @BeforeAll
    static void load() {
        imsClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.openlab.qualitos.quality.ims");
    }

    @Test
    void domain_isFreeOfSpringJpaServlet() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..ims.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.servlet..",
                        "org.hibernate.."
                )
                .because("Domain layer must remain a pure POJO (Clean Architecture).");
        rule.check(imsClasses);
    }

    @Test
    void application_doesNotDependOnInfrastructureOrPresentation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..ims.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..ims.infrastructure..",
                        "..ims.presentation.."
                )
                .because("Application use cases depend only on domain (ports), not on infra/REST.");
        rule.check(imsClasses);
    }

    @Test
    void infrastructure_doesNotDependOnPresentation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..ims.infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..ims.presentation..")
                .because("Infrastructure is a peer of presentation, not its consumer.");
        rule.check(imsClasses);
    }

    @Test
    void presentation_doesNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..ims.presentation..")
                .should().dependOnClassesThat().resideInAPackage("..ims.infrastructure..")
                .because("Presentation goes through application use cases, not directly to JPA repositories.");
        rule.check(imsClasses);
    }
}

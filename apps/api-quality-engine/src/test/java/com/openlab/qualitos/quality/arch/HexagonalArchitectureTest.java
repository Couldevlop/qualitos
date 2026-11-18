package com.openlab.qualitos.quality.arch;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Les invariants d'architecture du moteur, tenus par le {@code build}.
 *
 * <p><b>Les regles de la premiere section sont UNIVERSELLES</b> : elles portent sur
 * {@code ..domain..} et {@code ..application..} en general, et non sur une liste de
 * modules. C'est le point : pendant longtemps ce fichier n'enoncait que des regles
 * nommant un paquet precis, et sur les 54 paquets {@code domain} du moteur, 11
 * seulement etaient couverts. Un module qui oubliait d'ajouter ses deux regles
 * n'etait jamais verifie -- l'invariant tenait par la memoire des developpeurs, pas
 * par le {@code build} (ADR 0128).
 *
 * <p>Les regles de la seconde section restent nommees, parce qu'elles sont PLUS
 * STRICTES que la regle universelle : elles interdisent en plus un outil precis
 * (PDFBox dans l'instantane 8D, ZXing dans l'export) ou un module voisin. Elles ne
 * remplacent pas la regle generale, elles la durcissent la ou il le faut.
 *
 * <p>Les derogations sont nommees une par une, avec leur raison. Une derogation qui
 * reste sans raison ecrite est une regle qu'on a renonce a tenir.
 */
/*
 * La portee est le code de PRODUCTION, et c'est volontaire. Un banc assemble
 * legitimement un adaptateur reel pour l'eprouver, et peut emprunter le controleur
 * d'un autre module pour eprouver un filtre transverse -- StepUpEndpointTest se sert
 * du controleur des plans de surveillance pour verifier le second facteur. Mesurer
 * les bancs ferait echouer les regles sur des dependances qui sont precisement le
 * travail d'un banc, et la seule issue serait d'affaiblir les regles.
 */
@AnalyzeClasses(packages = "com.openlab.qualitos.quality",
                importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {


    // =====================================================================
    // 1. LES REGLES UNIVERSELLES -- elles portent sur TOUS les modules
    // =====================================================================

    /**
     * Aucun domaine ne connait de cadre technique, dans aucun module.
     *
     * <p>C'est la regle qui fait tenir tout le reste : un domaine sans framework se
     * teste sans base, sans contexte Spring et sans serveur, donc vite et partout.
     * Des qu'une entite JPA porte les regles, on ne peut plus verifier une regle sans
     * demarrer une base.
     *
     * <p>Derogation : {@code academy}. Ses neuf entites de domaine SONT des entites
     * JPA et six de ses services applicatifs sont annotes Spring. Ce module porte les
     * noms de couches sans en respecter la regle ; le remettre d'aplomb demande de
     * separer entites de persistance, entites de domaine et convertisseurs, ce qui est
     * un chantier a part entiere et non un effet de bord de cette decision.
     */
    @ArchTest
    static final ArchRule tout_domaine_se_tient_sans_framework =
        noClasses()
            .that().resideInAPackage("..domain..")
            .and().resideOutsideOfPackage("..academy..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..",
                "com.fasterxml.jackson..")
            .because("Un domaine sans framework se verifie sans base ni contexte. "
                + "Cette regle porte sur TOUS les modules, et non sur une liste.");

    /**
     * Aucun domaine ne regarde vers l'exterieur de son hexagone.
     *
     * <p>Le sens des dependances est le fond de l'affaire : l'infrastructure implemente
     * les ports que le domaine declare, jamais l'inverse. Un domaine qui connaitrait son
     * adaptateur ne serait plus remplacable, et c'est precisement ce qu'on achete en
     * ecrivant des ports.
     *
     * <p>Derogation : {@code academy}, pour la meme cause que ci-dessus -- ses entites
     * de domaine sont des entites JPA, et deux d'entre elles nomment un convertisseur
     * de leur propre couche infrastructure. C'est le symptome, pas une faute de plus.
     */
    @ArchTest
    static final ArchRule aucun_domaine_ne_regarde_vers_l_exterieur =
        noClasses()
            .that().resideInAPackage("..domain..")
            .and().resideOutsideOfPackage("..academy..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..web..")
            .because("L'infrastructure implemente les ports du domaine, jamais l'inverse.");

    /**
     * Aucune couche applicative ne touche un cadre technique ni la couche web.
     *
     * <p>Un cas d'usage qui depend de Spring ne se teste plus qu'avec un contexte ;
     * un cas d'usage qui depend du web ne se reutilise plus depuis un autre appelant
     * -- un travail planifie, un consommateur de file, un autre module.
     *
     * <p>UNE EXCEPTION RAISONNEE : {@code org.springframework.transaction.annotation}
     * est admis ici, et nulle part ailleurs. Decider qu'une operation doit etre atteinte
     * ou defaite ENTIEREMENT est une decision du CAS D'USAGE, pas de la persistance :
     * {@code ApiKeyService.rotate} le dit en toutes lettres -- emettre une cle peut
     * echouer apres l'avoir revoquee, et laisser le client sans aucune cle valide.
     * Pousser cette frontiere dans l'adaptateur la retirerait a celui qui sait ou elle
     * doit tomber. Ce qui est interdit, c'est que le domaine la connaisse.
     *
     * <p>Derogations : {@code academy} (voir ci-dessus) et {@code marketplace}, dont
     * {@code ManifestScanner} lit un manifeste avec Jackson -- un analyseur de fichier,
     * qui serait a sa place en infrastructure derriere un port. Ces deux-la restent des
     * chantiers, et chacun est nomme ici avec sa raison : une derogation sans raison
     * ecrite est une regle qu'on a renonce a tenir.
     */
    @ArchTest
    static final ArchRule toute_application_ne_connait_que_ses_ports =
        noClasses()
            .that().resideInAPackage("..application..")
            .and().resideOutsideOfPackages("..academy..", "..marketplace..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..infrastructure..",
                "..web..",
                "org.springframework.beans..",
                "org.springframework.boot..",
                "org.springframework.context..",
                "org.springframework.data..",
                "org.springframework.http..",
                "org.springframework.security..",
                "org.springframework.stereotype..",
                "org.springframework.web..",
                "jakarta.persistence..",
                "org.hibernate..",
                "com.fasterxml.jackson..")
            .because("Un cas d'usage ne connait que ses ports : c'est ce qui le rend "
                + "verifiable sans contexte et reutilisable hors du web.");

    /**
     * LE SOCLE NE DEPEND D'AUCUN MODULE.
     *
     * <p>{@code common} porte ce que tout le monde utilise -- le client du jeton,
     * l'utilisateur courant, le filtre, le second facteur : soixante-dix-huit modules en
     * dependent. Et il dependait lui-meme de soixante-six modules, a cause d'un seul
     * fichier : {@code GlobalExceptionHandler} importait l'exception de chaque module
     * pour savoir quel statut HTTP lui repondre. Soixante-six des soixante-douze cycles
     * entre modules du moteur n'existaient que par la, et tout module neuf devait
     * MODIFIER un fichier partage pour que ses refus sortent correctement (ADR 0128).
     *
     * <p>C'est la regle la plus facile a casser sans y penser : il suffit d'ajouter un
     * gestionnaire « pour aller plus vite ». D'ou cette garde.
     */
    @ArchTest
    static final ArchRule le_socle_ne_depend_d_aucun_module =
        noClasses()
            .that().resideInAPackage("..quality.common..")
            .should().dependOnClassesThat(
                JavaClass.Predicates.resideInAPackage("com.openlab.qualitos.quality..")
                    .and(DescribedPredicate.not(JavaClass.Predicates.resideInAPackage(
                        "com.openlab.qualitos.quality.common.."))))
            .because("Soixante-dix-huit modules dependent du socle : s'il depend d'eux, "
                + "le graphe se referme et plus rien ne se compile ni ne se teste isolement.");

    // =====================================================================
    // 2. LES REGLES NOMMEES -- plus strictes que la regle universelle
    // =====================================================================

    @ArchTest
    static final ArchRule moc_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..change.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..",
                "org.apache.poi..")
            .because("Domain (gestion des changements, ADR 0087) : règles de score, de classification "
                + "et de paramétrage sans framework.");

    @ArchTest
    static final ArchRule capa_effectiveness_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..capa.effectiveness.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..capa.effectiveness.infrastructure..",
                "..capa.effectiveness.application..",
                "..capa.effectiveness.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..",
                "com.fasterxml.jackson..")
            .because("Domain (efficacite CAPA, ADR 0108) : le verdict, son etape et les regles "
                + "d'ecriture d'une evaluation se tiennent sans framework. Ces regles sont "
                + "appelees depuis CapaService, qui est annote Spring : si le domaine se mettait "
                + "a dependre du contexte, elles ne seraient plus verifiables isolement.");

    @ArchTest
    static final ArchRule capa_effectiveness_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..capa.effectiveness.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..capa.effectiveness.infrastructure..",
                "..capa.effectiveness.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..",
                "com.fasterxml.jackson..")
            .because("Application (efficacite CAPA) ne connait que ses ports. C'est la raison pour "
                + "laquelle EffectivenessBoardService est declare dans une @Configuration plutot "
                + "qu'annote @Service, et pourquoi 'evaluable' est une composante du DTO et non "
                + "une methode annotee Jackson.");

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

    @ArchTest
    static final ArchRule ideas_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..ideas.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..ideas.infrastructure..",
                "..ideas.web..",
                "..circle..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..")
            .because("Le domaine des idées reste sans framework, et ignore le module cercle "
                   + "dont il ne fait que réutiliser les lignes (CLAUDE.md P5).");

    @ArchTest
    static final ArchRule ideas_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..ideas.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..ideas.infrastructure..",
                "..ideas.web..",
                "..circle..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..")
            .because("La couche application des idées ne dépend que de ports.");

    @ArchTest
    static final ArchRule eightd_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..nonconformity.eightd.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..nonconformity.eightd.infrastructure..",
                "..nonconformity.eightd.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..",
                "org.apache.pdfbox..",
                "com.google.zxing..",
                "com.fasterxml.jackson..")
            .because("L'instantané figé d'un rapport 8D est une donnée de preuve : il ne doit "
                   + "dépendre ni de PDFBox, ni de Jackson, ni de JPA, sous peine de changer "
                   + "de forme avec eux (CLAUDE.md P5).");

    @ArchTest
    static final ArchRule eightd_application_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..nonconformity.eightd.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..nonconformity.eightd.infrastructure..",
                "..nonconformity.eightd.web..",
                "..capa..",
                "..ishikawa..",
                "..fivewhys..",
                "..risk..",
                "..controlplan..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..",
                "org.apache.pdfbox..",
                "com.fasterxml.jackson..")
            .because("Le cas d'usage 8D agrège par un PORT : s'il touchait lui-même les modules "
                   + "CAPA, Ishikawa, 5 pourquoi, PFMEA ou control plan, il ne serait plus "
                   + "testable sans base.");

    @ArchTest
    static final ArchRule stakeholders_domain_has_no_framework_deps =
        noClasses()
            .that().resideInAPackage("..stakeholders.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..stakeholders.infrastructure..",
                "..stakeholders.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..",
                "com.fasterxml.jackson..")
            .because("Domaine du registre des parties intéressées (ADR 0104) : la criticité, la "
                   + "justification de pertinence et les règles d'exigence se testent sans base.");

    @ArchTest
    static final ArchRule stakeholders_application_touches_other_modules_by_a_port =
        noClasses()
            .that().resideInAPackage("..stakeholders.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..stakeholders.infrastructure..",
                "..stakeholders.web..",
                "..capa..",
                "..aigateway..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..",
                "com.fasterxml.jackson..")
            .because("Le cas d'usage ouvre une CAPA et interroge l'IA par des PORTS : s'il touchait "
                   + "lui-même ces modules, il ne serait testable ni sans base ni sans modèle.");
}

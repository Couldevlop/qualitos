package com.openlab.qualitos.quality.capa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le filtre « Échéance avant le » doit tenir sur un VRAI PostgreSQL.
 *
 * <p>Le registre CAPA se filtre par échéance depuis la liste. La règle est
 * « à cette date ou avant » : un dossier qui échoit le jour choisi répond à la
 * question, un dossier qui échoit le lendemain non, et un dossier sans
 * échéance non plus — il ne tombe jamais.
 *
 * <p>La requête écrivait {@code (:dueBefore is null or …)}, ce qui produit un
 * {@code ?} nu dans le SQL émis. PostgreSQL refuse de deviner son type et
 * rejette la requête entière :
 *
 * <pre>42P18 — could not determine data type of parameter $8</pre>
 *
 * <p>Le filtre tombait donc en 500 à chaque usage, et l'écran se vidait sans
 * rien dire. Rien ne l'a vu : le seul banc qui nommait {@code dueBefore} était
 * un {@code @WebMvcTest} à service simulé, et les bancs JPA du module tournent
 * sur H2 en mode PostgreSQL — H2 type ce paramètre sans broncher. Un défaut
 * qui n'existe qu'avec le vrai serveur se teste contre le vrai serveur : d'où
 * ce banc, qui joint les deux idiomes déjà posés dans le module (contexte
 * Spring complet de {@code IdeaCircleLazyLoadingTest}, conteneur Postgres des
 * bancs {@code *OnPostgresTest}).
 *
 * <p>La pagination demande DEUX requêtes — la page et son décompte. La page
 * est volontairement plus petite que le nombre de dossiers attendus, pour que
 * la requête de décompte parte elle aussi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("migration")
@Testcontainers(disabledWithoutDocker = true)
class CapaDueDateFilterOnPostgresTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void surLeVraiServeur(DynamicPropertyRegistry registre) {
        registre.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registre.add("spring.datasource.username", POSTGRES::getUsername);
        registre.add("spring.datasource.password", POSTGRES::getPassword);
        registre.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registre.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registre.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registre.add("spring.flyway.enabled", () -> "true");
        registre.add("spring.flyway.locations", () -> "classpath:db/migration");
        // Flyway porte sa PROPRE connexion dans application.yml (user/password
        // pris de l'environnement) : surcharger la source de donnees ne suffit
        // pas, il se connecterait au serveur du conteneur avec le compte de
        // production. Les trois lignes ci-dessous le ramenent sur le meme
        // serveur que JPA.
        registre.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registre.add("spring.flyway.user", POSTGRES::getUsername);
        registre.add("spring.flyway.password", POSTGRES::getPassword);
        registre.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:9999/realms/test");
    }

    @Autowired
    private CapaCaseRepository cases;

    private UUID tenant;
    private UUID pilote;

    @BeforeEach
    void unRegistreACinqDossiers() {
        tenant = UUID.randomUUID();
        pilote = UUID.randomUUID();

        dossier("échoit l'avant-veille", LocalDate.of(2026, 11, 18));
        dossier("échoit la veille", LocalDate.of(2026, 11, 19));
        dossier("échoit le jour même", LocalDate.of(2026, 11, 20));
        dossier("échoit le lendemain", LocalDate.of(2026, 11, 21));
        dossier("n'échoit jamais", null);
    }

    @Test
    void leFiltreRetientLeJourChoisiEtCeQuiPrecede() {
        Page<CapaCase> page = cases.search(tenant, null, null, null,
                LocalDate.of(2026, 11, 20), PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(cases.search(tenant, null, null, null,
                        LocalDate.of(2026, 11, 20), PageRequest.of(0, 20))
                .getContent())
                .extracting(CapaCase::getTitle)
                .containsExactlyInAnyOrder("échoit l'avant-veille", "échoit la veille",
                        "échoit le jour même");
    }

    @Test
    void sansFiltreLeRegistreRendTout() {
        assertThat(cases.search(tenant, null, null, null, null, PageRequest.of(0, 20))
                .getTotalElements())
                .isEqualTo(5);
    }

    @Test
    void laListeDeCeuxQuiMeConcernentFiltreDeLaMemeFacon() {
        Page<CapaCase> page = cases.findConcerning(tenant, null, null, null,
                LocalDate.of(2026, 11, 20), pilote, PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void unAutreClientNeVoitRien() {
        assertThat(cases.search(UUID.randomUUID(), null, null, null,
                        LocalDate.of(2026, 11, 20), PageRequest.of(0, 20))
                .getTotalElements())
                .isZero();
    }

    private void dossier(String titre, LocalDate echeance) {
        CapaCase c = new CapaCase();
        c.setTenantId(tenant);
        c.setTitle(titre);
        c.setType(CapaType.CORRECTIVE);
        c.setCriticity(CapaCriticity.MEDIUM);
        c.setStatus(CapaStatus.OPEN);
        c.setSourceType(CapaSourceType.OTHER);
        c.setOwnerId(pilote);
        c.setDueDate(echeance);
        cases.save(c);
    }
}

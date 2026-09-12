package com.openlab.qualitos.quality.migration;

import com.openlab.qualitos.quality.apqp.ApqpDeliverable;
import com.openlab.qualitos.quality.apqp.ApqpDeliverableKind;
import com.openlab.qualitos.quality.apqp.ApqpPhase;
import com.openlab.qualitos.quality.apqp.ApqpPhaseRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que H2 et les doublures ne peuvent pas dire sur une colonne {@code jsonb}.
 *
 * <p>Le défaut visé, constaté dans un navigateur le 12 septembre 2026 : la colonne
 * {@code apqp_deliverables.data} était en {@code jsonb}, et Hibernate 6 liait le
 * {@code String} comme un VARCHAR. PostgreSQL refusait l'insertion — « column
 * "data" is of type jsonb but expression is of type character varying » — <b>même
 * quand la valeur était {@code null}</b>. Résultat : l'amorçage du cycle APQP
 * échouait en 500 à la première ouverture de l'écran, qui affichait « Le cycle est
 * vide ».
 *
 * <p>La colonne est depuis en TEXT, et ce banc tient les deux moitiés du choix :
 * l'écriture passe, et le texte relu est <b>exactement</b> celui qu'on a écrit —
 * ce que {@code jsonb} ne garantissait pas, puisqu'il normalise.
 *
 * <p>Pourquoi 5442 bancs ne l'ont pas vu : les bancs de service utilisent des
 * dépôts simulés, qui rendent l'objet qu'on leur a donné ; le banc de contexte
 * boote sur H2, où {@code jsonb} n'existe pas et où le schéma vient d'Hibernate
 * lui-même ; et les bancs de migration écrivent en JDBC brut, sans Hibernate dans
 * la boucle. Aucun des trois ne traverse la chaîne réelle.
 *
 * <p>Celui-ci la traverse : vrai PostgreSQL, migrations Flyway, et écriture PAR
 * HIBERNATE. Sans Docker il se saute plutôt qu'il n'échoue — mais il dit pourquoi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("migration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApqpDeliverableJsonRoundTripOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startPostgres() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la traversee de la base reste non verifiee ici");
        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();
    }

    @AfterAll
    static void stopPostgres() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    /**
     * On remplace la source de données H2 du profil de test par le conteneur, et
     * on rend la main à Flyway : c'est le schéma RÉEL — {@code jsonb} compris —
     * qu'on veut sous Hibernate, pas celui qu'Hibernate se génère à lui-même.
     */
    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        // `none` et non `validate` : le schéma appartient à Flyway, et une
        // validation stricte échoue sur des écarts de type préexistants et sans
        // rapport (academy_certificates.created_at). Ce banc éprouve l'ÉCRITURE
        // d'une colonne jsonb, pas la conformité du schéma entier.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        // C'est `properties.hibernate.dialect` qu'il faut écraser, pas
        // `database-platform` : le profil de test fixe le premier sur H2, et il
        // l'emporte. Sous H2Dialect, Hibernate lie une colonne JSON en bytea et
        // PostgreSQL la refuse — le banc dirait alors « jsonb vs bytea » là où le
        // défaut réel était « jsonb vs varchar ».
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired ApqpPhaseRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("un livrable SANS contenu s'insère : un null lié en VARCHAR suffisait à tout casser")
    @Transactional
    void livrableSansContenu_sInsere() {
        UUID tenantId = UUID.randomUUID();
        ApqpPhase phase = phase(tenantId);
        ApqpDeliverable livrable = livrable("Product design requirements",
                ApqpDeliverableKind.ATTACHMENT, null);
        phase.addDeliverable(livrable);

        repository.saveAndFlush(phase);

        assertThat(livrable.getId()).isNotNull();
        assertThat(livrable.getData()).isNull();
    }

    @Test
    @DisplayName("le contenu d'une checklist fait l'aller-retour sans changer d'un caractère")
    @Transactional
    void contenuJson_faitLAllerRetour() {
        UUID tenantId = UUID.randomUUID();
        String json = "[{\"label\":\"safety\",\"checked\":false},"
                      + "{\"label\":\"cost\",\"checked\":true}]";

        ApqpPhase phase = phase(tenantId);
        phase.addDeliverable(livrable("Project targets", ApqpDeliverableKind.CHECKLIST, json));
        ApqpPhase enregistree = repository.saveAndFlush(phase);

        // Vider le contexte de persistance : sans cela on relirait l'objet de la
        // mémoire, et c'est précisément ce que ce banc refuse de faire.
        entityManager.clear();

        ApqpPhase relue = repository.findByIdAndTenantId(enregistree.getId(), tenantId)
                .orElseThrow();
        assertThat(relue.getDeliverables()).singleElement()
                .satisfies(d -> assertThat(d.getData()).isEqualTo(json));
    }

    @Test
    @DisplayName("des mesures aussi, avec leurs dates et leurs valeurs vides")
    @Transactional
    void mesures_fontLAllerRetour() {
        UUID tenantId = UUID.randomUUID();
        String json = "[{\"label\":\"Cpk\",\"value\":\"1.42\",\"unit\":\"\","
                      + "\"measuredAt\":\"2026-09-12\"},"
                      + "{\"label\":\"Pp\",\"value\":\"\",\"unit\":\"\",\"measuredAt\":null}]";

        ApqpPhase phase = phase(tenantId);
        phase.addDeliverable(livrable("Initial process capability studies",
                ApqpDeliverableKind.DATA_ENTRY, json));
        ApqpPhase enregistree = repository.saveAndFlush(phase);
        entityManager.clear();

        ApqpPhase relue = repository.findByIdAndTenantId(enregistree.getId(), tenantId)
                .orElseThrow();
        assertThat(relue.getDeliverables().get(0).getData()).isEqualTo(json);
    }

    // ---------- fabriques ----------

    private ApqpPhase phase(UUID tenantId) {
        ApqpPhase phase = new ApqpPhase();
        phase.setTenantId(tenantId);
        phase.setPosition(1);
        phase.setTitle("Planning");
        return phase;
    }

    private ApqpDeliverable livrable(String libelle, ApqpDeliverableKind genre, String data) {
        ApqpDeliverable livrable = new ApqpDeliverable();
        livrable.setLabel(libelle);
        livrable.setPosition(1);
        livrable.setKind(genre);
        livrable.setData(data);
        return livrable;
    }
}

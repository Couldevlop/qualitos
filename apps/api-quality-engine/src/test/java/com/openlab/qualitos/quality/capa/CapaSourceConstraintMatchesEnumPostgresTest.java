package com.openlab.qualitos.quality.capa;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La base et le code doivent nommer les mêmes origines de CAPA.
 *
 * <p>Deux fois déjà, une valeur ajoutée à {@link CapaSourceType} n'a pas été
 * reportée dans la contrainte {@code chk_capa_cases_source} : SPC_ALERT et
 * ANOMALY, rattrapées en V135, puis EXTERNAL_NON_CONFORMITY, rattrapée en
 * V168. Le défaut ne se voit pas à la compilation ni en test unitaire — H2 et
 * les doublures acceptent tout. Il se découvre au premier usage réel, par un
 * 409 sans explication : en l'espèce, impossible d'escalader une réclamation
 * client vers une CAPA.
 *
 * <p>Ce banc ferme la boucle. Il ne vérifie pas une valeur particulière — il
 * les vérifie TOUTES, en lisant l'énumération au moment du test : la
 * prochaine valeur ajoutée sans migration fera rougir la CI le jour où elle
 * est écrite, et non le jour où un client la rencontre.
 *
 * <p>Il lit la définition de la contrainte plutôt que d'insérer une ligne par
 * valeur : l'insertion exigerait de satisfaire toutes les autres contraintes de
 * la table, ce qui ferait porter l'échec sur autre chose que la question posée.
 */
@Tag("migration")
class CapaSourceConstraintMatchesEnumPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrateOnARealServer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : l'accord base/code reste non vérifié ici");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @AfterAll
    static void stop() throws SQLException {
        if (connection != null) connection.close();
        if (postgres != null) postgres.stop();
    }

    @Test
    void laContrainteAccepteToutesLesOriginesQueLeCodeSaitEcrire() throws SQLException {
        String definition = definitionDeLaContrainte();

        List<String> manquantes = Arrays.stream(CapaSourceType.values())
                .map(Enum::name)
                .filter(valeur -> !definition.contains("'" + valeur + "'"))
                .toList();

        assertThat(manquantes)
                .as("origines que le code écrit mais que la base refuserait — "
                        + "il manque une migration qui étende %s", "chk_capa_cases_source")
                .isEmpty();
    }

    @Test
    void laContrainteNAcceptePasDOrigineQueLeCodeNeConnaitPlus() throws SQLException {
        // L'autre sens, qui compte autant : une valeur restée dans la contrainte
        // après avoir disparu de l'énumération laisse en base des lignes que plus
        // aucun écran ne sait lire — et personne ne les cherche.
        List<String> connues = Arrays.stream(CapaSourceType.values()).map(Enum::name).toList();

        List<String> orphelines = Arrays.stream(definitionDeLaContrainte().split("'"))
                .filter(morceau -> morceau.matches("[A-Z][A-Z_]{2,}"))
                .distinct()
                .filter(valeur -> !connues.contains(valeur))
                .toList();

        assertThat(orphelines)
                .as("origines admises par la base mais inconnues du code")
                .isEmpty();
    }

    private static String definitionDeLaContrainte() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultat = statement.executeQuery(
                     "select pg_get_constraintdef(oid) from pg_constraint "
                             + "where conname = 'chk_capa_cases_source'")) {
            assertThat(resultat.next())
                    .as("la contrainte chk_capa_cases_source existe")
                    .isTrue();
            return resultat.getString(1);
        }
    }
}

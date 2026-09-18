package com.openlab.qualitos.quality.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que la V133 fait des projets APQP déjà saisis, vérifié sur un vrai moteur.
 *
 * <p>La migration ne se contente pas de renommer une constante : elle DÉPOSE une
 * contrainte {@code CHECK}, convertit des lignes existantes, puis repose la
 * contrainte avec un jeu de valeurs différent. Les trois temps doivent tenir
 * ensemble — reposer la contrainte avant d'avoir converti la donnée échouerait
 * sur la première ligne restée en {@code NEW_CUSTOMER}, et la mise à jour de
 * toute la base s'arrêterait là.
 *
 * <p>Aucune doublure de dépôt ne dirait cela : un dépôt simulé rend l'objet
 * qu'on lui a donné, et n'a ni contrainte ni moteur SQL pour la refuser. C'est
 * la leçon déjà payée sur les empreintes d'audit — ce qui se joue dans la base
 * se vérifie dans la base.
 *
 * <p>Le renommage du projet de reprise en anglais est vérifié, lui, par
 * {@link ApqpProjetsRepriseOnPostgresTest}, qui possède déjà le décor de la
 * V131.
 */
@Tag("migration")
class ApqpTypeModificationMajeureOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    private static final UUID TENANT = UUID.randomUUID();

    /** Un projet saisi sous l'ancien quatrième type : c'est lui qu'on convertit. */
    private static final UUID PROJET_ANCIEN_TYPE = UUID.randomUUID();
    /** Un projet d'un autre type : la migration ne doit pas y toucher. */
    private static final UUID PROJET_NPI = UUID.randomUUID();

    @BeforeAll
    static void migrerEnDeuxTemps() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la conversion de la V133 reste non verifiee sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        // On s'arrête APRÈS la V132 : la table des projets existe, son ancienne
        // contrainte aussi, et `NEW_CUSTOMER` y est encore une valeur admise.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("132")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        ecrireProjet(PROJET_ANCIEN_TYPE, "Pompe HP-40 pour Atlas Industries", "NEW_CUSTOMER");
        ecrireProjet(PROJET_NPI, "Vanne VX-9, première série", "NPI");

        // Puis la V133 passe.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @AfterAll
    static void stop() throws SQLException {
        if (connection != null) {
            connection.close();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    @DisplayName("un projet saisi en « nouveau client » devient « modification majeure »")
    void leProjetEstConvertiEtNonSupprime() throws SQLException {
        assertThat(typeDe(PROJET_ANCIEN_TYPE)).isEqualTo("MAJOR_MODIFICATION");

        // Et il est TOUJOURS LÀ, avec son intitulé : un projet porte un cycle,
        // des phases, des livrables et des pièces jointes. Le convertir est la
        // seule issue acceptable — le supprimer emporterait du travail réel.
        assertThat(nomDe(PROJET_ANCIEN_TYPE)).isEqualTo("Pompe HP-40 pour Atlas Industries");
    }

    @Test
    @DisplayName("les projets des autres types ne sont pas touchés")
    void lesAutresTypesSontIntacts() throws SQLException {
        assertThat(typeDe(PROJET_NPI)).isEqualTo("NPI");
    }

    @Test
    @DisplayName("la contrainte accepte le nouveau type")
    void laContrainteAccepteLeNouveauType() throws SQLException {
        UUID nouveau = UUID.randomUUID();
        ecrireProjet(nouveau, "Refonte du carter, série 2027", "MAJOR_MODIFICATION");
        assertThat(typeDe(nouveau)).isEqualTo("MAJOR_MODIFICATION");
    }

    @Test
    @DisplayName("la contrainte refuse désormais l'ancien type")
    void laContrainteRefuseLAncienType() {
        // C'est la moitié qu'on oublie : convertir la donnée sans resserrer la
        // contrainte laisserait une base qui accepte encore une valeur que plus
        // aucun code ne sait lire — et l'écran afficherait un type vide, sans
        // que rien ne se plaigne.
        assertThatThrownBy(() -> ecrireProjet(UUID.randomUUID(), "Projet douteux", "NEW_CUSTOMER"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_apqp_projects_type");
    }

    // ---------- utilitaires ----------

    private static void ecrireProjet(UUID id, String nom, String type) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO apqp_projects (id, tenant_id, name, type) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, TENANT);
            ps.setString(3, nom);
            ps.setString(4, type);
            ps.executeUpdate();
        }
    }

    private static String typeDe(UUID id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT type FROM apqp_projects WHERE id = ?")) {
            return lire(ps, id);
        }
    }

    private static String nomDe(UUID id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM apqp_projects WHERE id = ?")) {
            return lire(ps, id);
        }
    }

    /**
     * Exécute une requête à une colonne et rend sa valeur.
     *
     * <p>Chaque requête porte son texte en clair plutôt qu'un nom de colonne
     * assemblé : une requête construite par concaténation est refusée par la
     * CI, et la règle vaut ici comme ailleurs — un banc qui prend l'habitude
     * d'assembler du SQL finit par la transmettre au code de production.
     */
    private static String lire(PreparedStatement ps, UUID id) throws SQLException {
        ps.setObject(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).isTrue();
            return rs.getString(1);
        }
    }
}

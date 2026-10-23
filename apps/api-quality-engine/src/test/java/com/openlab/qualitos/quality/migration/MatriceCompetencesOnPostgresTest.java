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
 * La V147 sur un vrai moteur : unicité d'une personne et d'une compétence par
 * client, noms non vides, et la colonne qui part avec sa compétence du
 * catalogue.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore donc cette migration : ses contraintes ne se vérifient qu'ici.
 */
@Tag("migration")
class MatriceCompetencesOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V147 reste non verifiee sur cette machine");
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
        if (connection != null) {
            connection.close();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    @DisplayName("une personne n'est inscrite qu'une fois par client, avec un nom")
    void operateurs() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID personne = UUID.randomUUID();
        operateur(tenant, personne, "Martin Dubois");
        operateur(UUID.randomUUID(), personne, "Martin Dubois");
        assertThatThrownBy(() -> operateur(tenant, personne, "Martin")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> operateur(tenant, UUID.randomUUID(), "  ")).isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("une compétence n'entre qu'une fois, et quitte la matrice avec le catalogue")
    void competences() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID skill = competence(tenant);
        colonne(tenant, skill, "Karim Benali");
        assertThatThrownBy(() -> colonne(tenant, skill, "Autre")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> colonne(tenant, competence(tenant), " ")).isInstanceOf(SQLException.class);

        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM training_skills WHERE id = ?")) {
            ps.setObject(1, skill);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM competency_matrix_skills WHERE skill_id = ?")) {
            ps.setObject(1, skill);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    @Test
    @DisplayName("un atelier est unique par client et jamais vide")
    void ateliers() throws SQLException {
        UUID tenant = UUID.randomUUID();
        atelier(tenant, "Usinage", 0);
        atelier(UUID.randomUUID(), "Usinage", 0);
        assertThatThrownBy(() -> atelier(tenant, "Usinage", 1)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> atelier(tenant, " ", 1)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> atelier(tenant, "Peinture", -1)).isInstanceOf(SQLException.class);
    }

    private static void operateur(UUID tenant, UUID personne, String nom) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO competency_matrix_operators "
                + "(tenant_id, user_id, user_name, workshop, created_at) VALUES (?, ?, ?, 'Usinage', now())")) {
            ps.setObject(1, tenant);
            ps.setObject(2, personne);
            ps.setString(3, nom);
            ps.executeUpdate();
        }
    }

    private static UUID competence(UUID tenant) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO training_skills "
                + "(id, tenant_id, code, name, created_at, updated_at) VALUES (?, ?, ?, 'Tournage', now(), now())")) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, "c-" + id);
            ps.executeUpdate();
        }
        return id;
    }

    private static void colonne(UUID tenant, UUID skill, String formateur) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO competency_matrix_skills "
                + "(tenant_id, skill_id, trainer_user_id, trainer_name, created_at) VALUES (?, ?, ?, ?, now())")) {
            ps.setObject(1, tenant);
            ps.setObject(2, skill);
            ps.setObject(3, UUID.randomUUID());
            ps.setString(4, formateur);
            ps.executeUpdate();
        }
    }

    private static void atelier(UUID tenant, String nom, int position) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO competency_workshops "
                + "(tenant_id, name, position, updated_at) VALUES (?, ?, ?, now())")) {
            ps.setObject(1, tenant);
            ps.setString(2, nom);
            ps.setInt(3, position);
            ps.executeUpdate();
        }
    }
}

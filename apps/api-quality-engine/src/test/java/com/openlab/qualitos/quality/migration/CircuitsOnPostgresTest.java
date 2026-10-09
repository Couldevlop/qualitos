package com.openlab.qualitos.quality.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V137 sur un vrai moteur : les filets que la base tend derrière le domaine
 * des circuits (ADR 0080) — un seul passage ouvert par objet, quatre yeux, refus
 * motivé. Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore la migration (index partiel compris) : ces contraintes ne se vérifient qu'ici.
 */
@Tag("migration")
class CircuitsOnPostgresTest {

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V137 reste non verifiee sur cette machine");
        postgres = new PostgreSQLContainer<>("postgres:17-alpine");
        postgres.start();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
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

    private static void etape(UUID tenant, int ordinal, String role, int min) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO circuit_steps (tenant_id, subject, "
                + "ordinal, name, role_code, min_approvals, updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setObject(1, tenant);
            ps.setString(2, "document-version");
            ps.setInt(3, ordinal);
            ps.setString(4, "Étape " + ordinal);
            ps.setString(5, role);
            ps.setInt(6, min);
            ps.setObject(7, UUID.randomUUID());
            ps.setTimestamp(8, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    private static UUID passage(UUID tenant, UUID objet, String status, Instant fin) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO circuit_runs (id, tenant_id, subject, "
                + "subject_id, author_id, status, current_step, started_at, ended_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, "document-version");
            ps.setObject(4, objet);
            ps.setObject(5, UUID.randomUUID());
            ps.setString(6, status);
            ps.setTimestamp(7, Timestamp.from(Instant.now()));
            ps.setTimestamp(8, fin == null ? null : Timestamp.from(fin));
            ps.executeUpdate();
        }
        return id;
    }

    private static void decision(UUID run, int ordinal, UUID acteur, boolean approuve, String commentaire)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO circuit_decisions (run_id, ordinal, "
                + "step_index, actor_id, approved, comment, decided_at) VALUES (?, ?, 0, ?, ?, ?, ?)")) {
            ps.setObject(1, run);
            ps.setInt(2, ordinal);
            ps.setObject(3, acteur);
            ps.setBoolean(4, approuve);
            ps.setString(5, commentaire);
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    private static int compte(String sql, UUID id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Test
    void unObjetNaQuunPassageOuvert_maisGardeSesPassagesTermines() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID doc = UUID.randomUUID();
        passage(tenant, doc, "REJECTED", Instant.now());
        passage(tenant, doc, "IN_PROGRESS", null);

        assertThatThrownBy(() -> passage(tenant, doc, "IN_PROGRESS", null)).isInstanceOf(SQLException.class);
        // Le même objet dans un autre client est un autre passage.
        passage(UUID.randomUUID(), doc, "IN_PROGRESS", null);
        assertThat(compte("SELECT count(*) FROM circuit_runs WHERE subject_id = ?", doc)).isEqualTo(3);
    }

    @Test
    void unStatutEtSaDateDeFinVontEnsemble() {
        UUID tenant = UUID.randomUUID();
        assertThatThrownBy(() -> passage(tenant, UUID.randomUUID(), "APPROVED", null)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> passage(tenant, UUID.randomUUID(), "IN_PROGRESS", Instant.now()))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> passage(tenant, UUID.randomUUID(), "PENDING", null)).isInstanceOf(SQLException.class);
    }

    @Test
    void quatreYeux_uneDecisionParPersonne_etUnRefusSeMotive() throws SQLException {
        UUID run = passage(UUID.randomUUID(), UUID.randomUUID(), "IN_PROGRESS", null);
        UUID alice = UUID.randomUUID();
        decision(run, 0, alice, true, null);

        assertThatThrownBy(() -> decision(run, 1, alice, true, null)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> decision(run, 1, UUID.randomUUID(), false, null)).isInstanceOf(SQLException.class);
        decision(run, 1, UUID.randomUUID(), false, "Incomplet");

        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM circuit_runs WHERE id = ?")) {
            ps.setObject(1, run);
            ps.executeUpdate();
        }
        assertThat(compte("SELECT count(*) FROM circuit_decisions WHERE run_id = ?", run)).isZero();
    }

    @Test
    void lesEtapesReglees_refusentCeQueLeDomaineRefuse() throws SQLException {
        UUID tenant = UUID.randomUUID();
        etape(tenant, 0, "QUALITY_MANAGER", 2);
        assertThatThrownBy(() -> etape(tenant, 0, "QUALITY_DIRECTOR", 1)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> etape(tenant, 1, "SUPER_ADMIN", 1)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> etape(tenant, 1, "manager", 1)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> etape(tenant, 1, "QUALITY_DIRECTOR", 0)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> etape(tenant, 10, "QUALITY_DIRECTOR", 1)).isInstanceOf(SQLException.class);
    }

    @Test
    void uneVersionRefuseeGardeSaRaison() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT rejected_by, rejected_at, rejection_reason "
                + "FROM document_versions WHERE false")) {
            assertThat(ps.executeQuery().getMetaData().getColumnCount()).isEqualTo(3);
        }
    }
}

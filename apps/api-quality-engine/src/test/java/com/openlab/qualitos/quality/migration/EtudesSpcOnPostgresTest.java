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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V145 sur un vrai moteur : les filets que la base tend derrière le domaine
 * — USL au-dessus de LSL, taille de sous-groupe dans la table, seuils ordonnés.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore donc cette migration : ses contraintes ne se vérifient qu'ici.
 */
@Tag("migration")
class EtudesSpcOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V145 reste non verifiee sur cette machine");
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

    private static final String INSERT_STUDY = "INSERT INTO spc_capability_studies "
            + "(tenant_id, part_reference, study_date, lsl, usl, subgroup_size, measurements, cp, cpk, "
            + "capable_threshold, watch_threshold, created_at, updated_at) "
            + "VALUES (?, 'Shaft AX', ?, ?, ?, ?, '[[500]]', 1.84, 1.769, ?, ?, ?, ?)";

    @Test
    @DisplayName("une étude cohérente s'écrit, plan d'actions à non par défaut")
    void uneEtudeCoherente() throws SQLException {
        UUID tenant = UUID.randomUUID();
        inserer(tenant, 475, 525, 5, 1.67, 1.33);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT action_plan, cpk FROM spc_capability_studies WHERE tenant_id = ?")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean(1)).isFalse();
                assertThat(rs.getDouble(2)).isEqualTo(1.769);
            }
        }
    }

    @Test
    @DisplayName("USL sous LSL, taille hors table ou seuils inversés sont refusés par la base")
    void lesFiletsDeLaBase() {
        UUID tenant = UUID.randomUUID();
        assertThatThrownBy(() -> inserer(tenant, 525, 475, 5, 1.67, 1.33)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> inserer(tenant, 475, 525, 11, 1.67, 1.33)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> inserer(tenant, 475, 525, 5, 1.33, 1.67)).isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("un client n'a qu'une ligne de seuils, et ils restent ordonnés")
    void lesSeuilsDuClient() throws SQLException {
        UUID tenant = UUID.randomUUID();
        seuils(tenant, 1.67, 1.33);
        assertThatThrownBy(() -> seuils(tenant, 2.0, 1.5)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> seuils(UUID.randomUUID(), 1.0, 1.5)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> seuils(UUID.randomUUID(), 1.0, 0)).isInstanceOf(SQLException.class);
    }

    private static void inserer(UUID tenant, double lsl, double usl, int n, double capable, double watch)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_STUDY)) {
            Timestamp maintenant = Timestamp.from(Instant.now());
            ps.setObject(1, tenant);
            ps.setObject(2, LocalDate.of(2026, 9, 14));
            ps.setDouble(3, lsl);
            ps.setDouble(4, usl);
            ps.setInt(5, n);
            ps.setDouble(6, capable);
            ps.setDouble(7, watch);
            ps.setTimestamp(8, maintenant);
            ps.setTimestamp(9, maintenant);
            ps.executeUpdate();
        }
    }

    private static void seuils(UUID tenant, double capable, double watch) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO spc_settings (tenant_id, capable_threshold, watch_threshold, updated_at) "
                        + "VALUES (?, ?, ?, now())")) {
            ps.setObject(1, tenant);
            ps.setDouble(2, capable);
            ps.setDouble(3, watch);
            ps.executeUpdate();
        }
    }
}

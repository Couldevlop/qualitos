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
 * La V146 sur un vrai moteur : les filets que la base tend derrière le domaine
 * — USL au-dessus de LSL, plan dans la table AIAG, multiplicateur 5,15 ou 6,
 * verdict connu.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore donc cette migration : ses contraintes ne se vérifient qu'ici.
 */
@Tag("migration")
class EtudesGageRrOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V146 reste non verifiee sur cette machine");
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

    private static final String INSERT = "INSERT INTO spc_gage_rr_studies "
            + "(tenant_id, part_reference, lsl, usl, operator_count, part_count, trial_count, sigma_multiplier, "
            + "study_date, operators, measurements, grr_percent, grr_tolerance_percent, ndc, decision, "
            + "anova_grr_percent, anova_ndc, created_at, updated_at) "
            + "VALUES (?, 'Arbre AX-210', ?, ?, ?, ?, ?, ?, ?, '[]', '[]', 21.0, 24.74, 6, ?, 21.16, 6, ?, ?)";

    @Test
    @DisplayName("une étude cohérente s'écrit et se relit")
    void uneEtudeCoherente() throws SQLException {
        UUID tenant = UUID.randomUUID();
        inserer(tenant, 9.9, 10.1, 3, 10, 3, 5.15, "CONDITIONAL");
        inserer(tenant, 9.9, 10.1, 2, 2, 2, 6, "ACCEPTABLE");

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*), max(ndc) FROM spc_gage_rr_studies WHERE tenant_id = ?")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
                assertThat(rs.getInt(2)).isEqualTo(6);
            }
        }
    }

    @Test
    @DisplayName("limites inversées, plan hors table, multiplicateur ou verdict inconnus sont refusés")
    void lesFiletsDeLaBase() {
        UUID t = UUID.randomUUID();
        assertThatThrownBy(() -> inserer(t, 10.1, 9.9, 3, 10, 3, 5.15, "CONDITIONAL")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> inserer(t, 9.9, 10.1, 4, 10, 3, 5.15, "CONDITIONAL")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> inserer(t, 9.9, 10.1, 3, 11, 3, 5.15, "CONDITIONAL")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> inserer(t, 9.9, 10.1, 3, 10, 1, 5.15, "CONDITIONAL")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> inserer(t, 9.9, 10.1, 3, 10, 3, 5.5, "CONDITIONAL")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> inserer(t, 9.9, 10.1, 3, 10, 3, 5.15, "PEUT-ETRE")).isInstanceOf(SQLException.class);
    }

    @SuppressWarnings("java:S107")
    private static void inserer(UUID tenant, double lsl, double usl, int k, int n, int r, double mult, String decision)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            Timestamp maintenant = Timestamp.from(Instant.now());
            ps.setObject(1, tenant);
            ps.setDouble(2, lsl);
            ps.setDouble(3, usl);
            ps.setInt(4, k);
            ps.setInt(5, n);
            ps.setInt(6, r);
            ps.setDouble(7, mult);
            ps.setObject(8, LocalDate.of(2026, 10, 19));
            ps.setString(9, decision);
            ps.setTimestamp(10, maintenant);
            ps.setTimestamp(11, maintenant);
            ps.executeUpdate();
        }
    }
}

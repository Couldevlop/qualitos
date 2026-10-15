package com.openlab.qualitos.quality.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V142 sur un vrai moteur (ADR 0092) : chaque personne désignée en clair
 * gagne le compte de l'annuaire, facultatif, à côté du nom qui reste — les
 * saisies antérieures se relisent telles quelles.
 */
@Tag("migration")
class PersonnesDesigneesOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V142 reste non verifiee sur cette machine");
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

    @ParameterizedTest
    @CsvSource({
            "risk_register_risks, owner_user_id",
            "risk_register_opportunities, owner_user_id",
            "apqp_deliverables, owner_user_id",
            "coq_entries, responsible_user_id",
            "ishikawa_actions, responsible_user_id"
    })
    @DisplayName("le compte de la personne est un UUID facultatif")
    void leCompteEstUnUuidFacultatif(String table, String colonne) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT data_type, is_nullable "
                + "FROM information_schema.columns WHERE table_name = ? AND column_name = ?")) {
            ps.setString(1, table);
            ps.setString(2, colonne);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as(table + "." + colonne).isTrue();
                assertThat(rs.getString(1)).isEqualTo("uuid");
                assertThat(rs.getString(2)).isEqualTo("YES");
            }
        }
    }

    @Test
    @DisplayName("un risque garde son nom, avec ou sans compte")
    void unRisqueGardeSonNomAvecOuSansCompte() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID compte = UUID.randomUUID();
        risque(tenant, "R-001", null);
        risque(tenant, "R-002", compte);

        try (PreparedStatement ps = connection.prepareStatement("SELECT reference, owner, owner_user_id "
                + "FROM risk_register_risks WHERE tenant_id = ? ORDER BY reference")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("owner")).isEqualTo("M. Kone");
                assertThat(rs.getObject("owner_user_id")).isNull();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("owner")).isEqualTo("M. Kone");
                assertThat(rs.getObject("owner_user_id")).isEqualTo(compte);
            }
        }
    }

    private static void risque(UUID tenant, String reference, UUID compte) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO risk_register_risks (id, tenant_id, "
                + "reference, title, type, process, owner, owner_user_id, origin, gross_severity, gross_probability) "
                + "VALUES (?, ?, ?, 'Dérive', 'QUALITY', 'Production', 'M. Kone', ?, 'DIRECT', 3, 3)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setString(3, reference);
            ps.setObject(4, compte);
            ps.executeUpdate();
        }
    }
}

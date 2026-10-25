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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V151 sur un vrai moteur : un tenant n'a qu'une politique, et la base refuse
 * tout fournisseur hors de la liste, même si une écriture contournait le domaine.
 */
@Tag("migration")
class RoutageIaParTenantOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";
    private static final String INSERT =
            "INSERT INTO tenant_ai_policy (tenant_id, provider, updated_at, updated_by) VALUES (?, ?, ?, ?)";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V151 reste non verifiee sur cette machine");
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

    private static void inserer(UUID tenant, String provider) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            ps.setObject(1, tenant);
            ps.setString(2, provider);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.setObject(4, UUID.randomUUID());
            ps.executeUpdate();
        }
    }

    @Test
    @DisplayName("chaque fournisseur connu s'écrit et se relit")
    void lesFournisseursConnus() throws SQLException {
        for (String provider : new String[] {"ollama", "anthropic", "mistral"}) {
            UUID tenant = UUID.randomUUID();
            inserer(tenant, provider);
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT provider FROM tenant_ai_policy WHERE tenant_id = ?")) {
                ps.setObject(1, tenant);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString(1)).isEqualTo(provider);
                }
            }
        }
    }

    @Test
    @DisplayName("un fournisseur hors liste est refusé par la base")
    void unFournisseurInconnu() {
        assertThatThrownBy(() -> inserer(UUID.randomUUID(), "openai")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> inserer(UUID.randomUUID(), "OLLAMA")).isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("un tenant n'a qu'une politique")
    void unePolitiqueParTenant() throws SQLException {
        UUID tenant = UUID.randomUUID();
        inserer(tenant, "ollama");
        assertThatThrownBy(() -> inserer(tenant, "mistral")).isInstanceOf(SQLException.class);
    }
}

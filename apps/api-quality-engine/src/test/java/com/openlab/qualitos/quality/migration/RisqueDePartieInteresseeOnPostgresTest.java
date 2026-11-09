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
 * La V159 sur un vrai moteur : le registre des risques accepte l'origine
 * « exigence de partie intéressée », et rien d'autre n'a bougé.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore les migrations : une contrainte CHECK oubliée ne se verrait qu'en
 * production, au moment où un responsable qualité inscrit son premier risque.
 */
@Tag("migration")
class RisqueDePartieInteresseeOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V159 reste non verifiee sur cette machine");
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
    @DisplayName("un risque né d'une exigence s'écrit, avec sa citation et sans source")
    void leRisqueDUneExigence() throws SQLException {
        UUID tenant = UUID.randomUUID();

        risque(tenant, "R-001", "STAKEHOLDER", null);

        assertThat(compter("SELECT count(*) FROM risk_register_risks WHERE tenant_id = ? "
                + "AND origin = 'STAKEHOLDER' AND origin_ref = 'Client A / Tracer les lots'",
                tenant)).isEqualTo(1);
    }

    @Test
    @DisplayName("cette origine ne désigne pas d'objet : une source y est refusée")
    void laSourceResteRefusee() {
        // Une exigence est une ligne dans la fiche d'une partie intéressée, pas
        // un objet que le registre pourrait relire : la citation en tient lieu.
        assertThatThrownBy(() -> risque(UUID.randomUUID(), "R-002", "STAKEHOLDER", UUID.randomUUID()))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_risk_register_risks_source");
    }

    @Test
    @DisplayName("une origine inventée reste refusée")
    void uneOrigineInventee() {
        assertThatThrownBy(() -> risque(UUID.randomUUID(), "R-003", "PARTIE_PRENANTE", null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_risk_register_risks_origin");
    }

    @Test
    @DisplayName("une opportunité ne naît pas d'une exigence non tenue")
    void lOpportuniteNEnNaitPas() {
        // Ne pas tenir une exigence est un risque ; ce n'est pas une occasion à
        // saisir. La V159 n'a pas touché aux cinq origines des opportunités.
        assertThatThrownBy(() -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO risk_register_opportunities "
                    + "(id, tenant_id, reference, title, type, process, owner, origin, gain, feasibility) "
                    + "VALUES (?, ?, 'O-001', 'Automatiser', 'QUALITY', 'Production', 'Mme Diallo', "
                    + "'STAKEHOLDER', 4, 4)")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, UUID.randomUUID());
                ps.executeUpdate();
            }
        }).isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_risk_register_opportunities_origin");
    }

    // ---------- aides ----------

    private static void risque(UUID tenant, String ref, String origin, UUID source) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO risk_register_risks "
                + "(id, tenant_id, reference, title, type, process, owner, cause, effect, origin, origin_ref, "
                + "source_id, gross_severity, gross_probability) VALUES (?, ?, ?, 'Retard de livraison', "
                + "'QUALITY', 'Production', 'Alice Martin', 'Tracer les lots', 'Retard de livraison', ?, "
                + "'Client A / Tracer les lots', ?, 4, 3)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setString(3, ref);
            ps.setString(4, origin);
            ps.setObject(5, source);
            ps.executeUpdate();
        }
    }

    private static long compter(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}

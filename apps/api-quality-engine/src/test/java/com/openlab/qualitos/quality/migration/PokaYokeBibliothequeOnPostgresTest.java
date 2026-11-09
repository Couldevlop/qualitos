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
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V161 sur un vrai moteur : la bibliothèque de détrompeurs devient celle de
 * la plateforme, plus celle de chaque client.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore la migration : la contrainte d'origine citée et l'index partiel ne se
 * vérifient qu'ici. Ce qui est tenu : le catalogue déjà livré reste au partagé
 * (colonne nulle), un dispositif de client sans origine est refusé, et le
 * catalogue partagé, lui, n'a pas à en citer une.
 */
@Tag("migration")
class PokaYokeBibliothequeOnPostgresTest {

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V161 reste non verifiee sur cette machine");
        postgres = new PostgreSQLContainer<>("postgres:17-alpine");
        postgres.start();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword());
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

    private static void inscrire(UUID tenant, String code, String origine) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO pokayoke_devices (id, tenant_id, code, name, type, mechanism, "
                        + "source_reference, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 'Gabarit', 'PREVENTION', 'PHYSICAL_SHAPE', ?, now(), now())")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setString(3, code);
            ps.setString(4, origine);
            ps.executeUpdate();
        }
    }

    @Test
    void leCatalogueLivreResteAuPartage() throws SQLException {
        // Les dispositifs déjà en base sont ceux que la plateforme livre (V13) :
        // la colonne naît nulle, et c'est exactement ce qu'il faut. Le compte
        // porte sur ces seules lignes : les autres bancs de cette classe
        // inscrivent des dispositifs de clients, et l'ordre n'est pas garanti.
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT count(*) AS n, count(tenant_id) AS portes FROM pokayoke_devices "
                             + "WHERE code = 'PY-SHAPE-CONN-001' OR code = 'PY-INTERLOCK-DOOR-001' "
                             + "OR code = 'PY-SENSOR-WEIGHT-001' OR code = 'PY-VISION-LABEL-001'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("n")).isEqualTo(4);
            assertThat(rs.getLong("portes")).isZero();
        }
    }

    @Test
    void unDispositifDeClientSansOrigineEstRefuse() {
        assertThatThrownBy(() -> inscrire(UUID.randomUUID(), "PY-T1-001", null))
                .hasMessageContaining("ck_pokayoke_devices_origine_citee");
    }

    @Test
    void unDispositifDeClientAvecSonOrigineEstAccepte() throws SQLException {
        UUID tenant = UUID.randomUUID();
        inscrire(tenant, "PY-T2-001", "Projet DMAIC DM-1 — action n° 2 du plan d'actions");

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT source_reference FROM pokayoke_devices WHERE code = 'PY-T2-001'")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).contains("DM-1");
            }
        }
    }

    @Test
    void leCataloguePartageNaPasDOrigineACiter() throws SQLException {
        inscrire(null, "PY-PARTAGE-001", null);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT tenant_id FROM pokayoke_devices WHERE code = 'PY-PARTAGE-001'")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getObject(1)).isNull();
            }
        }
    }

    @Test
    void lIndexPartielSertLaPartQuiGrandit() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT indexdef FROM pg_indexes "
                             + "WHERE indexname = 'ix_pokayoke_devices_tenant'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).contains("WHERE (tenant_id IS NOT NULL)");
        }
    }

    @Test
    void leCodeResteUniqueSurToutePlateforme() throws SQLException {
        // Catalogue partagé et bibliothèques de clients cohabitent : deux
        // dispositifs du même code ne se distingueraient plus à la citation.
        UUID tenant = UUID.randomUUID();
        inscrire(tenant, "PY-UNIQUE-001", "origine");

        assertThatThrownBy(() -> inscrire(UUID.randomUUID(), "PY-UNIQUE-001", "autre origine"))
                .hasMessageContaining("code");
    }
}

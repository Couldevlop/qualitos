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
 * La V148 sur un vrai moteur : les signataires d'une version (ADR 0099) — trois
 * rangs au plus, un rang et une personne une seule fois par version, une
 * décision connue, et leur départ avec la version.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore donc cette migration : ses contraintes ne se vérifient qu'ici.
 */
@Tag("migration")
class GedSignatairesOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V148 reste non verifiee sur cette machine");
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
    @DisplayName("trois signataires s'écrivent, en attente par défaut")
    void troisSignataires() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID version = version(tenant);
        for (int rang = 1; rang <= 3; rang++) {
            signataire(tenant, version, rang, UUID.randomUUID(), null);
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*), min(decision), max(decision) FROM document_version_signatories WHERE version_id = ?")) {
            ps.setObject(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(3);
                assertThat(rs.getString(2)).isEqualTo("PENDING");
                assertThat(rs.getString(3)).isEqualTo("PENDING");
            }
        }
    }

    @Test
    @DisplayName("rang hors 1-3, rang ou personne en double, décision inconnue : refusés par la base")
    void lesFiletsDeLaBase() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID version = version(tenant);
        UUID marie = UUID.randomUUID();
        signataire(tenant, version, 1, marie, null);

        assertThatThrownBy(() -> signataire(tenant, version, 4, UUID.randomUUID(), null))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> signataire(tenant, version, 0, UUID.randomUUID(), null))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> signataire(tenant, version, 1, UUID.randomUUID(), null))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> signataire(tenant, version, 2, marie, null))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> signataire(tenant, version, 2, UUID.randomUUID(), "MAYBE"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> signataire(tenant, UUID.randomUUID(), 1, UUID.randomUUID(), null))
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("les signataires partent avec leur version")
    void suppressionEnCascade() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID version = version(tenant);
        signataire(tenant, version, 1, UUID.randomUUID(), "APPROVED");
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM document_versions WHERE id = ?")) {
            ps.setObject(1, version);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM document_version_signatories WHERE version_id = ?")) {
            ps.setObject(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    private static UUID version(UUID tenant) throws SQLException {
        UUID document = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO documents "
                + "(id, tenant_id, code, title, type, owner_id, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'Procédure', 'PROCEDURE', ?, now(), now())")) {
            ps.setObject(1, document);
            ps.setObject(2, tenant);
            ps.setString(3, "PRO-" + document);
            ps.setObject(4, UUID.randomUUID());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO document_versions "
                + "(id, document_id, version_number, status, author_id, created_at, updated_at) "
                + "VALUES (?, ?, 1, 'IN_REVIEW', ?, now(), now())")) {
            ps.setObject(1, version);
            ps.setObject(2, document);
            ps.setObject(3, UUID.randomUUID());
            ps.executeUpdate();
        }
        return version;
    }

    private static void signataire(UUID tenant, UUID version, int rang, UUID personne, String decision)
            throws SQLException {
        String sql = decision == null
                ? "INSERT INTO document_version_signatories (tenant_id, version_id, position, signer_id, signer_name) "
                        + "VALUES (?, ?, ?, ?, 'Marie Curie')"
                : "INSERT INTO document_version_signatories (tenant_id, version_id, position, signer_id, signer_name, "
                        + "decision) VALUES (?, ?, ?, ?, 'Marie Curie', ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, tenant);
            ps.setObject(2, version);
            ps.setInt(3, rang);
            ps.setObject(4, personne);
            if (decision != null) {
                ps.setString(5, decision);
            }
            ps.executeUpdate();
        }
    }
}

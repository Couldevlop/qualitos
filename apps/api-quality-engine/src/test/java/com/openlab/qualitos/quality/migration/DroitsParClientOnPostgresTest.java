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
 * La V136 sur un vrai moteur : les filets que la base tend derrière le domaine
 * des droits (ADR 0078). Le profil de test ordinaire bâtit son schéma depuis
 * les entités JPA et ignore la migration : ses contraintes ne se vérifient qu'ici.
 */
@Tag("migration")
class DroitsParClientOnPostgresTest {

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V136 reste non verifiee sur cette machine");
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

    private static UUID role(UUID tenant, String code, String name, boolean system) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO authz_roles (id, tenant_id, code, name, "
                + "system_role, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            Timestamp maintenant = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, code);
            ps.setString(4, name);
            ps.setBoolean(5, system);
            ps.setObject(6, UUID.randomUUID());
            ps.setTimestamp(7, maintenant);
            ps.setTimestamp(8, maintenant);
            ps.executeUpdate();
        }
        return id;
    }

    private static void droit(UUID roleId, String permission) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO authz_role_permissions (role_id, permission) VALUES (?, ?)")) {
            ps.setObject(1, roleId);
            ps.setString(2, permission);
            ps.executeUpdate();
        }
    }

    private static void attribution(UUID tenant, UUID user, String code) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO authz_member_roles (tenant_id, user_id, "
                + "role_code, assigned_by, assigned_at) VALUES (?, ?, ?, ?, ?)")) {
            ps.setObject(1, tenant);
            ps.setObject(2, user);
            ps.setString(3, code);
            ps.setObject(4, UUID.randomUUID());
            ps.setTimestamp(5, Timestamp.from(Instant.now()));
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
    void unRoleSeRegleUneFoisParClientEtSesDroitsPartentAvecLui() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID pilote = role(tenant, "PILOTE", "Pilote", false);
        droit(pilote, "nc.close");
        droit(pilote, "capa.action.update");

        assertThatThrownBy(() -> role(tenant, "PILOTE", "Autre", false)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> droit(pilote, "nc.close")).isInstanceOf(SQLException.class);
        // Le même code dans un autre client est un autre rôle.
        role(UUID.randomUUID(), "PILOTE", "Pilote", false);

        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM authz_roles WHERE id = ?")) {
            ps.setObject(1, pilote);
            ps.executeUpdate();
        }
        assertThat(compte("SELECT count(*) FROM authz_role_permissions WHERE role_id = ?", pilote)).isZero();
    }

    @Test
    void laBaseRefuseCeQueLeDomaineRefuse() {
        UUID tenant = UUID.randomUUID();
        // Le super administrateur appartient à l'éditeur.
        assertThatThrownBy(() -> role(tenant, "SUPER_ADMIN", null, true)).isInstanceOf(SQLException.class);
        // Code mal formé.
        assertThatThrownBy(() -> role(tenant, "pilote", "P", false)).isInstanceOf(SQLException.class);
        // Un rôle sur mesure porte un nom ; un rôle système réglé peut garder le sien.
        assertThatThrownBy(() -> role(tenant, "SANS_NOM", null, false)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> attribution(tenant, UUID.randomUUID(), "SUPER_ADMIN")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> attribution(tenant, UUID.randomUUID(), "bad code")).isInstanceOf(SQLException.class);
    }

    @Test
    void unRoleSystemeSeRegleSansNomEtSAttribueSansLigneDeRole() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID user = role(tenant, "USER", null, true);
        assertThatThrownBy(() -> droit(user, "Mauvais Code")).isInstanceOf(SQLException.class);

        UUID membre = UUID.randomUUID();
        attribution(tenant, membre, "AUDITOR");
        attribution(tenant, membre, "USER");
        assertThatThrownBy(() -> attribution(tenant, membre, "USER")).isInstanceOf(SQLException.class);
        assertThat(compte("SELECT count(*) FROM authz_member_roles WHERE user_id = ?", membre)).isEqualTo(2);
    }
}

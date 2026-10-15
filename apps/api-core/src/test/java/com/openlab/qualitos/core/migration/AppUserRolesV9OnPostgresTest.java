package com.openlab.qualitos.core.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V9 sur un vrai PostgreSQL, rejouée SUR DES DONNÉES : un schéma neuf est
 * monté jusqu'à la V8, garni de rôles à l'ancienne (majuscules, {@code ADMIN}),
 * puis migré. Les lignes doivent sortir dans la forme du realm, sans doublon,
 * et la nouvelle contrainte doit refuser l'ancienne écriture.
 */
@Tag("migration")
class AppUserRolesV9OnPostgresTest {

    /** Un schéma à part : le serveur est partagé avec les autres bancs. */
    private static final String SCHEMA = "v9_roles";

    @BeforeAll
    static void docker() {
        assumeTrue(MigrationPostgres.dockerAvailable(),
                "Docker indisponible : la V9 reste non verifiee sur cette machine");
    }

    @Test
    void lesAnciensRolesPassentALaFormeDuRealm() throws SQLException {
        Flyway jusquA8 = flyway(MigrationVersion.fromVersion("8"));
        jusquA8.clean();
        jusquA8.migrate();

        try (Connection c = DriverManager.getConnection(
                MigrationPostgres.jdbcUrl() + "&currentSchema=" + SCHEMA,
                MigrationPostgres.username(), MigrationPostgres.password())) {
            UUID tenant = UUID.randomUUID();
            try (PreparedStatement st = c.prepareStatement("INSERT INTO tenants (id, slug, name) VALUES (?, ?, ?)")) {
                st.setObject(1, tenant);
                st.setString(2, "v9-roles");
                st.setString(3, "V9");
                st.executeUpdate();
            }
            UUID admin = user(c, tenant, "admin@v9.example");
            UUID manager = user(c, tenant, "manager@v9.example");
            role(c, admin, "ADMIN");
            role(c, admin, "QUALITY_DIRECTOR");
            role(c, manager, "QUALITY_MANAGER");
            role(c, manager, "USER");

            flyway(MigrationVersion.LATEST).migrate();

            assertThat(roles(c, admin)).containsExactly("admin_tenant", "quality_director");
            assertThat(roles(c, manager)).containsExactly("quality_manager", "user");

            // La contrainte parle la langue du realm, et elle seule.
            role(c, manager, "auditor");
            assertThatThrownBy(() -> role(c, manager, "AUDITOR")).hasMessageContaining("chk_app_user_roles_role");
            assertThatThrownBy(() -> role(c, manager, "admin")).hasMessageContaining("chk_app_user_roles_role");
        }
    }

    private static Flyway flyway(MigrationVersion target) {
        return Flyway.configure()
                .dataSource(MigrationPostgres.jdbcUrl(), MigrationPostgres.username(), MigrationPostgres.password())
                .schemas(SCHEMA)
                .locations("classpath:db/migration")
                .target(target)
                .cleanDisabled(false)
                .load();
    }

    private static UUID user(Connection c, UUID tenant, String email) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement st = c.prepareStatement(
                "INSERT INTO app_users (id, tenant_id, keycloak_id, email) VALUES (?, ?, ?, ?)")) {
            st.setObject(1, id);
            st.setObject(2, tenant);
            st.setString(3, UUID.randomUUID().toString());
            st.setString(4, email);
            st.executeUpdate();
        }
        return id;
    }

    private static void role(Connection c, UUID user, String role) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("INSERT INTO app_user_roles (user_id, role) VALUES (?, ?)")) {
            st.setObject(1, user);
            st.setString(2, role);
            st.executeUpdate();
        }
    }

    private static Set<String> roles(Connection c, UUID user) throws SQLException {
        Set<String> roles = new TreeSet<>();
        try (PreparedStatement st = c.prepareStatement("SELECT role FROM app_user_roles WHERE user_id = ?")) {
            st.setObject(1, user);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) roles.add(rs.getString(1));
            }
        }
        return roles;
    }
}

package com.openlab.qualitos.core.migration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V8 (annuaire, ADR 0091) sur un vrai PostgreSQL : H2 ne rejoue pas les
 * migrations, et la recherche de l'annuaire repose sur deux comportements que
 * seul PostgreSQL peut confirmer — la valeur par défaut de {@code search_key}
 * pour une ligne écrite hors application, et l'échappement {@code !} du LIKE.
 */
@Tag("migration")
class AppUserDirectoryOnPostgresTest {

    private static Connection connection;

    @BeforeAll
    static void migrate() throws SQLException {
        assumeTrue(MigrationPostgres.dockerAvailable(),
                "Docker indisponible : la migration de l'annuaire reste non verifiee sur cette machine");
        connection = MigrationPostgres.connect();
    }

    @AfterAll
    static void close() throws SQLException {
        if (connection != null) connection.close();
    }

    @Test
    void aMemberWrittenOutsideTheApplicationGetsAnEmptyKeyAndNoName() throws SQLException {
        UUID tenant = insertTenant("annuaire-defaut");
        UUID user = insertUser(tenant, "defaut@acme.example", null);

        try (PreparedStatement st = connection.prepareStatement(
                "SELECT display_name, search_key FROM app_users WHERE id = ?")) {
            st.setObject(1, user);
            try (ResultSet rs = st.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("display_name")).isNull();
                assertThat(rs.getString("search_key")).isEmpty();
            }
        }
    }

    @Test
    void theEscapedLikeTreatsWildcardsLiterally() throws SQLException {
        UUID tenant = insertTenant("annuaire-like");
        insertUser(tenant, "pct@acme.example", "taux 100% conforme");
        insertUser(tenant, "autre@acme.example", "taux 1000 conforme");

        assertThat(keysMatching(tenant, "%100!%%")).containsExactly("taux 100% conforme");
        assertThat(keysMatching(tenant, "%taux%")).hasSize(2);
    }

    @Test
    void theDirectorySortIndexExists() throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_app_users_directory'");
             ResultSet rs = st.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).contains("WHERE (active = true)");
        }
    }

    private static List<String> keysMatching(UUID tenant, String pattern) throws SQLException {
        List<String> keys = new ArrayList<>();
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT search_key FROM app_users WHERE tenant_id = ? AND search_key LIKE ? ESCAPE '!'"
                        + " ORDER BY search_key")) {
            st.setObject(1, tenant);
            st.setString(2, pattern);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) keys.add(rs.getString(1));
            }
        }
        return keys;
    }

    private static UUID insertTenant(String slug) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO tenants (id, slug, name, plan, active, created_at, updated_at)"
                        + " VALUES (?, ?, 'Client de test', 'STARTER', TRUE, now(), now())")) {
            st.setObject(1, id);
            st.setString(2, slug);
            st.executeUpdate();
        }
        return id;
    }

    /** {@code searchKey} null : la colonne garde sa valeur par défaut. */
    private static UUID insertUser(UUID tenant, String email, String searchKey) throws SQLException {
        UUID id = UUID.randomUUID();
        String sql = searchKey == null
                ? "INSERT INTO app_users (id, tenant_id, keycloak_id, email) VALUES (?, ?, ?, ?)"
                : "INSERT INTO app_users (id, tenant_id, keycloak_id, email, search_key) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setObject(1, id);
            st.setObject(2, tenant);
            st.setString(3, UUID.randomUUID().toString());
            st.setString(4, email);
            if (searchKey != null) st.setString(5, searchKey);
            st.executeUpdate();
        }
        return id;
    }
}

package com.openlab.qualitos.quality.rls;

import com.openlab.qualitos.quality.common.TenantContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * L'isolation par la base, éprouvée sur un vrai PostgreSQL (ADR 0086) : toutes
 * les migrations, puis le rôle applicatif tel que la production l'utilise.
 * Une doublure ne dirait rien ici — tout se joue dans le moteur de la base.
 */
@Tag("migration")
class RlsPostgresTest {

    private static final String APP = "qualitos_app";
    // Tiré à chaque passage : aucun secret, même factice, dans le dépôt.
    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static PostgreSQLContainer<?> postgres;
    private static Connection owner;

    @BeforeAll
    static void migrate() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : l'isolation par la base reste non vérifiée sur cette machine");
        postgres = new PostgreSQLContainer<>("postgres:17-alpine");
        postgres.start();
        RlsProperties props = props(true);
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .callbacks(new RlsConfiguration.RlsCallback(props))
                .load()
                .migrate();
        owner = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        // Une table de banc, comme en créerait une migration future : couverte au passage suivant.
        exec(owner, "CREATE TABLE rls_banc (id serial PRIMARY KEY, tenant_id uuid, valeur text)");
        exec(owner, "INSERT INTO rls_banc (tenant_id, valeur) VALUES "
                + "('" + A + "', 'a'), ('" + B + "', 'b'), (NULL, 'partage')");
        RlsConfiguration.apply(owner, props);
    }

    @AfterAll
    static void stop() throws SQLException {
        if (owner != null) owner.close();
        if (postgres != null) postgres.stop();
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void chaqueTableAClientPorteLaPolitique() throws SQLException {
        int tables = count(owner, "SELECT count(*) FROM information_schema.columns c "
                + "JOIN pg_class t ON t.relname = c.table_name AND t.relkind IN ('r','p') "
                + "WHERE c.table_schema = 'public' AND c.column_name = 'tenant_id' "
                + "AND c.data_type IN ('uuid','text','character varying')");
        int policies = count(owner, "SELECT count(*) FROM pg_policies WHERE policyname = '" + RlsSchema.POLICY + "'");
        assertThat(tables).isGreaterThan(50);
        assertThat(policies).isEqualTo(tables);
        assertThat(count(owner, "SELECT count(*) FROM pg_class WHERE relname = 'rls_banc' AND relrowsecurity")).isEqualTo(1);
    }

    @Test
    void unClientNeVoitQueSesLignesEtLesLignesPartagees() throws SQLException {
        try (SingleConnectionDataSource pool = appPool()) {
            TenantContext.setTenantId(A.toString());
            assertThat(values(new TenantAwareDataSource(pool))).containsExactlyInAnyOrder("a", "partage");
            TenantContext.setTenantId(B.toString());
            assertThat(values(new TenantAwareDataSource(pool))).containsExactlyInAnyOrder("b", "partage");
        }
    }

    @Test
    void sansClientLesTravauxDeFondVoientTout() throws SQLException {
        try (SingleConnectionDataSource pool = appPool()) {
            assertThat(values(new TenantAwareDataSource(pool))).containsExactlyInAnyOrder("a", "b", "partage");
        }
    }

    @Test
    void leClientDUnEmpruntNeDeteintPasSurLeSuivant() throws SQLException {
        try (SingleConnectionDataSource pool = appPool()) {
            TenantAwareDataSource ds = new TenantAwareDataSource(pool);
            TenantContext.setTenantId(A.toString());
            assertThat(values(ds)).containsExactlyInAnyOrder("a", "partage");
            TenantContext.clear();
            // MÊME connexion physique, rendue puis reprise.
            assertThat(values(ds)).containsExactlyInAnyOrder("a", "b", "partage");
        }
    }

    @Test
    void unClientNePeutPasEcrireChezUnAutre() throws SQLException {
        try (SingleConnectionDataSource pool = appPool()) {
            TenantContext.setTenantId(A.toString());
            try (Connection c = new TenantAwareDataSource(pool).getConnection()) {
                assertThatThrownBy(() -> exec(c, "INSERT INTO rls_banc (tenant_id, valeur) VALUES ('" + B + "', 'intrus')"))
                        .hasMessageContaining("row-level security");
                assertThat(update(c, "UPDATE rls_banc SET valeur = 'vole' WHERE valeur = 'b'")).isZero();
                assertThat(update(c, "DELETE FROM rls_banc WHERE valeur = 'b'")).isZero();
            }
        }
    }

    @Test
    void leRoleApplicatifNEstNiSuperutilisateurNiMaitreDesMigrations() throws SQLException {
        assertThat(count(owner, "SELECT count(*) FROM pg_roles WHERE rolname = '" + APP
                + "' AND NOT rolsuper AND NOT rolbypassrls")).isEqualTo(1);
        try (Connection app = DriverManager.getConnection(postgres.getJdbcUrl(), APP, APP_PASSWORD)) {
            assertThatThrownBy(() -> count(app, "SELECT count(*) FROM flyway_schema_history"))
                    .hasMessageContaining("permission denied");
        }
    }

    @Test
    void desactiverRendLeComportementDAvant() throws SQLException {
        RlsConfiguration.apply(owner, props(false));
        try (SingleConnectionDataSource pool = appPool()) {
            TenantContext.setTenantId(A.toString());
            assertThat(values(new TenantAwareDataSource(pool))).containsExactlyInAnyOrder("a", "b", "partage");
        } finally {
            RlsConfiguration.apply(owner, props(true));
        }
    }

    @Test
    void rejouerNeChangeRien() throws SQLException {
        RlsConfiguration.apply(owner, props(true));
        RlsConfiguration.apply(owner, props(true));
        try (SingleConnectionDataSource pool = appPool()) {
            TenantContext.setTenantId(A.toString());
            assertThat(values(new TenantAwareDataSource(pool))).containsExactlyInAnyOrder("a", "partage");
        }
    }

    // ---------------------------------------------------------------------------

    private static RlsProperties props(boolean enabled) {
        RlsProperties p = new RlsProperties();
        p.setEnabled(enabled);
        p.setAppUser(APP);
        p.setAppPassword(APP_PASSWORD);
        return p;
    }

    private static SingleConnectionDataSource appPool() {
        return new SingleConnectionDataSource(postgres.getJdbcUrl(), APP, APP_PASSWORD, true);
    }

    private static List<String> values(javax.sql.DataSource ds) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT valeur FROM rls_banc")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    private static void exec(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private static int update(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            return st.executeUpdate(sql);
        }
    }

    private static int count(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}

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
 * La V160 sur un vrai moteur : un écart porte le cycle PDCA ou le projet DMAIC
 * qui le traite — jamais les deux.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore les migrations : une contrainte CHECK oubliée ne se verrait qu'en
 * production, le jour où un double clic ouvrirait deux chantiers sur le même
 * écart et où chacun compterait dans les indicateurs.
 */
@Tag("migration")
class NcConversionOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";
    private static final UUID TENANT = UUID.randomUUID();

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V160 reste non verifiee sur cette machine");
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
    @DisplayName("un écart converti en cycle PDCA garde le lien")
    void leLienVersLeCycle() throws SQLException {
        UUID cycle = UUID.randomUUID();

        ecart("NC-2026-0101", cycle, null);

        assertThat(compter("SELECT count(*) FROM non_conformities WHERE tenant_id = ? "
                + "AND reference = 'NC-2026-0101' AND pdca_cycle_id = ? AND dmaic_project_id IS NULL",
                TENANT, cycle)).isEqualTo(1);
    }

    @Test
    @DisplayName("un écart converti en projet DMAIC garde le lien")
    void leLienVersLeProjet() throws SQLException {
        UUID projet = UUID.randomUUID();

        ecart("NC-2026-0102", null, projet);

        assertThat(compter("SELECT count(*) FROM non_conformities WHERE tenant_id = ? "
                + "AND reference = 'NC-2026-0102' AND dmaic_project_id = ? AND pdca_cycle_id IS NULL",
                TENANT, projet)).isEqualTo(1);
    }

    @Test
    @DisplayName("les deux à la fois : la base refuse, pas seulement le service")
    void jamaisLesDeux() {
        // Deux chantiers ouverts sur le meme ecart le traiteraient deux fois, et
        // chacun compterait dans les indicateurs. Le service le refuse ; un
        // import, un correctif en base ou un second service ne passeraient pas
        // par lui.
        assertThatThrownBy(() -> ecart("NC-2026-0103", UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_non_conformities_une_methode");
    }

    @Test
    @DisplayName("un écart non converti reste la situation normale")
    void niLUnNiLAutre() throws SQLException {
        ecart("NC-2026-0104", null, null);

        assertThat(compter("SELECT count(*) FROM non_conformities WHERE tenant_id = ? "
                + "AND reference = 'NC-2026-0104' AND pdca_cycle_id IS NULL "
                + "AND dmaic_project_id IS NULL", TENANT)).isEqualTo(1);
    }

    @Test
    @DisplayName("le lien n'est pas une dépendance : aucune clé étrangère ne le retient")
    void aucuneCleEtrangere() throws SQLException {
        // Le cycle, une fois ouvert, vit sa vie et peut etre archive par son
        // module. Une cle etrangere rendrait alors la fiche de l'ecart
        // inenregistrable, pour une trace qui n'est qu'une trace.
        assertThat(compter("""
                SELECT count(*) FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage k
                  ON k.constraint_name = tc.constraint_name
                WHERE tc.table_name = 'non_conformities'
                  AND tc.constraint_type = 'FOREIGN KEY'
                  AND k.column_name IN ('pdca_cycle_id', 'dmaic_project_id')
                """)).isZero();
    }

    @Test
    @DisplayName("les deux index partiels sont posés : « quel écart a ouvert ce cycle ? »")
    void lesIndexPartiels() throws SQLException {
        assertThat(compter("SELECT count(*) FROM pg_indexes WHERE tablename = 'non_conformities' "
                + "AND indexname = 'ix_non_conformities_pdca_cycle'")).isEqualTo(1);
        assertThat(compter("SELECT count(*) FROM pg_indexes WHERE tablename = 'non_conformities' "
                + "AND indexname = 'ix_non_conformities_dmaic_project'")).isEqualTo(1);
    }

    // ---------- aides ----------

    private static void ecart(String reference, UUID cycle, UUID projet) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO non_conformities
                  (id, tenant_id, reference, title, category, severity, status,
                   origin, detected_at, pdca_cycle_id, dmaic_project_id, created_at, updated_at)
                VALUES (?, ?, ?, 'Jeu excessif sur le mandrin', 'PROCESS', 'MAJOR', 'OPEN',
                        'INTERNAL', ?, ?, ?, ?, ?)
                """)) {
            Timestamp maintenant = Timestamp.from(Instant.parse("2026-11-09T08:00:00Z"));
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, TENANT);
            ps.setString(3, reference);
            ps.setTimestamp(4, maintenant);
            ps.setObject(5, cycle);
            ps.setObject(6, projet);
            ps.setTimestamp(7, maintenant);
            ps.setTimestamp(8, maintenant);
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

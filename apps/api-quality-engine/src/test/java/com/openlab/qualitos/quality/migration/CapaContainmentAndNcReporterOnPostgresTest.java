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
 * Ce que la suite H2 ne peut pas dire de ces deux migrations.
 *
 * <p>H2 en mode PostgreSQL applique bien une contrainte CHECK, mais rien ne
 * garantit que la V120 — qui remplace une contrainte existante — s'applique
 * proprement sur une base où l'ancienne est déjà posée. Or c'est exactement ce
 * qui se passera en préproduction. Un ALTER qui échoue à cet endroit laisserait
 * un domaine fermé sur CORRECTIVE/PREVENTIVE, et le premier dossier
 * d'endiguement partirait en erreur de base, longtemps après le déploiement.
 *
 * <p>Ce banc rejoue TOUTES les migrations dans l'ordre sur un vrai PostgreSQL,
 * puis écrit réellement les lignes concernées. Sans Docker, il se saute — mais
 * il dit pourquoi.
 */
@Tag("migration")
class CapaContainmentAndNcReporterOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrateOnARealServer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : ces deux migrations restent non vérifiées sur cette machine");

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
        if (connection != null) connection.close();
        if (postgres != null) postgres.stop();
    }

    // ---------- V120 : le dossier d'endiguement ----------

    @Test
    void aContainmentCaseIsAcceptedAfterTheConstraintHasBeenReplaced() throws SQLException {
        UUID tenant = UUID.randomUUID();

        UUID id = insertCapaCase(tenant, "Lot 4471 bloqué", "CONTAINMENT");

        assertThat(readCaseType(id)).isEqualTo("CONTAINMENT");
    }

    @Test
    void theTwoHistoricalTypesStillPass() throws SQLException {
        // Remplacer une contrainte, c'est risquer d'en oublier la moitié.
        UUID tenant = UUID.randomUUID();

        assertThat(readCaseType(insertCapaCase(tenant, "Corriger la presse", "CORRECTIVE")))
                .isEqualTo("CORRECTIVE");
        assertThat(readCaseType(insertCapaCase(tenant, "Anticiper la dérive", "PREVENTIVE")))
                .isEqualTo("PREVENTIVE");
    }

    @Test
    void aTypeInventedByAnImportScriptIsStillRefusedAtTheDatabase() throws SQLException {
        // Le domaine reste fermé : une valeur inventée doit échouer ici, pas
        // plus tard au premier chargement JPA, avec un message qui ne dirait
        // pas d'où elle vient.
        UUID tenant = UUID.randomUUID();

        assertThatThrownBy(() -> insertCapaCase(tenant, "Type fantaisiste", "MITIGATION"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_capa_cases_type");
    }

    // ---------- V121 : « détecté par » ----------

    @Test
    void aNonConformityCarriesTheNameOfWhoeverReportedIt() throws SQLException {
        UUID tenant = UUID.randomUUID();

        UUID id = insertNonConformity(tenant, "NC-2026-9001", "Amina Dridi");

        assertThat(readReporterName(id)).isEqualTo("Amina Dridi");
    }

    @Test
    void aNonConformityOlderThanTheColumnKeepsAnEmptyNameRatherThanAnInventedOne() throws SQLException {
        UUID tenant = UUID.randomUUID();

        UUID id = insertNonConformity(tenant, "NC-2026-9002", null);

        assertThat(readReporterName(id)).isNull();
    }

    // ---------- helpers ----------

    private UUID insertCapaCase(UUID tenant, String title, String type) throws SQLException {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO capa_cases (id, tenant_id, title, type, criticity, status,
                                        source_type, owner_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'MEDIUM', 'OPEN', 'INTERNAL', ?, ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, title);
            ps.setString(4, type);
            ps.setObject(5, UUID.randomUUID());
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);
            ps.executeUpdate();
        }
        return id;
    }

    private String readCaseType(UUID id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT type FROM capa_cases WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getString(1);
            }
        }
    }

    private UUID insertNonConformity(UUID tenant, String reference, String reporterName)
            throws SQLException {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO non_conformities (id, tenant_id, reference, title, category, severity,
                                              status, detected_at, reporter_name,
                                              created_at, updated_at)
                VALUES (?, ?, ?, 'Étiquetage illisible', 'PROCESS', 'MAJOR', 'OPEN', ?, ?, ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, reference);
            ps.setTimestamp(4, now);
            ps.setString(5, reporterName);
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);
            ps.executeUpdate();
        }
        return id;
    }

    private String readReporterName(UUID id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT reporter_name FROM non_conformities WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getString(1);
            }
        }
    }
}

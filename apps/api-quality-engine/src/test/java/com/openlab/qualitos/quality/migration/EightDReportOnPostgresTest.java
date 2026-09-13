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
 * Ce que la V130 garantit, vérifié sur un vrai moteur.
 *
 * <p>Trois invariants ne vivent que dans la base, et une doublure de dépôt n'en
 * dirait rien : un seul rapport par non-conformité, un rapport émis qui porte ses six
 * champs de preuve, et la disparition du rapport avec la non-conformité qu'il décrit.
 * L'agrégat les tient aussi — mais une écriture qui contournerait le service laisserait
 * sinon une ligne indéfendable.
 */
@Tag("migration")
class EightDReportOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID NC = UUID.randomUUID();
    private static final String SHA = "a".repeat(64);

    @BeforeAll
    static void migrate() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V130 reste non verifiee sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        insererNonConformite(NC);
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
    void un_brouillon_s_ecrit_sans_aucun_champ_de_preuve() throws SQLException {
        UUID id = UUID.randomUUID();
        insererRapport(id, NC, "DRAFT", null, null, null, null, null, null, null);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT status, team FROM nc_eightd_reports WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("status")).isEqualTo("DRAFT");
            }
        }
        supprimerRapports();
    }

    @Test
    void un_rapport_emis_sans_ancrage_est_refuse_par_la_base() {
        // Une empreinte signée mais non ancrée est une demi-preuve : la base la refuse,
        // et pas seulement l'agrégat.
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> insererRapport(id, NC, "ISSUED", "{}", SHA, "enveloppe",
                null, "ABCDEFGHIJKLMNOP", Instant.now(), UUID.randomUUID()))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_nc_eightd_issued_is_sealed");
    }

    @Test
    void un_rapport_emis_complet_est_accepte() throws SQLException {
        UUID id = UUID.randomUUID();
        insererRapport(id, NC, "ISSUED", "{}", SHA, "enveloppe", "tx-1",
                "ABCDEFGHIJKLMNOP", Instant.now(), UUID.randomUUID());

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT anchor_tx_ref FROM nc_eightd_reports WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("anchor_tx_ref")).isEqualTo("tx-1");
            }
        }
        supprimerRapports();
    }

    @Test
    void un_statut_inconnu_est_refuse() {
        assertThatThrownBy(() -> insererRapport(UUID.randomUUID(), NC, "PUBLIE",
                null, null, null, null, null, null, null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_nc_eightd_status");
    }

    @Test
    void deux_rapports_pour_la_meme_non_conformite_sont_refuses() throws SQLException {
        // Deux émissions concurrentes produiraient deux documents de même référence et
        // d'empreintes différentes, dont aucun ne serait opposable.
        insererRapport(UUID.randomUUID(), NC, "DRAFT", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> insererRapport(UUID.randomUUID(), NC, "DRAFT",
                null, null, null, null, null, null, null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_nc_eightd_reports_nc");

        supprimerRapports();
    }

    @Test
    void deux_rapports_ne_partagent_pas_un_code_de_verification() throws SQLException {
        UUID autreNc = UUID.randomUUID();
        insererNonConformite(autreNc);
        insererRapport(UUID.randomUUID(), NC, "ISSUED", "{}", SHA, "env", "tx-1",
                "CODEPARTAGE123456", Instant.now(), UUID.randomUUID());

        // Le code est l'autorité d'une route qui n'a aucun tenant : il doit être unique
        // globalement, sans quoi la vérification publique désignerait deux documents.
        assertThatThrownBy(() -> insererRapport(UUID.randomUUID(), autreNc, "ISSUED", "{}", SHA,
                "env", "tx-2", "CODEPARTAGE123456", Instant.now(), UUID.randomUUID()))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_nc_eightd_reports_code");

        supprimerRapports();
        supprimerNonConformite(autreNc);
    }

    @Test
    void le_rapport_disparait_avec_la_non_conformite_qu_il_decrit() throws SQLException {
        UUID ncSupprimee = UUID.randomUUID();
        insererNonConformite(ncSupprimee);
        insererRapport(UUID.randomUUID(), ncSupprimee, "DRAFT",
                null, null, null, null, null, null, null);

        supprimerNonConformite(ncSupprimee);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM nc_eightd_reports WHERE nc_id = ?")) {
            ps.setObject(1, ncSupprimee);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                // Un rapport 8D n'a pas d'existence propre : il décrit un écart.
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    // ---------- écritures de banc ----------

    private static void insererNonConformite(UUID id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO non_conformities
                  (id, tenant_id, reference, title, category, severity, status,
                   origin, detected_at, created_at, updated_at)
                VALUES (?, ?, ?, 'Fuite au presse-etoupe', 'PROCESS', 'MAJOR', 'CLOSED',
                        'INTERNAL', ?, ?, ?)
                """)) {
            Timestamp maintenant = Timestamp.from(Instant.parse("2026-09-13T10:00:00Z"));
            ps.setObject(1, id);
            ps.setObject(2, TENANT);
            ps.setString(3, "NC-2026-" + id.toString().substring(0, 4));
            ps.setTimestamp(4, maintenant);
            ps.setTimestamp(5, maintenant);
            ps.setTimestamp(6, maintenant);
            ps.executeUpdate();
        }
    }

    private static void supprimerNonConformite(UUID id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM non_conformities WHERE id = ?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    private static void supprimerRapports() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM nc_eightd_reports")) {
            ps.executeUpdate();
        }
    }

    private static void insererRapport(UUID id, UUID ncId, String status, String snapshot,
                                       String sha256, String signature, String anchorTxRef,
                                       String code, Instant issuedAt, UUID issuedBy)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO nc_eightd_reports
                  (id, tenant_id, nc_id, status, snapshot_json, sha256_hex, signature,
                   anchor_tx_ref, verification_code, issued_at, issued_by,
                   created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            Timestamp maintenant = Timestamp.from(Instant.parse("2026-09-13T10:00:00Z"));
            ps.setObject(1, id);
            ps.setObject(2, TENANT);
            ps.setObject(3, ncId);
            ps.setString(4, status);
            ps.setString(5, snapshot);
            ps.setString(6, sha256);
            ps.setString(7, signature);
            ps.setString(8, anchorTxRef);
            ps.setString(9, code);
            ps.setTimestamp(10, issuedAt == null ? null : Timestamp.from(issuedAt));
            ps.setObject(11, issuedBy);
            ps.setTimestamp(12, maintenant);
            ps.setTimestamp(13, maintenant);
            ps.executeUpdate();
        }
    }
}

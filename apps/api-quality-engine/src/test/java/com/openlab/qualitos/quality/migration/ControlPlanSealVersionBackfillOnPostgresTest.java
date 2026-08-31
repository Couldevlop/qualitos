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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Le rattrapage de la V122, vérifié sur des lignes ÉCRITES AVANT elle.
 *
 * <p>Un test qui rejoue toutes les migrations d'un coup sur une base vide ne
 * peut rien dire d'un rattrapage : il n'y a rien à rattraper. Ce banc migre donc
 * jusqu'à la V121, écrit deux plans — l'un scellé, l'autre non — puis applique
 * la V122 et regarde ce qu'elle en a fait.
 *
 * <p>L'enjeu est précis. Un plan scellé avant le versionnement l'a été avec le
 * calcul d'origine, incomplet. S'il ressortait marqué « version 2 », un auditeur
 * rejouerait le hachage avec le calcul complet, obtiendrait une autre valeur, et
 * conclurait à une falsification sur un document intact.
 */
@Tag("migration")
class ControlPlanSealVersionBackfillOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SEALED = UUID.randomUUID();
    private static final UUID DRAFT = UUID.randomUUID();

    @BeforeAll
    static void migrateInTwoSteps() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : le rattrapage de la V122 reste non vérifié sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        // Étape 1 : l'état du monde AVANT la migration étudiée.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("121")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        insertProduct();
        insertPlan(SEALED, "CP-SEALED", "9f2c1b7ea3d4");
        insertPlan(DRAFT, "CP-DRAFT", null);

        // Étape 2 : la migration à l'étude, sur des données existantes.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @AfterAll
    static void stop() throws SQLException {
        if (connection != null) connection.close();
        if (postgres != null) postgres.stop();
    }

    @Test
    void aPlanSealedBeforeTheVersioningIsMarkedAsVersionOne() throws SQLException {
        // La seule version qui existait quand il a été scellé.
        assertThat(sealVersionOf(SEALED)).isEqualTo(1);
    }

    @Test
    void aPlanThatWasNeverSealedCarriesNoVersionAtAll() throws SQLException {
        // 0 se lit « pas encore scellé », et non « scellé avec le calcul d'origine ».
        assertThat(sealVersionOf(DRAFT)).isZero();
    }

    @Test
    void noSealIsRecomputedByTheMigration() throws SQLException {
        // Une preuve passée ne se réécrit pas : l'empreinte doit être exactement
        // celle qui a été signée puis ancrée.
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT seal_sha256 FROM control_plans WHERE id = ?")) {
            ps.setObject(1, SEALED);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("9f2c1b7ea3d4");
            }
        }
    }

    @Test
    void theSpecifiedCharacteristicColumnAcceptsTheTrameValue() throws SQLException {
        UUID line = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO control_plan_lines (id, tenant_id, plan_id, sequence_no,
                                                characteristic_label, characteristic_type,
                                                special_class, specified_characteristic)
                VALUES (?, ?, ?, 10, 'Longueur de fil', 'PRODUCT', 'STANDARD', ?)
                """)) {
            ps.setObject(1, line);
            ps.setObject(2, TENANT);
            ps.setObject(3, DRAFT);
            ps.setString(4, "Cote de coupe");
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT specified_characteristic FROM control_plan_lines WHERE id = ?")) {
            ps.setObject(1, line);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("Cote de coupe");
            }
        }
    }

    // ---------- montage ----------

    private static void insertProduct() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO products (id, tenant_id, code, designation, status,
                                      created_by, created_at, updated_at)
                VALUES (?, ?, 'REF-4471', 'Faisceau', 'ACTIVE', ?, ?, ?)
                """)) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, PRODUCT);
            ps.setObject(2, TENANT);
            ps.setObject(3, UUID.randomUUID());
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.executeUpdate();
        }
    }

    private static final UUID PRODUCT = UUID.randomUUID();

    private static void insertPlan(UUID id, String code, String seal) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO control_plans (id, tenant_id, product_id, phase, code, revision,
                                           status, created_by, created_at, updated_at,
                                           seal_sha256, seal_signature, anchor_tx_ref)
                VALUES (?, ?, ?, 'PRODUCTION', ?, 1, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setObject(2, TENANT);
            ps.setObject(3, PRODUCT);
            ps.setString(4, code);
            ps.setString(5, seal != null ? "ACTIVE" : "DRAFT");
            ps.setObject(6, UUID.randomUUID());
            ps.setTimestamp(7, now);
            ps.setTimestamp(8, now);
            ps.setString(9, seal);
            ps.setString(10, seal != null ? "signature" : null);
            ps.setString(11, seal != null ? "tx-1" : null);
            ps.executeUpdate();
        }
    }

    private int sealVersionOf(UUID planId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT seal_version FROM control_plans WHERE id = ?")) {
            ps.setObject(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getInt(1);
            }
        }
    }
}

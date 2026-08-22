package com.openlab.qualitos.quality.migration;

import com.openlab.qualitos.quality.risk.ActionPriority;
import com.openlab.qualitos.quality.risk.ActionPriorityCalculator;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que H2 ne peut pas dire.
 *
 * <p>La suite tourne sur H2 en mode PostgreSQL, et H2 ignore les index partiels :
 * il les accepte à la création sans jamais les faire mordre. Les garanties
 * d'unicité conditionnelles de ce lot — un seul PFMEA en vigueur par produit, un
 * seul control plan en vigueur par phase, une seule demande de révision en
 * attente par cible — n'y sont donc vérifiées par rien.
 *
 * <p>Ce banc démarre un vrai PostgreSQL, y rejoue TOUTES les migrations dans
 * l'ordre, puis vérifie que les index existent ET qu'ils refusent une seconde
 * écriture. Il vérifie aussi que le rattrapage de la V111 calcule exactement la
 * même priorité d'action que {@link ActionPriorityCalculator} : une divergence
 * donnerait un tri faux sur l'historique, sans la moindre erreur visible.
 *
 * <p>Sans Docker, le banc se saute plutôt qu'il n'échoue — mais il ne se tait
 * pas : il le dit dans le motif du saut.
 */
@Tag("migration")
class PartialIndexesOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrateOnARealServer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : les index partiels restent non vérifiés sur cette machine");

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

    // ---------- les index existent, avec leur condition ----------

    @Test
    void theThreeConditionalUniquenessIndexesExistWithTheirCondition() throws SQLException {
        assertThat(indexDefinition("uk_pfmea_active_per_product"))
                .contains("UNIQUE")
                .contains("product_id IS NOT NULL")
                .contains("PROCESS_FMEA")
                .contains("ACTIVE");
        assertThat(indexDefinition("uk_control_plan_active_per_phase"))
                .contains("UNIQUE")
                .contains("phase")
                .contains("ACTIVE");
        assertThat(indexDefinition("uk_revision_request_pending"))
                .contains("UNIQUE")
                .contains("PENDING")
                .contains("target_id IS NOT NULL");
    }

    // ---------- et ils mordent ----------

    @Test
    void aProductCannotCarryTwoActiveProcessFmeas() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID product = insertProduct(tenant, "REF-PFMEA");
        insertFmeaProject(tenant, product, "PF-1", "PROCESS_FMEA", "ACTIVE");

        assertThatThrownBy(() -> insertFmeaProject(tenant, product, "PF-2", "PROCESS_FMEA", "ACTIVE"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_pfmea_active_per_product");
    }

    @Test
    void aDesignFmeaAndADraftCoexistWithTheActiveProcessFmea() throws SQLException {
        // L'index est partiel pour cela : les révisions passées et le brouillon en
        // préparation ne doivent pas entrer en collision avec le document applicable.
        UUID tenant = UUID.randomUUID();
        UUID product = insertProduct(tenant, "REF-COEXIST");
        insertFmeaProject(tenant, product, "PF-1", "PROCESS_FMEA", "ACTIVE");

        insertFmeaProject(tenant, product, "DF-1", "DESIGN_FMEA", "ACTIVE");
        insertFmeaProject(tenant, product, "PF-2", "PROCESS_FMEA", "DRAFT");
        insertFmeaProject(tenant, product, "PF-0", "PROCESS_FMEA", "ARCHIVED");

        assertThat(countFmeaProjects(tenant, product)).isEqualTo(4);
    }

    @Test
    void twoTenantsMayHoldTheSameProductShapeWithoutColliding() throws SQLException {
        // Le tenant fait partie de la clé : sans lui, le premier tenant à activer
        // un PFMEA empêcherait tous les autres de le faire.
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID productOfFirst = insertProduct(first, "REF-SHARED");
        UUID productOfSecond = insertProduct(second, "REF-SHARED");

        insertFmeaProject(first, productOfFirst, "PF-1", "PROCESS_FMEA", "ACTIVE");
        insertFmeaProject(second, productOfSecond, "PF-1", "PROCESS_FMEA", "ACTIVE");

        assertThat(countFmeaProjects(first, productOfFirst)).isEqualTo(1);
        assertThat(countFmeaProjects(second, productOfSecond)).isEqualTo(1);
    }

    @Test
    void aProductCannotCarryTwoActiveControlPlansOnTheSamePhase() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID product = insertProduct(tenant, "REF-CP");
        insertControlPlan(tenant, product, "CP-1", "PRODUCTION", "ACTIVE");

        assertThatThrownBy(() -> insertControlPlan(tenant, product, "CP-2", "PRODUCTION", "ACTIVE"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_control_plan_active_per_phase");
    }

    @Test
    void aProductInPreSeriesAndInSeriesLegitimatelyHoldsTwoActivePlans() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID product = insertProduct(tenant, "REF-CP-PHASES");

        insertControlPlan(tenant, product, "CP-1", "PRODUCTION", "ACTIVE");
        insertControlPlan(tenant, product, "CP-2", "PRE_LAUNCH", "ACTIVE");
        insertControlPlan(tenant, product, "CP-3", "PRODUCTION", "DRAFT");

        assertThat(countControlPlans(tenant, product)).isEqualTo(3);
    }

    @Test
    void onlyOneRevisionRequestMayWaitOnTheSameTarget() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID product = insertProduct(tenant, "REF-REV");
        UUID target = UUID.randomUUID();
        insertRevisionRequest(tenant, product, "PFMEA_ITEM", target, "PENDING");

        assertThatThrownBy(() -> insertRevisionRequest(tenant, product, "PFMEA_ITEM", target, "PENDING"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_revision_request_pending");
    }

    @Test
    void aSupersededRequestDoesNotBlockTheFreshOneThatReplacedIt() throws SQLException {
        // C'est tout l'intérêt de SUPERSEDED plutôt qu'une suppression : la trace
        // reste, sans bloquer la proposition suivante.
        UUID tenant = UUID.randomUUID();
        UUID product = insertProduct(tenant, "REF-REV-SUP");
        UUID target = UUID.randomUUID();
        insertRevisionRequest(tenant, product, "PFMEA_ITEM", target, "SUPERSEDED");
        insertRevisionRequest(tenant, product, "PFMEA_ITEM", target, "REJECTED");

        insertRevisionRequest(tenant, product, "PFMEA_ITEM", target, "PENDING");

        assertThat(countRevisionRequests(tenant, product)).isEqualTo(3);
    }

    @Test
    void severalCreationRequestsMayWaitTogetherBecauseTheyHaveNoTarget() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID product = insertProduct(tenant, "REF-REV-CREATE");

        insertRevisionRequest(tenant, product, "CONTROL_PLAN_LINE_CREATE", null, "PENDING");
        insertRevisionRequest(tenant, product, "CONTROL_PLAN_LINE_CREATE", null, "PENDING");

        assertThat(countRevisionRequests(tenant, product)).isEqualTo(2);
    }

    // ---------- le rattrapage de la V111 dit la même chose que le calculateur ----------

    @Test
    void theV111BackfillAgreesWithTheJavaMatrixOnEveryRating() throws SQLException {
        StringBuilder divergences = new StringBuilder();
        for (int severity = 1; severity <= 10; severity++) {
            for (int occurrence = 1; occurrence <= 10; occurrence++) {
                for (int detection = 1; detection <= 10; detection++) {
                    ActionPriority expected =
                            ActionPriorityCalculator.of(severity, occurrence, detection);
                    String actual = backfilledPriority(severity, occurrence, detection);
                    if (!expected.name().equals(actual)) {
                        divergences.append(String.format("%d/%d/%d : SQL=%s, Java=%s%n",
                                severity, occurrence, detection, actual, expected));
                    }
                }
            }
        }
        assertThat(divergences.toString()).isEmpty();
    }

    @Test
    void anUnratedRowIsLeftWithoutAnyPriority() throws SQLException {
        assertThat(backfilledPriority(0, 5, 5)).isNull();
        assertThat(backfilledPriority(5, 0, 5)).isNull();
        assertThat(backfilledPriority(5, 5, 0)).isNull();
    }

    // ---------- accès base ----------

    /**
     * Le CASE est recopié de la migration V111, à dessein : le test compare deux
     * expressions du même arbitrage, l'une en SQL, l'autre en Java. Les lire au
     * même endroit ferait disparaître la comparaison.
     */
    private static String backfilledPriority(int severity, int occurrence, int detection)
            throws SQLException {
        String sql = """
                SELECT CASE
                    WHEN ? < 1 OR ? < 1 OR ? < 1 THEN NULL
                    WHEN ? >= 9 THEN
                        CASE WHEN ? <= 2 AND ? <= 6 THEN 'MEDIUM' ELSE 'HIGH' END
                    WHEN ? >= 5 THEN
                        CASE WHEN ? >= 6 THEN 'HIGH'
                             WHEN ? >= 3 THEN CASE WHEN ? >= 7 THEN 'HIGH' ELSE 'MEDIUM' END
                             ELSE CASE WHEN ? >= 4 THEN 'MEDIUM' ELSE 'LOW' END END
                    ELSE
                        CASE WHEN ? >= 6 THEN 'MEDIUM'
                             ELSE CASE WHEN ? >= 7 THEN 'MEDIUM' ELSE 'LOW' END END
                END
                """;
        int[] arguments = {severity, occurrence, detection,
                severity, occurrence, detection,
                severity, occurrence, occurrence, detection, detection,
                occurrence, detection};
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < arguments.length; i++) {
                statement.setInt(i + 1, arguments[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getString(1);
            }
        }
    }

    private static String indexDefinition(String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT indexdef FROM pg_indexes WHERE indexname = ?")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                assertThat(rows.next()).as("index %s absent", name).isTrue();
                return rows.getString(1);
            }
        }
    }

    private static UUID insertProduct(UUID tenant, String code) throws SQLException {
        UUID id = UUID.randomUUID();
        execute("INSERT INTO products (id, tenant_id, code, designation, status,"
                        + " created_by, created_at, updated_at)"
                        + " VALUES (?, ?, ?, 'Support', 'ACTIVE', ?, now(), now())",
                id, tenant, code, UUID.randomUUID());
        return id;
    }

    private static void insertFmeaProject(UUID tenant, UUID product, String code,
                                          String type, String status) throws SQLException {
        execute("INSERT INTO fmea_projects (id, tenant_id, code, name, type, status,"
                        + " critical_rpn_threshold, revision, product_id, created_by,"
                        + " created_at, updated_at)"
                        + " VALUES (?, ?, ?, 'Assemblage', ?, ?, 100, 1, ?, ?, now(), now())",
                UUID.randomUUID(), tenant, code, type, status, product, UUID.randomUUID());
    }

    private static void insertControlPlan(UUID tenant, UUID product, String code,
                                          String phase, String status) throws SQLException {
        execute("INSERT INTO control_plans (id, tenant_id, product_id, code, phase, revision,"
                        + " status, created_by, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, 1, ?, ?, now(), now())",
                UUID.randomUUID(), tenant, product, code, phase, status, UUID.randomUUID());
    }

    private static void insertRevisionRequest(UUID tenant, UUID product, String targetType,
                                              UUID targetId, String status) throws SQLException {
        execute("INSERT INTO quality_revision_requests (id, tenant_id, product_id, target_type,"
                        + " target_id, trigger_type, trigger_ref_id, trigger_ref_label, rationale,"
                        + " proposed_change, status, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, 'NC_CREATED', ?, 'NC-1', 'justifié', '{}',"
                        + " ?, now(), now())",
                UUID.randomUUID(), tenant, product, targetType, targetId, UUID.randomUUID(), status);
    }

    private static int countFmeaProjects(UUID tenant, UUID product) throws SQLException {
        return count("SELECT count(*) FROM fmea_projects WHERE tenant_id = ? AND product_id = ?",
                tenant, product);
    }

    private static int countControlPlans(UUID tenant, UUID product) throws SQLException {
        return count("SELECT count(*) FROM control_plans WHERE tenant_id = ? AND product_id = ?",
                tenant, product);
    }

    private static int countRevisionRequests(UUID tenant, UUID product) throws SQLException {
        return count("SELECT count(*) FROM quality_revision_requests"
                + " WHERE tenant_id = ? AND product_id = ?", tenant, product);
    }

    private static int count(String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, arguments);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static void execute(String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, arguments);
            statement.executeUpdate();
        }
    }

    private static void bind(PreparedStatement statement, Object... arguments) throws SQLException {
        for (int i = 0; i < arguments.length; i++) {
            statement.setObject(i + 1, arguments[i]);
        }
    }
}

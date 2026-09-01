package com.openlab.qualitos.core.migration;

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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que H2 ne peut pas dire sur {@code subscriptions}.
 *
 * <p>Même motif que {@link ModulePriceConstraintsOnPostgresTest}, avec un enjeu
 * plus fort : la règle centrale des abonnements est un <b>index unique
 * PARTIEL</b> ({@code uk_subscription_vivante}), et un index partiel n'existe
 * pas en H2. Aucun banc Mockito ni aucun démarrage en {@code create-drop} ne
 * peut donc l'exercer — {@code SubscriptionServiceTest} vérifie que le service
 * refuse la double souscription, mais rien, jusqu'ici, ne vérifiait que la BASE
 * la refuse aussi. C'est pourtant elle le dernier rempart le jour où deux
 * requêtes concurrentes franchissent le contrôle applicatif en même temps :
 * sans l'index, le client se retrouve avec deux abonnements vivants pour un
 * seul module, et donc deux lignes sur sa facture.
 *
 * <p>Le banc vérifie aussi ce que l'index doit LAISSER passer : re-souscrire un
 * module précédemment résilié. Un index total, plus simple à écrire, l'aurait
 * interdit à jamais — et la faute ne se serait vue qu'au premier client
 * revenant sur sa décision.
 *
 * <p>Sans Docker, le banc se saute plutôt qu'il n'échoue — mais il le dit dans
 * le motif du saut, il ne se tait pas.
 */
@Tag("migration")
class SubscriptionConstraintsOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrateOnARealServer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : les contraintes d'abonnement restent non verifiees sur cette machine");

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

    // ---------- uk_subscription_vivante : la règle centrale ----------

    @Test
    void twoLiveSubscriptionsForTheSameModuleAreRejected() throws SQLException {
        // Deux abonnements vivants pour un seul module = deux lignes de facture
        // pour une seule prestation. Le service le refuse deja ; l'index est ce
        // qui tient quand deux requetes concurrentes passent le controle en
        // meme temps.
        UUID client = insertTenant("client-doublon");
        insertSubscription(client, "controlplan", null);

        assertThatThrownBy(() -> insertSubscription(client, "controlplan", null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_subscription_vivante");
    }

    @Test
    void resubscribingACancelledModuleIsAccepted() throws SQLException {
        // Ce que l'index partiel doit LAISSER passer. Un index total, plus
        // simple a ecrire, aurait interdit a jamais de revenir sur une
        // resiliation — et la faute ne se serait vue qu'au premier client qui
        // change d'avis.
        UUID client = insertTenant("client-revient");
        insertSubscription(client, "controlplan", Instant.parse("2026-06-30T00:00:00Z"));

        insertSubscription(client, "controlplan", null);

        assertThat(countSubscriptions(client)).isEqualTo(2);
        assertThat(countLiveSubscriptions(client)).isEqualTo(1);
    }

    @Test
    void twoCancelledSubscriptionsForTheSameModuleCoexist() throws SQLException {
        // L'historique s'empile : un client peut avoir souscrit et resilie le
        // meme module plusieurs fois, et chaque contrat justifie ses factures.
        UUID client = insertTenant("client-historique");
        insertSubscription(client, "controlplan", Instant.parse("2025-06-30T00:00:00Z"));
        insertSubscription(client, "controlplan", Instant.parse("2026-06-30T00:00:00Z"));

        assertThat(countSubscriptions(client)).isEqualTo(2);
        assertThat(countLiveSubscriptions(client)).isZero();
    }

    @Test
    void twoDifferentModulesForTheSameClientCoexist() throws SQLException {
        UUID client = insertTenant("client-multimodule");
        insertSubscription(client, "controlplan", null);

        insertSubscription(client, "risk", null);

        assertThat(countLiveSubscriptions(client)).isEqualTo(2);
    }

    // ---------- chk_sub_amount ----------

    @Test
    void aNegativeAmountIsRejected() throws SQLException {
        UUID client = insertTenant("client-montant-negatif");

        assertThatThrownBy(() -> insertSubscription(client, "controlplan", null, -1,
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15), "STANDARD", "MONTHLY"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_sub_amount");
    }

    @Test
    void aZeroAmountIsAccepted() throws SQLException {
        // Le palier FREE : un abonnement a zero euro reste un abonnement, et
        // c'est lui qui dit quels modules le client a le droit d'utiliser.
        UUID client = insertTenant("client-gratuit");

        insertSubscription(client, "controlplan", null, 0,
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15), "FREE", "MONTHLY");

        assertThat(countLiveSubscriptions(client)).isEqualTo(1);
    }

    // ---------- chk_sub_tier / chk_sub_period ----------

    @Test
    void anUnknownTierIsRejected() throws SQLException {
        UUID client = insertTenant("client-palier-inconnu");

        assertThatThrownBy(() -> insertSubscription(client, "controlplan", null, 9900,
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15), "GOLD", "MONTHLY"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_sub_tier");
    }

    @Test
    void anUnknownPeriodIsRejected() throws SQLException {
        UUID client = insertTenant("client-periode-inconnue");

        assertThatThrownBy(() -> insertSubscription(client, "controlplan", null, 9900,
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15), "STANDARD", "WEEKLY"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_sub_period");
    }

    // ---------- chk_sub_renewal_after_start ----------

    @Test
    void aRenewalBeforeTheStartIsRejected() throws SQLException {
        // Un contrat deja echu le jour de sa signature ne decrit rien : ce
        // serait le symptome d'un calcul d'echeance casse, et il vaut mieux le
        // decouvrir a l'ecriture qu'a la premiere facture manquante.
        UUID client = insertTenant("client-echeance-inversee");

        assertThatThrownBy(() -> insertSubscription(client, "controlplan", null, 9900,
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 1), "STANDARD", "MONTHLY"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_sub_renewal_after_start");
    }

    // ---------- chk_sub_cancellation_complete ----------

    @Test
    void aCancellationWithoutAnAuthorIsRejected() throws SQLException {
        // Une moitie de resiliation laisserait un contrat ferme que personne
        // n'aurait ferme — et la date qui decide de la derniere periode
        // facturee ne serait attribuable a personne.
        UUID client = insertTenant("client-resiliation-anonyme");

        assertThatThrownBy(() -> insertHalfCancelledSubscription(client))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_sub_cancellation_complete");
    }

    // ---------- FK ----------

    @Test
    void aSubscriptionForAnUnknownClientIsRejected() throws SQLException {
        assertThatThrownBy(() -> insertSubscription(UUID.randomUUID(), "controlplan", null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("tenant_id");
    }

    // ---------- montage ----------

    private static UUID insertTenant(String slug) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tenants (id, slug, name, plan, active, created_at, updated_at)"
                        + " VALUES (?, ?, 'Client de test', 'STARTER', TRUE, now(), now())")) {
            statement.setObject(1, id);
            statement.setString(2, slug);
            statement.executeUpdate();
        }
        return id;
    }

    private static void insertSubscription(UUID tenant, String moduleCode, Instant cancelledAt)
            throws SQLException {
        insertSubscription(tenant, moduleCode, cancelledAt, 9900,
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15), "STANDARD", "MONTHLY");
    }

    private static void insertSubscription(UUID tenant, String moduleCode, Instant cancelledAt,
                                            long amountCents, LocalDate startedOn,
                                            LocalDate nextRenewal, String tier, String period)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO subscriptions (id, tenant_id, module_code, billing_tier, period,
                                           amount_cents, currency, started_on, next_renewal,
                                           cancelled_at, cancelled_by, created_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, 'EUR', ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, tenant);
            statement.setString(3, moduleCode);
            statement.setString(4, tier);
            statement.setString(5, period);
            statement.setLong(6, amountCents);
            statement.setObject(7, startedOn);
            statement.setObject(8, nextRenewal);
            if (cancelledAt == null) {
                statement.setNull(9, Types.TIMESTAMP);
                statement.setNull(10, Types.OTHER);
            } else {
                statement.setTimestamp(9, Timestamp.from(cancelledAt));
                statement.setObject(10, UUID.randomUUID());
            }
            statement.setTimestamp(11, Timestamp.from(Instant.parse("2026-09-15T10:00:00Z")));
            statement.setObject(12, UUID.randomUUID());
            statement.executeUpdate();
        }
    }

    /** Une date de résiliation sans auteur : la moitié que la contrainte refuse. */
    private static void insertHalfCancelledSubscription(UUID tenant) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO subscriptions (id, tenant_id, module_code, billing_tier, period,
                                           amount_cents, currency, started_on, next_renewal,
                                           cancelled_at, cancelled_by, created_at, created_by)
                VALUES (?, ?, 'controlplan', 'STANDARD', 'MONTHLY', 9900, 'EUR',
                        DATE '2026-09-15', DATE '2026-10-15', ?, NULL, ?, ?)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, tenant);
            statement.setTimestamp(3, Timestamp.from(Instant.parse("2026-09-20T08:00:00Z")));
            statement.setTimestamp(4, Timestamp.from(Instant.parse("2026-09-15T10:00:00Z")));
            statement.setObject(5, UUID.randomUUID());
            statement.executeUpdate();
        }
    }

    private static int countSubscriptions(UUID tenant) throws SQLException {
        return count("SELECT count(*) FROM subscriptions WHERE tenant_id = ?", tenant);
    }

    private static int countLiveSubscriptions(UUID tenant) throws SQLException {
        return count("SELECT count(*) FROM subscriptions WHERE tenant_id = ? AND cancelled_at IS NULL",
                tenant);
    }

    private static int count(String sql, UUID tenant) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, tenant);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }
}

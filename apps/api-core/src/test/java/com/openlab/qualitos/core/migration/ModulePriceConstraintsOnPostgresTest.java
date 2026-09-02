package com.openlab.qualitos.core.migration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que H2 ne peut pas dire sur {@code module_prices}.
 *
 * <p>Même motif que {@link BillingProfileConstraintsOnPostgresTest} : le
 * profil "test" d'api-core désactive Flyway et laisse Hibernate créer le
 * schéma en {@code create-drop} — les contraintes CHECK et la contrainte
 * UNIQUE de la V5 ne sont donc jamais exercées par la suite de tests Mockito
 * habituelle ({@code ModulePriceServiceTest}). Elles portent pourtant la
 * règle la plus dangereuse du module : {@code chk_price_amount} interdit un
 * prix négatif, et {@code uk_module_price} interdit deux tarifs pour le même
 * triplet (module, palier, période), ce qui rendrait {@code priceOf()}
 * ambigu en base même si le service, lui, ne crée jamais de doublon.
 *
 * <p>Ce banc démarre un vrai PostgreSQL, y rejoue TOUTES les migrations
 * d'api-core dans l'ordre (V1 à V5), puis vérifie que ces contraintes
 * existent ET qu'elles refusent réellement l'écriture — pas qu'une
 * validation Java les imite en amont.
 *
 * <p>Sans Docker, le banc se saute plutôt qu'il n'échoue — mais il le dit dans
 * le motif du saut, il ne se tait pas.
 */
@Tag("migration")
class ModulePriceConstraintsOnPostgresTest {

    private static Connection connection;

    @BeforeAll
    static void migrateOnASharedServer() throws SQLException {
        assumeTrue(MigrationPostgres.dockerAvailable(),
                "Docker indisponible : ces contraintes restent non verifiees sur cette machine");
        // Un serveur PostgreSQL pour TOUS les bancs de migration, une
        // connexion par banc : les classes tournent en parallele, et
        // java.sql.Connection n'est pas sure entre fils d'execution.
        connection = MigrationPostgres.connect();
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        if (connection != null) connection.close();
    }

    // ---------- tarif valide ----------

    @Test
    void anOrdinaryPriceIsInserted() throws SQLException {
        insertModulePrice("controlplan-ordinaire", "STANDARD", "MONTHLY", 9900);

        assertThat(countModulePrices("controlplan-ordinaire")).isEqualTo(1);
    }

    @Test
    void aZeroPriceIsAccepted() throws SQLException {
        // Un prix nul est legitime : c'est celui du palier FREE.
        insertModulePrice("controlplan-gratuit", "FREE", "MONTHLY", 0);

        assertThat(countModulePrices("controlplan-gratuit")).isEqualTo(1);
    }

    // ---------- chk_price_amount ----------

    @Test
    void aNegativePriceIsRejected() throws SQLException {
        assertThatThrownBy(() -> insertModulePrice("controlplan-negatif", "STANDARD", "MONTHLY", -1))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_price_amount");
    }

    // ---------- chk_price_tier ----------

    @Test
    void anUnknownTierIsRejected() throws SQLException {
        assertThatThrownBy(() -> insertModulePrice("controlplan-palier", "GOLD", "MONTHLY", 9900))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_price_tier");
    }

    // ---------- chk_price_period ----------

    @Test
    void anUnknownPeriodIsRejected() throws SQLException {
        assertThatThrownBy(() -> insertModulePrice("controlplan-periode", "STANDARD", "WEEKLY", 9900))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_price_period");
    }

    // ---------- uk_module_price (un seul tarif par triplet) ----------

    @Test
    void twoPricesForTheSameModuleTierAndPeriodAreRejected() throws SQLException {
        insertModulePrice("controlplan-doublon", "STANDARD", "MONTHLY", 9900);

        assertThatThrownBy(() ->
                insertModulePrice("controlplan-doublon", "STANDARD", "MONTHLY", 12000))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_module_price");
    }

    @Test
    void theSameModuleWithADifferentTierIsAccepted() throws SQLException {
        // Meme module, palier different : ce n'est pas un doublon, uk_module_price
        // ne doit pas s'en melanger.
        insertModulePrice("controlplan-multipalier", "STANDARD", "MONTHLY", 9900);

        insertModulePrice("controlplan-multipalier", "PRO", "MONTHLY", 19900);

        assertThat(countModulePrices("controlplan-multipalier")).isEqualTo(2);
    }

    // ---------- montage ----------

    private static void insertModulePrice(String moduleCode, String tier, String period, long amountCents)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO module_prices (id, module_code, billing_tier, period, amount_cents,
                                            currency, updated_at, updated_by)
                VALUES (?, ?, ?, ?, ?, 'EUR', ?, ?)
                """)) {
            Timestamp now = Timestamp.from(Instant.now());
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, moduleCode);
            statement.setString(3, tier);
            statement.setString(4, period);
            statement.setLong(5, amountCents);
            statement.setTimestamp(6, now);
            statement.setObject(7, UUID.randomUUID());
            statement.executeUpdate();
        }
    }

    private static int countModulePrices(String moduleCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM module_prices WHERE module_code = ?")) {
            statement.setString(1, moduleCode);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }
}

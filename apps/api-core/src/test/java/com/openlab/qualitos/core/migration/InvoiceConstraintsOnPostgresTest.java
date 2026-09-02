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
 * Ce que H2 ne peut pas dire sur {@code invoices} et {@code invoice_lines}.
 *
 * <p>Trois règles ne vivent qu'ici, et chacune protège de l'erreur la plus
 * chère du module :
 *
 * <ul>
 *   <li>{@code uk_invoice_tenant_period} — une seule facture par client et par
 *       période. C'est l'IDEMPOTENCE de l'émission, et c'est ce qui tient quand
 *       deux exécutions du traitement mensuel se croisent, là où le contrôle
 *       applicatif lit avant d'écrire ;</li>
 *   <li>{@code uk_invoice_number} — la numérotation ne se réattribue pas. Deux
 *       émissions concurrentes peuvent calculer le même « numéro suivant » ;
 *       c'est cet index qui les départage ;</li>
 *   <li>{@code chk_line_product} — le total d'une ligne EST le produit. Sans
 *       lui, une facture peut afficher 2 × 99,00 € = 150,00 € et rester
 *       parfaitement valide.</li>
 * </ul>
 *
 * <p>Sans Docker, le banc se saute plutôt qu'il n'échoue — mais il le dit dans
 * le motif du saut, il ne se tait pas.
 */
@Tag("migration")
class InvoiceConstraintsOnPostgresTest {

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

    // ---------- idempotence de l'émission ----------

    @Test
    void twoInvoicesForTheSameClientAndPeriodAreRejected() throws SQLException {
        UUID client = insertTenant("client-double-facture");
        insertInvoice(client, "FA-2026-0001", 2026, 9);

        assertThatThrownBy(() -> insertInvoice(client, "FA-2026-0002", 2026, 9))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_invoice_tenant_period");
    }

    @Test
    void twoPeriodsForTheSameClientCoexist() throws SQLException {
        UUID client = insertTenant("client-deux-mois");
        insertInvoice(client, "FA-2026-0010", 2026, 9);

        insertInvoice(client, "FA-2026-0011", 2026, 10);

        assertThat(countInvoices(client)).isEqualTo(2);
    }

    // ---------- numérotation ----------

    @Test
    void theSameNumberTwiceIsRejectedEvenForDifferentClients() throws SQLException {
        // La sequence est celle de l'EDITEUR, pas celle d'un client : deux
        // emissions concurrentes peuvent calculer le meme numero suivant, et
        // c'est cet index qui les departage.
        UUID premier = insertTenant("client-numero-a");
        UUID second = insertTenant("client-numero-b");
        insertInvoice(premier, "FA-2026-0020", 2026, 9);

        assertThatThrownBy(() -> insertInvoice(second, "FA-2026-0020", 2026, 9))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_invoice_number");
    }

    @Test
    void aMalformedNumberIsRejected() throws SQLException {
        UUID client = insertTenant("client-numero-abime");

        assertThatThrownBy(() -> insertInvoice(client, "2026/41", 2026, 9))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_invoice_number");
    }

    @Test
    void aFiveDigitRankIsAccepted() throws SQLException {
        // Au-dela de 9999, le rang deborde vers le haut : la contrainte doit
        // l'accepter, sinon la dix-millieme facture de l'exercice ne peut plus
        // etre emise du tout.
        UUID client = insertTenant("client-dix-mille");

        insertInvoice(client, "FA-2026-10000", 2026, 9);

        assertThat(countInvoices(client)).isEqualTo(1);
    }

    // ---------- cohérence des lignes ----------

    @Test
    void aLineTotalThatIsNotTheProductIsRejected() throws SQLException {
        // Sans chk_line_product, une facture peut afficher 2 x 99,00 EUR =
        // 150,00 EUR et rester parfaitement valide pour la base.
        UUID client = insertTenant("client-ligne-fausse");
        UUID invoice = insertInvoice(client, "FA-2026-0030", 2026, 9);

        assertThatThrownBy(() -> insertLine(invoice, 1, 2, 9900, 15000))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_line_product");
    }

    @Test
    void aCorrectProductIsAccepted() throws SQLException {
        UUID client = insertTenant("client-ligne-juste");
        UUID invoice = insertInvoice(client, "FA-2026-0031", 2026, 9);

        insertLine(invoice, 1, 2, 9900, 19800);

        assertThat(countLines(invoice)).isEqualTo(1);
    }

    @Test
    void aZeroQuantityIsRejected() throws SQLException {
        UUID client = insertTenant("client-quantite-nulle");
        UUID invoice = insertInvoice(client, "FA-2026-0032", 2026, 9);

        assertThatThrownBy(() -> insertLine(invoice, 1, 0, 9900, 0))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_line_quantity");
    }

    @Test
    void twoLinesForTheSameSubscriptionAreRejected() throws SQLException {
        // Deux lignes pour le meme contrat le factureraient deux fois sur la
        // meme piece.
        UUID client = insertTenant("client-ligne-doublon");
        UUID invoice = insertInvoice(client, "FA-2026-0033", 2026, 9);
        UUID subscription = UUID.randomUUID();
        insertLine(invoice, 1, 1, 9900, 9900, subscription);

        assertThatThrownBy(() -> insertLine(invoice, 2, 1, 9900, 9900, subscription))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_line_subscription");
    }

    @Test
    void deletingAnInvoiceRemovesItsLines() throws SQLException {
        // Les lignes n'existent pas sans leur facture : une facture orpheline
        // de ses lignes ne dit plus ce qu'elle facture.
        UUID client = insertTenant("client-cascade");
        UUID invoice = insertInvoice(client, "FA-2026-0034", 2026, 9);
        insertLine(invoice, 1, 1, 9900, 9900);

        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM invoices WHERE id = ?")) {
            statement.setObject(1, invoice);
            statement.executeUpdate();
        }

        assertThat(countLines(invoice)).isZero();
    }

    // ---------- la facture survit à son client ----------

    @Test
    void deletingAClientThatHasInvoicesIsRejected() throws SQLException {
        // PAS de ON DELETE CASCADE, contrairement aux abonnements : effacer une
        // piece comptable pour faire de la place est precisement ce qu'un
        // controle fiscal cherche.
        UUID client = insertTenant("client-indestructible");
        insertInvoice(client, "FA-2026-0040", 2026, 9);

        assertThatThrownBy(() -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("DELETE FROM tenants WHERE id = ?")) {
                statement.setObject(1, client);
                statement.executeUpdate();
            }
        }).isInstanceOf(SQLException.class)
                .hasMessageContaining("tenant_id");
    }

    // ---------- envoi ----------

    @Test
    void aSendingWithoutARecipientIsRejected() throws SQLException {
        // Une moitie d'envoi affirmerait qu'une facture est partie sans pouvoir
        // dire a qui.
        UUID client = insertTenant("client-envoi-anonyme");

        assertThatThrownBy(() -> insertHalfSentInvoice(client))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_invoice_sending_complete");
    }

    @Test
    void anInvalidPeriodMonthIsRejected() throws SQLException {
        UUID client = insertTenant("client-mois-invalide");

        assertThatThrownBy(() -> insertInvoice(client, "FA-2026-0050", 2026, 13))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_invoice_month");
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

    private static UUID insertInvoice(UUID tenant, String number, int year, int month)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO invoices (id, tenant_id, number, fiscal_year, period_year, period_month,
                                      currency, total_cents, issued_at, issued_by)
                VALUES (?, ?, ?, ?, ?, ?, 'EUR', 9900, ?, ?)
                """)) {
            statement.setObject(1, id);
            statement.setObject(2, tenant);
            statement.setString(3, number);
            statement.setInt(4, year);
            statement.setInt(5, year);
            statement.setInt(6, month);
            statement.setTimestamp(7, Timestamp.from(Instant.parse("2026-10-01T06:00:00Z")));
            statement.setObject(8, UUID.randomUUID());
            statement.executeUpdate();
        }
        return id;
    }

    private static void insertHalfSentInvoice(UUID tenant) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO invoices (id, tenant_id, number, fiscal_year, period_year, period_month,
                                      currency, total_cents, issued_at, issued_by, sent_at, sent_to)
                VALUES (?, ?, 'FA-2026-0060', 2026, 2026, 9, 'EUR', 9900, ?, ?, ?, NULL)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, tenant);
            statement.setTimestamp(3, Timestamp.from(Instant.parse("2026-10-01T06:00:00Z")));
            statement.setObject(4, UUID.randomUUID());
            statement.setTimestamp(5, Timestamp.from(Instant.parse("2026-10-01T07:00:00Z")));
            statement.executeUpdate();
        }
    }

    private static void insertLine(UUID invoice, int lineNo, int quantity,
                                    long unitCents, long totalCents) throws SQLException {
        insertLine(invoice, lineNo, quantity, unitCents, totalCents, UUID.randomUUID());
    }

    private static void insertLine(UUID invoice, int lineNo, int quantity, long unitCents,
                                    long totalCents, UUID subscription) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO invoice_lines (id, invoice_id, subscription_id, line_no, module_code,
                                           billing_tier, period, quantity,
                                           unit_amount_cents, line_total_cents)
                VALUES (?, ?, ?, ?, 'controlplan', 'STANDARD', 'MONTHLY', ?, ?, ?)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, invoice);
            statement.setObject(3, subscription);
            statement.setInt(4, lineNo);
            statement.setInt(5, quantity);
            statement.setLong(6, unitCents);
            statement.setLong(7, totalCents);
            statement.executeUpdate();
        }
    }

    private static int countInvoices(UUID tenant) throws SQLException {
        return count("SELECT count(*) FROM invoices WHERE tenant_id = ?", tenant);
    }

    private static int countLines(UUID invoice) throws SQLException {
        return count("SELECT count(*) FROM invoice_lines WHERE invoice_id = ?", invoice);
    }

    private static int count(String sql, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }
}

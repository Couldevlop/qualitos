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
 * Ce que H2 ne peut pas dire sur {@code billing_profiles}.
 *
 * <p>Le profil "test" d'api-core désactive Flyway ({@code flyway.enabled: false})
 * et laisse Hibernate créer le schéma en {@code create-drop} : le SQL des
 * migrations — donc les contraintes CHECK de la V4 — n'est jamais exécuté par la
 * suite de tests habituelle. Deux d'entre elles protègent directement la
 * facturation : {@code chk_exemption_motivee} (on ne renonce pas à facturer sans
 * dire pourquoi) et {@code chk_billing_email} (une adresse de facturation a la
 * forme d'une adresse). Une régression sur l'une d'elles — affaiblie ou
 * supprimée par erreur dans une migration future — ne ferait rougir aucun banc
 * existant.
 *
 * <p>Ce test démarre un vrai PostgreSQL, y rejoue TOUTES les migrations
 * d'api-core dans l'ordre (V1 à V4), puis vérifie que ces contraintes existent
 * ET qu'elles refusent réellement l'écriture — pas qu'une validation Java les
 * imite en amont.
 *
 * <p>Sans Docker, le banc se saute plutôt qu'il n'échoue — mais il le dit dans
 * le motif du saut, il ne se tait pas.
 */
@Tag("migration")
class BillingProfileConstraintsOnPostgresTest {

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

    // ---------- profil valide ----------

    @Test
    void aValidNonExemptProfileIsInserted() throws SQLException {
        UUID tenant = insertTenant("acme-valide");

        insertBillingProfile(tenant, false, null, "compta@acme.example");

        assertThat(countBillingProfiles(tenant)).isEqualTo(1);
    }

    // ---------- chk_exemption_motivee ----------

    @Test
    void anExemptionWithANullReasonIsRejected() throws SQLException {
        UUID tenant = insertTenant("acme-exempt-null");

        assertThatThrownBy(() -> insertBillingProfile(tenant, true, null, "compta@acme.example"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_exemption_motivee");
    }

    @Test
    void anExemptionWithAnEmptyReasonIsRejected() throws SQLException {
        UUID tenant = insertTenant("acme-exempt-vide");

        assertThatThrownBy(() -> insertBillingProfile(tenant, true, "", "compta@acme.example"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_exemption_motivee");
    }

    @Test
    void anExemptionWithAReasonMadeOnlyOfSpacesIsRejected() throws SQLException {
        // IS NOT NULL seul laisse passer '   ' : c'est precisement le btrim(...) <> ''
        // de la contrainte qui doit l'attraper.
        UUID tenant = insertTenant("acme-exempt-espaces");

        assertThatThrownBy(() -> insertBillingProfile(tenant, true, "   ", "compta@acme.example"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_exemption_motivee");
    }

    @Test
    void anExemptionWithAGenuineReasonIsAccepted() throws SQLException {
        UUID tenant = insertTenant("acme-exempt-motive");

        insertBillingProfile(tenant, true, "Compte de demonstration interne", "compta@acme.example");

        assertThat(countBillingProfiles(tenant)).isEqualTo(1);
    }

    // ---------- chk_billing_email ----------

    @Test
    void aBillingEmailThatIsJustAnAtSignIsRejected() throws SQLException {
        UUID tenant = insertTenant("acme-email-arobase");

        assertThatThrownBy(() -> insertBillingProfile(tenant, false, null, "@"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_billing_email");
    }

    @Test
    void anOrdinaryBillingEmailIsAccepted() throws SQLException {
        UUID tenant = insertTenant("acme-email-ordinaire");

        insertBillingProfile(tenant, false, null, "compta@acme.example");

        assertThat(countBillingProfiles(tenant)).isEqualTo(1);
    }

    // ---------- uq_billing_profiles_tenant (un seul profil par client) ----------

    @Test
    void twoBillingProfilesForTheSameTenantAreRejected() throws SQLException {
        UUID tenant = insertTenant("acme-double-profil");
        insertBillingProfile(tenant, false, null, "premier@acme.example");

        assertThatThrownBy(() -> insertBillingProfile(tenant, false, null, "second@acme.example"))
                .isInstanceOf(SQLException.class);
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

    private static void insertBillingProfile(UUID tenant, boolean exempt, String exemptionReason,
                                              String billingEmail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO billing_profiles (id, tenant_id, legal_name, address_line1, postal_code,
                                              city, country_code, billing_email, currency,
                                              billing_exempt, exemption_reason, created_at, updated_at)
                VALUES (?, ?, 'Acme SAS', '1 rue de la Facture', '75000', 'Paris', 'FR', ?, 'EUR',
                        ?, ?, ?, ?)
                """)) {
            Timestamp now = Timestamp.from(Instant.now());
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, tenant);
            statement.setString(3, billingEmail);
            statement.setBoolean(4, exempt);
            statement.setString(5, exemptionReason);
            statement.setTimestamp(6, now);
            statement.setTimestamp(7, now);
            statement.executeUpdate();
        }
    }

    private static int countBillingProfiles(UUID tenant) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM billing_profiles WHERE tenant_id = ?")) {
            statement.setObject(1, tenant);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }
}

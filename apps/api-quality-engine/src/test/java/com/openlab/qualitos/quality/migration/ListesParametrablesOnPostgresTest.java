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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V164 sur un vrai moteur : la table unique des listes déroulantes
 * paramétrées par un client.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore les contraintes de la migration : l'unicité d'un code dans une liste,
 * le rang positif, le code en majuscules et le libellé non vide ne se vérifient
 * qu'ici. Ce qui est tenu, surtout : une même valeur peut exister dans deux
 * listes et chez deux clients, mais jamais deux fois dans la même liste du même
 * client — c'est ce code que portent les écarts, les audits, les fournisseurs.
 */
@Tag("migration")
class ListesParametrablesOnPostgresTest {

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V164 reste non verifiee sur cette machine");
        postgres = new PostgreSQLContainer<>("postgres:17-alpine");
        postgres.start();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword());
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

    private static void inscrire(UUID tenant, String liste, String code, String libelle,
                                 int rang) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO tenant_catalog_entries (tenant_id, catalogue, code, label, position) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            ps.setObject(1, tenant);
            ps.setString(2, liste);
            ps.setString(3, code);
            ps.setString(4, libelle);
            ps.setInt(5, rang);
            ps.executeUpdate();
        }
    }

    @Test
    void uneListeSEnregistreDansSonOrdre_etActiveParDefaut() throws SQLException {
        UUID tenant = UUID.randomUUID();
        inscrire(tenant, "fives-zone", "BLOC-A", "Bloc opératoire A", 0);
        inscrire(tenant, "fives-zone", "BLOC-B", "Bloc opératoire B", 1);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT code, active, updated_at FROM tenant_catalog_entries "
                        + "WHERE tenant_id = ? AND catalogue = ? ORDER BY position")) {
            ps.setObject(1, tenant);
            ps.setString(2, "fives-zone");
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("code")).isEqualTo("BLOC-A");
                // Une valeur naît en service : la désactivation est un geste explicite.
                assertThat(rs.getBoolean("active")).isTrue();
                assertThat(rs.getTimestamp("updated_at")).isNotNull();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("code")).isEqualTo("BLOC-B");
                assertThat(rs.next()).isFalse();
            }
        }
    }

    @Test
    void unCodeNeDesignePasDeuxChosesDansLaMemeListe() throws SQLException {
        UUID tenant = UUID.randomUUID();
        inscrire(tenant, "audit-type", "INTERNAL", "Interne", 0);

        assertThatThrownBy(() -> inscrire(tenant, "audit-type", "INTERNAL", "Audit interne", 1))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_tenant_catalog_entries");
    }

    @Test
    void leMemeCodeVitSousDeuxListesEtChezDeuxClients() throws SQLException {
        UUID unClient = UUID.randomUUID();
        UUID unAutre = UUID.randomUUID();

        inscrire(unClient, "audit-type", "SUPPLIER", "Fournisseur", 0);
        // Même code, autre liste : ce sont deux choses différentes.
        inscrire(unClient, "nc-category", "SUPPLIER", "Achats", 0);
        // Même code, même liste, autre client : le cloisonnement est entier.
        inscrire(unAutre, "audit-type", "SUPPLIER", "Audit de prestataire", 0);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM tenant_catalog_entries WHERE code = 'SUPPLIER'")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(3);
            }
        }
    }

    @Test
    void unRangNegatif_unCodeEnMinuscules_unLibelleVideSontRefuses() {
        UUID tenant = UUID.randomUUID();

        assertThatThrownBy(() -> inscrire(tenant, "fives-zone", "ZONE-A", "Zone A", -1))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_tenant_catalog_entries_position");
        // Un code en deux casses finirait par exister en deux versions qui ne se
        // rejoignent plus : la base refuse, comme le domaine.
        assertThatThrownBy(() -> inscrire(tenant, "fives-zone", "zone-a", "Zone A", 0))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_tenant_catalog_entries_code");
        assertThatThrownBy(() -> inscrire(tenant, "fives-zone", "ZONE-B", "   ", 0))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_tenant_catalog_entries_label");
    }

    @Test
    void laListeDUnClientEstIndexeeParListeEtParRang() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT indexdef FROM pg_indexes WHERE tablename = 'tenant_catalog_entries' "
                        + "AND indexname = 'ix_tenant_catalog_entries_liste'")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("indexdef"))
                        .contains("tenant_id").contains("catalogue").contains("position");
            }
        }
    }

    /**
     * La table porte un {@code tenant_id} : l'isolation par la base la couvre
     * d'elle-même (ADR 0086), sans que personne n'y pense.
     */
    @Test
    void laTablePorteUnClient_doncLIsolationLaCouvre() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'tenant_catalog_entries' AND column_name = 'tenant_id'")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("is_nullable")).isEqualTo("NO");
            }
        }
    }
}

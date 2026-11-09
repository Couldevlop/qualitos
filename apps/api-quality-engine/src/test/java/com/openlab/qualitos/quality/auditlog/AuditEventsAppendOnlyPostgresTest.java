package com.openlab.qualitos.quality.auditlog;

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
 * La différence entre détecter et empêcher.
 *
 * <p>La chaîne SHA-256 de la V26 permet de constater qu'une ligne a été
 * réécrite — encore faut-il que quelqu'un lance la vérification, ce que
 * personne ne fait périodiquement. Entre la réécriture et sa découverte, le
 * journal ment. La V157 ferme la porte plutôt que de poser une alarme : la
 * base refuse la suppression et la modification.
 *
 * <p>Ce banc ne peut pas se jouer ailleurs que sur un vrai PostgreSQL : un
 * déclencheur plpgsql n'existe que là. H2 accepterait tout, et la suite
 * tournerait au vert sur une garantie absente — c'est précisément le genre de
 * silence que la V157 corrige.
 *
 * <p>Il écrit sous le compte PROPRIÉTAIRE de la base, le plus privilégié qui
 * soit. Si la règle tient pour lui, elle tient pour le rôle applicatif, qui en
 * a moins. L'inverse ne serait pas vrai : la vérifier sous le rôle applicatif
 * laisserait croire qu'un REVOKE suffit, alors que {@code RlsSchema} ré-accorde
 * {@code UPDATE, DELETE ON ALL TABLES} après chaque migration.
 */
@Tag("migration")
class AuditEventsAppendOnlyPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";
    /** Le code SQLSTATE que les déclencheurs lèvent : « privilège insuffisant ». */
    private static final String REFUS = "42501";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrateOnARealServer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : l'ajout seul reste non vérifié sur cette machine");

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

    // ---------- ce que la base refuse ----------

    @Test
    void unEvenementNeSeSupprimePas() throws SQLException {
        UUID tenant = UUID.randomUUID();
        insert(tenant, 1L, "empreinte-a");

        assertThatThrownBy(() -> executer(
                "delete from audit_events where tenant_id = '" + tenant + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ajout seul");

        assertThat(compter(tenant)).as("l'événement est toujours là").isEqualTo(1);
    }

    @Test
    void leResumeDUnEvenementNeSeReecritPas() throws SQLException {
        // Le cas réel qu'on cherche à empêcher : quelqu'un adoucit la phrase qui
        // décrit ce qu'il a fait. Sans la V157, la ligne changeait et la chaîne
        // ne le disait qu'à la prochaine vérification — c'est-à-dire jamais.
        UUID tenant = UUID.randomUUID();
        insert(tenant, 1L, "empreinte-b");

        assertThatThrownBy(() -> executer(
                "update audit_events set summary = 'Rien de notable' where tenant_id = '"
                        + tenant + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ajout seul");
    }

    @Test
    void lEmpreinteElleMemeNeSeRecalculePas() throws SQLException {
        // La parade « intelligente » : réécrire la ligne ET son empreinte, pour
        // que la vérification de chaîne retombe sur ses pieds. Elle échoue au
        // même endroit, et c'est ce qui rend la chaîne utile plutôt que
        // décorative.
        UUID tenant = UUID.randomUUID();
        insert(tenant, 1L, "empreinte-c");

        assertThatThrownBy(() -> executer(
                "update audit_events set integrity_hash = '" + "f".repeat(64)
                        + "' where tenant_id = '" + tenant + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ajout seul");
    }

    @Test
    void lActeurDUnEvenementNeChangePasDeMain() throws SQLException {
        UUID tenant = UUID.randomUUID();
        insert(tenant, 1L, "empreinte-d");

        assertThatThrownBy(() -> executer(
                "update audit_events set actor_user_id = '" + UUID.randomUUID()
                        + "' where tenant_id = '" + tenant + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ajout seul");
    }

    @Test
    void leRefusPorteLeCodeDUnPrivilegeInsuffisant() throws SQLException {
        // Le code SQLSTATE compte : c'est lui qui permet à la couche au-dessus de
        // distinguer « interdit » d'une panne, et donc de répondre autre chose
        // qu'une erreur 500 au bout de la chaîne.
        UUID tenant = UUID.randomUUID();
        insert(tenant, 1L, "empreinte-e");

        assertThatThrownBy(() -> executer(
                "delete from audit_events where tenant_id = '" + tenant + "'"))
                .isInstanceOfSatisfying(SQLException.class,
                        e -> assertThat(e.getSQLState()).isEqualTo(REFUS));
    }

    // ---------- ce que la base accepte encore ----------

    @Test
    void lAncrageSePoseUneFois() throws SQLException {
        // Seule écriture permise après coup. `blockchain_tx_ref` n'entre pas dans
        // l'empreinte (cf. AuditEventHasher) : elle dit où la preuve est déposée,
        // pas ce qui s'est passé. La renseigner ne réécrit donc rien.
        UUID tenant = UUID.randomUUID();
        insert(tenant, 1L, "empreinte-f");

        executer("update audit_events set blockchain_tx_ref = 'tx-0001' where tenant_id = '"
                + tenant + "'");

        assertThat(ancrage(tenant)).isEqualTo("tx-0001");
    }

    @Test
    void lAncrageNeSePoseQuUneFois() throws SQLException {
        // Sinon la référence d'ancrage deviendrait le champ libre par lequel on
        // raconte autre chose — et le seul que la chaîne ne protège pas.
        UUID tenant = UUID.randomUUID();
        insert(tenant, 1L, "empreinte-g");
        executer("update audit_events set blockchain_tx_ref = 'tx-0001' where tenant_id = '"
                + tenant + "'");

        assertThatThrownBy(() -> executer(
                "update audit_events set blockchain_tx_ref = 'tx-9999' where tenant_id = '"
                        + tenant + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("une fois");

        assertThat(ancrage(tenant)).isEqualTo("tx-0001");
    }

    @Test
    void unNouvelEvenementSAjouteToujours() throws SQLException {
        // La vérification qui manque dans beaucoup de durcissements : on a fermé
        // l'écriture, est-ce qu'on a fermé l'AJOUT avec ? Un journal où l'on ne
        // peut plus rien inscrire est une panne, pas une protection.
        UUID tenant = UUID.randomUUID();
        insert(tenant, 1L, "empreinte-h");
        insert(tenant, 2L, "empreinte-i");

        assertThat(compter(tenant)).isEqualTo(2);
    }

    // ---------- le compteur de séquence ----------

    @Test
    void leCompteurAvance() throws SQLException {
        UUID tenant = UUID.randomUUID();
        executer("insert into audit_event_counters (tenant_id, last_sequence_no) values ('"
                + tenant + "', 1)");

        executer("update audit_event_counters set last_sequence_no = 2 where tenant_id = '"
                + tenant + "'");

        assertThat(compteur(tenant)).isEqualTo(2L);
    }

    @Test
    void leCompteurNeReculePas() throws SQLException {
        // Réécrire l'histoire par l'AJOUT plutôt que par la modification :
        // ramener le compteur en arrière rouvre un numéro de séquence déjà
        // utilisé. La contrainte d'unicité refuserait le doublon, mais seulement
        // après la bascule — mieux vaut refuser la bascule.
        UUID tenant = UUID.randomUUID();
        executer("insert into audit_event_counters (tenant_id, last_sequence_no) values ('"
                + tenant + "', 7)");

        assertThatThrownBy(() -> executer(
                "update audit_event_counters set last_sequence_no = 3 where tenant_id = '"
                        + tenant + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ne recule pas");

        assertThat(compteur(tenant)).isEqualTo(7L);
    }

    @Test
    void leCompteurNeSeSupprimePas() throws SQLException {
        // Le supprimer le ferait repartir de zéro à la prochaine écriture, donc
        // rouvrirait toute la plage de séquences d'un coup.
        UUID tenant = UUID.randomUUID();
        executer("insert into audit_event_counters (tenant_id, last_sequence_no) values ('"
                + tenant + "', 4)");

        assertThatThrownBy(() -> executer(
                "delete from audit_event_counters where tenant_id = '" + tenant + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ne se supprime pas");

        assertThat(compteur(tenant)).isEqualTo(4L);
    }

    // ---------- plomberie ----------

    private static void executer(String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private static void insert(UUID tenant, long seq, String empreinte) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                insert into audit_events (id, tenant_id, sequence_no, occurred_at, recorded_at,
                                          actor_type, actor_user_id, action, resource_type,
                                          resource_id, summary, payload_json, integrity_hash,
                                          previous_hash)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            Instant maintenant = Instant.parse("2026-11-07T10:00:00Z").plusSeconds(seq);
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setLong(3, seq);
            ps.setTimestamp(4, Timestamp.from(maintenant));
            ps.setTimestamp(5, Timestamp.from(maintenant));
            ps.setString(6, ActorType.USER.name());
            ps.setObject(7, UUID.randomUUID());
            ps.setString(8, "capa.effectiveness.recorded");
            ps.setString(9, "capa_case");
            ps.setObject(10, UUID.randomUUID());
            ps.setString(11, "Verdict rendu contre le critère posé");
            ps.setString(12, "{\"verdict\":\"PARTIAL\"}");
            ps.setString(13, pad(empreinte));
            ps.setString(14, null);
            ps.executeUpdate();
        }
    }

    /** La colonne est un CHAR(64) : les libellés lisibles des bancs y sont complétés. */
    private static String pad(String valeur) {
        return valeur.length() >= 64 ? valeur.substring(0, 64)
                : valeur + "0".repeat(64 - valeur.length());
    }

    private static int compter(UUID tenant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "select count(*) from audit_events where tenant_id = ?")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String ancrage(UUID tenant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "select blockchain_tx_ref from audit_events where tenant_id = ?")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static long compteur(UUID tenant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "select last_sequence_no from audit_event_counters where tenant_id = ?")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}

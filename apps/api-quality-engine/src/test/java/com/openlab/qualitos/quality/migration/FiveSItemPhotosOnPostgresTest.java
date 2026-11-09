package com.openlab.qualitos.quality.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V163 sur un vrai moteur : les photos de terrain d'un audit 5S.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore la migration : les contraintes ne se vérifient qu'ici. Ce qui est tenu :
 * un pilier hors des cinq est refusé, une photo de taille nulle aussi, une clé
 * d'objet ne sert qu'une fois, et la suppression de l'audit emporte ses photos —
 * sans quoi la table garderait des lignes pointant vers un audit disparu.
 */
@Tag("migration")
class FiveSItemPhotosOnPostgresTest {

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    private static final UUID TENANT = UUID.randomUUID();

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V163 reste non verifiee sur cette machine");
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

    // ---------- fixtures ----------

    private static UUID audit() throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO fives_audits (id, tenant_id, zone, status, auditor_id,"
                        + " created_at, updated_at)"
                        + " VALUES (?, ?, 'Atelier B', 'IN_PROGRESS', ?, now(), now())")) {
            ps.setObject(1, id);
            ps.setObject(2, TENANT);
            ps.setObject(3, UUID.randomUUID());
            ps.executeUpdate();
        }
        return id;
    }

    private static void photo(UUID auditId, String pilier, String cle, long taille)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO fives_item_photos (id, tenant_id, audit_id, pillar, object_key,"
                        + " content_type, size_bytes, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, 'image/jpeg', ?, now())")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, TENANT);
            ps.setObject(3, auditId);
            ps.setString(4, pilier);
            ps.setString(5, cle);
            ps.setLong(6, taille);
            ps.executeUpdate();
        }
    }

    private static long compte(UUID auditId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM fives_item_photos WHERE audit_id = ?")) {
            ps.setObject(1, auditId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // ---------- ce que la table accepte ----------

    @Test
    @DisplayName("plusieurs photos sur un meme pilier : une zone se montre sous plusieurs angles")
    void plusieursPhotosParPilier() throws SQLException {
        UUID a = audit();

        photo(a, "SEIRI", "k/" + UUID.randomUUID() + ".jpg", 2048);
        photo(a, "SEIRI", "k/" + UUID.randomUUID() + ".jpg", 4096);

        assertThat(compte(a)).isEqualTo(2);
    }

    @Test
    @DisplayName("les cinq piliers sont admis, et eux seuls")
    void cinqPiliersAdmis() throws SQLException {
        UUID a = audit();

        for (String p : new String[] { "SEIRI", "SEITON", "SEISO", "SEIKETSU", "SHITSUKE" }) {
            photo(a, p, "k/" + UUID.randomUUID() + ".jpg", 1024);
        }

        assertThat(compte(a)).isEqualTo(5);
        assertThatThrownBy(() -> photo(a, "SEIRO", "k/" + UUID.randomUUID() + ".jpg", 1024))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_fives_item_photos_pillar");
    }

    // ---------- ce qu'elle refuse ----------

    @Test
    @DisplayName("une photo de taille nulle n'est pas une preuve")
    void tailleNulle_refusee() throws SQLException {
        UUID a = audit();

        assertThatThrownBy(() -> photo(a, "SEIRI", "k/" + UUID.randomUUID() + ".jpg", 0))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_fives_item_photos_size");
    }

    /**
     * Deux métadonnées pour un même binaire feraient disparaître la photo de
     * l'une quand l'autre est retirée : le retrait supprime l'objet.
     */
    @Test
    @DisplayName("une cle d'objet ne sert qu'une fois")
    void cleDobjetUnique() throws SQLException {
        UUID a = audit();
        String cle = "k/" + UUID.randomUUID() + ".jpg";
        photo(a, "SEIRI", cle, 1024);

        assertThatThrownBy(() -> photo(a, "SEITON", cle, 1024))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_fives_item_photos_object_key");
    }

    @Test
    @DisplayName("une photo sans audit existant est refusee")
    void auditInconnu_refuse() {
        assertThatThrownBy(() -> photo(UUID.randomUUID(), "SEIRI",
                "k/" + UUID.randomUUID() + ".jpg", 1024))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("fk_fives_item_photos_audit");
    }

    @Test
    @DisplayName("supprimer l'audit emporte ses photos")
    void suppressionEnCascade() throws SQLException {
        UUID a = audit();
        photo(a, "SEIRI", "k/" + UUID.randomUUID() + ".jpg", 1024);

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM fives_audits WHERE id = ?")) {
            ps.setObject(1, a);
            ps.executeUpdate();
        }

        assertThat(compte(a)).isZero();
    }

    @Test
    @DisplayName("l'index de lecture porte le client, l'audit et le pilier")
    void indexDeLecture() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT indexdef FROM pg_indexes"
                             + " WHERE tablename = 'fives_item_photos'"
                             + " AND indexname = 'ix_fives_item_photos_audit'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).contains("tenant_id", "audit_id", "pillar");
        }
    }
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que la V126 ajoute à un livrable APQP, vérifié sur un vrai moteur.
 *
 * <p>Ce banc protège l'achèvement d'un livrable : ses colonnes, leurs valeurs par
 * défaut sur les lignes écrites AVANT la migration, et les contraintes qui
 * empêchent un renvoi à moitié posé. Une doublure de dépôt rend l'objet qu'on lui
 * a donné : elle ne dirait rien de tout cela.
 */
@Tag("migration")
class ApqpDeliverableCompletionOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID PHASE = UUID.randomUUID();
    private static final UUID LIVRABLE = UUID.randomUUID();

    @BeforeAll
    static void migrateInTwoSteps() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : l'ajout de la V126 reste non verifie sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        // La V124 crée le cycle APQP ; on s'arrête là pour écrire une phase et un
        // livrable « d'avant », puis on laisse la V126 les reprendre.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("124")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        insertPhase(PHASE, TENANT, 1, "Planifier et definir");
        insertDeliverable(LIVRABLE, TENANT, PHASE, 1, "Plan d'assurance produit");

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
    void unLivrableEcritAvantLaMigrationNEstNiCocheNiPpap() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT ppap, kind, done, done_at, done_by, comment, data,
                       linked_kind, linked_id
                  FROM apqp_deliverables WHERE id = ?""")) {
            ps.setObject(1, LIVRABLE);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean("ppap")).isFalse();
                // Un livrable d'avant est une pièce jointe : c'est le genre qui
                // n'impose rien et ne suppose rien.
                assertThat(rs.getString("kind")).isEqualTo("ATTACHMENT");
                assertThat(rs.getBoolean("done")).isFalse();
                assertThat(rs.getTimestamp("done_at")).isNull();
                assertThat(rs.getObject("done_by")).isNull();
                assertThat(rs.getString("comment")).isNull();
                assertThat(rs.getString("data")).isNull();
                assertThat(rs.getString("linked_kind")).isNull();
                assertThat(rs.getObject("linked_id")).isNull();
            }
        }
    }

    @Test
    void unGenreInconnuEstRefuseParLaBaseEtPasSeulementParLeCode() {
        assertThatThrownBy(() -> majLivrable(
                "UPDATE apqp_deliverables SET kind = 'FORMULAIRE_MAISON' WHERE id = ?"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void unRenvoiAMoitiePoseNeDesigneRienEtEstRefuse() {
        // Les deux colonnes vivent ou meurent ensemble : un `linked_kind` sans
        // identifiant afficherait un lien qui ne mène nulle part.
        assertThatThrownBy(() -> majLivrable(
                "UPDATE apqp_deliverables SET linked_kind = 'FMEA' WHERE id = ?"))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> majLivrable(
                "UPDATE apqp_deliverables SET linked_id = gen_random_uuid() WHERE id = ?"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void unRenvoiCompletEtConnuEstAccepte() throws SQLException {
        majLivrable("UPDATE apqp_deliverables SET kind = 'MODULE_LINK',"
                + " linked_kind = 'CONTROL_PLAN', linked_id = gen_random_uuid() WHERE id = ?");

        assertThat(genreDuRenvoi()).isEqualTo("CONTROL_PLAN");

        // Remise en etat : les autres tests lisent ce meme livrable.
        majLivrable("UPDATE apqp_deliverables SET kind = 'ATTACHMENT', linked_kind = NULL,"
                + " linked_id = NULL WHERE id = ?");
    }

    @Test
    void laTableDesPiecesExisteEtLeursClesSontUniques() throws SQLException {
        UUID piece = UUID.randomUUID();
        insertEvidence(piece, "tenants/" + TENANT + "/apqp/" + PHASE + "/deliverables/"
                + LIVRABLE + "/" + UUID.randomUUID() + ".docx");

        assertThat(compterPieces()).isEqualTo(1);

        // Deux lignes pour un même binaire feraient mentir le balayeur
        // d'orphelins, qui demande « cette clé est-elle encore revendiquée ? ».
        assertThatThrownBy(() -> insertEvidence(UUID.randomUUID(), cleDe(piece)))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void retirerUnLivrableEmporteSesPieces() throws SQLException {
        UUID autreLivrable = UUID.randomUUID();
        insertDeliverable(autreLivrable, TENANT, PHASE, 2, "Engagement de la direction");
        insertEvidence(UUID.randomUUID(), "tenants/" + TENANT + "/apqp/x/" + autreLivrable + ".pdf");

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM apqp_deliverables WHERE id = ?")) {
            ps.setObject(1, autreLivrable);
            ps.executeUpdate();
        }

        // Une preuve sans livrable ne prouve plus rien, et son binaire doit
        // redevenir un orphelin que le balayeur effacera.
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM apqp_deliverable_evidences WHERE deliverable_id = ?")) {
            ps.setObject(1, autreLivrable);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getLong(1)).isZero();
            }
        }
    }

    @Test
    void lIndexDesLivrablesPpapExiste() throws SQLException {
        List<String> index = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'apqp_deliverables'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) index.add(rs.getString(1));
        }
        // Le dossier PPAP ne lit que les livrables étoilés : sans index partiel,
        // il balaie tout le cycle de tous les clients.
        assertThat(index).contains("idx_apqp_deliverables_tenant_ppap");
    }

    // ---------- écritures d'appoint ----------

    private static void insertPhase(UUID id, UUID tenant, int position, String titre)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_phases (id, tenant_id, position, title, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)""")) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setInt(3, position);
            ps.setString(4, titre);
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);
            ps.executeUpdate();
        }
    }

    private static void insertDeliverable(UUID id, UUID tenant, UUID phase, int position,
                                         String libelle) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_deliverables
                  (id, tenant_id, phase_id, position, label, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setObject(3, phase);
            ps.setInt(4, position);
            ps.setString(5, libelle);
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);
            ps.executeUpdate();
        }
    }

    private static void insertEvidence(UUID id, String cle) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_deliverable_evidences
                  (id, tenant_id, phase_id, deliverable_id, object_key, content_type,
                   size_bytes, original_filename, uploaded_by, created_at)
                VALUES (?, ?, ?, ?, ?, 'application/pdf', 1024, 'preuve.pdf', ?, ?)""")) {
            ps.setObject(1, id);
            ps.setObject(2, TENANT);
            ps.setObject(3, PHASE);
            ps.setObject(4, cle.contains(LIVRABLE.toString()) ? LIVRABLE : livrableDe(cle));
            ps.setString(5, cle);
            ps.setObject(6, UUID.randomUUID());
            ps.setTimestamp(7, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    /** Le livrable visé par une clé d'objet de test : son avant-dernier segment. */
    private static UUID livrableDe(String cle) {
        String[] segments = cle.split("/");
        String dernier = segments[segments.length - 1];
        return UUID.fromString(dernier.substring(0, dernier.lastIndexOf('.')));
    }

    /**
     * Joue une mise a jour du livrable de reference.
     *
     * <p>L'ordre est ecrit en clair dans chaque test et ne porte qu'un seul
     * parametre, l'identifiant : ce qu'on eprouve ici, ce sont les contraintes
     * de la base, pas la composition d'une requete.
     */
    private static void majLivrable(String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, LIVRABLE);
            ps.executeUpdate();
        }
    }

    private static String genreDuRenvoi() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT linked_kind FROM apqp_deliverables WHERE id = ?")) {
            ps.setObject(1, LIVRABLE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static String cleDe(UUID piece) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT object_key FROM apqp_deliverable_evidences WHERE id = ?")) {
            ps.setObject(1, piece);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static long compterPieces() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM apqp_deliverable_evidences");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}

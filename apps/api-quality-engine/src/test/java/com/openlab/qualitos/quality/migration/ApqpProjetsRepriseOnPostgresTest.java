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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que la V131 fait des cycles APQP déjà saisis, vérifié sur un vrai moteur.
 *
 * <p>C'est le point le plus risqué du lot : le cycle unique d'un client devient
 * le cycle d'un PROJET, et le livrable perd son genre. Une reprise fautive
 * effacerait des cases cochées, des pièces jointes, ou le contenu saisi dans les
 * anciens formulaires — c'est-à-dire du travail réel.
 *
 * <p>Une doublure de dépôt ne dirait rien de tout cela : le versement passe par
 * des clauses {@code WITH}, du {@code jsonb} et des cascades de contraintes, que
 * seul PostgreSQL sait exécuter.
 */
@Tag("migration")
class ApqpProjetsRepriseOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    /** Un client qui travaille : deux phases, des cases cochées, des pièces. */
    private static final UUID TENANT_A = UUID.randomUUID();
    /** Un second client, pour vérifier que les projets ne se mélangent pas. */
    private static final UUID TENANT_B = UUID.randomUUID();

    private static final UUID PHASE_A1 = UUID.randomUUID();
    private static final UUID PHASE_A2 = UUID.randomUUID();
    private static final UUID PHASE_B1 = UUID.randomUUID();

    private static final UUID LIVRABLE_COCHE = UUID.randomUUID();
    private static final UUID LIVRABLE_PROUVE = UUID.randomUUID();
    private static final UUID LIVRABLE_MESURES = UUID.randomUUID();
    private static final UUID LIVRABLE_POINTS_VIERGES = UUID.randomUUID();
    private static final UUID LIVRABLE_B = UUID.randomUUID();

    private static final UUID PIECE = UUID.randomUUID();
    private static final UUID ACTEUR = UUID.randomUUID();

    @BeforeAll
    static void migrerEnDeuxTemps() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la reprise de la V131 reste non verifiee sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        // On s'arrête AVANT la V131 pour écrire des cycles « d'avant », puis on
        // la laisse les reprendre.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("130")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        ecrirePhase(PHASE_A1, TENANT_A, 1, "Planification");
        ecrirePhase(PHASE_A2, TENANT_A, 2, "Conception du produit");
        ecrirePhase(PHASE_B1, TENANT_B, 1, "Planification");

        // Un livrable coché, marqué PPAP, commenté : le cœur de ce qu'on ne doit
        // pas perdre.
        ecrireLivrable(LIVRABLE_COCHE, TENANT_A, PHASE_A1, 1, "Plan de projet",
                "ATTACHMENT", true, true, "reçu le 3 septembre", null);
        // Un livrable qu'une pièce prouve : la cascade doit l'épargner.
        ecrireLivrable(LIVRABLE_PROUVE, TENANT_A, PHASE_A1, 2, "Revue du SOW",
                "ATTACHMENT", false, false, null, null);
        // Des mesures RENSEIGNÉES : elles doivent se retrouver dans les notes.
        ecrireLivrable(LIVRABLE_MESURES, TENANT_A, PHASE_A2, 1, "Capabilité initiale",
                "DATA_ENTRY", true, false, null,
                "[{\"label\":\"Cp\",\"value\":\"1,45\",\"unit\":\"\",\"measuredAt\":\"2026-05-12\"},"
                + "{\"label\":\"Cpk\",\"value\":\"1,20\",\"unit\":\"\",\"measuredAt\":null}]");
        // Des sous-points tous vierges : c'est l'amorçage, rien à reprendre.
        ecrireLivrable(LIVRABLE_POINTS_VIERGES, TENANT_A, PHASE_A2, 2, "Objectifs du projet",
                "CHECKLIST", false, false, null,
                "[{\"label\":\"sécurité\",\"checked\":false},"
                + "{\"label\":\"coût\",\"checked\":false}]");
        ecrireLivrable(LIVRABLE_B, TENANT_B, PHASE_B1, 1, "Nomenclature préliminaire",
                "ATTACHMENT", false, false, null, null);

        ecrirePiece(PIECE, TENANT_A, PHASE_A1, LIVRABLE_PROUVE);

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
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

    // ---------- le versement dans un projet ----------

    @Test
    @DisplayName("chaque client qui avait un cycle reçoit UN projet par défaut, de type Other")
    void chaqueClientRecoitSonProjetParDefaut() throws SQLException {
        assertThat(compterProjets(TENANT_A)).isEqualTo(1);
        assertThat(compterProjets(TENANT_B)).isEqualTo(1);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name, type FROM apqp_projects WHERE tenant_id = ?")) {
            ps.setObject(1, TENANT_A);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Projet par défaut");
                assertThat(rs.getString("type")).isEqualTo("OTHER");
            }
        }
    }

    @Test
    @DisplayName("toutes les phases rejoignent le projet de LEUR client, aucune n'est orpheline")
    void toutesLesPhasesRejoignentLeurProjet() throws SQLException {
        assertThat(projetDe(PHASE_A1)).isEqualTo(projetDe(PHASE_A2));
        // Les deux clients ne se mélangent pas : c'est la garantie que le
        // versement a suivi le tenant et non un simple « premier projet venu ».
        assertThat(projetDe(PHASE_B1)).isNotEqualTo(projetDe(PHASE_A1));

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM apqp_phases WHERE project_id IS NULL");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            assertThat(rs.getLong(1)).isZero();
        }
    }

    @Test
    @DisplayName("un client qui n'avait aucun cycle ne reçoit aucun projet posé d'office")
    void unClientSansCycleNeRecoitRien() throws SQLException {
        assertThat(compterProjets(UUID.randomUUID())).isZero();
    }

    // ---------- rien n'est perdu ----------

    @Test
    @DisplayName("les cases cochées, les marques PPAP et les commentaires survivent")
    void lesCasesEtLesMarquesSurvivent() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT label, done, ppap, comment, status, percent_complete
                  FROM apqp_deliverables WHERE id = ?""")) {
            ps.setObject(1, LIVRABLE_COCHE);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("label")).isEqualTo("Plan de projet");
                assertThat(rs.getBoolean("done")).isTrue();
                assertThat(rs.getBoolean("ppap")).isTrue();
                assertThat(rs.getString("comment")).isEqualTo("reçu le 3 septembre");
                // La case pilote (ADR 0072) : sans cette reprise, tout le cycle
                // s'afficherait « non démarré » sous des cases cochées.
                assertThat(rs.getString("status")).isEqualTo("DONE");
                assertThat(rs.getInt("percent_complete")).isEqualTo(100);
            }
        }
    }

    @Test
    @DisplayName("les pièces jointes restent rattachées à leur livrable")
    void lesPiecesJointesSurvivent() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT deliverable_id, original_filename
                  FROM apqp_deliverable_evidences WHERE id = ?""")) {
            ps.setObject(1, PIECE);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getObject("deliverable_id")).isEqualTo(LIVRABLE_PROUVE);
                assertThat(rs.getString("original_filename")).isEqualTo("preuve.pdf");
            }
        }
    }

    @Test
    @DisplayName("le contenu saisi dans les anciens formulaires est replié dans les notes")
    void leContenuSaisiEstReplieDansLesNotes() throws SQLException {
        String notes = notesDe(LIVRABLE_MESURES);

        assertThat(notes).contains("Contenu repris de l'ancien formulaire")
                .contains("Cp : 1,45 (2026-05-12)")
                .contains("Cpk : 1,20");
    }

    @Test
    @DisplayName("un amorçage jamais renseigné ne pollue pas les notes")
    void lAmorcageIntactNePolluePasLesNotes() throws SQLException {
        // Recopier ce que le référentiel disait déjà aurait rempli les notes de
        // tout le monde d'un texte que personne n'a écrit.
        assertThat(notesDe(LIVRABLE_POINTS_VIERGES)).isNull();
    }

    @Test
    @DisplayName("un livrable qui n'avait rien reste à zéro et non démarré")
    void unLivrableVideResteANonDemarre() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT status, percent_complete, expected_artifact"
                + " FROM apqp_deliverables WHERE id = ?")) {
            ps.setObject(1, LIVRABLE_B);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("status")).isEqualTo("NOT_STARTED");
                assertThat(rs.getInt("percent_complete")).isZero();
                // L'artefact reste vide : les quarante-huit textes vivent dans le
                // code, et la lecture retombe dessus par la clé de référentiel.
                assertThat(rs.getString("expected_artifact")).isNull();
            }
        }
    }

    @Test
    @DisplayName("aucun livrable ni aucune phase n'a disparu au passage")
    void rienNAEteSupprime() throws SQLException {
        assertThat(compterLivrables()).isEqualTo(5);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM apqp_phases");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            assertThat(rs.getLong(1)).isEqualTo(3);
        }
    }

    // ---------- ce que la base refuse désormais ----------

    @Test
    @DisplayName("le genre et son contenu ont bien disparu de la table")
    void leGenreADisparuDeLaTable() {
        assertThatThrownBy(() -> executer("SELECT kind FROM apqp_deliverables"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> executer("SELECT data FROM apqp_deliverables"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("deux projets d'un même client ont chacun leur phase 1")
    void deuxProjetsOntChacunLeurPhaseUn() throws SQLException {
        // Un client à part : les bancs de comptage lisent TENANT_A et TENANT_B, et
        // leur ajouter un projet ici les ferait dépendre de l'ordre d'exécution,
        // que JUnit ne garantit pas.
        UUID client = UUID.randomUUID();
        UUID premier = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ecrireProjet(premier, client, "Support moteur 2027", "NPI");
        ecrireProjet(second, client, "Transfert ligne 4", "TOW");

        // L'ancienne contrainte portait sur (client, rang) : elle aurait déclaré
        // ces deux phases 1 en conflit, alors que deux programmes ont chacun la
        // leur.
        UUID phaseA = UUID.randomUUID();
        UUID phaseB = UUID.randomUUID();
        ecrirePhaseDansProjet(phaseA, client, premier, 1, "Planification");
        ecrirePhaseDansProjet(phaseB, client, second, 1, "Planification");

        assertThat(projetDe(phaseA)).isEqualTo(premier);
        assertThat(projetDe(phaseB)).isEqualTo(second);
    }

    @Test
    @DisplayName("deux phases au même rang DANS un projet restent refusées")
    void deuxPhasesAuMemeRangDansUnProjetSontRefusees() throws SQLException {
        UUID projet = projetDe(PHASE_A1);
        assertThatThrownBy(() -> ecrirePhaseDansProjet(
                UUID.randomUUID(), TENANT_A, projet, 1, "Doublon"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("un statut ou un avancement hors bornes est refusé par la base")
    void lesBornesSontTenuesParLaBase() {
        assertThatThrownBy(() -> executer(
                "UPDATE apqp_deliverables SET status = 'PRESQUE' WHERE id = '"
                + LIVRABLE_B + "'"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> executer(
                "UPDATE apqp_deliverables SET percent_complete = 140 WHERE id = '"
                + LIVRABLE_B + "'"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("supprimer un projet emporte son cycle, ses livrables et leurs preuves")
    void supprimerUnProjetEmporteToutSonCycle() throws SQLException {
        // Un client à part, pour la même raison : ce banc écrit puis efface, et
        // les bancs de comptage ne doivent pas dépendre de son passage.
        UUID client = UUID.randomUUID();
        UUID projet = UUID.randomUUID();
        UUID phase = UUID.randomUUID();
        UUID livrable = UUID.randomUUID();
        UUID piece = UUID.randomUUID();

        ecrireProjet(projet, client, "Transfert ligne 4", "TOW");
        ecrirePhaseDansProjet(phase, client, projet, 1, "Planification");
        ecrireLivrableDans(livrable, client, phase, 1, "Plan projet");
        ecrirePiece(piece, client, phase, livrable);

        executer("DELETE FROM apqp_projects WHERE id = '" + projet + "'");

        assertThat(existe("apqp_phases", phase)).isFalse();
        assertThat(existe("apqp_deliverables", livrable)).isFalse();
        // Une preuve sans livrable ne prouve plus rien, et son binaire redevient
        // un orphelin que le balayeur effacera.
        assertThat(existe("apqp_deliverable_evidences", piece)).isFalse();
    }

    // ---------- écritures d'appoint ----------

    private static void ecrirePhase(UUID id, UUID tenant, int rang, String titre)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_phases (id, tenant_id, position, title, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)""")) {
            Timestamp maintenant = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setInt(3, rang);
            ps.setString(4, titre);
            ps.setTimestamp(5, maintenant);
            ps.setTimestamp(6, maintenant);
            ps.executeUpdate();
        }
    }

    private static void ecrireProjet(UUID id, UUID tenant, String nom, String type)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO apqp_projects (id, tenant_id, name, type) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, nom);
            ps.setString(4, type);
            ps.executeUpdate();
        }
    }

    private static void ecrirePhaseDansProjet(UUID id, UUID tenant, UUID projet, int rang,
                                              String titre) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_phases
                  (id, tenant_id, project_id, position, title, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")) {
            Timestamp maintenant = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setObject(3, projet);
            ps.setInt(4, rang);
            ps.setString(5, titre);
            ps.setTimestamp(6, maintenant);
            ps.setTimestamp(7, maintenant);
            ps.executeUpdate();
        }
    }

    private static void ecrireLivrable(UUID id, UUID tenant, UUID phase, int rang, String libelle,
                                       String genre, boolean ppap, boolean coche,
                                       String commentaire, String contenu) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_deliverables
                  (id, tenant_id, phase_id, position, label, kind, ppap, done, done_at, done_by,
                   comment, data, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
            Timestamp maintenant = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setObject(3, phase);
            ps.setInt(4, rang);
            ps.setString(5, libelle);
            ps.setString(6, genre);
            ps.setBoolean(7, ppap);
            ps.setBoolean(8, coche);
            ps.setTimestamp(9, coche ? maintenant : null);
            ps.setObject(10, coche ? ACTEUR : null);
            ps.setString(11, commentaire);
            ps.setString(12, contenu);
            ps.setTimestamp(13, maintenant);
            ps.setTimestamp(14, maintenant);
            ps.executeUpdate();
        }
    }

    /** Un livrable écrit APRÈS la V131 : il n'a plus ni genre ni contenu. */
    private static void ecrireLivrableDans(UUID id, UUID tenant, UUID phase, int rang,
                                           String libelle) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_deliverables
                  (id, tenant_id, phase_id, position, label, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")) {
            Timestamp maintenant = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setObject(3, phase);
            ps.setInt(4, rang);
            ps.setString(5, libelle);
            ps.setTimestamp(6, maintenant);
            ps.setTimestamp(7, maintenant);
            ps.executeUpdate();
        }
    }

    private static void ecrirePiece(UUID id, UUID tenant, UUID phase, UUID livrable)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_deliverable_evidences
                  (id, tenant_id, phase_id, deliverable_id, object_key, content_type,
                   size_bytes, original_filename, uploaded_by, created_at)
                VALUES (?, ?, ?, ?, ?, 'application/pdf', 2048, 'preuve.pdf', ?, ?)""")) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setObject(3, phase);
            ps.setObject(4, livrable);
            ps.setString(5, "tenants/" + tenant + "/apqp/" + phase + "/deliverables/"
                    + livrable + "/" + id + ".pdf");
            ps.setObject(6, ACTEUR);
            ps.setTimestamp(7, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    /**
     * Joue un ordre SQL écrit en clair.
     *
     * <p>Sans paramètre : ce qu'on éprouve ici, ce sont les contraintes de la
     * base et l'existence des colonnes, pas la composition d'une requête.
     */
    private static void executer(String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private static long compterProjets(UUID tenant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM apqp_projects WHERE tenant_id = ?")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static long compterLivrables() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM apqp_deliverables");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static UUID projetDe(UUID phase) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT project_id FROM apqp_phases WHERE id = ?")) {
            ps.setObject(1, phase);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return (UUID) rs.getObject(1);
            }
        }
    }

    private static String notesDe(UUID livrable) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT comment FROM apqp_deliverables WHERE id = ?")) {
            ps.setObject(1, livrable);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static boolean existe(String table, UUID id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM " + table + " WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }
}

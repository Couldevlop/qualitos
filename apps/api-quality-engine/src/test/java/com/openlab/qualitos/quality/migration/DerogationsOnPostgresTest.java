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
 * Ce que la V181 pose, vérifié sur un <b>vrai</b> PostgreSQL.
 *
 * <p>Le profil {@code test} construit H2 depuis les entités : la migration n'y
 * tourne jamais, et aucun de ses contrôles ne s'y applique. Ce banc est donc le
 * seul endroit où l'on apprend si les listes {@code CHECK} correspondent
 * réellement à ce que le mapper écrit — une valeur oubliée dans une liste ne se
 * verrait qu'au premier enregistrement en production, sur le geste même qu'on
 * voulait protéger.
 *
 * <p>Il vérifie aussi ce que la §10.3 impose à toute table neuve : la colonne
 * {@code tenant_id}, sur laquelle {@code RlsSchema} découvre les tables à
 * cloisonner.
 */
@Tag("migration")
class DerogationsOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    private static final UUID CLIENT = UUID.randomUUID();
    private static final UUID DEMANDEUR = UUID.randomUUID();

    @BeforeAll
    static void migrate() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V181 reste non verifiee sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("181")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
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

    @Test
    void unBrouillonSEcritSansClientNiPieceNiQuantite() throws SQLException {
        // La maquette enregistre un brouillon « autant de fois qu'il veut » : des
        // colonnes NOT NULL l'auraient interdit, et le defaut n'aurait paru qu'au
        // premier enregistrement reel.
        UUID id = UUID.randomUUID();
        inserer(id, "DER-2026-0001", "DRAFT", null, null, null);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT customer_name, part_reference, quantity, customer_decision,"
                        + " pdf_language, version FROM derogations WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("customer_name")).isNull();
                assertThat(rs.getString("part_reference")).isNull();
                assertThat(rs.getBigDecimal("quantity")).isNull();
                // La decision par defaut : « En attente », pas une absence.
                assertThat(rs.getString("customer_decision")).isEqualTo("PENDING");
                assertThat(rs.getString("pdf_language")).isNull();
                assertThat(rs.getLong("version")).isZero();
            }
        }
    }

    @Test
    void lesSeptEtatsDuCycleSontTousAcceptes() throws SQLException {
        // La liste du CHECK et l'enumeration Java doivent dire la meme chose : un
        // etat oublie ne se verrait qu'au moment d'y passer, c'est-a-dire sur le
        // geste qu'on voulait proteger.
        String[] etats = {"DRAFT", "PENDING_INTERNAL", "TO_AMEND", "QUALITY_APPROVED",
                "PENDING_CUSTOMER", "ACCEPTED", "REFUSED"};
        for (int i = 0; i < etats.length; i++) {
            inserer(UUID.randomUUID(), "DER-2026-10" + i, etats[i], "ACME", "P-1", "12");
        }
        // Borne au prefixe DE CE BANC : la base est partagee entre les bancs de
        // la classe, et compter tout le registre melangerait les cas.
        assertThat(compter("SELECT count(*) FROM derogations"
                + " WHERE reference LIKE 'DER-2026-10%'")).isEqualTo(etats.length);
    }

    @Test
    void unEtatInventeEstRefuse() {
        // « SIGNING » tient dans VARCHAR(20) : sans cela, c'est la LONGUEUR qui
        // refuserait, et le CHECK ne serait jamais eprouve.
        assertThatThrownBy(() -> inserer(UUID.randomUUID(), "DER-2026-0200",
                "SIGNING", "ACME", "P-1", "12"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_derogations_status");
    }

    @Test
    void lesQuatreDecisionsDuClientSontAccepteesEtPasUneCinquieme() throws SQLException {
        String[] decisions = {"PENDING", "ACCEPTED", "ACCEPTED_WITH_CONDITIONS", "REFUSED"};
        for (int i = 0; i < decisions.length; i++) {
            String decision = decisions[i];
            UUID id = UUID.randomUUID();
            // Numerote, et non initiale : ACCEPTED et ACCEPTED_WITH_CONDITIONS
            // partagent la leur, et la reference est unique par client.
            inserer(id, "DER-2026-030" + i, "ACCEPTED", "ACME", "P-1", "12");
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE derogations SET customer_decision = ? WHERE id = ?")) {
                ps.setString(1, decision);
                ps.setObject(2, id);
                assertThat(ps.executeUpdate()).isEqualTo(1);
            }
        }
        UUID id = UUID.randomUUID();
        inserer(id, "DER-2026-0399", "ACCEPTED", "ACME", "P-1", "12");
        assertThatThrownBy(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE derogations SET customer_decision = 'PEUT_ETRE' WHERE id = ?")) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }
        }).hasMessageContaining("ck_derogations_decision");
    }

    @Test
    void laLangueNAccepteQueLesDeuxCodesEnMinuscules() throws SQLException {
        // Le mapper ecrit `code()`, pas `name()` : « fr » et non « FR ». Si la
        // liste portait les majuscules, tout envoi echouerait.
        UUID id = UUID.randomUUID();
        inserer(id, "DER-2026-0400", "PENDING_CUSTOMER", "ACME", "P-1", "12");
        for (String code : new String[]{"fr", "en"}) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE derogations SET pdf_language = ? WHERE id = ?")) {
                ps.setString(1, code);
                ps.setObject(2, id);
                assertThat(ps.executeUpdate()).isEqualTo(1);
            }
        }
        assertThatThrownBy(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE derogations SET pdf_language = 'FR' WHERE id = ?")) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }
        }).hasMessageContaining("ck_derogations_language");
    }

    @Test
    void laReferenceEstUniqueParClientEtPasEntreClients() throws SQLException {
        inserer(UUID.randomUUID(), "DER-2026-0500", "DRAFT", "ACME", "P-1", "12");
        assertThatThrownBy(() -> inserer(UUID.randomUUID(), "DER-2026-0500", "DRAFT",
                "ACME", "P-1", "12"))
                .hasMessageContaining("derogations");
        // Un AUTRE client peut porter la meme reference : la numerotation est la
        // sienne, et deux organisations n'ont pas a se gener.
        UUID autre = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO derogations (id, tenant_id, reference, status, request_date,
                                         created_by_user_id, created_by_name,
                                         created_at, updated_at)
                VALUES (?, ?, ?, 'DRAFT', DATE '2026-11-22', ?, 'Autre', now(), now())""")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, autre);
            ps.setString(3, "DER-2026-0500");
            ps.setObject(4, DEMANDEUR);
            assertThat(ps.executeUpdate()).isEqualTo(1);
        }
    }

    @Test
    void uneQuantiteNegativeEstRefusee() throws SQLException {
        assertThatThrownBy(() -> inserer(UUID.randomUUID(), "DER-2026-0600", "DRAFT",
                "ACME", "P-1", "-3"))
                .hasMessageContaining("ck_derogations_quantity");
    }

    @Test
    void deuxLignesNePartagentPasUnRangDansLeMemeDossier() throws SQLException {
        // La contrainte qui rend le piege d'Hibernate visible : reecrire les
        // lignes doit supprimer AVANT d'inserer.
        UUID dossier = UUID.randomUUID();
        inserer(dossier, "DER-2026-0700", "DRAFT", "ACME", "P-1", "12");
        insererLigne(dossier, 1, "Cote 12,4");
        assertThatThrownBy(() -> insererLigne(dossier, 1, "Doublon"))
                .hasMessageContaining("uq_derogation_lines_position");
        // Le meme rang dans un AUTRE dossier ne gene pas.
        UUID voisin = UUID.randomUUID();
        inserer(voisin, "DER-2026-0701", "DRAFT", "ACME", "P-1", "12");
        insererLigne(voisin, 1, "Son propre rang 1");
    }

    @Test
    void supprimerUnDossierEmporteSesLignes() throws SQLException {
        UUID dossier = UUID.randomUUID();
        inserer(dossier, "DER-2026-0800", "DRAFT", "ACME", "P-1", "12");
        insererLigne(dossier, 1, "A");
        insererLigne(dossier, 2, "B");
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM derogations WHERE id = ?")) {
            ps.setObject(1, dossier);
            ps.executeUpdate();
        }
        assertThat(compter("SELECT count(*) FROM derogation_deviation_lines"
                + " WHERE derogation_id = '" + dossier + "'")).isZero();
    }

    @Test
    void uneLigneOrphelineEstRefusee() {
        assertThatThrownBy(() -> insererLigne(UUID.randomUUID(), 1, "Sans dossier"))
                .hasMessageContaining("derogation");
    }

    @Test
    void lesDeuxTablesPortentTenantIdDoncRlsLesDecouvre() throws SQLException {
        // `RlsSchema` decouvre les tables a cloisonner en cherchant cette colonne :
        // une table neuve qui ne la porterait pas sortirait du cloisonnement sans
        // qu'aucune ligne de code ne l'annonce (10.3).
        for (String table : new String[]{"derogations", "derogation_deviation_lines"}) {
            assertThat(compter("""
                    SELECT count(*) FROM information_schema.columns
                     WHERE table_name = '""" + table + "' AND column_name = 'tenant_id'"))
                    .as("%s doit porter tenant_id", table)
                    .isEqualTo(1);
        }
    }

    @Test
    void lIndexPartielDesValidationsEnAttenteExiste() throws SQLException {
        // « Mes validations » affiche un compteur a chaque chargement d'ecran :
        // sans cet index, chaque affichage balaierait tout le registre.
        assertThat(compter("""
                SELECT count(*) FROM pg_indexes
                 WHERE tablename = 'derogations'
                   AND indexname = 'ix_derogations_manager_pending'""")).isEqualTo(1);
    }

    // ---- écritures -----------------------------------------------------------

    private static void inserer(UUID id, String reference, String statut, String client,
                                String piece, String quantite) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO derogations (id, tenant_id, reference, status, customer_name,
                                         part_reference, quantity, request_date,
                                         created_by_user_id, created_by_name,
                                         created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, DATE '2026-11-22', ?, 'Paul Girard',
                        now(), now())""")) {
            ps.setObject(1, id);
            ps.setObject(2, CLIENT);
            ps.setString(3, reference);
            ps.setString(4, statut);
            ps.setString(5, client);
            ps.setString(6, piece);
            if (quantite == null) {
                ps.setNull(7, java.sql.Types.NUMERIC);
            } else {
                ps.setBigDecimal(7, new java.math.BigDecimal(quantite));
            }
            ps.setObject(8, DEMANDEUR);
            ps.executeUpdate();
        }
    }

    private static void insererLigne(UUID dossier, int rang, String specification)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO derogation_deviation_lines (id, tenant_id, derogation_id,
                                                        position, specification)
                VALUES (?, ?, ?, ?, ?)""")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, CLIENT);
            ps.setObject(3, dossier);
            ps.setInt(4, rang);
            ps.setString(5, specification);
            ps.executeUpdate();
        }
    }

    private static long compter(String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}

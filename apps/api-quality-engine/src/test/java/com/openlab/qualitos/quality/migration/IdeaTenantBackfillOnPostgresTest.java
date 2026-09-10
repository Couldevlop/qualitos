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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La reprise de la V125, vérifiée sur des lignes ÉCRITES AVANT elle.
 *
 * <p>Ce que ce banc protège : `circle_proposals` ne portait aucun tenant, son
 * isolation reposant sur le cercle parent. La V125 rend ce cercle facultatif —
 * sans reprise, les propositions existantes se retrouveraient sans tenant, donc
 * mélangées entre clients à la première liste.
 *
 * <p>Sur un vrai moteur, et pas sur une doublure : un dépôt simulé rend l'objet
 * qu'on lui a donné et ne rejoue ni la reprise, ni l'unicité, ni le déclencheur.
 */
@Tag("migration")
class IdeaTenantBackfillOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();
    private static final UUID CIRCLE_A = UUID.randomUUID();
    private static final UUID CIRCLE_B = UUID.randomUUID();
    private static final UUID PROPOSAL_A = UUID.randomUUID();
    private static final UUID PROPOSAL_B = UUID.randomUUID();

    @BeforeAll
    static void migrateInTwoSteps() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la reprise de la V125 reste non verifiee sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("124")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        insertCircle(CIRCLE_A, TENANT_A, "Cercle A");
        insertCircle(CIRCLE_B, TENANT_B, "Cercle B");
        insertProposal(PROPOSAL_A, CIRCLE_A, "Eclairage LED atelier");
        insertProposal(PROPOSAL_B, CIRCLE_B, "Check-list demarrage");

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
    void laRepriseDonneAChaquePropositionLeTenantDeSonCercle() throws SQLException {
        assertThat(tenantOf(PROPOSAL_A)).isEqualTo(TENANT_A);
        assertThat(tenantOf(PROPOSAL_B)).isEqualTo(TENANT_B);
    }

    @Test
    void uneIdeeSansCercleEstDesormaisPossible() throws SQLException {
        UUID orpheline = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO circle_proposals
                  (id, circle_id, tenant_id, title, status, proposed_by, created_at, updated_at)
                VALUES (?, NULL, ?, 'Bac de tri recyclage', 'PROPOSED', ?, ?, ?)""")) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, orpheline);
            ps.setObject(2, TENANT_A);
            ps.setObject(3, UUID.randomUUID());
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.executeUpdate();
        }
        assertThat(tenantOf(orpheline)).isEqualTo(TENANT_A);
    }

    @Test
    void uneIdeeNePeutPasEtreRattacheeAuCercleDUnAutreClient() {
        // Le declencheur est le filet : sans lui, un identifiant de cercle glisse
        // d'un client a l'autre par une simple mise a jour.
        assertThatThrownBy(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE circle_proposals SET circle_id = ? WHERE id = ?")) {
                ps.setObject(1, CIRCLE_B);
                ps.setObject(2, PROPOSAL_A);
                ps.executeUpdate();
            }
        }).hasMessageContaining("tenant");
    }

    @Test
    void unMemeVotantNeVotePasDeuxFoisPourLaMemeIdee() throws SQLException {
        UUID votant = UUID.randomUUID();
        insertVote(PROPOSAL_A, votant, TENANT_A);

        assertThatThrownBy(() -> insertVote(PROPOSAL_A, votant, TENANT_A))
                .hasMessageContaining("pk_proposal_votes");
    }

    @Test
    void lesVoixDisparaissentAvecLIdee() throws SQLException {
        UUID ephemere = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO circle_proposals
                  (id, circle_id, tenant_id, title, status, proposed_by, created_at, updated_at)
                VALUES (?, NULL, ?, 'Idee ephemere', 'PROPOSED', ?, now(), now())""")) {
            ps.setObject(1, ephemere);
            ps.setObject(2, TENANT_A);
            ps.setObject(3, UUID.randomUUID());
            ps.executeUpdate();
        }
        insertVote(ephemere, UUID.randomUUID(), TENANT_A);

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM circle_proposals WHERE id = ?")) {
            ps.setObject(1, ephemere);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM proposal_votes WHERE proposal_id = ?")) {
            ps.setObject(1, ephemere);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    // ---------- fabriques ----------

    private static void insertCircle(UUID id, UUID tenant, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO quality_circles (id, tenant_id, name, status, created_at, updated_at)
                VALUES (?, ?, ?, 'ACTIVE', now(), now())""")) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, name);
            ps.executeUpdate();
        }
    }

    private static void insertProposal(UUID id, UUID circle, String title) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO circle_proposals
                  (id, circle_id, title, status, proposed_by, created_at, updated_at)
                VALUES (?, ?, ?, 'PROPOSED', ?, now(), now())""")) {
            ps.setObject(1, id);
            ps.setObject(2, circle);
            ps.setString(3, title);
            ps.setObject(4, UUID.randomUUID());
            ps.executeUpdate();
        }
    }

    private static void insertVote(UUID proposal, UUID voter, UUID tenant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO proposal_votes (proposal_id, voter_id, tenant_id)
                VALUES (?, ?, ?)""")) {
            ps.setObject(1, proposal);
            ps.setObject(2, voter);
            ps.setObject(3, tenant);
            ps.executeUpdate();
        }
    }

    private static UUID tenantOf(UUID proposalId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT tenant_id FROM circle_proposals WHERE id = ?")) {
            ps.setObject(1, proposalId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject(1, UUID.class);
            }
        }
    }
}

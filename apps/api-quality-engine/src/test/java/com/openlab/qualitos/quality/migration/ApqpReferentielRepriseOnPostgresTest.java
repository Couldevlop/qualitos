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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La reprise de la V127, vérifiée sur des cycles ÉCRITS AVANT elle.
 *
 * <p>Ce que ce banc protège : un client qui n'a jamais adapté son cycle doit
 * recevoir le nouveau référentiel, et un client qui l'a adapté ne doit RIEN
 * perdre. Les deux moitiés comptent autant — une reprise trop prudente laisse
 * tout le monde sur l'ancienne liste, une reprise trop large efface le travail
 * d'un client sans le lui dire.
 *
 * <p>Sur un vrai moteur : la condition « non touché » est une requête, pas une
 * règle Java. Une doublure de dépôt ne la jouerait jamais.
 */
@Tag("migration")
class ApqpReferentielRepriseOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    /** Cycle d'amorçage jamais touché : il doit disparaître, puis se réamorcer. */
    private static final UUID TENANT_INTACT = UUID.randomUUID();

    /** Une phase renommée : tout doit survivre. */
    private static final UUID TENANT_RENOMME = UUID.randomUUID();

    /** Un livrable ajouté : tout doit survivre. */
    private static final UUID TENANT_ENRICHI = UUID.randomUUID();

    /** Un livrable reformulé, sans toucher aux titres : tout doit survivre. */
    private static final UUID TENANT_REFORMULE = UUID.randomUUID();

    private static final List<String> ANCIENS_TITRES = List.of(
            "Planifier et définir",
            "Conception du produit",
            "Conception du processus",
            "Validation",
            "Production série et retour d'expérience");

    /** Un libellé d'origine par phase : il suffit pour que la condition porte. */
    private static final List<String> ANCIENS_LIVRABLES = List.of(
            "Objectifs de conception",
            "Revues de conception",
            "Instructions de travail",
            "Essais de validation de production",
            "Satisfaction client");

    @BeforeAll
    static void migrateInTwoSteps() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la reprise de la V127 reste non verifiee sur cette machine");

        postgres = new PostgreSQLContainer<>(IMAGE);
        postgres.start();

        // On s'arrête à la V126 : le schéma porte alors les colonnes d'achèvement,
        // mais la reprise n'a pas encore eu lieu.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("126")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        ecrireAncienCycle(TENANT_INTACT);
        ecrireAncienCycle(TENANT_RENOMME);
        ecrireAncienCycle(TENANT_ENRICHI);
        ecrireAncienCycle(TENANT_REFORMULE);

        // Le client a renommé une phase : le titre sort de l'ancienne liste, et la
        // date de modification se décale.
        executer("UPDATE apqp_phases SET title = 'Notre cadrage',"
                 + " updated_at = created_at + interval '1 second'"
                 + " WHERE tenant_id = ? AND position = 1", TENANT_RENOMME);

        // Le client a ajouté un livrable de son cru.
        ajouterLivrable(TENANT_ENRICHI, 1, "Revue de contrat client");

        // Une pièce versée sur le cycle intact : on vérifie plus bas qu'elle part
        // avec lui, sans quoi le test de cascade ne prouverait rien.
        ecrirePiece(TENANT_INTACT);

        // Le client a reformulé un libellé sans toucher aux titres de phases.
        executer("UPDATE apqp_deliverables SET label = 'Objectifs de conception (révisés)',"
                 + " updated_at = created_at + interval '1 second'"
                 + " WHERE tenant_id = ? AND label = 'Objectifs de conception'", TENANT_REFORMULE);

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

    @Test
    @DisplayName("un cycle d'amorçage jamais touché disparaît, pour se réamorcer à la prochaine lecture")
    void cycleIntact_disparait() throws SQLException {
        // L'amorçage étant paresseux, supprimer SUFFIT : l'écran le reconstruit
        // depuis le code. Réécrire ici aurait créé une seconde définition du
        // référentiel, qui divergerait du code au premier ajustement.
        assertThat(comptePhases(TENANT_INTACT)).isZero();
        assertThat(compteLivrables(TENANT_INTACT)).isZero();
    }

    @Test
    @DisplayName("un cycle dont une phase a été renommée survit intact")
    void cycleRenomme_survit() throws SQLException {
        assertThat(comptePhases(TENANT_RENOMME)).isEqualTo(5);
        assertThat(compteLivrables(TENANT_RENOMME)).isEqualTo(46);
    }

    @Test
    @DisplayName("un cycle enrichi d'un livrable maison survit intact")
    void cycleEnrichi_survit() throws SQLException {
        assertThat(comptePhases(TENANT_ENRICHI)).isEqualTo(5);
        assertThat(compteLivrables(TENANT_ENRICHI)).isEqualTo(47);
    }

    @Test
    @DisplayName("un cycle dont un seul libellé a été reformulé survit intact")
    void cycleReformule_survit() throws SQLException {
        // Le cas le plus traître : les titres de phases sont ceux de l'amorçage,
        // et seul un libellé de livrable a bougé. Sans la clause sur les
        // livrables, ce client perdrait son travail sans qu'on le sache.
        assertThat(comptePhases(TENANT_REFORMULE)).isEqualTo(5);
        assertThat(compteLivrables(TENANT_REFORMULE)).isEqualTo(46);
    }

    @Test
    @DisplayName("les pièces d'un cycle effacé partent avec lui")
    void piecesDuCycleEfface_partentAussi() throws SQLException {
        // La cascade de la V126 : une preuve sans livrable ne prouve plus rien, et
        // son binaire redevient un orphelin que le balayeur effacera.
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM apqp_deliverable_evidences WHERE tenant_id = ?")) {
            ps.setObject(1, TENANT_INTACT);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getLong(1)).isZero();
            }
        }
    }

    // ---------- écriture d'un cycle « d'avant » ----------

    /**
     * Écrit un cycle tel que l'ancien amorçage le produisait : titres français,
     * 46 livrables, et {@code updated_at = created_at} sur chaque ligne.
     */
    private static void ecrireAncienCycle(UUID tenant) throws SQLException {
        for (int rang = 1; rang <= 5; rang++) {
            UUID phaseId = UUID.randomUUID();
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO apqp_phases (id, tenant_id, position, title, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)""")) {
                Timestamp now = Timestamp.from(Instant.now());
                ps.setObject(1, phaseId);
                ps.setObject(2, tenant);
                ps.setInt(3, rang);
                ps.setString(4, ANCIENS_TITRES.get(rang - 1));
                ps.setTimestamp(5, now);
                ps.setTimestamp(6, now);
                ps.executeUpdate();
            }
            // Les comptes de l'ancien référentiel : 12, 11, 11, 8, 4.
            int combien = List.of(12, 11, 11, 8, 4).get(rang - 1);
            for (int i = 1; i <= combien; i++) {
                // Le premier porte un libellé d'origine reconnaissable ; les autres
                // aussi doivent appartenir à l'ancienne liste, sinon le cycle
                // passerait pour adapté. On reprend donc le même libellé pour les
                // suivants — ce qui suffit à la condition, qui teste l'appartenance.
                ecrireLivrable(tenant, phaseId, i, ANCIENS_LIVRABLES.get(rang - 1));
            }
        }
    }

    private static void ecrireLivrable(UUID tenant, UUID phaseId, int rang, String libelle)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_deliverables
                  (id, tenant_id, phase_id, position, label, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setObject(3, phaseId);
            ps.setInt(4, rang);
            ps.setString(5, libelle);
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);
            ps.executeUpdate();
        }
    }

    private static void ajouterLivrable(UUID tenant, int rangPhase, String libelle)
            throws SQLException {
        UUID phaseId;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM apqp_phases WHERE tenant_id = ? AND position = ?")) {
            ps.setObject(1, tenant);
            ps.setInt(2, rangPhase);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                phaseId = rs.getObject(1, UUID.class);
            }
        }
        ecrireLivrable(tenant, phaseId, 99, libelle);
    }

    /** Une pièce versée sur le premier livrable du client, pour éprouver la cascade. */
    private static void ecrirePiece(UUID tenant) throws SQLException {
        UUID phaseId;
        UUID livrableId;
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT d.phase_id, d.id
                  FROM apqp_deliverables d
                 WHERE d.tenant_id = ?
                 ORDER BY d.position
                 LIMIT 1""")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                phaseId = rs.getObject(1, UUID.class);
                livrableId = rs.getObject(2, UUID.class);
            }
        }
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO apqp_deliverable_evidences
                  (id, tenant_id, phase_id, deliverable_id, object_key, content_type,
                   size_bytes, original_filename, uploaded_by, created_at)
                VALUES (?, ?, ?, ?, ?, 'application/pdf', 4096, 'preuve.pdf', ?, ?)""")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setObject(3, phaseId);
            ps.setObject(4, livrableId);
            ps.setString(5, "tenants/" + tenant + "/apqp/" + phaseId + "/deliverables/"
                            + livrableId + "/" + UUID.randomUUID() + ".pdf");
            ps.setObject(6, UUID.randomUUID());
            ps.setTimestamp(7, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    private static void executer(String sql, UUID tenant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, tenant);
            ps.executeUpdate();
        }
    }

    private static long comptePhases(UUID tenant) throws SQLException {
        return compte("SELECT count(*) FROM apqp_phases WHERE tenant_id = ?", tenant);
    }

    private static long compteLivrables(UUID tenant) throws SQLException {
        return compte("SELECT count(*) FROM apqp_deliverables WHERE tenant_id = ?", tenant);
    }

    private static long compte(String sql, UUID tenant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}

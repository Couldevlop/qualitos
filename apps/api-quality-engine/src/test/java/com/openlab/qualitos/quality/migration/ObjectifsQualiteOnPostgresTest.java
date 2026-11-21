package com.openlab.qualitos.quality.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
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
 * La V179 sur un vrai moteur : les objectifs qualité et les CAPA qu'ils proposent.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et ignore
 * les contraintes de la migration. Ce qui ne se vérifie QU'ICI :
 * <ul>
 *   <li>la période fermée à 1, 3, 6 ou 12 mois — une période libre rendrait les
 *       dépassements consécutifs incomparables d'un objectif à l'autre ;</li>
 *   <li>l'unicité d'une proposition par objectif, niveau et période — sans elle,
 *       un écran rafraîchi trois fois proposerait trois fois le même dossier ;</li>
 *   <li>la cohérence de la décision : acceptée, elle cite son dossier ; rejetée,
 *       elle dit pourquoi. Un rejet sans motif est exactement ce qu'un auditeur
 *       ne peut pas instruire ;</li>
 *   <li>l'effacement en cascade d'un objectif vers ses propositions.</li>
 * </ul>
 */
@Tag("migration")
@DisplayName("V179 — les objectifs qualité sur PostgreSQL (ADR 0133)")
class ObjectifsQualiteOnPostgresTest {

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    private static final UUID TENANT = UUID.randomUUID();

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V179 reste non verifiee sur cette machine");
        postgres = new PostgreSQLContainer<>("postgres:17-alpine");
        postgres.start();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        connection = DriverManager.getConnection(postgres.getJdbcUrl(),
                postgres.getUsername(), postgres.getPassword());
    }

    @AfterAll
    static void stop() throws SQLException {
        if (connection != null) connection.close();
        if (postgres != null) postgres.stop();
    }

    // ---------- fixtures ----------

    /** Un KPI du catalogue : un objectif n'existe pas sans lui. */
    private static UUID kpi(String code) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO kpi_definitions (id, tenant_id, code, name, direction, frequency, "
                + "status, created_by, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, 'LOWER_IS_BETTER', 'MONTHLY', 'ACTIVE', ?, now(), now())")) {
            ps.setObject(1, id);
            ps.setObject(2, TENANT);
            ps.setString(3, code);
            ps.setString(4, "Taux de NC client");
            ps.setObject(5, UUID.randomUUID());
            ps.executeUpdate();
        }
        return id;
    }

    private static UUID objectif(UUID kpiId, int periodMonths) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO quality_objectives (id, tenant_id, title, kpi_id, period_months, "
                + "target_value, threshold_warning, threshold_critical, capa_owner_id, created_by) "
                + "VALUES (?, ?, ?, ?, ?, 500, 600, 800, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, TENANT);
            ps.setString(3, "Taux de NC client");
            ps.setObject(4, kpiId);
            ps.setInt(5, periodMonths);
            ps.setObject(6, UUID.randomUUID());
            ps.setObject(7, UUID.randomUUID());
            ps.executeUpdate();
        }
        return id;
    }

    private static void proposition(UUID objectiveId, String level, Instant debut,
                                    String status, UUID capaCaseId, String motif)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO quality_objective_proposals (tenant_id, objective_id, level, "
                + "period_start, period_end, observed_value, breach_periods, title, "
                + "effectiveness_criterion, status, capa_case_id, rejection_reason) "
                + "VALUES (?, ?, ?, ?, ?, ?, 1, 'Objectif critique', 'Retour sous la cible', "
                + "?, ?, ?)")) {
            ps.setObject(1, TENANT);
            ps.setObject(2, objectiveId);
            ps.setString(3, level);
            ps.setTimestamp(4, Timestamp.from(debut));
            ps.setTimestamp(5, Timestamp.from(debut.plusSeconds(86400L * 30)));
            ps.setBigDecimal(6, new BigDecimal("900"));
            ps.setString(7, status);
            ps.setObject(8, capaCaseId);
            ps.setString(9, motif);
            ps.executeUpdate();
        }
    }

    // ---------- l'objectif ----------

    @Test
    void lesQuatrePeriodesSontAdmises_etRienDAutre() throws SQLException {
        UUID k = kpi("periodes-" + UUID.randomUUID());
        for (int mois : new int[] {1, 3, 6, 12}) {
            assertThat(objectif(k, mois)).isNotNull();
        }
        assertThatThrownBy(() -> objectif(k, 7))
                .hasMessageContaining("ck_quality_objectives_period");
        assertThatThrownBy(() -> objectif(k, 0))
                .hasMessageContaining("ck_quality_objectives_period");
    }

    @Test
    void unIntituleVide_estRefuse() throws SQLException {
        UUID k = kpi("intitule-" + UUID.randomUUID());
        assertThatThrownBy(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO quality_objectives (tenant_id, title, kpi_id, period_months, "
                    + "target_value, threshold_warning, threshold_critical, capa_owner_id, "
                    + "created_by) VALUES (?, '   ', ?, 1, 500, 600, 800, ?, ?)")) {
                ps.setObject(1, TENANT);
                ps.setObject(2, k);
                ps.setObject(3, UUID.randomUUID());
                ps.setObject(4, UUID.randomUUID());
                ps.executeUpdate();
            }
        }).hasMessageContaining("ck_quality_objectives_title");
    }

    @Test
    void unObjectifSansKpiDuCatalogue_estRefuse() {
        assertThatThrownBy(() -> objectif(UUID.randomUUID(), 1))
                .hasMessageContaining("fk_quality_objectives_kpi");
    }

    @Test
    void unNombreDePeriodesNul_estRefuse() throws SQLException {
        UUID k = kpi("regle-" + UUID.randomUUID());
        UUID id = objectif(k, 1);
        assertThatThrownBy(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE quality_objectives SET warning_periods = 0 WHERE id = ?")) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }
        }).hasMessageContaining("ck_quality_objectives_warning_periods");
        assertThatThrownBy(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE quality_objectives SET recovery_periods = 0 WHERE id = ?")) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }
        }).hasMessageContaining("ck_quality_objectives_recovery_periods");
    }

    @Test
    void lesValeursParDefautSontCellesDeLaMaquette() throws SQLException {
        UUID id = objectif(kpi("defauts-" + UUID.randomUUID()), 1);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT warning_periods, critical_periods, recovery_periods, "
                + "root_cause_due_days, status FROM quality_objectives WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);   // deux périodes en alerte
                assertThat(rs.getInt(2)).isEqualTo(1);   // une période en critique
                assertThat(rs.getInt(3)).isEqualTo(1);
                assertThat(rs.getInt(4)).isEqualTo(5);   // cinq jours ouvrés
                assertThat(rs.getString(5)).isEqualTo("ACTIVE");
            }
        }
    }

    // ---------- la proposition ----------

    @Test
    void lesTroisEtatsSontAdmis_etRienDAutre() throws SQLException {
        UUID o = objectif(kpi("etats-" + UUID.randomUUID()), 1);
        Instant t = Instant.parse("2026-10-01T00:00:00Z");
        proposition(o, "CRITICAL", t, "PENDING", null, null);
        proposition(o, "WARNING", t, "ACCEPTED", UUID.randomUUID(), null);
        proposition(o, "WARNING", t.plusSeconds(1), "REJECTED", null, "déjà traité");
        // Un état inconnu est refusé ; c'est la contrainte de cohérence de la
        // décision qui parle la première, et peu importe laquelle des deux :
        // ce qui compte est que la ligne n'entre pas.
        assertThatThrownBy(() -> proposition(o, "CRITICAL", t.plusSeconds(2), "PEUT-ETRE",
                null, null)).hasMessageContaining("ck_qo_proposals_");
    }

    @Test
    void unNiveauInconnu_estRefuse() throws SQLException {
        UUID o = objectif(kpi("niveau-" + UUID.randomUUID()), 1);
        assertThatThrownBy(() -> proposition(o, "MOYEN",
                Instant.parse("2026-10-01T00:00:00Z"), "PENDING", null, null))
                .hasMessageContaining("ck_qo_proposals_level");
    }

    @Test
    void laMemePeriodeEtLeMemeNiveau_neSeProposentQuUneFois() throws SQLException {
        UUID o = objectif(kpi("doublon-" + UUID.randomUUID()), 1);
        Instant t = Instant.parse("2026-10-01T00:00:00Z");
        proposition(o, "CRITICAL", t, "PENDING", null, null);
        assertThatThrownBy(() -> proposition(o, "CRITICAL", t, "PENDING", null, null))
                .hasMessageContaining("uk_qo_proposals_periode");
        // Un AUTRE niveau sur la même période reste possible : l'alerte et le
        // critique ne disent pas la même chose.
        proposition(o, "WARNING", t, "PENDING", null, null);
    }

    @Test
    void acceptee_elleDoitCiterSonDossier() throws SQLException {
        UUID o = objectif(kpi("acceptee-" + UUID.randomUUID()), 1);
        assertThatThrownBy(() -> proposition(o, "CRITICAL",
                Instant.parse("2026-10-01T00:00:00Z"), "ACCEPTED", null, null))
                .hasMessageContaining("ck_qo_proposals_decision");
    }

    @Test
    void rejetee_elleDoitDirePourquoi() throws SQLException {
        UUID o = objectif(kpi("rejetee-" + UUID.randomUUID()), 1);
        Instant t = Instant.parse("2026-10-01T00:00:00Z");
        // Un motif ABSENT. Ce cas passait : dans une contrainte SQL, un prédicat
        // qui vaut NULL n'est pas une violation, et `btrim(NULL) <> ''` vaut
        // NULL. D'où le `coalesce` de la V179.
        assertThatThrownBy(() -> proposition(o, "CRITICAL", t, "REJECTED", null, null))
                .hasMessageContaining("ck_qo_proposals_decision");
        // Un motif VIDE, ou fait d'espaces : un rejet qui n'explique rien.
        assertThatThrownBy(() -> proposition(o, "WARNING", t, "REJECTED", null, "   "))
                .hasMessageContaining("ck_qo_proposals_decision");
        assertThatThrownBy(() -> proposition(o, "WARNING", t.plusSeconds(1), "REJECTED",
                null, "")).hasMessageContaining("ck_qo_proposals_decision");
    }

    @Test
    void enAttente_elleNeCiteNiDossierNiMotif() throws SQLException {
        UUID o = objectif(kpi("attente-" + UUID.randomUUID()), 1);
        Instant t = Instant.parse("2026-10-01T00:00:00Z");
        assertThatThrownBy(() -> proposition(o, "CRITICAL", t, "PENDING",
                UUID.randomUUID(), null))
                .hasMessageContaining("ck_qo_proposals_decision");
        assertThatThrownBy(() -> proposition(o, "WARNING", t, "PENDING", null, "un motif"))
                .hasMessageContaining("ck_qo_proposals_decision");
    }

    @Test
    void unObjectifEfface_emporteSesPropositions() throws SQLException {
        UUID o = objectif(kpi("cascade-" + UUID.randomUUID()), 1);
        proposition(o, "CRITICAL", Instant.parse("2026-10-01T00:00:00Z"), "PENDING", null, null);
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM quality_objectives WHERE id = ?")) {
            ps.setObject(1, o);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM quality_objective_proposals WHERE objective_id = ?")) {
            ps.setObject(1, o);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getLong(1)).isZero();
            }
        }
    }

    @Test
    void unePropositionSansObjectif_estRefusee() {
        assertThatThrownBy(() -> proposition(UUID.randomUUID(), "CRITICAL",
                Instant.parse("2026-10-01T00:00:00Z"), "PENDING", null, null))
                .hasMessageContaining("fk_qo_proposals_objective");
    }
}

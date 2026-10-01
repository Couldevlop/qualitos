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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V135 sur un vrai moteur : les filets que la base tend derrière le domaine
 * du registre — résiduelle sous la brute, référence unique par client, origine
 * propre à chaque registre, action rattachée au client de son opportunité — et
 * la contrainte d'origine des CAPA, réécrite sur l'énumération complète.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore la migration : ses contraintes et son déclencheur ne se vérifient qu'ici.
 */
@Tag("migration")
class RegistreRisquesOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V135 reste non verifiee sur cette machine");
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
        if (connection != null) {
            connection.close();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }

    private static final String INSERT_RISK = "INSERT INTO risk_register_risks (id, tenant_id, reference, title, "
            + "type, process, owner, origin, source_id, gross_severity, gross_probability, residual_severity, "
            + "residual_probability) VALUES (?, ?, ?, 'Dérive', 'QUALITY', 'Production', 'M. Kone', ?, ?, ?, ?, ?, ?)";

    private static void risque(UUID tenant, String ref, String origin, UUID source, int g, int p,
                               Integer rg, Integer rp) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_RISK)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setString(3, ref);
            ps.setString(4, origin);
            ps.setObject(5, source);
            ps.setInt(6, g);
            ps.setInt(7, p);
            ps.setObject(8, rg);
            ps.setObject(9, rp);
            ps.executeUpdate();
        }
    }

    private static UUID opportunite(UUID tenant, String ref, String origin) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO risk_register_opportunities "
                + "(id, tenant_id, reference, title, type, process, owner, origin, gain, feasibility) "
                + "VALUES (?, ?, ?, 'Automatiser', 'QUALITY', 'Production', 'Mme Diallo', ?, 4, 4)")) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, ref);
            ps.setString(4, origin);
            ps.executeUpdate();
        }
        return id;
    }

    private static void action(UUID tenant, UUID opportunity, int number) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO risk_register_actions "
                + "(id, tenant_id, opportunity_id, number, title) VALUES (?, ?, ?, ?, 'Chiffrer')")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setObject(3, opportunity);
            ps.setInt(4, number);
            ps.executeUpdate();
        }
    }

    private static long compter(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    @Test
    @DisplayName("un risque complet s'écrit, avec ou sans résiduelle")
    void unRisqueComplet() throws SQLException {
        UUID tenant = UUID.randomUUID();
        risque(tenant, "R-001", "DIRECT", null, 4, 3, null, null);
        risque(tenant, "R-002", "FMEA", UUID.randomUUID(), 4, 3, 4, 2);

        assertThat(compter("SELECT count(*) FROM risk_register_risks WHERE tenant_id = ?", tenant)).isEqualTo(2);
        assertThat(compter("SELECT count(*) FROM risk_register_risks WHERE tenant_id = ? AND status = 'TO_TREAT' "
                + "AND decision = 'UNDECIDED' AND requirements = ''", tenant)).isEqualTo(2);
    }

    @Test
    @DisplayName("une résiduelle au-dessus de la brute, ou à moitié saisie, est refusée")
    void laResiduelle() {
        assertThatThrownBy(() -> risque(UUID.randomUUID(), "R-001", "DIRECT", null, 2, 2, 3, 2))
                .isInstanceOf(SQLException.class).hasMessageContaining("ck_risk_register_risks_residual");
        assertThatThrownBy(() -> risque(UUID.randomUUID(), "R-001", "DIRECT", null, 4, 3, 2, null))
                .isInstanceOf(SQLException.class).hasMessageContaining("ck_risk_register_risks_residual");
    }

    @Test
    @DisplayName("une note hors de 1 à 5 est refusée")
    void uneNoteHorsBornes() {
        assertThatThrownBy(() -> risque(UUID.randomUUID(), "R-001", "DIRECT", null, 6, 3, null, null))
                .isInstanceOf(SQLException.class).hasMessageContaining("ck_risk_register_risks_gross");
    }

    @Test
    @DisplayName("la référence est unique dans un client, pas d'un client à l'autre")
    void laReference() throws SQLException {
        UUID a = UUID.randomUUID();
        risque(a, "R-001", "DIRECT", null, 2, 2, null, null);
        risque(UUID.randomUUID(), "R-001", "DIRECT", null, 2, 2, null, null);

        assertThatThrownBy(() -> risque(a, "R-001", "DIRECT", null, 2, 2, null, null))
                .isInstanceOf(SQLException.class).hasMessageContaining("uq_risk_register_risks_reference");
    }

    @Test
    @DisplayName("une source n'a de sens que pour une origine qui désigne un objet")
    void laSource() {
        assertThatThrownBy(() -> risque(UUID.randomUUID(), "R-001", "DIRECT", UUID.randomUUID(), 2, 2, null, null))
                .isInstanceOf(SQLException.class).hasMessageContaining("ck_risk_register_risks_source");
    }

    @Test
    @DisplayName("une opportunité ne naît ni d'une AMDEC ni d'un changement")
    void lOrigineDUneOpportunite() throws SQLException {
        opportunite(UUID.randomUUID(), "O-001", "AUDIT");
        assertThatThrownBy(() -> opportunite(UUID.randomUUID(), "O-001", "FMEA"))
                .isInstanceOf(SQLException.class).hasMessageContaining("ck_risk_register_opportunities_origin");
        assertThatThrownBy(() -> opportunite(UUID.randomUUID(), "O-001", "CHANGE"))
                .isInstanceOf(SQLException.class).hasMessageContaining("ck_risk_register_opportunities_origin");
    }

    @Test
    @DisplayName("une action appartient au client de son opportunité, et son numéro est unique dans le client")
    void lesActions() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID opp = opportunite(tenant, "O-001", "DIRECT");
        action(tenant, opp, 1);

        assertThatThrownBy(() -> action(UUID.randomUUID(), opp, 2))
                .isInstanceOf(SQLException.class).hasMessageContaining("differs from the tenant");
        assertThatThrownBy(() -> action(tenant, opp, 1))
                .isInstanceOf(SQLException.class).hasMessageContaining("uq_risk_register_actions_number");
        assertThatThrownBy(() -> action(tenant, UUID.randomUUID(), 3))
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("une CAPA peut naître d'un risque, d'une alerte SPC ou d'une anomalie — et pas d'une origine inconnue")
    void laContrainteDOrigineDesCapa() throws SQLException {
        for (String source : new String[] {"RISK", "SPC_ALERT", "ANOMALY", "NON_CONFORMITY"}) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO capa_cases (id, tenant_id, title, "
                    + "type, criticity, status, source_type, source_ref, owner_id, created_at, updated_at) "
                    + "VALUES (?, ?, 'Carte SPC', 'PREVENTIVE', 'HIGH', 'OPEN', ?, 'R-014', ?, now(), now())")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, UUID.randomUUID());
                ps.setString(3, source);
                ps.setObject(4, UUID.randomUUID());
                ps.executeUpdate();
            }
        }
        assertThat(compter("SELECT count(*) FROM capa_cases WHERE source_ref = 'R-014'")).isEqualTo(4);

        assertThatThrownBy(() -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO capa_cases (id, tenant_id, title, "
                    + "type, criticity, status, source_type, owner_id, created_at, updated_at) "
                    + "VALUES (?, ?, 'x', 'PREVENTIVE', 'HIGH', 'OPEN', 'PIRATE', ?, now(), now())")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, UUID.randomUUID());
                ps.setObject(3, UUID.randomUUID());
                ps.executeUpdate();
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("chk_capa_cases_source");
    }
}

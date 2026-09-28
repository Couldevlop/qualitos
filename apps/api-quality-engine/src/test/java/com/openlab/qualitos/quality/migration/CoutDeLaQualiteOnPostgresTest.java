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
import java.sql.Types;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La V134 sur un vrai moteur : le catalogue livré, et les filets que la base
 * tend derrière le domaine — champs pièces obligatoires, libellé d'un autre
 * client refusé, doublon de libellé refusé.
 *
 * <p>Le profil de test ordinaire bâtit son schéma depuis les entités JPA et
 * ignore donc cette migration : ses contraintes et son déclencheur ne se
 * vérifient qu'ici.
 */
@Tag("migration")
class CoutDeLaQualiteOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";
    private static final UUID REBUTS = UUID.fromString("c0a10000-0000-4000-8000-000000000301");
    private static final UUID FORMATION = UUID.fromString("c0a10000-0000-4000-8000-000000000101");

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrer() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la V134 reste non verifiee sur cette machine");
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

    private static final String INSERT_LABEL =
            "INSERT INTO coq_labels (id, tenant_id, category, code, name) VALUES (?, ?, ?, ?, ?)";

    @Test
    @DisplayName("le catalogue livré compte seize libellés, dont sept de contrôle de pièces")
    void leCatalogueLivre() throws SQLException {
        assertThat(compter("SELECT count(*) FROM coq_labels WHERE tenant_id IS NULL")).isEqualTo(16);
        assertThat(compter("SELECT count(*) FROM coq_labels WHERE tenant_id IS NULL AND part_control"))
                .isEqualTo(7);
        assertThat(compter("SELECT count(*) FROM coq_labels WHERE id = ? AND code = 'INTERNAL_SCRAP' "
                + "AND part_control", REBUTS)).isEqualTo(1);
    }

    @Test
    @DisplayName("une ligne de pièces complète s'écrit")
    void uneLigneDePiecesComplete() throws SQLException {
        UUID tenant = UUID.randomUUID();
        inserer(tenant, REBUTS, "INTERNAL_FAILURE", "1263.50", true, "P-4410", 12, "L-2609",
                LocalDate.of(2026, 9, 12));

        assertThat(compter("SELECT count(*) FROM coq_entries WHERE tenant_id = ?", tenant)).isEqualTo(1);
    }

    @Test
    @DisplayName("une ligne de pièces sans lot est refusée par la base elle-même")
    void uneLigneDePiecesSansLot() {
        assertThatThrownBy(() -> inserer(UUID.randomUUID(), REBUTS, "INTERNAL_FAILURE", "1263.50", true,
                "P-4410", 12, null, LocalDate.of(2026, 9, 12)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_coq_entries_part_fields");
    }

    @Test
    @DisplayName("une ligne ordinaire n'exige aucun champ pièce")
    void uneLigneOrdinaire() throws SQLException {
        UUID tenant = UUID.randomUUID();
        inserer(tenant, FORMATION, "PREVENTION", "180", false, null, null, null, null);

        assertThat(compter("SELECT count(*) FROM coq_entries WHERE tenant_id = ?", tenant)).isEqualTo(1);
    }

    @Test
    @DisplayName("un montant négatif est refusé")
    void unMontantNegatif() {
        assertThatThrownBy(() -> inserer(UUID.randomUUID(), FORMATION, "PREVENTION", "-1", false,
                null, null, null, null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_coq_entries_amount");
    }

    @Test
    @DisplayName("un client ne peut pas imputer sous le libellé saisi par un autre")
    void leLibelleDUnAutreClient() throws SQLException {
        UUID proprietaire = UUID.randomUUID();
        UUID libelle = UUID.randomUUID();
        libelle(libelle, proprietaire, "PREVENTION", null, "Kaizen");

        inserer(proprietaire, libelle, "PREVENTION", "10", false, null, null, null, null);
        assertThatThrownBy(() -> inserer(UUID.randomUUID(), libelle, "PREVENTION", "10", false,
                null, null, null, null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("cannot use label");
    }

    @Test
    @DisplayName("deux libellés de même nom dans la même famille d'un client sont refusés, casse ignorée")
    void unDoublonDeLibelle() throws SQLException {
        UUID tenant = UUID.randomUUID();
        libelle(UUID.randomUUID(), tenant, "APPRAISAL", null, "Tri 100 %");

        assertThatThrownBy(() -> libelle(UUID.randomUUID(), tenant, "APPRAISAL", null, "TRI 100 %"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ux_coq_labels_tenant_category_name");
        // Le même nom chez un autre client, ou dans une autre famille, reste permis.
        libelle(UUID.randomUUID(), UUID.randomUUID(), "APPRAISAL", null, "Tri 100 %");
        libelle(UUID.randomUUID(), tenant, "INTERNAL_FAILURE", null, "Tri 100 %");
    }

    @Test
    @DisplayName("un libellé saisi ne porte pas de code, un libellé livré en porte un")
    void codeSeulementSurLeLivre() {
        assertThatThrownBy(() -> libelle(UUID.randomUUID(), UUID.randomUUID(), "APPRAISAL", "X", "X"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_coq_labels_code_iff_builtin");
    }

    @Test
    @DisplayName("la devise est un code de trois lettres majuscules")
    void laDevise() throws SQLException {
        devise(UUID.randomUUID(), "USD");
        assertThatThrownBy(() -> devise(UUID.randomUUID(), "us1"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_coq_settings_currency");
    }

    @SuppressWarnings("java:S107")
    private static void inserer(UUID tenant, UUID label, String category, String montant, boolean pieces,
                                String reference, Integer quantite, String lot, LocalDate date)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO coq_entries (id, tenant_id, label_id, category, amount, responsible, "
                        + "imputation_date, part_control, part_reference, part_quantity, lot, "
                        + "received_or_made_on) VALUES (?, ?, ?, ?, ?, 'Mme Diallo', DATE '2026-09-15', "
                        + "?, ?, ?, ?, ?)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, tenant);
            ps.setObject(3, label);
            ps.setString(4, category);
            ps.setBigDecimal(5, new BigDecimal(montant));
            ps.setBoolean(6, pieces);
            ps.setString(7, reference);
            ps.setObject(8, quantite, Types.INTEGER);
            ps.setString(9, lot);
            ps.setObject(10, date, Types.DATE);
            ps.executeUpdate();
        }
    }

    private static void libelle(UUID id, UUID tenant, String category, String code, String name)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_LABEL)) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setString(3, category);
            ps.setString(4, code);
            ps.setString(5, name);
            ps.executeUpdate();
        }
    }

    private static void devise(UUID tenant, String currency) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO coq_settings (tenant_id, currency) VALUES (?, ?)")) {
            ps.setObject(1, tenant);
            ps.setString(2, currency);
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
}

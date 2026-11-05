package com.openlab.qualitos.core.migration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ce que H2 ne peut pas dire sur les tables du domaine partenaire (V11).
 *
 * <p>Le profil « test » d'api-core désactive Flyway et laisse Hibernate créer le
 * schéma : le SQL de la V11 n'est jamais exécuté par la suite habituelle. Or
 * deux de ses garanties sont des garanties de SÉCURITÉ, pas de confort :
 *
 * <ul>
 *   <li>l'index unique partiel qui interdit <b>deux accords vivants</b> sur le
 *       même couple client/cabinet — sans lui, deux accords concurrents
 *       rendraient indécidable lequel borne l'accès, et le service ne peut pas
 *       l'empêcher seul : deux requêtes simultanées passent toutes les deux son
 *       contrôle avant que l'une n'écrive ;</li>
 *   <li>la contrainte qui exige des <b>dates sur un accord accordé</b> — un
 *       accès sans terme devient un accès oublié.</li>
 * </ul>
 *
 * <p>Sans Docker, le banc se saute plutôt qu'il n'échoue — mais il le dit.
 */
@Tag("migration")
class PartnerConstraintsOnPostgresTest {

    private static Connection connection;

    /** Propres à ce banc : les classes de migration tournent en parallèle. */
    private static final UUID CLIENT = UUID.fromString("0b11c0de-0000-0000-0000-000000000c01");
    private static final UUID CABINET = UUID.fromString("0b11c0de-0000-0000-0000-000000000f01");

    @BeforeAll
    static void migrateOnASharedServer() throws SQLException {
        assumeTrue(MigrationPostgres.dockerAvailable(),
                "Docker indisponible : ces contraintes restent non verifiees sur cette machine");
        connection = MigrationPostgres.connect();
        insertTenant(CLIENT, "banc-partenaire-client");
        insertFirm(CABINET, "banc-partenaire-cabinet");
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    // ---------- un seul accord vivant ----------

    @Test
    void deux_accords_vivants_sur_le_meme_couple_sont_refuses_PAR_LA_BASE() throws SQLException {
        UUID autreCabinet = UUID.fromString("0b11c0de-0000-0000-0000-000000000f02");
        insertFirm(autreCabinet, "banc-partenaire-cabinet-2");
        insertEngagement(UUID.randomUUID(), CLIENT, autreCabinet, "PROPOSED", null, null);

        // Le second accord vivant sur le MÊME couple : refusé.
        assertThatThrownBy(() ->
                insertEngagement(UUID.randomUUID(), CLIENT, autreCabinet, "GRANTED",
                        LocalDate.now(), LocalDate.now().plusDays(30)))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void un_accord_repris_libere_la_place_pour_un_nouveau() throws SQLException {
        UUID cabinet = UUID.fromString("0b11c0de-0000-0000-0000-000000000f03");
        insertFirm(cabinet, "banc-partenaire-cabinet-3");
        insertEngagement(UUID.randomUUID(), CLIENT, cabinet, "REVOKED", null, null);
        insertEngagement(UUID.randomUUID(), CLIENT, cabinet, "DECLINED", null, null);

        // L'historique n'est pas contraint : seule la paire VIVANTE l'est, sans
        // quoi un client n'aurait jamais le droit de refaire appel à un cabinet
        // qu'il a déjà remercié.
        assertThatCode(() ->
                insertEngagement(UUID.randomUUID(), CLIENT, cabinet, "PROPOSED", null, null))
                .doesNotThrowAnyException();
    }

    // ---------- un accord accordé est daté des deux bouts ----------

    @Test
    void un_accord_accorde_sans_dates_est_refuse() throws SQLException {
        UUID cabinet = UUID.fromString("0b11c0de-0000-0000-0000-000000000f04");
        insertFirm(cabinet, "banc-partenaire-cabinet-4");

        assertThatThrownBy(() ->
                insertEngagement(UUID.randomUUID(), CLIENT, cabinet, "GRANTED", null, null))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void une_fin_anterieure_au_debut_est_refusee() throws SQLException {
        UUID cabinet = UUID.fromString("0b11c0de-0000-0000-0000-000000000f05");
        insertFirm(cabinet, "banc-partenaire-cabinet-5");

        assertThatThrownBy(() ->
                insertEngagement(UUID.randomUUID(), CLIENT, cabinet, "GRANTED",
                        LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void une_portee_inconnue_est_refusee() throws SQLException {
        UUID cabinet = UUID.fromString("0b11c0de-0000-0000-0000-000000000f06");
        insertFirm(cabinet, "banc-partenaire-cabinet-6");

        try (PreparedStatement st = connection.prepareStatement("""
                INSERT INTO partner_engagements (id, tenant_id, firm_id, scope, status)
                VALUES (?, ?, ?, 'TOUT_VOIR', 'PROPOSED')
                """)) {
            st.setObject(1, UUID.randomUUID());
            st.setObject(2, CLIENT);
            st.setObject(3, cabinet);
            assertThatThrownBy(st::executeUpdate).isInstanceOf(SQLException.class);
        }
    }

    // ---------- un consultant appartient à un seul cabinet ----------

    @Test
    void un_meme_compte_ne_sert_pas_deux_cabinets() throws SQLException {
        UUID a = UUID.fromString("0b11c0de-0000-0000-0000-000000000f07");
        UUID b = UUID.fromString("0b11c0de-0000-0000-0000-000000000f08");
        insertFirm(a, "banc-partenaire-cabinet-7");
        insertFirm(b, "banc-partenaire-cabinet-8");
        UUID compte = UUID.randomUUID();
        insertConsultant(a, compte, "claire@banc-a.fr");

        // Sinon il serait indécidable au nom de quel cabinet la personne entre
        // chez un client — et donc quel accord l'autorise.
        assertThatThrownBy(() -> insertConsultant(b, compte, "claire@banc-b.fr"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void le_meme_courriel_ne_s_inscrit_pas_deux_fois_dans_un_cabinet() throws SQLException {
        UUID cabinet = UUID.fromString("0b11c0de-0000-0000-0000-000000000f09");
        insertFirm(cabinet, "banc-partenaire-cabinet-9");
        insertConsultant(cabinet, null, "doublon@banc.fr");

        assertThatThrownBy(() -> insertConsultant(cabinet, null, "doublon@banc.fr"))
                .isInstanceOf(SQLException.class);
    }

    // ---------- écriture ----------

    private static void insertTenant(UUID id, String slug) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO tenants (id, slug, name, plan, active) VALUES (?, ?, ?, 'STARTER', TRUE)")) {
            st.setObject(1, id);
            st.setString(2, slug);
            st.setString(3, slug);
            st.executeUpdate();
        }
    }

    private static void insertFirm(UUID id, String slug) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO partner_firms (id, slug, name, active) VALUES (?, ?, ?, TRUE)")) {
            st.setObject(1, id);
            st.setString(2, slug);
            st.setString(3, slug);
            st.executeUpdate();
        }
    }

    private static void insertConsultant(UUID firm, UUID account, String email) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("""
                INSERT INTO partner_consultants (id, firm_id, account_id, display_name, email, active)
                VALUES (?, ?, ?, ?, ?, TRUE)
                """)) {
            st.setObject(1, UUID.randomUUID());
            st.setObject(2, firm);
            st.setObject(3, account);
            st.setString(4, "Banc");
            st.setString(5, email);
            st.executeUpdate();
        }
    }

    private static void insertEngagement(UUID id, UUID tenant, UUID firm, String status,
                                         LocalDate debut, LocalDate fin) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("""
                INSERT INTO partner_engagements (id, tenant_id, firm_id, scope, status, starts_on, ends_on)
                VALUES (?, ?, ?, 'READ', ?, ?, ?)
                """)) {
            st.setObject(1, id);
            st.setObject(2, tenant);
            st.setObject(3, firm);
            st.setString(4, status);
            st.setObject(5, debut);
            st.setObject(6, fin);
            st.executeUpdate();
        }
    }
}

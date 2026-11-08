package com.openlab.qualitos.core.migration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La différence entre détecter et empêcher, côté plateforme (V12).
 *
 * <p>La chaîne d'empreintes permet de CONSTATER qu'un acte a été réécrit, à
 * condition que quelqu'un lance la vérification. Entre la réécriture et sa
 * découverte, le journal ment. La V12 ferme la porte plutôt que de poser une
 * alarme : la base refuse la suppression et la modification.
 *
 * <p><b>Ce banc ne peut pas se jouer ailleurs que sur un vrai PostgreSQL.</b>
 * Un déclencheur plpgsql n'existe que là. Le profil « test » laisse Hibernate
 * créer le schéma, donc la suite habituelle tourne sur une table SANS ces
 * garanties — et serait verte sur leur absence. C'est précisément le silence
 * que la V12 corrige.
 *
 * <p>Il écrit sous le compte PROPRIÉTAIRE de la base, le plus privilégié qui
 * soit. Si la règle tient pour lui, elle tient pour le rôle applicatif, qui en
 * a moins. L'inverse ne serait pas vrai : la vérifier sous le rôle applicatif
 * laisserait croire qu'un retrait de privilège suffirait, alors qu'un tel
 * retrait est défait au déploiement suivant par le provisionnement du schéma.
 */
@Tag("migration")
class PlatformAuditAppendOnlyOnPostgresTest {

    /** Le code SQLSTATE que les déclencheurs lèvent : « privilège insuffisant ». */
    private static final String REFUS = "42501";

    private static Connection connection;

    @BeforeAll
    static void migrateOnASharedServer() throws SQLException {
        assumeTrue(MigrationPostgres.dockerAvailable(),
                "Docker indisponible : l'ajout seul de la piste d'audit reste non verifie ici");
        connection = MigrationPostgres.connect();
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    // ---------- ce que la base refuse ----------

    @Test
    void un_acte_ne_se_supprime_pas() throws SQLException {
        UUID acte = inserer(sequenceLibre(), "Client cree : banc-suppression");

        assertThatThrownBy(() -> executer("delete from platform_audit_events where id = '" + acte + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ajout seul");

        assertThat(existe(acte)).as("l'acte est toujours la").isTrue();
    }

    @Test
    void le_resume_d_un_acte_ne_se_reecrit_pas() throws SQLException {
        // Le cas réel qu'on cherche à empêcher : quelqu'un adoucit la phrase qui
        // décrit ce qu'il a fait. Sans la V12, la ligne changeait et la chaîne
        // ne le disait qu'à la prochaine vérification — c'est-à-dire jamais,
        // puisque personne ne la lance.
        UUID acte = inserer(sequenceLibre(), "Acces accorde au cabinet X");

        assertThatThrownBy(() -> executer(
                "update platform_audit_events set summary = 'Rien de notable' where id = '" + acte + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ajout seul");

        assertThat(resume(acte)).isEqualTo("Acces accorde au cabinet X");
    }

    @Test
    void l_empreinte_elle_meme_ne_se_recalcule_pas() throws SQLException {
        // La parade « intelligente » : réécrire la ligne ET son empreinte, pour
        // que la vérification de chaîne retombe sur ses pieds. Elle échoue au
        // même endroit, et c'est ce qui rend la chaîne utile plutôt que
        // décorative.
        UUID acte = inserer(sequenceLibre(), "Client desactive");

        assertThatThrownBy(() -> executer("update platform_audit_events set integrity_hash = '"
                + "f".repeat(64) + "' where id = '" + acte + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ajout seul");
    }

    @Test
    void l_acteur_d_un_acte_ne_change_pas_de_main() throws SQLException {
        UUID acte = inserer(sequenceLibre(), "Tarif fixe");

        assertThatThrownBy(() -> executer("update platform_audit_events set actor_user_id = '"
                + UUID.randomUUID() + "' where id = '" + acte + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ajout seul");
    }

    @Test
    void le_refus_porte_le_code_d_un_privilege_insuffisant() throws SQLException {
        // Le code SQLSTATE compte : c'est lui qui permet à la couche au-dessus
        // de distinguer « interdit » d'une panne, donc de répondre autre chose
        // qu'une erreur 500 au bout de la chaîne.
        UUID acte = inserer(sequenceLibre(), "Client cree : banc-sqlstate");

        assertThatThrownBy(() -> executer("delete from platform_audit_events where id = '" + acte + "'"))
                .isInstanceOfSatisfying(SQLException.class,
                        erreur -> assertThat(erreur.getSQLState()).isEqualTo(REFUS));
    }

    @Test
    void un_nouvel_acte_s_ajoute_toujours() throws SQLException {
        // La vérification qui manque dans beaucoup de durcissements : on a fermé
        // l'écriture, est-ce qu'on a fermé l'AJOUT avec ? Un journal où l'on ne
        // peut plus rien inscrire est une panne, pas une protection.
        UUID premier = inserer(sequenceLibre(), "Premier acte du banc");
        UUID second = inserer(sequenceLibre(), "Second acte du banc");

        assertThat(existe(premier)).isTrue();
        assertThat(existe(second)).isTrue();
    }

    // ---------- ce que les contraintes de colonnes refusent ----------

    @Test
    void un_acte_humain_sans_identite_est_refuse() throws SQLException {
        // Une piste d'audit qui dit « quelqu'un a changé le tarif » sans dire
        // qui n'engage personne. La contrainte le rend impossible plutôt que de
        // compter sur le code appelant.
        assertThatThrownBy(() -> insererAvecActeur(sequenceLibre(), "USER", null))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void un_acte_machine_ne_s_invente_pas_un_acteur() throws SQLException {
        assertThatThrownBy(() -> insererAvecActeur(sequenceLibre(), "SYSTEM", UUID.randomUUID()))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void un_acte_machine_sans_acteur_est_accepte() {
        assertThatCode(() -> insererAvecActeur(sequenceLibre(), "SCHEDULER", null))
                .doesNotThrowAnyException();
    }

    @Test
    void un_type_d_acteur_inconnu_est_refuse() {
        assertThatThrownBy(() -> insererAvecActeur(sequenceLibre(), "EXTERNAL", null))
                .isInstanceOf(SQLException.class);
    }

    // ---------- le compteur de séquence ----------

    @Test
    void le_compteur_avance() throws SQLException {
        long avant = compteur();

        executer("update platform_audit_counters set last_sequence_no = " + (avant + 1)
                + " where scope = 'PLATFORM'");

        assertThat(compteur()).isEqualTo(avant + 1);
    }

    @Test
    void le_compteur_ne_recule_pas() throws SQLException {
        // Réécrire l'histoire par l'AJOUT plutôt que par la modification :
        // ramener le compteur en arrière rouvre un numéro de séquence déjà
        // utilisé. La contrainte d'unicité refuserait le doublon, mais seulement
        // après la bascule — mieux vaut refuser la bascule.
        // On fait avancer le compteur d'abord : sans cela, un journal encore
        // vide (compteur à 0) ne laisse aucune valeur inférieure à tenter, et
        // le banc passerait au vert sans avoir rien éprouvé.
        long depart = compteur();
        executer("update platform_audit_counters set last_sequence_no = " + (depart + 5)
                + " where scope = 'PLATFORM'");
        long avant = compteur();

        assertThatThrownBy(() -> executer("update platform_audit_counters set last_sequence_no = "
                + (avant - 1) + " where scope = 'PLATFORM'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ne recule pas");

        assertThat(compteur()).isEqualTo(avant);
    }

    @Test
    void le_compteur_ne_se_supprime_pas() throws SQLException {
        // Le supprimer le ferait repartir de zéro à la prochaine écriture, donc
        // rouvrirait toute la plage de séquences d'un coup.
        long avant = compteur();

        assertThatThrownBy(() ->
                executer("delete from platform_audit_counters where scope = 'PLATFORM'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ne se supprime pas");

        assertThat(compteur()).isEqualTo(avant);
    }

    @Test
    void une_seconde_chaine_ne_peut_pas_apparaitre() {
        // Une deuxième portée rendrait l'ordre de deux actes indécidable : c'est
        // exactement ce que la chaîne unique garantit.
        assertThatThrownBy(() -> executer(
                "insert into platform_audit_counters (scope, last_sequence_no) values ('AUTRE', 0)"))
                .isInstanceOf(SQLException.class);
    }

    // ---------- plomberie ----------

    private static void executer(String sql) throws SQLException {
        try (PreparedStatement ordre = connection.prepareStatement(sql)) {
            ordre.executeUpdate();
        }
    }

    /**
     * Un numéro de séquence que ce banc est seul à employer.
     *
     * <p>Le serveur PostgreSQL est PARTAGÉ entre les classes de migration, qui
     * tournent en parallèle, et {@code sequence_no} est unique. Partir d'un
     * palier très haut, propre à ce banc, évite d'entrer en collision avec un
     * autre — et avec la vraie séquence applicative, qui part de 1.
     */
    private static long sequenceLibre() {
        return 900_000_000L + COMPTEUR_DU_BANC.incrementAndGet();
    }

    private static final java.util.concurrent.atomic.AtomicLong COMPTEUR_DU_BANC =
            new java.util.concurrent.atomic.AtomicLong();

    private static UUID inserer(long sequence, String resume) throws SQLException {
        return insererComplet(sequence, resume, "USER", UUID.randomUUID());
    }

    private static UUID insererAvecActeur(long sequence, String typeActeur, UUID acteur)
            throws SQLException {
        return insererComplet(sequence, "Banc de contrainte d'acteur", typeActeur, acteur);
    }

    private static UUID insererComplet(long sequence, String resume, String typeActeur, UUID acteur)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ordre = connection.prepareStatement("""
                insert into platform_audit_events (id, sequence_no, occurred_at, recorded_at,
                                                   actor_type, actor_user_id, action, target_type,
                                                   target_id, target_tenant_id, summary,
                                                   payload_json, ip_address, user_agent,
                                                   integrity_hash, previous_hash)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            Instant quand = Instant.parse("2026-11-08T09:00:00Z").plusSeconds(sequence);
            ordre.setObject(1, id);
            ordre.setLong(2, sequence);
            ordre.setTimestamp(3, Timestamp.from(quand));
            ordre.setTimestamp(4, Timestamp.from(quand));
            ordre.setString(5, typeActeur);
            ordre.setObject(6, acteur);
            ordre.setString(7, "TENANT_CREATED");
            ordre.setString(8, "tenant");
            ordre.setObject(9, UUID.randomUUID());
            ordre.setObject(10, UUID.randomUUID());
            ordre.setString(11, resume);
            ordre.setString(12, null);
            ordre.setString(13, "203.0.113.7");
            ordre.setString(14, "Chrome/130");
            // L'empreinte est unique en base : un libellé lisible complété sur
            // 64 caractères suffit ici, la chaîne elle-même est vérifiée par
            // les bancs unitaires du service.
            ordre.setString(15, complete("banc-" + sequence));
            ordre.setString(16, null);
            ordre.executeUpdate();
        }
        return id;
    }

    /** La colonne est un CHAR(64) : les libellés lisibles des bancs y sont complétés. */
    private static String complete(String valeur) {
        return valeur.length() >= 64 ? valeur.substring(0, 64)
                : valeur + "0".repeat(64 - valeur.length());
    }

    private static boolean existe(UUID id) throws SQLException {
        try (PreparedStatement requete = connection.prepareStatement(
                "select 1 from platform_audit_events where id = ?")) {
            requete.setObject(1, id);
            try (ResultSet lignes = requete.executeQuery()) {
                return lignes.next();
            }
        }
    }

    private static String resume(UUID id) throws SQLException {
        try (PreparedStatement requete = connection.prepareStatement(
                "select summary from platform_audit_events where id = ?")) {
            requete.setObject(1, id);
            try (ResultSet lignes = requete.executeQuery()) {
                lignes.next();
                return lignes.getString(1);
            }
        }
    }

    private static long compteur() throws SQLException {
        try (PreparedStatement requete = connection.prepareStatement(
                "select last_sequence_no from platform_audit_counters where scope = 'PLATFORM'")) {
            try (ResultSet lignes = requete.executeQuery()) {
                lignes.next();
                return lignes.getLong(1);
            }
        }
    }
}

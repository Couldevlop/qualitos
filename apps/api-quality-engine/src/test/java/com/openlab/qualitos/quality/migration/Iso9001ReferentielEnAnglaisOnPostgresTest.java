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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * La frontière de l'ADR 0116, vérifiée sur la base telle que les migrations la laissent.
 *
 * <p>Cette frontière est une décision de produit, et elle a deux côtés. Ce qui
 * <strong>cite la norme</strong> — sa fiche, ses sections, ses clauses, ses
 * exigences, et l'historique de ses éditions — est rédigé dans la langue où la
 * norme sera opposée, l'anglais : c'est la phrase que l'auditeur citera. Ce que
 * <strong>le client produit</strong> — son manuel qualité, sa politique, ses
 * procédures, ses processus types — reste dans sa langue : c'est son document,
 * et il le remet à son auditeur en français.
 *
 * <p>Pourquoi un banc là-dessus. La V167 avait fait passer l'arbre des exigences
 * en anglais et oublié les quatre lignes de veille normative, qui parlent
 * pourtant de la norme seule. Le défaut n'était visible qu'en ouvrant la fiche :
 * des exigences anglaises sous un onglet de veille en français. Rien, dans le
 * code, ne tenait l'invariant ; une vague de traduction suivante pouvait refaire
 * le même oubli, ou l'excès inverse en traduisant les gabarits du client.
 *
 * <p>Le test ne compare pas des phrases — elles seront réécrites. Il interroge la
 * langue : un texte français se trahit par ses mots grammaticaux, qu'aucune phrase
 * anglaise ne porte. C'est grossier, et c'est exactement ce qu'il faut pour
 * attraper un oubli de traduction entière.
 */
@Tag("migration")
class Iso9001ReferentielEnAnglaisOnPostgresTest {

    private static final String IMAGE = "postgres:17-alpine";
    private static final String ISO_9001 = "11111111-1111-1111-1111-111111111111";

    /**
     * Mots grammaticaux français qui n'existent pas en anglais. Choisis pour ne
     * pas se confondre : « de » apparaît dans des noms propres anglais, « est »
     * est un mot anglais rare mais réel, et « la » est une note de musique — tous
     * trois sont donc écartés.
     */
    private static final List<String> MOTS_FRANCAIS = List.of(
            " des ", " une ", " aux ", " doit ", " être ", " selon ",
            " leurs ", " dans ", " pour ", " avec ", " sont ", " cette ");

    private static PostgreSQLContainer<?> postgres;
    private static Connection connection;

    @BeforeAll
    static void migrate() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponible : la langue du référentiel reste non vérifiée sur cette machine");

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

    @Test
    void laFicheLesSectionsEtLesClausesSontEnAnglais() throws SQLException {
        assertThat(textes("SELECT full_name, description FROM standards WHERE id = '" + ISO_9001 + "'"))
                .isNotEmpty()
                .allSatisfy(Iso9001ReferentielEnAnglaisOnPostgresTest::estEnAnglais);

        assertThat(textes("SELECT title, description FROM standard_sections"
                + " WHERE standard_id = '" + ISO_9001 + "'"))
                .hasSizeGreaterThanOrEqualTo(7)
                .allSatisfy(Iso9001ReferentielEnAnglaisOnPostgresTest::estEnAnglais);

        assertThat(textes("SELECT c.title, c.description FROM standard_clauses c"
                + " JOIN standard_sections s ON s.id = c.section_id"
                + " WHERE s.standard_id = '" + ISO_9001 + "'"))
                .isNotEmpty()
                .allSatisfy(Iso9001ReferentielEnAnglaisOnPostgresTest::estEnAnglais);
    }

    @Test
    void lesExigencesEtLeursCriteresSontEnAnglais() throws SQLException {
        List<String> textes = textes(
                "SELECT r.text, r.measurable_criteria, r.evidence_types FROM standard_requirements r"
                        + " JOIN standard_clauses c ON c.id = r.clause_id"
                        + " JOIN standard_sections s ON s.id = c.section_id"
                        + " WHERE s.standard_id = '" + ISO_9001 + "'");

        // 42 exigences semées : le compte protège contre un test qui passerait
        // sur une base où l'arbre n'aurait pas été chargé du tout.
        assertThat(textes).hasSizeGreaterThanOrEqualTo(42);
        assertThat(textes).allSatisfy(Iso9001ReferentielEnAnglaisOnPostgresTest::estEnAnglais);
    }

    @Test
    void laVeilleNormativeEstEnAnglais() throws SQLException {
        // L'oubli de la V167, réparé par la V169 : ces lignes ne parlent que de
        // la norme — ses éditions successives — et appartiennent donc au côté
        // anglais de la frontière.
        List<String> textes = textes("SELECT version, summary, impact_note FROM standard_revisions"
                + " WHERE standard_id = '" + ISO_9001 + "'");

        assertThat(textes).hasSizeGreaterThanOrEqualTo(4);
        assertThat(textes).allSatisfy(Iso9001ReferentielEnAnglaisOnPostgresTest::estEnAnglais);
    }

    @Test
    void lesLivrablesDuClientRestentEnFrancais() throws SQLException {
        // L'autre côté de la frontière, et il compte autant. Un manuel qualité
        // livré en anglais à un organisme français serait à retraduire avant
        // d'être présenté : la traduction du référentiel ne doit pas déborder
        // jusqu'ici.
        assertThat(textes("SELECT name, description FROM standard_document_templates"
                + " WHERE standard_id = '" + ISO_9001 + "'"))
                .isNotEmpty()
                .anySatisfy(texte -> assertThat(texte).containsIgnoringCase("qualité"));

        assertThat(textes("SELECT name, description FROM standard_process_templates"
                + " WHERE standard_id = '" + ISO_9001 + "'"))
                .isNotEmpty()
                .anySatisfy(texte -> assertThat(texte).containsIgnoringCase("processus"));
    }

    private static void estEnAnglais(String texte) {
        String avecBords = " " + texte.toLowerCase().replace('\n', ' ') + " ";
        for (String mot : MOTS_FRANCAIS) {
            assertThat(avecBords)
                    .as("texte resté en français (« %s ») : %s", mot.trim(), extrait(texte))
                    .doesNotContain(mot);
        }
    }

    private static String extrait(String texte) {
        return texte.length() <= 120 ? texte : texte.substring(0, 120) + "…";
    }

    private static List<String> textes(String sql) throws SQLException {
        List<String> valeurs = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int colonnes = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= colonnes; i++) {
                    String valeur = rs.getString(i);
                    if (valeur != null && !valeur.isBlank()) {
                        valeurs.add(valeur);
                    }
                }
            }
        }
        return valeurs;
    }
}

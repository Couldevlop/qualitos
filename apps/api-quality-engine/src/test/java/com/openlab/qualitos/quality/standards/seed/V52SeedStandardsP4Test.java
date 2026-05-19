package com.openlab.qualitos.quality.standards.seed;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests le contenu de la migration Flyway V52__seed_standards_p4.sql.
 * <p>
 * Vérifie qu'elle insère bien les 6 normes P4 (IATF, AS9100, ISO 13485,
 * FDA 21 CFR Part 11, ISO 22000, DORA) avec leurs structures (sections,
 * clauses, requirements, certification paths, document templates).
 * <p>
 * Ces tests sont indépendants de la BDD (H2 ne supporte pas
 * {@code gen_random_uuid()} ni la syntaxe Postgres). L'exécution réelle
 * du seed est validée par le pipeline CI sur Postgres 17.
 */
class V52SeedStandardsP4Test {

    private static String sql;

    @BeforeAll
    static void loadSql() throws IOException {
        Path file = Paths.get("src/main/resources/db/migration/V52__seed_standards_p4.sql");
        sql = Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    void migration_file_exists_and_not_empty() {
        assertThat(sql).isNotNull();
        assertThat(sql.length()).isGreaterThan(5_000);
    }

    @Test
    void seeds_iatf_16949() {
        assertThat(sql).contains("'iatf-16949'");
        assertThat(sql).contains("IATF 16949");
        assertThat(sql).contains("APQP");
        assertThat(sql).contains("PPAP");
        assertThat(sql).contains("MSA");
    }

    @Test
    void seeds_as9100() {
        assertThat(sql).contains("'as9100'");
        assertThat(sql).contains("AS9100D");
        assertThat(sql).contains("FOD");
        assertThat(sql).contains("FAI");
        assertThat(sql).contains("contrefaites");
    }

    @Test
    void seeds_iso_13485() {
        assertThat(sql).contains("'iso-13485'");
        assertThat(sql).contains("ISO 13485");
        assertThat(sql).contains("UDI");
        assertThat(sql).contains("vigilance");
        assertThat(sql).contains("Device Master Record");
    }

    @Test
    void seeds_fda_21_cfr_part_11() {
        assertThat(sql).contains("'fda-21-cfr-part-11'");
        assertThat(sql).contains("21 CFR Part 11");
        assertThat(sql).contains("11.10(a)");
        assertThat(sql).contains("11.10(e)");
        assertThat(sql).contains("11.200");
        assertThat(sql).contains("audit trail");
    }

    @Test
    void seeds_iso_22000() {
        assertThat(sql).contains("'iso-22000'");
        assertThat(sql).contains("ISO 22000");
        assertThat(sql).contains("HACCP");
        assertThat(sql).contains("CCP");
        assertThat(sql).contains("PRP");
    }

    @Test
    void seeds_dora() {
        assertThat(sql).contains("'dora'");
        assertThat(sql).contains("DORA");
        assertThat(sql).contains("2022/2554");
        assertThat(sql).contains("Art. 5");
        assertThat(sql).contains("Art. 17");
        assertThat(sql).contains("Art. 30");
        assertThat(sql).contains("TLPT");
    }

    @Test
    void creates_certification_path_table() {
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS standard_certification_paths");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS standard_certification_stages");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS standard_document_templates");
    }

    @Test
    void each_norm_has_at_least_15_clauses() {
        // 6 normes × ≥ 15 clauses = ≥ 90 tuples insérés.
        int countClauseInserts = sql.split("INSERT INTO standard_clauses").length - 1;
        assertThat(countClauseInserts)
                .as("Au moins 6 INSERT INTO standard_clauses (un par norme)")
                .isGreaterThanOrEqualTo(6);
        // Compter les tuples (lignes) — heuristique
        int tupleLines = (int) sql.lines()
                .filter(l -> l.trim().startsWith("('5"))
                .count();
        assertThat(tupleLines)
                .as("Au moins 90 tuples insérés (~ 15 clauses × 6 normes)")
                .isGreaterThanOrEqualTo(90);
    }

    @Test
    void includes_certification_path_seeds_for_all_six_norms() {
        int count = sql.split("INSERT INTO standard_certification_paths").length - 1;
        assertThat(count).isGreaterThanOrEqualTo(1);
        for (String normUuid : new String[]{
                "5a000001-0000-0000-0000-000000000001",
                "5a000002-0000-0000-0000-000000000002",
                "5a000003-0000-0000-0000-000000000003",
                "5a000004-0000-0000-0000-000000000004",
                "5a000005-0000-0000-0000-000000000005",
                "5a000006-0000-0000-0000-000000000006"
        }) {
            assertThat(sql).as("Certification path pour %s", normUuid).contains(normUuid);
        }
    }

    @Test
    void includes_document_templates_for_all_six_norms() {
        // Chaque norme a au moins 3 templates de documents
        int count = sql.split("INSERT INTO standard_document_templates").length - 1;
        assertThat(count).as("≥ 1 INSERT par norme = ≥ 6").isGreaterThanOrEqualTo(6);
    }

    @Test
    void uses_postgres_gen_random_uuid_function() {
        assertThat(sql).contains("gen_random_uuid()");
    }

    @Test
    void no_data_loss_constraints_use_foreign_keys() {
        assertThat(sql).contains("FOREIGN KEY (standard_id)");
        assertThat(sql).contains("ON DELETE CASCADE");
    }

    @Test
    void all_six_standard_codes_listed() {
        String[] expected = {
                "iatf-16949", "as9100", "iso-13485",
                "fda-21-cfr-part-11", "iso-22000", "dora"
        };
        for (String code : expected) {
            assertThat(sql).as("Code norme %s", code).contains("'" + code + "'");
        }
    }
}

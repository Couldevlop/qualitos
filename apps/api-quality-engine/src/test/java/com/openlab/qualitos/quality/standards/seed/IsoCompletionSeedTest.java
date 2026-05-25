package com.openlab.qualitos.quality.standards.seed;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le contenu des migrations qui complètent les référentiels ISO en
 * structure HLS §4→§10 : V57 (ISO 9001) et V59 (ISO 27001 / 14001 / 45001).
 * Tests indépendants de la BDD (le seed réel est validé en rollback + CI Postgres).
 */
class IsoCompletionSeedTest {

    private static String read(String migration) throws IOException {
        Path file = Paths.get("src/main/resources/db/migration/" + migration);
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    void v57_completes_iso9001_sections_6_7_8() throws IOException {
        String sql = read("V57__seed_iso9001_complete_clauses.sql");
        // sections 6,7,8 d'ISO 9001 référencées par leurs UUID existants (V10)
        assertThat(sql).contains("a3333333-3333-3333-3333-333333333333"); // §6
        assertThat(sql).contains("a4444444-4444-4444-4444-444444444444"); // §7
        assertThat(sql).contains("a5555555-5555-5555-5555-555555555555"); // §8
        assertThat(sql).contains("'7.5.3'");   // maîtrise des informations documentées
        assertThat(sql).contains("'8.7'");     // sorties non conformes
        assertThat(sql).contains("INSERT INTO standard_requirements");
    }

    @Test
    void v59_completes_iso27001_with_support_and_operation_sections() throws IOException {
        String sql = read("V59__seed_iso27001_14001_45001_complete.sql");
        assertThat(sql).contains("'22222222-2222-2222-2222-222222222222'"); // ISO 27001
        assertThat(sql).contains("b4444444-4444-4444-4444-444444444444");   // §7 ajoutée
        assertThat(sql).contains("b5555555-5555-5555-5555-555555555555");   // §8 ajoutée
        assertThat(sql).contains("'8.3'");  // traitement des risques
        assertThat(sql).contains("SoA");
    }

    @Test
    void v59_completes_iso14001_and_iso45001() throws IOException {
        String sql = read("V59__seed_iso27001_14001_45001_complete.sql");
        assertThat(sql).contains("'33333333-3333-3333-3333-333333333333'"); // ISO 14001
        assertThat(sql).contains("'44444444-4444-4444-4444-444444444444'"); // ISO 45001
        assertThat(sql).contains("e4444444-4444-4444-4444-444444444444");   // 14001 §7
        assertThat(sql).contains("64444444-4444-4444-4444-444444444444");   // 45001 §4
        assertThat(sql).contains("hiérarchie des mesures");                 // 45001 §8.1
    }

    @Test
    void v59_uses_foreign_key_safe_existing_sections_and_obligations() throws IOException {
        String sql = read("V59__seed_iso27001_14001_45001_complete.sql");
        // obligations valides selon chk_requirement_obligation
        assertThat(sql).contains("'MUST'");
        // au moins une INSERT par table de structure
        assertThat(sql).contains("INSERT INTO standard_sections");
        assertThat(sql).contains("INSERT INTO standard_clauses");
        assertThat(sql).contains("INSERT INTO standard_requirements");
    }

    @Test
    void v60_completes_iso22301_full_hls() throws IOException {
        String sql = read("V60__seed_iso22301_complete.sql");
        assertThat(sql).contains("'55555555-5555-5555-5555-555555555555'"); // ISO 22301
        assertThat(sql).contains("84444444-4444-4444-4444-444444444444");   // §4 ajoutée
        assertThat(sql).contains("85555555-5555-5555-5555-555555555555");   // §5 ajoutée
        assertThat(sql).contains("87777777-7777-7777-7777-777777777777");   // §7 ajoutée
        assertThat(sql).contains("88888888-8888-8888-8888-888888888888");   // §10 ajoutée
        assertThat(sql).contains("'8.4.4'");                                // plans de continuité
        assertThat(sql).contains("INSERT INTO standard_requirements");
    }
}

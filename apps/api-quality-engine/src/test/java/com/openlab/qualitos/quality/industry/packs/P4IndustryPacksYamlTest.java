package com.openlab.qualitos.quality.industry.packs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie la conformité au contrat des 6 Industry Packs P4 livrés
 * sous {@code libs/industry-packs/packs/}.
 * <p>
 * Sécurité OWASP A03/A08 : parsing avec SafeConstructor (pas de
 * désérialisation d'objets arbitraires).
 */
class P4IndustryPacksYamlTest {

    private static final Path PACKS_DIR = Paths.get("..", "..", "libs", "industry-packs", "packs");

    private static Yaml safeYaml() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(String filename) throws IOException {
        Path file = PACKS_DIR.resolve(filename);
        assertThat(file).as("Pack file %s exists", filename).exists();
        try (var r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return (Map<String, Object>) safeYaml().load(r);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "automotive.yaml", "aerospace.yaml", "pharma.yaml",
            "agro.yaml", "banking.yaml", "public.yaml"
    })
    void pack_has_minimum_contract_fields(String filename) throws IOException {
        Map<String, Object> pack = load(filename);
        assertThat(pack).containsKeys(
                "pack_id", "version", "name", "sectors", "norms",
                "kpis", "glossary", "connectors_required",
                "ishikawa_templates", "poka_yoke_library",
                "training_paths", "documents_templates", "processes_templates"
        );
        assertThat(pack.get("pack_id")).isEqualTo(filename.replace(".yaml", ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "automotive.yaml", "aerospace.yaml", "pharma.yaml",
            "agro.yaml", "banking.yaml", "public.yaml"
    })
    @SuppressWarnings("unchecked")
    void pack_has_at_least_6_sector_kpis(String filename) throws IOException {
        Map<String, Object> pack = load(filename);
        List<Map<String, Object>> kpis = (List<Map<String, Object>>) pack.get("kpis");
        assertThat(kpis).as("KPIs in %s", filename).hasSizeGreaterThanOrEqualTo(6);
        for (Map<String, Object> kpi : kpis) {
            assertThat(kpi).as("KPI in %s", filename)
                    .containsKeys("kpi_id", "name", "formula", "unit", "target", "data_source", "owner");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "automotive.yaml", "aerospace.yaml", "pharma.yaml",
            "agro.yaml", "banking.yaml", "public.yaml"
    })
    @SuppressWarnings("unchecked")
    void pack_has_at_least_8_ishikawa_templates(String filename) throws IOException {
        Map<String, Object> pack = load(filename);
        List<?> ishikawa = (List<?>) pack.get("ishikawa_templates");
        assertThat(ishikawa).as("Ishikawa templates in %s", filename)
                .hasSizeGreaterThanOrEqualTo(8);
        for (Object item : ishikawa) {
            Map<String, Object> tmpl = (Map<String, Object>) item;
            assertThat(tmpl).containsKey("problem_archetype").containsKey("branches");
            Map<String, Object> branches = (Map<String, Object>) tmpl.get("branches");
            assertThat(branches).containsKeys(
                    "man", "machine", "material", "method", "measurement", "environment"
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "automotive.yaml", "aerospace.yaml", "pharma.yaml",
            "agro.yaml", "banking.yaml", "public.yaml"
    })
    @SuppressWarnings("unchecked")
    void pack_has_at_least_4_poka_yoke_entries(String filename) throws IOException {
        Map<String, Object> pack = load(filename);
        List<Map<String, Object>> pokas = (List<Map<String, Object>>) pack.get("poka_yoke_library");
        assertThat(pokas).as("Poka-Yoke entries in %s", filename)
                .hasSizeGreaterThanOrEqualTo(4);
        for (Map<String, Object> p : pokas) {
            assertThat(p).containsKeys("id", "name", "description", "sector_fit");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "automotive.yaml", "aerospace.yaml", "pharma.yaml",
            "agro.yaml", "banking.yaml", "public.yaml"
    })
    void pack_id_matches_filename(String filename) throws IOException {
        Map<String, Object> pack = load(filename);
        String expected = filename.replace(".yaml", "");
        assertThat(pack.get("pack_id")).isEqualTo(expected);
    }

    @Test
    @SuppressWarnings("unchecked")
    void automotive_references_iatf_16949() throws IOException {
        Map<String, Object> pack = load("automotive.yaml");
        assertThat((List<String>) pack.get("norms")).contains("iatf-16949");
    }

    @Test
    @SuppressWarnings("unchecked")
    void aerospace_references_as9100() throws IOException {
        Map<String, Object> pack = load("aerospace.yaml");
        assertThat((List<String>) pack.get("norms")).contains("as9100");
    }

    @Test
    @SuppressWarnings("unchecked")
    void pharma_references_iso_13485_and_part11() throws IOException {
        Map<String, Object> pack = load("pharma.yaml");
        assertThat((List<String>) pack.get("norms"))
                .contains("iso-13485")
                .contains("fda-21-cfr-part-11");
    }

    @Test
    @SuppressWarnings("unchecked")
    void agro_references_iso_22000() throws IOException {
        Map<String, Object> pack = load("agro.yaml");
        assertThat((List<String>) pack.get("norms")).contains("iso-22000");
    }

    @Test
    @SuppressWarnings("unchecked")
    void banking_references_dora() throws IOException {
        Map<String, Object> pack = load("banking.yaml");
        assertThat((List<String>) pack.get("norms")).contains("dora");
    }

    @Test
    @SuppressWarnings("unchecked")
    void public_references_iso_18091() throws IOException {
        Map<String, Object> pack = load("public.yaml");
        assertThat((List<String>) pack.get("norms")).contains("iso-18091");
    }
}

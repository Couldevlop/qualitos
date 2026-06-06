package com.openlab.qualitos.quality.industry.packs;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de contrat du <b>schéma riche canonique</b> des Industry Packs (ADR 0019).
 *
 * <p>Depuis l'ADR 0019, le contenu canonique vit dans le classpath de l'engine
 * ({@code src/main/resources/industry-packs/}). Ce test scanne ce répertoire et
 * applique le contrat <em>uniquement aux fichiers au schéma riche</em> (présence de
 * la clé {@code pack_id}) — les packs au schéma plat historique sont ignorés.
 *
 * <p>Les assertions sont <b>par pack présent</b> (pas de compte global en dur) : le test
 * reste vert que 0, 3 ou 9 packs riches soient livrés au moment de l'exécution — le
 * contenu de production est ajouté en parallèle par un autre chantier.
 *
 * <p>Sécurité OWASP A03/A08 : parsing avec {@link SafeConstructor} (pas de
 * désérialisation d'objets arbitraires).
 */
class P4IndustryPacksYamlTest {

    private static final Path PACKS_DIR =
            Paths.get("src", "main", "resources", "industry-packs");

    private static Yaml safeYaml() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(Path file) {
        try (var r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object root = safeYaml().load(r);
            return root instanceof Map ? (Map<String, Object>) root : Map.of();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tous les fichiers .yml/.yaml du répertoire de ressources de l'engine. */
    private static List<Path> packFiles() {
        if (!Files.isDirectory(PACKS_DIR)) return List.of();
        try (Stream<Path> s = Files.list(PACKS_DIR)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".yml") || n.endsWith(".yaml");
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Fichiers au schéma riche canonique uniquement (clé {@code pack_id}). */
    private static List<Path> richPackFiles() {
        return packFiles().stream()
                .filter(p -> load(p).containsKey("pack_id"))
                .toList();
    }

    @TestFactory
    @SuppressWarnings("unchecked")
    Stream<DynamicTest> rich_pack_honours_canonical_contract() {
        return richPackFiles().stream().map(file -> DynamicTest.dynamicTest(
                "contract: " + file.getFileName(), () -> {
            Map<String, Object> pack = load(file);

            // --- Champs de tête obligatoires ---
            assertThat(pack).as("top-level keys in %s", file.getFileName())
                    .containsKeys("pack_id", "version", "name", "sectors", "norms",
                            "kpis", "glossary", "ishikawa_templates", "poka_yoke_library");

            String fileName = file.getFileName().toString();
            String expectedId = fileName.substring(0, fileName.lastIndexOf('.'));
            assertThat(pack.get("pack_id")).as("pack_id matches filename in %s", fileName)
                    .isEqualTo(expectedId);

            // --- KPIs §6.6 : >= 6, champs complets ---
            List<Map<String, Object>> kpis = (List<Map<String, Object>>) pack.get("kpis");
            assertThat(kpis).as("KPIs in %s", fileName).hasSizeGreaterThanOrEqualTo(6);
            for (Map<String, Object> kpi : kpis) {
                assertThat(kpi).as("KPI in %s", fileName).containsKeys(
                        "kpi_id", "name", "formula", "unit", "target", "data_source", "owner");
            }

            // --- Ishikawa : >= 8, branches 6M ---
            List<Map<String, Object>> ishikawa =
                    (List<Map<String, Object>>) pack.get("ishikawa_templates");
            assertThat(ishikawa).as("Ishikawa templates in %s", fileName)
                    .hasSizeGreaterThanOrEqualTo(8);
            for (Map<String, Object> tmpl : ishikawa) {
                assertThat(tmpl).as("Ishikawa template in %s", fileName)
                        .containsKey("problem_archetype").containsKey("branches");
                Map<String, Object> branches = (Map<String, Object>) tmpl.get("branches");
                assertThat(branches).as("6M branches in %s", fileName).containsKeys(
                        "man", "machine", "material", "method", "measurement", "environment");
            }

            // --- Poka-Yoke : >= 4, champs requis ---
            List<Map<String, Object>> pokas =
                    (List<Map<String, Object>>) pack.get("poka_yoke_library");
            assertThat(pokas).as("Poka-Yoke entries in %s", fileName)
                    .hasSizeGreaterThanOrEqualTo(4);
            for (Map<String, Object> p : pokas) {
                assertThat(p).as("Poka-Yoke in %s", fileName)
                        .containsKeys("id", "name", "description", "sector_fit");
            }

            // --- Glossaire : liste d'objets {term, definition} ---
            List<Map<String, Object>> glossary = (List<Map<String, Object>>) pack.get("glossary");
            assertThat(glossary).as("glossary in %s", fileName).isNotEmpty();
            for (Map<String, Object> g : glossary) {
                assertThat(g).as("glossary entry in %s", fileName)
                        .containsKeys("term", "definition");
            }
        }));
    }
}

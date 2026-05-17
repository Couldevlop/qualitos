package com.openlab.qualitos.industry.infrastructure.yaml;

import com.openlab.qualitos.industry.domain.model.ConnectorRef;
import com.openlab.qualitos.industry.domain.model.DocumentTemplate;
import com.openlab.qualitos.industry.domain.model.IndustryPack;
import com.openlab.qualitos.industry.domain.model.IshikawaTemplate;
import com.openlab.qualitos.industry.domain.model.KpiDefinition;
import com.openlab.qualitos.industry.domain.model.PokaYokeDevice;
import com.openlab.qualitos.industry.domain.model.TrainingPath;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * OWASP-safe loader for {@link IndustryPack} YAML files.
 *
 * <p>Hardening (OWASP A03 Injection / A08 Integrity / Deserialization Top 10):
 * <ul>
 *   <li>{@link SafeConstructor} only — rejects {@code !!java/object} and any
 *       global tag that would instantiate arbitrary classes.</li>
 *   <li>Explicit pre-scan rejects any {@code !!java/} token in the raw bytes
 *       (defense-in-depth — a malformed tag could still confuse SafeConstructor).</li>
 *   <li>Document size capped at {@value #MAX_BYTES} bytes; alias / anchor /
 *       collection bombs capped via {@link LoaderOptions}.</li>
 *   <li>SHA-256 fingerprint computed on the raw bytes — stored alongside the
 *       pack so we can prove integrity at activation time.</li>
 * </ul>
 *
 * <p>This class is pure infrastructure: it depends on no Spring code so it can
 * be reused by any module (api-core, api-iot-hub, future industry-adapter).
 */
public final class IndustryPackYamlLoader {

  /** Maximum allowed pack size — 256 KiB. Reject larger files to defend against alias bombs. */
  public static final int MAX_BYTES = 256 * 1024;

  /** Patterns that the raw bytes must NOT contain (any Java type tag). */
  private static final String[] FORBIDDEN_TAGS = {"!!java/", "!!java.", "!!javax/", "!!sun."};

  public IndustryPack load(InputStream in) throws IOException {
    Objects.requireNonNull(in, "input");
    byte[] bytes = in.readAllBytes();
    return load(bytes);
  }

  public IndustryPack load(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes");
    if (bytes.length == 0) {
      throw new IndustryPackParseException("Empty pack");
    }
    if (bytes.length > MAX_BYTES) {
      throw new IndustryPackParseException(
          "Pack exceeds maximum size of " + MAX_BYTES + " bytes (got " + bytes.length + ")");
    }
    String text = new String(bytes, StandardCharsets.UTF_8);
    for (String forbidden : FORBIDDEN_TAGS) {
      if (text.contains(forbidden)) {
        throw new IndustryPackParseException(
            "Forbidden YAML tag detected: " + forbidden + " (deserialization gadget refused)");
      }
    }
    String sha256 = sha256Hex(bytes);

    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    options.setMaxAliasesForCollections(50);
    options.setProcessComments(false);
    options.setCodePointLimit(MAX_BYTES);

    Yaml yaml = new Yaml(new SafeConstructor(options));
    Object root = yaml.load(text);
    if (!(root instanceof Map<?, ?> rootMap)) {
      throw new IndustryPackParseException("Pack root must be a mapping");
    }

    return fromMap(rootMap, sha256);
  }

  @SuppressWarnings("unchecked")
  private IndustryPack fromMap(Map<?, ?> raw, String sha256) {
    Map<String, Object> root = (Map<String, Object>) raw;
    String id = required(root, "id");
    String version = required(root, "version");
    String name = required(root, "name");
    List<String> sectors = strList(root.get("sectors"));
    List<String> supportedNorms = strList(root.get("supported_norms"));
    Map<String, String> glossary = strMap(root.get("glossary"));

    List<KpiDefinition> kpis = mapList(root.get("kpis"), this::toKpi);
    List<ConnectorRef> connectors = mapList(root.get("connectors"), this::toConnector);
    List<IshikawaTemplate> ishikawa = mapList(root.get("ishikawa_templates"), this::toIshikawa);
    List<PokaYokeDevice> poka = mapList(root.get("poka_yoke_library"), this::toPoka);
    List<TrainingPath> training = mapList(root.get("training_paths"), this::toTraining);
    List<DocumentTemplate> docs = mapList(root.get("documents_templates"), this::toDocTemplate);

    Instant publishedAt = Optional.ofNullable(root.get("published_at"))
        .map(Object::toString)
        .map(Instant::parse)
        .orElse(Instant.now());

    return new IndustryPack(
        id, version, name, sectors, supportedNorms,
        kpis, glossary, connectors, ishikawa, poka, training, docs,
        publishedAt, sha256);
  }

  @SuppressWarnings("unchecked")
  private KpiDefinition toKpi(Map<String, Object> m) {
    return new KpiDefinition(
        required(m, "id"),
        required(m, "name"),
        str(m.get("category")),
        required(m, "formula"),
        str(m.get("unit")),
        str(m.get("target")),
        str(m.get("threshold_warning")),
        str(m.get("threshold_critical")),
        str(m.get("data_source")),
        str(m.get("refresh_frequency")),
        str(m.get("owner")),
        strList(m.get("applicable_industries")),
        strList(m.get("related_kpis")),
        str(m.get("explainability")));
  }

  @SuppressWarnings("unchecked")
  private ConnectorRef toConnector(Map<String, Object> m) {
    return new ConnectorRef(required(m, "type"), required(m, "name"), strMap(m.get("config")));
  }

  @SuppressWarnings("unchecked")
  private IshikawaTemplate toIshikawa(Map<String, Object> m) {
    Map<String, List<String>> seeds = new LinkedHashMap<>();
    Object raw = m.get("seed_causes");
    if (raw instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> e : map.entrySet()) {
        seeds.put(e.getKey().toString(), strList(e.getValue()));
      }
    }
    return new IshikawaTemplate(
        required(m, "id"),
        required(m, "name"),
        str(m.get("problem_archetype")),
        strList(m.get("branches")),
        seeds);
  }

  @SuppressWarnings("unchecked")
  private PokaYokeDevice toPoka(Map<String, Object> m) {
    return new PokaYokeDevice(
        required(m, "id"),
        required(m, "name"),
        str(m.get("type")),
        str(m.get("category")),
        str(m.get("description")),
        strList(m.get("applies_to")));
  }

  @SuppressWarnings("unchecked")
  private TrainingPath toTraining(Map<String, Object> m) {
    Object dh = m.get("duration_hours");
    Integer dur = dh instanceof Number n ? n.intValue() : null;
    return new TrainingPath(
        required(m, "id"),
        required(m, "name"),
        str(m.get("target_role")),
        str(m.get("level")),
        dur,
        strList(m.get("modules")));
  }

  @SuppressWarnings("unchecked")
  private DocumentTemplate toDocTemplate(Map<String, Object> m) {
    return new DocumentTemplate(
        required(m, "id"),
        required(m, "name"),
        str(m.get("kind")),
        str(m.get("media_type")),
        str(m.get("path")),
        strList(m.get("maps_to_norms")));
  }

  // ---- helpers --------------------------------------------------------

  @SuppressWarnings("unchecked")
  private <T> List<T> mapList(Object raw, java.util.function.Function<Map<String, Object>, T> f) {
    if (raw == null) return List.of();
    if (!(raw instanceof List<?> list)) {
      throw new IndustryPackParseException("Expected list, got " + raw.getClass().getSimpleName());
    }
    List<T> out = new ArrayList<>(list.size());
    for (Object o : list) {
      if (!(o instanceof Map<?, ?> m)) {
        throw new IndustryPackParseException("Expected mapping inside list");
      }
      out.add(f.apply((Map<String, Object>) m));
    }
    return out;
  }

  private static String required(Map<String, Object> m, String key) {
    Object v = m.get(key);
    if (v == null) {
      throw new IndustryPackParseException("Missing required field: " + key);
    }
    return v.toString();
  }

  private static String str(Object v) {
    return v == null ? null : v.toString();
  }

  @SuppressWarnings("unchecked")
  private static List<String> strList(Object v) {
    if (v == null) return List.of();
    if (v instanceof List<?> list) {
      List<String> out = new ArrayList<>(list.size());
      for (Object o : list) out.add(o == null ? null : o.toString());
      return out;
    }
    throw new IndustryPackParseException("Expected list of strings");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> strMap(Object v) {
    if (v == null) return Map.of();
    if (v instanceof Map<?, ?> map) {
      Map<String, String> out = new LinkedHashMap<>();
      for (Map.Entry<?, ?> e : map.entrySet()) {
        out.put(e.getKey().toString(), e.getValue() == null ? null : e.getValue().toString());
      }
      return out;
    }
    throw new IndustryPackParseException("Expected map of strings");
  }

  static String sha256Hex(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}

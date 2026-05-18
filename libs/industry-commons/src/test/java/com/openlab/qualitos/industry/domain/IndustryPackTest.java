package com.openlab.qualitos.industry.domain;

import com.openlab.qualitos.industry.domain.model.ApplyResult;
import com.openlab.qualitos.industry.domain.model.ConnectorRef;
import com.openlab.qualitos.industry.domain.model.DocumentTemplate;
import com.openlab.qualitos.industry.domain.model.IndustryPack;
import com.openlab.qualitos.industry.domain.model.IshikawaTemplate;
import com.openlab.qualitos.industry.domain.model.KpiDefinition;
import com.openlab.qualitos.industry.domain.model.PokaYokeDevice;
import com.openlab.qualitos.industry.domain.model.TrainingPath;
import com.openlab.qualitos.industry.domain.model.ValidationResult;
import com.openlab.qualitos.industry.infrastructure.external.YamlIndustryPackProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndustryPackTest {

  @Test
  void recordIsImmutableAndDefensivelyCopies() {
    java.util.List<String> mutable = new java.util.ArrayList<>(List.of("ISIC-C"));
    IndustryPack p = new IndustryPack(
        "x", "1.0.0", "X",
        mutable, List.of("iso-9001"),
        List.of(), Map.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "deadbeef");
    mutable.add("MUTATED");
    assertThat(p.sectors()).containsExactly("ISIC-C");
  }

  @Test
  void coordinateConcatenatesIdAndVersion() {
    IndustryPack p = new IndustryPack(
        "manufacturing", "1.0.0", "Mfg",
        List.of(), List.of(), List.of(), Map.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "x");
    assertThat(p.coordinate()).isEqualTo("manufacturing@1.0.0");
  }

  @Test
  void kpiDefinitionRejectsNullId() {
    assertThatThrownBy(() -> new KpiDefinition(
        null, "n", "c", "f", "%", "t", "tw", "tc",
        "ds", "rf", "o", List.of(), List.of(), "e"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void providerApplyProducesAuditEntry() {
    IndustryPack pack = new IndustryPack(
        "test", "1.0.0", "Test",
        List.of("X"), List.of("iso-9001"),
        List.of(new KpiDefinition("k1","K1","c","f", "%","t","tw","tc","ds","rf","o", List.of(), List.of(),"e")),
        Map.of(),
        List.of(new ConnectorRef("opc-ua", "plc1", Map.of())),
        List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "abc123");
    YamlIndustryPackProvider provider = new YamlIndustryPackProvider(pack);

    ValidationResult v = provider.validate();
    assertThat(v.valid()).isTrue();

    UUID tenant = UUID.randomUUID();
    var result = provider.apply(tenant, "tester@qualitos.example");
    assertThat(result.tenantId()).isEqualTo(tenant);
    assertThat(result.packId()).isEqualTo("test");
    assertThat(result.activatedKpis()).contains("k1");
    assertThat(result.activatedConnectors()).contains("opc-ua:plc1");
    assertThat(result.activatedNorms()).contains("iso-9001");
  }

  @Test
  void providerRejectsBlankTenant() {
    IndustryPack pack = new IndustryPack(
        "test", "1.0.0", "Test",
        List.of(), List.of(), List.of(), Map.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "x");
    YamlIndustryPackProvider provider = new YamlIndustryPackProvider(pack);
    assertThatThrownBy(() -> provider.apply(null, "x")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> provider.apply(UUID.randomUUID(), "")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void providerValidationFailsOnBlankId() {
    IndustryPack pack = new IndustryPack(
        "", "1.0.0", "Test",
        List.of(), List.of(), List.of(), Map.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "x");
    YamlIndustryPackProvider provider = new YamlIndustryPackProvider(pack);
    ValidationResult v = provider.validate();
    assertThat(v.valid()).isFalse();
    assertThat(v.errors()).anyMatch(e -> e.contains("id"));
  }

  @Test
  void providerValidationFailsOnBlankVersion() {
    IndustryPack pack = new IndustryPack(
        "id", "", "Test",
        List.of(), List.of(), List.of(), Map.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "x");
    ValidationResult v = new YamlIndustryPackProvider(pack).validate();
    assertThat(v.valid()).isFalse();
    assertThat(v.errors()).anyMatch(e -> e.contains("version"));
  }

  @Test
  void providerValidationFailsOnBlankName() {
    IndustryPack pack = new IndustryPack(
        "id", "1.0.0", "",
        List.of(), List.of(), List.of(), Map.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "x");
    ValidationResult v = new YamlIndustryPackProvider(pack).validate();
    assertThat(v.valid()).isFalse();
    assertThat(v.errors()).anyMatch(e -> e.contains("name"));
  }

  @Test
  void providerApplyRejectsInvalidPack() {
    // Blank id ⇒ validate() returns invalid ⇒ apply() must throw IllegalStateException
    IndustryPack pack = new IndustryPack(
        "", "1.0.0", "Test",
        List.of(), List.of(), List.of(), Map.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "x");
    YamlIndustryPackProvider provider = new YamlIndustryPackProvider(pack);
    assertThatThrownBy(() -> provider.apply(UUID.randomUUID(), "alice"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Pack invalid");
  }

  @Test
  void providerApplyRejectsNullActivatedBy() {
    // Exercises isBlank(null) branch in YamlIndustryPackProvider
    IndustryPack pack = new IndustryPack(
        "id", "1.0.0", "Test",
        List.of(), List.of(), List.of(), Map.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "x");
    assertThatThrownBy(() -> new YamlIndustryPackProvider(pack).apply(UUID.randomUUID(), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("activatedBy");
  }

  @Test
  void providerExposesPackAccessors() {
    IndustryPack pack = new IndustryPack(
        "p", "2.0.0", "Pack",
        List.of("A"), List.of("iso-9001"), List.of(), Map.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "h");
    YamlIndustryPackProvider provider = new YamlIndustryPackProvider(pack);
    assertThat(provider.id()).isEqualTo("p");
    assertThat(provider.version()).isEqualTo("2.0.0");
    assertThat(provider.sectors()).containsExactly("A");
    assertThat(provider.supportedNorms()).containsExactly("iso-9001");
    assertThat(provider.getPack()).isSameAs(pack);
  }

  @Test
  void providerRejectsNullPack() {
    assertThatThrownBy(() -> new YamlIndustryPackProvider(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---- Canonical-constructor null-coalescing branches ---------------------

  @Test
  void industryPackCoalescesNullCollections() {
    // Exercises every `xxx == null ? List.of() : List.copyOf(xxx)` branch
    IndustryPack p = new IndustryPack(
        "x", "1.0.0", "X",
        null, null, null, null,
        null, null, null, null, null,
        Instant.now(), "h");
    assertThat(p.sectors()).isEmpty();
    assertThat(p.supportedNorms()).isEmpty();
    assertThat(p.kpis()).isEmpty();
    assertThat(p.glossary()).isEmpty();
    assertThat(p.connectors()).isEmpty();
    assertThat(p.ishikawaTemplates()).isEmpty();
    assertThat(p.pokaYokeLibrary()).isEmpty();
    assertThat(p.trainingPaths()).isEmpty();
    assertThat(p.documentsTemplates()).isEmpty();
  }

  @Test
  void kpiDefinitionCoalescesNullLists() {
    KpiDefinition k = new KpiDefinition(
        "k", "K", null, "f", null, null, null, null,
        null, null, null, null, null, null);
    assertThat(k.applicableIndustries()).isEmpty();
    assertThat(k.relatedKpis()).isEmpty();
  }

  @Test
  void connectorRefCoalescesNullConfig() {
    ConnectorRef c = new ConnectorRef("mqtt", "broker", null);
    assertThat(c.config()).isEmpty();
  }

  @Test
  void ishikawaTemplateCoalescesNulls() {
    IshikawaTemplate t = new IshikawaTemplate("t", "T", null, null, null);
    assertThat(t.branches()).isEmpty();
    assertThat(t.seedCauses()).isEmpty();
  }

  @Test
  void pokaYokeDeviceCoalescesNullAppliesTo() {
    PokaYokeDevice d = new PokaYokeDevice("d", "D", null, null, null, null);
    assertThat(d.appliesTo()).isEmpty();
  }

  @Test
  void trainingPathCoalescesNullModules() {
    TrainingPath t = new TrainingPath("t", "T", null, null, null, null);
    assertThat(t.modules()).isEmpty();
  }

  @Test
  void documentTemplateCoalescesNullMapsToNorms() {
    DocumentTemplate d = new DocumentTemplate("d", "D", null, null, null, null);
    assertThat(d.mapsToNorms()).isEmpty();
  }

  @Test
  void applyResultCoalescesNullLists() {
    ApplyResult r = new ApplyResult(
        UUID.randomUUID(), "p", "1", "h", Instant.now(),
        null, null, null);
    assertThat(r.activatedConnectors()).isEmpty();
    assertThat(r.activatedKpis()).isEmpty();
    assertThat(r.activatedNorms()).isEmpty();
  }

  // ---- ValidationResult static factory + null guards ----------------------

  @Test
  void validationResultOkNoArgsHasEmptyErrorsAndWarnings() {
    ValidationResult r = ValidationResult.ok();
    assertThat(r.valid()).isTrue();
    assertThat(r.errors()).isEmpty();
    assertThat(r.warnings()).isEmpty();
  }
}

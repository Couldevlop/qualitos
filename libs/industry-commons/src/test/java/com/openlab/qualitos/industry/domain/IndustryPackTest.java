package com.openlab.qualitos.industry.domain;

import com.openlab.qualitos.industry.domain.model.ConnectorRef;
import com.openlab.qualitos.industry.domain.model.IndustryPack;
import com.openlab.qualitos.industry.domain.model.KpiDefinition;
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
}

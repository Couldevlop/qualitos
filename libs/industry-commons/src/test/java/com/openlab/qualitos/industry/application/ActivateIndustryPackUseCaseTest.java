package com.openlab.qualitos.industry.application;

import com.openlab.qualitos.industry.application.usecase.ActivateIndustryPackUseCase;
import com.openlab.qualitos.industry.domain.model.IndustryPack;
import com.openlab.qualitos.industry.infrastructure.external.InMemoryIndustryPackRegistry;
import com.openlab.qualitos.industry.infrastructure.external.YamlIndustryPackProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivateIndustryPackUseCaseTest {

  private static IndustryPack samplePack(String id) {
    return new IndustryPack(
        id, "1.0.0", id + " name",
        List.of("ISIC-C"), List.of("iso-9001"),
        List.of(), Map.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
        Instant.now(), "deadbeef");
  }

  @Test
  void activatesKnownPack() {
    var registry = new InMemoryIndustryPackRegistry(List.of(
        new YamlIndustryPackProvider(samplePack("manufacturing"))));
    var useCase = new ActivateIndustryPackUseCase(registry);
    UUID tenant = UUID.randomUUID();
    var result = useCase.activate(tenant, "manufacturing", "alice");
    assertThat(result.tenantId()).isEqualTo(tenant);
    assertThat(result.packId()).isEqualTo("manufacturing");
  }

  @Test
  void rejectsUnknownPack() {
    var registry = new InMemoryIndustryPackRegistry(List.of());
    var useCase = new ActivateIndustryPackUseCase(registry);
    assertThatThrownBy(() -> useCase.activate(UUID.randomUUID(), "nope", "u"))
        .isInstanceOf(ActivateIndustryPackUseCase.IndustryPackNotFoundException.class);
  }

  @Test
  void registryListsAllPacks() {
    var registry = new InMemoryIndustryPackRegistry(List.of(
        new YamlIndustryPackProvider(samplePack("a")),
        new YamlIndustryPackProvider(samplePack("b"))));
    assertThat(registry.listAll()).hasSize(2);
    assertThat(registry.find("a")).isPresent();
    assertThat(registry.find("missing")).isEmpty();
  }

  @Test
  void registryIgnoresNullProviders() {
    var registry = new InMemoryIndustryPackRegistry(java.util.Arrays.asList(
        null,
        new YamlIndustryPackProvider(samplePack("ok"))));
    assertThat(registry.listAll()).hasSize(1);
  }

  @Test
  void registryIgnoresProvidersWithNullId() {
    // Exercises the `p.id() == null` branch in the registry's filter
    var brokenProvider = new com.openlab.qualitos.industry.domain.port.IndustryPackProvider() {
      @Override public String id() { return null; }
      @Override public String version() { return "0"; }
      @Override public java.util.List<String> sectors() { return java.util.List.of(); }
      @Override public java.util.List<String> supportedNorms() { return java.util.List.of(); }
      @Override public IndustryPack getPack() { return samplePack("ignored"); }
      @Override public com.openlab.qualitos.industry.domain.model.ValidationResult validate() {
        return com.openlab.qualitos.industry.domain.model.ValidationResult.ok();
      }
      @Override public com.openlab.qualitos.industry.domain.model.ApplyResult apply(UUID t, String by) {
        throw new UnsupportedOperationException();
      }
    };
    var registry = new InMemoryIndustryPackRegistry(java.util.List.of(
        brokenProvider,
        new YamlIndustryPackProvider(samplePack("good"))));
    assertThat(registry.listAll()).hasSize(1);
    assertThat(registry.findProvider("good")).isPresent();
  }
}

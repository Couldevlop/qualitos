package com.openlab.qualitos.iot.infrastructure.fhir;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du connecteur d'ingestion FHIR R5 (CLAUDE.md §9.4 — Santé).
 *
 * <p>Actif par défaut (endpoint REST entrant, aucun système externe requis) ;
 * désactivable par {@code qualitos.iot.fhir.enabled=false} — dans ce cas ni le
 * handler ni le contrôleur ne sont créés.
 */
@Configuration
@EnableConfigurationProperties(FhirProperties.class)
@ConditionalOnProperty(
    prefix = "qualitos.iot.fhir", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FhirConnectorConfig {

  @Bean
  public FhirObservationMapper fhirObservationMapper() {
    return new FhirObservationMapper();
  }

  @Bean
  public FhirIngestionHandler fhirIngestionHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      FhirObservationMapper mapper,
      FhirProperties props) {
    return new FhirIngestionHandler(
        deviceRepository, ingestUseCase, mapper, props.getMaxBundleEntries());
  }
}

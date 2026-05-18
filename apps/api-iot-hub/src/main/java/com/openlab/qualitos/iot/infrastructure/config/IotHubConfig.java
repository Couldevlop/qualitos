package com.openlab.qualitos.iot.infrastructure.config;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.application.usecase.RegisterDeviceUseCase;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.NonConformancePublisher;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import com.openlab.qualitos.iot.domain.port.ThresholdRegistry;
import com.openlab.qualitos.iot.domain.service.StreamRuleEngine;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class IotHubConfig {

  @Bean
  public StreamRuleEngine streamRuleEngine(ThresholdRegistry registry) {
    return new StreamRuleEngine(registry);
  }

  @Bean
  public RegisterDeviceUseCase registerDeviceUseCase(DeviceRepository deviceRepository) {
    return new RegisterDeviceUseCase(deviceRepository);
  }

  @Bean
  public IngestTelemetryUseCase ingestTelemetryUseCase(
      DeviceRepository deviceRepository,
      TelemetryRepository telemetryRepository,
      StreamRuleEngine engine,
      NonConformancePublisher publisher) {
    return new IngestTelemetryUseCase(deviceRepository, telemetryRepository, engine, publisher);
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .connectTimeout(Duration.ofSeconds(2))
        .readTimeout(Duration.ofSeconds(5))
        .build();
  }
}

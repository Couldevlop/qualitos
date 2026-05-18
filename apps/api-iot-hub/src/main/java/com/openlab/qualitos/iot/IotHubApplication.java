package com.openlab.qualitos.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** QualitOS IoT Hub — Device Registry + Telemetry ingestion + Stream rule engine. */
@SpringBootApplication
public class IotHubApplication {
  public static void main(String[] args) {
    SpringApplication.run(IotHubApplication.class, args);
  }
}

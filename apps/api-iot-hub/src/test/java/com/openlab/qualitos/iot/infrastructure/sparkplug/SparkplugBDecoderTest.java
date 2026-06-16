package com.openlab.qualitos.iot.infrastructure.sparkplug;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Décodeur Sparkplug B (JSON) : mapping, horodatage, robustesse — unitaire pur. */
class SparkplugBDecoderTest {

  private final ObjectMapper json = new ObjectMapper();
  private final SparkplugBDecoder decoder = new SparkplugBDecoder();

  private SparkplugBDecoder.DecodeResult decode(String s) throws Exception {
    return decoder.decode(json.readTree(s));
  }

  @Test
  @DisplayName("Payload DDATA valide → deviceId + métriques numériques décodées")
  void validDdataDecoded() throws Exception {
    var r = decode("""
        {"groupId":"usine-A","edgeNodeId":"node-1","deviceId":"capteur-01",
         "metrics":[
           {"name":"Temperature","value":4.2,"unit":"degC"},
           {"name":"Vibration","value":1.5,"unit":"g"}
         ],
         "timestamp":1749722400000}""");

    assertThat(r.isDropped()).isFalse();
    assertThat(r.deviceCode()).isEqualTo("capteur-01");
    assertThat(r.metrics()).hasSize(2);
    assertThat(r.metrics().get(0).name()).isEqualTo("Temperature");
    assertThat(r.metrics().get(0).value()).isEqualTo(4.2);
    assertThat(r.metrics().get(0).unit()).isEqualTo("degC");
    assertThat(r.metrics().get(0).recordedAt()).isEqualTo(Instant.ofEpochMilli(1749722400000L));
  }

  @Test
  @DisplayName("Timestamp ISO-8601 (texte) accepté ; métrique sans ts hérite du payload")
  void isoTimestampAndInheritance() throws Exception {
    var r = decode("""
        {"deviceId":"d1","timestamp":"2026-06-12T10:00:00Z",
         "metrics":[{"name":"m","value":1}]}""");

    assertThat(r.isDropped()).isFalse();
    assertThat(r.metrics().get(0).recordedAt()).isEqualTo(Instant.parse("2026-06-12T10:00:00Z"));
    assertThat(r.metrics().get(0).unit()).isNull();
  }

  @Test
  @DisplayName("Timestamp propre à la métrique l'emporte sur celui du payload")
  void metricTimestampOverridesPayload() throws Exception {
    var r = decode("""
        {"deviceId":"d1","timestamp":1000,
         "metrics":[{"name":"m","value":1,"timestamp":2000}]}""");

    assertThat(r.metrics().get(0).recordedAt()).isEqualTo(Instant.ofEpochMilli(2000L));
  }

  @Test
  @DisplayName("NDATA sans deviceId → le code est l'Edge Node")
  void ndataFallsBackToEdgeNode() throws Exception {
    var r = decode("""
        {"groupId":"g","edgeNodeId":"node-1","metrics":[{"name":"m","value":3}]}""");

    assertThat(r.isDropped()).isFalse();
    assertThat(r.deviceCode()).isEqualTo("node-1");
  }

  @Test
  void noDeviceCodeIsDropped() throws Exception {
    var r = decode("{\"groupId\":\"g\",\"metrics\":[{\"name\":\"m\",\"value\":1}]}");
    assertThat(r.isDropped()).isTrue();
    assertThat(r.dropReason()).contains("device code");
  }

  @Test
  void noMetricsArrayIsDropped() throws Exception {
    var r = decode("{\"deviceId\":\"d1\"}");
    assertThat(r.isDropped()).isTrue();
    assertThat(r.dropReason()).contains("metrics");
  }

  @Test
  @DisplayName("Métriques pourries (sans name ou valeur non-numérique) sont ignorées une à une")
  void rottenMetricsAreSkipped() throws Exception {
    var r = decode("""
        {"deviceId":"d1","metrics":[
          {"value":1},
          {"name":"ok","value":7.7},
          {"name":"text","value":"NaN"},
          {"name":"obj","value":{"x":1}}
        ]}""");

    assertThat(r.isDropped()).isFalse();
    assertThat(r.metrics()).hasSize(1);
    assertThat(r.metrics().get(0).name()).isEqualTo("ok");
  }

  @Test
  @DisplayName("Aucune métrique exploitable → rejet avec motif")
  void noUsableMetricIsDropped() throws Exception {
    var r = decode("{\"deviceId\":\"d1\",\"metrics\":[{\"name\":\"x\"},{\"value\":1}]}");
    assertThat(r.isDropped()).isTrue();
    assertThat(r.dropReason()).contains("valid metric");
  }

  @Test
  void nonObjectPayloadIsDropped() {
    assertThat(decoder.decode(null).isDropped()).isTrue();
  }

  @Test
  void invalidTimestampLeavesRecordedAtNull() throws Exception {
    var r = decode("""
        {"deviceId":"d1","timestamp":"not-a-date","metrics":[{"name":"m","value":1}]}""");
    assertThat(r.isDropped()).isFalse();
    assertThat(r.metrics().get(0).recordedAt()).isNull();
  }
}

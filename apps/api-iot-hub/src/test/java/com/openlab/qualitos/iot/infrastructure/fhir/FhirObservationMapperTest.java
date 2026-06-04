package com.openlab.qualitos.iot.infrastructure.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper FHIR R5 Observation → télémétrie : tests purs (aucun contexte Spring).
 */
class FhirObservationMapperTest {

  private final ObjectMapper json = new ObjectMapper();
  private final FhirObservationMapper mapper = new FhirObservationMapper();

  private JsonNode parse(String s) throws Exception {
    return json.readTree(s);
  }

  // ---- chemins nominaux -------------------------------------------------------

  @Test
  @DisplayName("Observation complète (identifier + LOINC + valueQuantity + effectiveDateTime)")
  void fullObservationIsMapped() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"identifier":{"system":"urn:qualitos:device","value":"FRIDGE-PH-01"}},
         "code":{"coding":[{"system":"http://loinc.org","code":"8310-5"}],"text":"Temp"},
         "valueQuantity":{"value":7.4,"unit":"Cel","code":"Cel"},
         "effectiveDateTime":"2026-06-04T10:15:30Z"}"""));

    assertThat(result.isDropped()).isFalse();
    var obs = result.observation();
    assertThat(obs.deviceCode()).isEqualTo("FRIDGE-PH-01");
    assertThat(obs.metric()).isEqualTo("8310-5");
    assertThat(obs.value()).isEqualTo(7.4);
    assertThat(obs.unit()).isEqualTo("Cel");
    assertThat(obs.recordedAt()).isEqualTo(Instant.parse("2026-06-04T10:15:30Z"));
  }

  @Test
  @DisplayName("device.reference 'Device/<code>' accepté quand identifier absent")
  void deviceReferenceFormIsAccepted() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/AUTOCLAVE-3"},
         "code":{"coding":[{"code":"temp"}]},
         "valueQuantity":{"value":121.0}}"""));

    assertThat(result.isDropped()).isFalse();
    assertThat(result.observation().deviceCode()).isEqualTo("AUTOCLAVE-3");
  }

  @Test
  @DisplayName("device.display en dernier recours")
  void deviceDisplayFallback() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"display":"SONDE-T-12"},
         "code":{"text":"temperature"},
         "valueQuantity":{"value":4.1}}"""));

    assertThat(result.isDropped()).isFalse();
    assertThat(result.observation().deviceCode()).isEqualTo("SONDE-T-12");
    assertThat(result.observation().metric()).isEqualTo("temperature");
  }

  @Test
  @DisplayName("effectiveDateTime avec offset local (+02:00) — toléré par FHIR")
  void offsetDateTimeIsParsed() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/D1"},
         "code":{"coding":[{"code":"m"}]},
         "valueQuantity":{"value":1},
         "effectiveDateTime":"2026-06-04T12:00:00+02:00"}"""));

    assertThat(result.observation().recordedAt())
        .isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
  }

  @Test
  @DisplayName("date invalide → repli sur 'issued', sinon null (horodatage serveur)")
  void invalidDateFallsBackToIssuedThenNull() throws Exception {
    var withIssued = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/D1"},
         "code":{"coding":[{"code":"m"}]},
         "valueQuantity":{"value":1},
         "effectiveDateTime":"pas-une-date",
         "issued":"2026-06-04T09:00:00Z"}"""));
    assertThat(withIssued.observation().recordedAt())
        .isEqualTo(Instant.parse("2026-06-04T09:00:00Z"));

    var withoutDates = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/D1"},
         "code":{"coding":[{"code":"m"}]},
         "valueQuantity":{"value":1}}"""));
    assertThat(withoutDates.observation().recordedAt()).isNull();
  }

  @Test
  @DisplayName("unit absent → repli sur le code UCUM")
  void unitFallsBackToUcumCode() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/D1"},
         "code":{"coding":[{"code":"m"}]},
         "valueQuantity":{"value":1,"code":"mmHg"}}"""));
    assertThat(result.observation().unit()).isEqualTo("mmHg");
  }

  // ---- rejets (fail-closed) ---------------------------------------------------

  @Test
  void nullOrNonObjectIsDropped() throws Exception {
    assertThat(mapper.map(null).isDropped()).isTrue();
    assertThat(mapper.map(parse("[1,2]")).isDropped()).isTrue();
    assertThat(mapper.map(parse("\"texte\"")).isDropped()).isTrue();
  }

  @Test
  void wrongResourceTypeIsDropped() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Patient","id":"p1"}"""));
    assertThat(result.isDropped()).isTrue();
    assertThat(result.dropReason()).contains("Observation");
  }

  @Test
  void missingDeviceIsDropped() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Observation",
         "code":{"coding":[{"code":"m"}]},
         "valueQuantity":{"value":1}}"""));
    assertThat(result.isDropped()).isTrue();
    assertThat(result.dropReason()).contains("device");
  }

  @Test
  @DisplayName("reference 'Device/' vide et sans display → rejet")
  void blankDeviceReferenceIsDropped() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/"},
         "code":{"coding":[{"code":"m"}]},
         "valueQuantity":{"value":1}}"""));
    assertThat(result.isDropped()).isTrue();
  }

  @Test
  void missingMetricIsDropped() throws Exception {
    var result = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/D1"},
         "valueQuantity":{"value":1}}"""));
    assertThat(result.isDropped()).isTrue();
    assertThat(result.dropReason()).contains("metric");
  }

  @Test
  void missingOrNonNumericValueIsDropped() throws Exception {
    var missing = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/D1"},
         "code":{"coding":[{"code":"m"}]}}"""));
    assertThat(missing.isDropped()).isTrue();

    var textual = mapper.map(parse("""
        {"resourceType":"Observation",
         "device":{"reference":"Device/D1"},
         "code":{"coding":[{"code":"m"}]},
         "valueQuantity":{"value":"sept"}}"""));
    assertThat(textual.isDropped()).isTrue();
    assertThat(textual.dropReason()).contains("valueQuantity");
  }
}

package com.openlab.qualitos.iot.infrastructure.modbus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Décodeur de lectures Modbus TCP/RTU : parsing des registres, unités, horodatage
 * (ISO/offset/epoch), robustesse registres pourris — tests unitaires purs, sans Spring.
 */
class ModbusReadingDecoderTest {

  private final ObjectMapper json = new ObjectMapper();
  private final ModbusReadingDecoder decoder = new ModbusReadingDecoder();

  private JsonNode parse(String s) throws Exception {
    return json.readTree(s);
  }

  @Test
  @DisplayName("Lecture complète → code device + mesures par registre + unités + horodatage")
  void decodesFullReading() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-ATELIER-3",
         "readings":[
           {"register":40001,"metric":"pressure","value":4.2,"unit":"bar"},
           {"register":40002,"metric":"temperature","value":55.0,"unit":"degC"}],
         "time":"2026-06-16T08:30:00Z"}"""));

    assertThat(result.deviceCode()).isEqualTo("PLC-ATELIER-3");
    assertThat(result.droppedReadings()).isZero();
    assertThat(result.measurements()).hasSize(2);
    assertThat(result.measurements())
        .anySatisfy(m -> {
          assertThat(m.metric()).isEqualTo("pressure");
          assertThat(m.value()).isEqualTo(4.2);
          assertThat(m.unit()).isEqualTo("bar");
          assertThat(m.recordedAt()).isEqualTo(Instant.parse("2026-06-16T08:30:00Z"));
        })
        .anySatisfy(m -> {
          assertThat(m.metric()).isEqualTo("temperature");
          assertThat(m.value()).isEqualTo(55.0);
          assertThat(m.unit()).isEqualTo("degC");
        });
  }

  @Test
  @DisplayName("Métrique absente → nom dérivé du numéro de registre")
  void derivesMetricFromRegisterWhenMissing() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[{"register":40005,"value":12.5}]}"""));

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> {
          assertThat(m.metric()).isEqualTo("register_40005");
          assertThat(m.value()).isEqualTo(12.5);
          assertThat(m.unit()).isNull();
        });
  }

  @Test
  @DisplayName("Horodatage en epoch millisecondes accepté")
  void parsesEpochMillis() throws Exception {
    long epoch = Instant.parse("2026-06-16T08:30:00Z").toEpochMilli();
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[{"register":40001,"value":1}],"time":%d}"""
        .formatted(epoch)));

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.recordedAt()).isEqualTo(Instant.parse("2026-06-16T08:30:00Z")));
  }

  @Test
  @DisplayName("Offset local toléré sur le champ time")
  void parsesOffsetDateTime() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[{"register":40001,"value":1}],
         "time":"2026-06-16T10:30:00+02:00"}"""));

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.recordedAt()).isEqualTo(Instant.parse("2026-06-16T08:30:00Z")));
  }

  @Test
  @DisplayName("time absent → recordedAt null (horodatage serveur appliqué en aval)")
  void missingTimeYieldsNullRecordedAt() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[{"register":40001,"value":1}]}"""));

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.recordedAt()).isNull());
  }

  @Test
  @DisplayName("time non parsable → recordedAt null, mesures conservées")
  void invalidTimeYieldsNullRecordedAt() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[{"register":40001,"value":1}],"time":"not-a-date"}"""));

    assertThat(result.measurements()).hasSize(1);
    assertThat(result.measurements().get(0).recordedAt()).isNull();
  }

  @Test
  @DisplayName("Registre absent ou non entier → lecture ignorée et comptée")
  void dropsReadingsWithInvalidRegister() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[
           {"register":40001,"value":1.0},
           {"metric":"x","value":2.0},
           {"register":"40003","value":3.0},
           {"register":4.5,"value":4.0}]}"""));

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.value()).isEqualTo(1.0));
    assertThat(result.droppedReadings()).isEqualTo(3);
  }

  @Test
  @DisplayName("Valeur non numérique, non finie ou absente → lecture ignorée et comptée")
  void dropsReadingsWithInvalidValue() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[
           {"register":40001,"value":21.5},
           {"register":40002,"value":"NaN"},
           {"register":40003,"value":true},
           {"register":40004},
           {"register":40005,"value":null}]}"""));

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.value()).isEqualTo(21.5));
    assertThat(result.droppedReadings()).isEqualTo(4);
  }

  @Test
  @DisplayName("Entrée non-objet dans readings → ignorée et comptée")
  void dropsNonObjectEntries() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[
           {"register":40001,"value":1},
           42,
           "junk",
           [1,2]]}"""));

    assertThat(result.measurements()).hasSize(1);
    assertThat(result.droppedReadings()).isEqualTo(3);
  }

  @Test
  @DisplayName("Valeurs négatives et entières acceptées comme Double")
  void acceptsNegativeAndIntegerValues() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[
           {"register":40001,"value":-3},
           {"register":40002,"value":12}]}"""));

    assertThat(result.measurements()).hasSize(2)
        .anySatisfy(m -> assertThat(m.value()).isEqualTo(-3.0))
        .anySatisfy(m -> assertThat(m.value()).isEqualTo(12.0));
  }

  @Test
  @DisplayName("readings absent ou vide → aucune mesure, device conservé")
  void noReadingsArray() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1"}"""));

    assertThat(result.deviceCode()).isEqualTo("PLC-1");
    assertThat(result.measurements()).isEmpty();
    assertThat(result.droppedReadings()).isZero();

    var empty = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":[]}"""));
    assertThat(empty.measurements()).isEmpty();
  }

  @Test
  @DisplayName("readings non-tableau → aucune mesure")
  void readingsNotArray() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceCode":"PLC-1","readings":{"register":40001,"value":1}}"""));

    assertThat(result.measurements()).isEmpty();
  }

  @Test
  @DisplayName("deviceCode absent → code null, hasDeviceCode false, mesures décodées quand même")
  void missingDeviceCode() throws Exception {
    var result = decoder.decode(parse("""
        {"readings":[{"register":40001,"value":1}]}"""));

    assertThat(result.deviceCode()).isNull();
    assertThat(result.hasDeviceCode()).isFalse();
    // C'est le handler qui rejette faute de device.
    assertThat(result.measurements()).hasSize(1);
  }

  @Test
  @DisplayName("Payload null ou non-objet → résultat vide, jamais d'exception")
  void nullOrNonObjectPayload() throws Exception {
    var nullResult = decoder.decode(null);
    assertThat(nullResult.deviceCode()).isNull();
    assertThat(nullResult.measurements()).isEmpty();

    var arrayResult = decoder.decode(parse("[1,2,3]"));
    assertThat(arrayResult.deviceCode()).isNull();
    assertThat(arrayResult.measurements()).isEmpty();
  }
}

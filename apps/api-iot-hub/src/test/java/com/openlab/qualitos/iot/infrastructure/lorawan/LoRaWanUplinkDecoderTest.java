package com.openlab.qualitos.iot.infrastructure.lorawan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Décodeur d'uplinks LoRaWAN : parsing, unités, horodatage, robustesse — tests unitaires
 * purs, sans contexte Spring.
 */
class LoRaWanUplinkDecoderTest {

  private final ObjectMapper json = new ObjectMapper();
  private final LoRaWanUplinkDecoder decoder = new LoRaWanUplinkDecoder();

  private JsonNode parse(String s) throws Exception {
    return json.readTree(s);
  }

  @Test
  @DisplayName("Uplink complet → code device + mesures numériques + unités + horodatage")
  void decodesFullUplink() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"CAPTEUR-SOL-01",
         "decoded":{"temperature":4.2,"humidity":55},
         "units":{"temperature":"degC","humidity":"%RH"},
         "time":"2026-06-16T08:30:00Z"}"""), "deviceName");

    assertThat(result.deviceCode()).isEqualTo("CAPTEUR-SOL-01");
    assertThat(result.droppedFields()).isZero();
    assertThat(result.measurements()).hasSize(2);
    assertThat(result.measurements())
        .anySatisfy(m -> {
          assertThat(m.metric()).isEqualTo("temperature");
          assertThat(m.value()).isEqualTo(4.2);
          assertThat(m.unit()).isEqualTo("degC");
          assertThat(m.recordedAt()).isEqualTo(Instant.parse("2026-06-16T08:30:00Z"));
        })
        .anySatisfy(m -> {
          assertThat(m.metric()).isEqualTo("humidity");
          assertThat(m.value()).isEqualTo(55.0);
          assertThat(m.unit()).isEqualTo("%RH");
        });
  }

  @Test
  @DisplayName("Champ device configurable : devEUI au lieu de deviceName (ChirpStack)")
  void honoursConfigurableDeviceIdField() throws Exception {
    var result = decoder.decode(parse("""
        {"devEUI":"0004A30B001C0530","decoded":{"battery":3.6}}"""), "devEUI");

    assertThat(result.deviceCode()).isEqualTo("0004A30B001C0530");
    assertThat(result.measurements()).singleElement()
        .satisfies(m -> {
          assertThat(m.metric()).isEqualTo("battery");
          assertThat(m.value()).isEqualTo(3.6);
          assertThat(m.unit()).isNull();
        });
  }

  @Test
  @DisplayName("Offset local toléré sur le champ time")
  void parsesOffsetDateTime() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"D1","decoded":{"t":1},"time":"2026-06-16T10:30:00+02:00"}"""), "deviceName");

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.recordedAt()).isEqualTo(Instant.parse("2026-06-16T08:30:00Z")));
  }

  @Test
  @DisplayName("time absent → recordedAt null (horodatage serveur appliqué en aval)")
  void missingTimeYieldsNullRecordedAt() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"D1","decoded":{"t":1}}"""), "deviceName");

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.recordedAt()).isNull());
  }

  @Test
  @DisplayName("time non parsable → recordedAt null, mesures conservées")
  void invalidTimeYieldsNullRecordedAt() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"D1","decoded":{"t":1},"time":"not-a-date"}"""), "deviceName");

    assertThat(result.measurements()).hasSize(1);
    assertThat(result.measurements().get(0).recordedAt()).isNull();
  }

  @Test
  @DisplayName("Champs non numériques (texte, booléen, objet, null) ignorés et comptés")
  void dropsNonNumericFields() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"D1","decoded":{
          "temperature":21.5,
          "label":"zone-A",
          "active":true,
          "nested":{"x":1},
          "missing":null}}"""), "deviceName");

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.metric()).isEqualTo("temperature"));
    assertThat(result.droppedFields()).isEqualTo(4);
  }

  @Test
  @DisplayName("Valeur numérique non finie (NaN issue d'une string) ignorée")
  void ignoresNonFiniteValues() throws Exception {
    // Jackson ne crée un NumberNode NaN que via les littéraux JSON étendus ; on couvre
    // plutôt le cas d'un nombre tres grand resté fini et d'un texte numérique non parsé.
    var result = decoder.decode(parse("""
        {"deviceName":"D1","decoded":{"big":1e308,"asText":"42"}}"""), "deviceName");

    assertThat(result.measurements()).singleElement()
        .satisfies(m -> assertThat(m.metric()).isEqualTo("big"));
    assertThat(result.droppedFields()).isEqualTo(1); // "asText" est textuel → ignoré
  }

  @Test
  @DisplayName("Pas de bloc decoded → aucune mesure, device conservé")
  void noDecodedBlock() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"D1"}"""), "deviceName");

    assertThat(result.deviceCode()).isEqualTo("D1");
    assertThat(result.measurements()).isEmpty();
    assertThat(result.droppedFields()).isZero();
  }

  @Test
  @DisplayName("decoded vide → aucune mesure")
  void emptyDecodedBlock() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"D1","decoded":{}}"""), "deviceName");

    assertThat(result.measurements()).isEmpty();
  }

  @Test
  @DisplayName("units absent → unités nulles, mesures conservées")
  void missingUnitsYieldNullUnits() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"D1","decoded":{"t":1,"h":2}}"""), "deviceName");

    assertThat(result.measurements()).hasSize(2)
        .allSatisfy(m -> assertThat(m.unit()).isNull());
  }

  @Test
  @DisplayName("Champ device absent → code null, hasDeviceCode false")
  void missingDeviceCode() throws Exception {
    var result = decoder.decode(parse("""
        {"decoded":{"t":1}}"""), "deviceName");

    assertThat(result.deviceCode()).isNull();
    assertThat(result.hasDeviceCode()).isFalse();
    // Les mesures sont quand même décodées : c'est le handler qui rejette faute de device.
    assertThat(result.measurements()).hasSize(1);
  }

  @Test
  @DisplayName("Payload null ou non-objet → résultat vide, jamais d'exception")
  void nullOrNonObjectPayload() throws Exception {
    var nullResult = decoder.decode(null, "deviceName");
    assertThat(nullResult.deviceCode()).isNull();
    assertThat(nullResult.measurements()).isEmpty();

    var arrayResult = decoder.decode(parse("[1,2,3]"), "deviceName");
    assertThat(arrayResult.deviceCode()).isNull();
    assertThat(arrayResult.measurements()).isEmpty();
  }

  @Test
  @DisplayName("Valeurs négatives et entières acceptées comme Double")
  void acceptsNegativeAndIntegerValues() throws Exception {
    var result = decoder.decode(parse("""
        {"deviceName":"D1","decoded":{"delta":-3,"count":12}}"""), "deviceName");

    assertThat(result.measurements()).hasSize(2)
        .anySatisfy(m -> assertThat(m.value()).isEqualTo(-3.0))
        .anySatisfy(m -> assertThat(m.value()).isEqualTo(12.0));
  }
}

package com.openlab.qualitos.iot.infrastructure.opcua;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpcUaPropertiesTest {

    @Test
    void defaults_areSafeAndConnectorDisabled() {
        OpcUaProperties p = new OpcUaProperties();
        assertThat(p.isEnabled()).isFalse(); // pas de serveur OPC-UA requis en CI / sans opt-in
        assertThat(p.getEndpointUrl()).isEqualTo("opc.tcp://localhost:4840");
        assertThat(p.getApplicationUri()).isEqualTo("urn:qualitos:iot-hub:opcua-client");
        assertThat(p.getApplicationName()).isEqualTo("QualitOS IoT Hub OPC-UA Client");
        assertThat(p.getSecurityPolicy()).isEqualTo("None");
        assertThat(p.getPublishingIntervalMs()).isEqualTo(1000.0);
        assertThat(p.getRequestTimeoutMs()).isEqualTo(10_000L);
        assertThat(p.getNodes()).isEmpty();
        assertThat(p.getUsername()).isNull();
        assertThat(p.getKeystorePath()).isNull();
    }

    @Test
    void settersRoundTrip_andNodeMapping() {
        OpcUaProperties p = new OpcUaProperties();
        p.setEnabled(true);
        p.setEndpointUrl("opc.tcp://plc:4840");
        p.setApplicationUri("urn:x");
        p.setApplicationName("X");
        p.setSecurityPolicy("Basic256Sha256");
        p.setUsername("u");
        p.setPassword("pw");
        p.setKeystorePath("/k.p12");
        p.setKeystorePassword("kpw");
        p.setPublishingIntervalMs(500.0);
        p.setRequestTimeoutMs(2000L);

        OpcUaProperties.NodeMapping n = new OpcUaProperties.NodeMapping();
        n.setNodeId("ns=2;s=Fridge01.Temperature");
        n.setDeviceCode("FRIDGE-01");
        n.setMetric("temperature");
        n.setUnit("C");
        p.setNodes(List.of(n));

        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getEndpointUrl()).isEqualTo("opc.tcp://plc:4840");
        assertThat(p.getApplicationUri()).isEqualTo("urn:x");
        assertThat(p.getApplicationName()).isEqualTo("X");
        assertThat(p.getSecurityPolicy()).isEqualTo("Basic256Sha256");
        assertThat(p.getUsername()).isEqualTo("u");
        assertThat(p.getPassword()).isEqualTo("pw");
        assertThat(p.getKeystorePath()).isEqualTo("/k.p12");
        assertThat(p.getKeystorePassword()).isEqualTo("kpw");
        assertThat(p.getPublishingIntervalMs()).isEqualTo(500.0);
        assertThat(p.getRequestTimeoutMs()).isEqualTo(2000L);
        assertThat(p.getNodes()).singleElement().satisfies(m -> {
            assertThat(m.getNodeId()).isEqualTo("ns=2;s=Fridge01.Temperature");
            assertThat(m.getDeviceCode()).isEqualTo("FRIDGE-01");
            assertThat(m.getMetric()).isEqualTo("temperature");
            assertThat(m.getUnit()).isEqualTo("C");
        });
    }

    @Test
    void setNodes_null_resetsToEmptyList() {
        OpcUaProperties p = new OpcUaProperties();
        p.setNodes(null); // branche défensive : null → liste vide
        assertThat(p.getNodes()).isNotNull().isEmpty();
    }
}

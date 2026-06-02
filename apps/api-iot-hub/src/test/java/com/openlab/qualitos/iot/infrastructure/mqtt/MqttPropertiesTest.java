package com.openlab.qualitos.iot.infrastructure.mqtt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttPropertiesTest {

    @Test
    void defaults_areSafeAndConnectorDisabled() {
        MqttProperties p = new MqttProperties();
        assertThat(p.isEnabled()).isFalse(); // pas de broker requis en CI / sans opt-in
        assertThat(p.getBrokerUrl()).isEqualTo("tcp://localhost:1883");
        assertThat(p.getClientId()).isEqualTo("qualitos-iot-hub");
        assertThat(p.getTopicFilter()).isEqualTo("qualitos/+/+");
        assertThat(p.getQos()).isEqualTo(1);
        assertThat(p.isTlsEnabled()).isFalse();
        assertThat(p.getConnectionTimeoutSeconds()).isEqualTo(10);
        assertThat(p.getKeepAliveIntervalSeconds()).isEqualTo(30);
        assertThat(p.isAutomaticReconnect()).isTrue();
        assertThat(p.getUsername()).isNull();
        assertThat(p.getPassword()).isNull();
    }

    @Test
    void settersRoundTrip() {
        MqttProperties p = new MqttProperties();
        p.setEnabled(true);
        p.setBrokerUrl("ssl://emqx:8883");
        p.setClientId("c1");
        p.setUsername("u");
        p.setPassword("pw");
        p.setTopicFilter("qualitos/FRIDGE-01/#");
        p.setQos(2);
        p.setTlsEnabled(true);
        p.setConnectionTimeoutSeconds(5);
        p.setKeepAliveIntervalSeconds(15);
        p.setAutomaticReconnect(false);

        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getBrokerUrl()).isEqualTo("ssl://emqx:8883");
        assertThat(p.getClientId()).isEqualTo("c1");
        assertThat(p.getUsername()).isEqualTo("u");
        assertThat(p.getPassword()).isEqualTo("pw");
        assertThat(p.getTopicFilter()).isEqualTo("qualitos/FRIDGE-01/#");
        assertThat(p.getQos()).isEqualTo(2);
        assertThat(p.isTlsEnabled()).isTrue();
        assertThat(p.getConnectionTimeoutSeconds()).isEqualTo(5);
        assertThat(p.getKeepAliveIntervalSeconds()).isEqualTo(15);
        assertThat(p.isAutomaticReconnect()).isFalse();
    }
}

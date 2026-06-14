package com.homesmartfactory.collector.mqtt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttConfigTest {

    @Test
    @DisplayName("全フィールドを指定して構築したとき、getter が正しい値を返すこと")
    void constructor_allFields_returnsCorrectValues() {
        MqttConfig config = new MqttConfig(
                "endpoint.iot.amazonaws.com",
                8883,
                "client-001",
                "/etc/ssl/iot/root-CA.crt",
                "/etc/ssl/iot/client.crt",
                "/etc/ssl/iot/private.key");

        assertThat(config.getEndpoint()).isEqualTo("endpoint.iot.amazonaws.com");
        assertThat(config.getPort()).isEqualTo(8883);
        assertThat(config.getClientId()).isEqualTo("client-001");
        assertThat(config.getCaCertPath()).isEqualTo("/etc/ssl/iot/root-CA.crt");
        assertThat(config.getClientCertPath()).isEqualTo("/etc/ssl/iot/client.crt");
        assertThat(config.getPrivateKeyPath()).isEqualTo("/etc/ssl/iot/private.key");
    }
}

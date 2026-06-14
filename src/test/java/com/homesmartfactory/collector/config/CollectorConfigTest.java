package com.homesmartfactory.collector.config;

import com.homesmartfactory.collector.mqtt.MqttConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorConfigTest {

    @Test
    @DisplayName("全フィールドを指定して構築したとき、getter が正しい値を返すこと")
    void constructor_allFields_returnsCorrectValues() {
        MqttConfig mqttConfig = new MqttConfig(
                "endpoint.iot.amazonaws.com", 8883, "client-001",
                "/ca.crt", "/client.crt", "/private.key");
        CollectorConfig config = new CollectorConfig(mqttConfig, "room01", 60L);

        assertThat(config.getMqttConfig()).isSameAs(mqttConfig);
        assertThat(config.getDeviceId()).isEqualTo("room01");
        assertThat(config.getIntervalSeconds()).isEqualTo(60L);
    }
}

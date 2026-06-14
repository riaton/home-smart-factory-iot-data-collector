package com.homesmartfactory.collector.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectorConfigLoaderTest {

    private final CollectorConfigLoader loader = new CollectorConfigLoader();

    private String testPropertiesPath() {
        return CollectorConfigLoaderTest.class.getClassLoader()
                .getResource("collector-test.properties").getPath();
    }

    @Test
    @DisplayName("正常なプロパティファイルを読み込んだとき、各フィールドが正しく設定された CollectorConfig が返ること")
    void load_validFile_returnsCorrectConfig() {
        CollectorConfig config = loader.load(testPropertiesPath());

        assertThat(config.getMqttConfig().getEndpoint())
                .isEqualTo("endpoint.iot.ap-northeast-1.amazonaws.com");
        assertThat(config.getMqttConfig().getPort()).isEqualTo(8883);
        assertThat(config.getMqttConfig().getClientId()).isEqualTo("raspberry-pi-room01");
        assertThat(config.getMqttConfig().getCaCertPath()).isEqualTo("/etc/ssl/iot/root-CA.crt");
        assertThat(config.getMqttConfig().getClientCertPath()).isEqualTo("/etc/ssl/iot/client.crt");
        assertThat(config.getMqttConfig().getPrivateKeyPath()).isEqualTo("/etc/ssl/iot/private.key");
        assertThat(config.getDeviceId()).isEqualTo("room01");
        assertThat(config.getIntervalSeconds()).isEqualTo(60L);
    }

    @Test
    @DisplayName("存在しないファイルパスを渡したとき、ConfigLoadException がスローされること")
    void load_fileNotFound_throwsConfigLoadException() {
        assertThatThrownBy(() -> loader.load("/no/such/file.properties"))
                .isInstanceOf(ConfigLoadException.class);
    }

    @Test
    @DisplayName("mqtt.endpoint が欠落しているとき、ConfigLoadException がスローされること")
    void load_missingMqttEndpoint_throwsConfigLoadException() {
        String path = CollectorConfigLoaderTest.class.getClassLoader()
                .getResource("collector-missing-endpoint.properties").getPath();

        assertThatThrownBy(() -> loader.load(path))
                .isInstanceOf(ConfigLoadException.class)
                .hasMessageContaining("mqtt.endpoint");
    }

    @Test
    @DisplayName("mqtt.port が数値でないとき、ConfigLoadException がスローされること")
    void load_invalidPort_throwsConfigLoadException() {
        String path = CollectorConfigLoaderTest.class.getClassLoader()
                .getResource("collector-invalid-port.properties").getPath();

        assertThatThrownBy(() -> loader.load(path))
                .isInstanceOf(ConfigLoadException.class)
                .hasMessageContaining("mqtt.port");
    }
}

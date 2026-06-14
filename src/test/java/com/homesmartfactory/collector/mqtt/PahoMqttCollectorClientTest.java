package com.homesmartfactory.collector.mqtt;

import com.homesmartfactory.collector.util.ExponentialBackoff;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PahoMqttCollectorClientTest {

    @Mock
    private IMqttClient pahoClient;

    private MqttConfig config;

    private PahoMqttCollectorClient client;

    @BeforeEach
    void setup() {
        config = new MqttConfig(
                "endpoint.iot.amazonaws.com", 8883, "client-001",
                "/ca.crt", "/client.crt", "/private.key");
        ExponentialBackoff connectionBackoff = new ExponentialBackoff(1, 2.0, 2);
        ExponentialBackoff publishBackoff = new ExponentialBackoff(1, 2.0, 2);
        client = new PahoMqttCollectorClient(pahoClient, config, connectionBackoff, publishBackoff);
    }

    // --- connect() テスト ---

    @Test
    @DisplayName("接続成功のとき、例外なく終了すること")
    void connect_success_noException() throws Exception {
        willDoNothing().given(pahoClient).connect(any(MqttConnectOptions.class));

        client.connect();

        then(pahoClient).should().connect(any(MqttConnectOptions.class));
    }

    @Test
    @DisplayName("認証エラー(REASON_CODE_NOT_AUTHORIZED)のとき、MqttAuthException をスローすること")
    void connect_authError_throwsMqttAuthException() throws Exception {
        willThrow(new MqttException(MqttException.REASON_CODE_NOT_AUTHORIZED))
                .given(pahoClient).connect(any(MqttConnectOptions.class));

        assertThatThrownBy(() -> client.connect())
                .isInstanceOf(MqttAuthException.class);
    }

    @Test
    @DisplayName("1回ネットワーク断後に成功するとき、2回接続を試みること")
    void connect_networkErrorThenSuccess_retriesOnce() throws Exception {
        willThrow(new MqttException(MqttException.REASON_CODE_CONNECTION_LOST))
                .willDoNothing()
                .given(pahoClient).connect(any(MqttConnectOptions.class));

        client.connect();

        then(pahoClient).should(times(2)).connect(any(MqttConnectOptions.class));
    }

    // --- publish() テスト ---

    @Test
    @DisplayName("Publish 成功のとき、例外なく終了すること")
    void publish_success_noException() throws Exception {
        String topic = "home/devices/room01/data";
        willDoNothing().given(pahoClient).publish(eq(topic), any(MqttMessage.class));

        client.publish(topic, "{\"device_id\":\"room01\"}");

        then(pahoClient).should().publish(eq(topic), any(MqttMessage.class));
    }

    @Test
    @DisplayName("1回失敗後に成功するとき、2回 publish を試みること")
    void publish_onceFailThenSuccess_retriesOnce() throws Exception {
        String topic = "home/devices/room01/data";
        willThrow(new MqttException(MqttException.REASON_CODE_CONNECTION_LOST))
                .willDoNothing()
                .given(pahoClient).publish(eq(topic), any(MqttMessage.class));

        client.publish(topic, "{\"device_id\":\"room01\"}");

        then(pahoClient).should(times(2)).publish(eq(topic), any(MqttMessage.class));
    }

    @Test
    @DisplayName("5回連続失敗のとき、MqttPublishException をスローすること")
    void publish_fiveConsecutiveFailures_throwsMqttPublishException() throws Exception {
        String topic = "home/devices/room01/data";
        willThrow(new MqttException(MqttException.REASON_CODE_CONNECTION_LOST))
                .given(pahoClient).publish(eq(topic), any(MqttMessage.class));

        assertThatThrownBy(() -> client.publish(topic, "{\"device_id\":\"room01\"}"))
                .isInstanceOf(MqttPublishException.class);

        then(pahoClient).should(times(5)).publish(eq(topic), any(MqttMessage.class));
    }

    @Test
    @DisplayName("Publish 時に QoS=1 で送信されること")
    void publish_setsQos1() throws Exception {
        String topic = "home/devices/room01/data";
        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        willDoNothing().given(pahoClient).publish(eq(topic), captor.capture());

        client.publish(topic, "{\"device_id\":\"room01\"}");

        assertThat(captor.getValue().getQos()).isEqualTo(1);
    }
}

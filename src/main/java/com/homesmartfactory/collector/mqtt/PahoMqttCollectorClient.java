package com.homesmartfactory.collector.mqtt;

import com.homesmartfactory.collector.util.ExponentialBackoff;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class PahoMqttCollectorClient implements MqttCollectorClient {

    private static final int MAX_PUBLISH_ATTEMPTS = 5;

    private final IMqttClient pahoClient;

    private final MqttConfig config;

    private final ExponentialBackoff connectionBackoff;

    private final ExponentialBackoff publishBackoff;

    public PahoMqttCollectorClient(IMqttClient pahoClient, MqttConfig config,
            ExponentialBackoff connectionBackoff, ExponentialBackoff publishBackoff) {
        this.pahoClient = pahoClient;
        this.config = config;
        this.connectionBackoff = connectionBackoff;
        this.publishBackoff = publishBackoff;
    }

    @Override
    public void connect() throws MqttAuthException {
        while (true) {
            try {
                pahoClient.connect(buildConnectOptions());
                connectionBackoff.reset();
                return;
            } catch (MqttException e) {
                if (e.getReasonCode() == MqttException.REASON_CODE_NOT_AUTHORIZED) {
                    throw new MqttAuthException("MQTT 認証エラー: 証明書を確認してください", e);
                }
                long delay = connectionBackoff.next();
                sleep(delay);
            }
        }
    }

    @Override
    public void publish(String topic, String payload) throws MqttPublishException {
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        message.setRetained(false);

        for (int attempt = 0; attempt < MAX_PUBLISH_ATTEMPTS; attempt++) {
            try {
                pahoClient.publish(topic, message);
                publishBackoff.reset();
                return;
            } catch (MqttException e) {
                if (attempt < MAX_PUBLISH_ATTEMPTS - 1) {
                    long delay = publishBackoff.next();
                    sleep(delay);
                }
            }
        }
        throw new MqttPublishException("Publish に " + MAX_PUBLISH_ATTEMPTS + " 回失敗しました: topic=" + topic);
    }

    @Override
    public void disconnect() {
        try {
            pahoClient.disconnect();
        } catch (MqttException e) {
            // 切断失敗は無視して終了
        }
    }

    private MqttConnectOptions buildConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        return options;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

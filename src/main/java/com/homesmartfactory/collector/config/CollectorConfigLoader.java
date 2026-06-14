package com.homesmartfactory.collector.config;

import com.homesmartfactory.collector.mqtt.MqttConfig;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class CollectorConfigLoader {

    public CollectorConfig load(String filePath) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            props.load(fis);
        } catch (FileNotFoundException e) {
            throw new ConfigLoadException("設定ファイルが見つかりません: " + filePath, e);
        } catch (IOException e) {
            throw new ConfigLoadException("設定ファイルの読み込みに失敗しました: " + filePath, e);
        }

        String endpoint = requireString(props, "mqtt.endpoint");
        int port = requireInt(props, "mqtt.port");
        String clientId = requireString(props, "mqtt.clientId");
        String caCertPath = requireString(props, "mqtt.caCertPath");
        String clientCertPath = requireString(props, "mqtt.clientCertPath");
        String privateKeyPath = requireString(props, "mqtt.privateKeyPath");
        String deviceId = requireString(props, "collector.deviceId");
        long intervalSeconds = requireLong(props, "collector.intervalSeconds");

        MqttConfig mqttConfig = new MqttConfig(endpoint, port, clientId,
                caCertPath, clientCertPath, privateKeyPath);
        return new CollectorConfig(mqttConfig, deviceId, intervalSeconds);
    }

    private String requireString(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new ConfigLoadException("必須プロパティが欠落しています: " + key);
        }
        return value;
    }

    private int requireInt(Properties props, String key) {
        String value = requireString(props, key);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new ConfigLoadException("プロパティは正の整数である必要があります: " + key);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ConfigLoadException("プロパティが整数ではありません: " + key, e);
        }
    }

    private long requireLong(Properties props, String key) {
        String value = requireString(props, key);
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new ConfigLoadException("プロパティは正の整数である必要があります: " + key);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ConfigLoadException("プロパティが整数ではありません: " + key, e);
        }
    }
}

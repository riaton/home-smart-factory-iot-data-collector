package com.homesmartfactory.collector.config;

import com.homesmartfactory.collector.mqtt.MqttConfig;

public class CollectorConfig {

    private final MqttConfig mqttConfig;

    private final String deviceId;

    private final long intervalSeconds;

    public CollectorConfig(MqttConfig mqttConfig, String deviceId, long intervalSeconds) {
        this.mqttConfig = mqttConfig;
        this.deviceId = deviceId;
        this.intervalSeconds = intervalSeconds;
    }

    public MqttConfig getMqttConfig() {
        return mqttConfig;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }
}

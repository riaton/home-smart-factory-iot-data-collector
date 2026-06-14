package com.homesmartfactory.collector.mqtt;

public interface MqttCollectorClient {

    void connect() throws MqttAuthException;

    void publish(String topic, String payload) throws MqttPublishException;

    void disconnect();
}

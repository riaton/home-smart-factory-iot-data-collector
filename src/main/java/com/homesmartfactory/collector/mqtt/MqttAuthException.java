package com.homesmartfactory.collector.mqtt;

public class MqttAuthException extends RuntimeException {

    public MqttAuthException(String message) {
        super(message);
    }

    public MqttAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

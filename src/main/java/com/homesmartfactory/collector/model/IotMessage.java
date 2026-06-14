package com.homesmartfactory.collector.model;

public class IotMessage {

    private final String deviceId;

    private final Double temperature;

    private final Double humidity;

    private final Integer motion;

    private final Double powerW;

    private final String recordedAt;

    public IotMessage(String deviceId, Double temperature, Double humidity,
            Integer motion, Double powerW, String recordedAt) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.motion = motion;
        this.powerW = powerW;
        this.recordedAt = recordedAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public Integer getMotion() {
        return motion;
    }

    public Double getPowerW() {
        return powerW;
    }

    public String getRecordedAt() {
        return recordedAt;
    }
}

package com.homesmartfactory.collector.sensor;

public class SensorData {

    private final Double temperature;

    private final Double humidity;

    private final Integer motion;

    private final Double powerW;

    public SensorData(Double temperature, Double humidity, Integer motion, Double powerW) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.motion = motion;
        this.powerW = powerW;
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
}

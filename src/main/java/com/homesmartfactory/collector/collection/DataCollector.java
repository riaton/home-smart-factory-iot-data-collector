package com.homesmartfactory.collector.collection;

import com.homesmartfactory.collector.model.IotMessage;
import com.homesmartfactory.collector.model.IotMessageSerializer;
import com.homesmartfactory.collector.mqtt.MqttCollectorClient;
import com.homesmartfactory.collector.mqtt.MqttPublishException;
import com.homesmartfactory.collector.sensor.SensorData;
import com.homesmartfactory.collector.sensor.SensorReader;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class DataCollector {

    private static final Logger LOGGER = Logger.getLogger(DataCollector.class.getName());

    private final String deviceId;

    private final SensorReader sensorReader;

    private final IotMessageSerializer serializer;

    private final MqttCollectorClient mqttClient;

    private final Clock clock;

    public DataCollector(String deviceId, SensorReader sensorReader,
            IotMessageSerializer serializer, MqttCollectorClient mqttClient, Clock clock) {
        this.deviceId = deviceId;
        this.sensorReader = sensorReader;
        this.serializer = serializer;
        this.mqttClient = mqttClient;
        this.clock = clock;
    }

    public void collect() {
        SensorData data;
        try {
            data = sensorReader.read();
        } catch (Exception e) {
            LOGGER.severe("センサー読み取り失敗: " + e.getMessage());
            return;
        }

        String recordedAt = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        IotMessage message = new IotMessage(
                deviceId,
                data.getTemperature(),
                data.getHumidity(),
                data.getMotion(),
                data.getPowerW(),
                recordedAt);
        String json = serializer.serialize(message);
        String topic = "home/devices/" + deviceId + "/data";

        try {
            mqttClient.publish(topic, json);
        } catch (MqttPublishException e) {
            LOGGER.severe("Publish 失敗: " + e.getMessage());
        }
    }
}

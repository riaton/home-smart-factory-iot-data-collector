package com.homesmartfactory.collector;

import com.homesmartfactory.collector.collection.CollectionScheduler;
import com.homesmartfactory.collector.mqtt.MqttAuthException;
import com.homesmartfactory.collector.mqtt.MqttCollectorClient;

public class CollectorApplication {

    private final MqttCollectorClient mqttClient;

    private final CollectionScheduler scheduler;

    public CollectorApplication(MqttCollectorClient mqttClient, CollectionScheduler scheduler) {
        this.mqttClient = mqttClient;
        this.scheduler = scheduler;
    }

    public void start() throws MqttAuthException {
        mqttClient.connect();
        scheduler.start();
    }

    public void stop() {
        scheduler.stop();
        mqttClient.disconnect();
    }
}

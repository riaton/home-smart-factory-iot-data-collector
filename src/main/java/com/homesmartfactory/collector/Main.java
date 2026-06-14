package com.homesmartfactory.collector;

import com.homesmartfactory.collector.collection.CollectionScheduler;
import com.homesmartfactory.collector.collection.DataCollector;
import com.homesmartfactory.collector.config.CollectorConfig;
import com.homesmartfactory.collector.config.CollectorConfigLoader;
import com.homesmartfactory.collector.config.ConfigLoadException;
import com.homesmartfactory.collector.model.IotMessageSerializer;
import com.homesmartfactory.collector.mqtt.MqttAuthException;
import com.homesmartfactory.collector.mqtt.MqttConfig;
import com.homesmartfactory.collector.mqtt.PahoMqttCollectorClient;
import com.homesmartfactory.collector.sensor.SensorData;
import com.homesmartfactory.collector.util.ExponentialBackoff;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "collector.properties";

        CollectorConfig config;
        try {
            config = new CollectorConfigLoader().load(configPath);
        } catch (ConfigLoadException e) {
            LOGGER.severe("設定ファイルの読み込みに失敗しました: " + e.getMessage());
            System.exit(1);
            return;
        }

        MqttConfig mqttConfig = config.getMqttConfig();
        MqttClient pahoClient;
        try {
            pahoClient = new MqttClient(
                    "ssl://" + mqttConfig.getEndpoint() + ":" + mqttConfig.getPort(),
                    mqttConfig.getClientId()
            );
        } catch (MqttException e) {
            LOGGER.severe("MQTT クライアントの初期化に失敗しました: " + e.getMessage());
            System.exit(1);
            return;
        }

        ExponentialBackoff connBackoff = new ExponentialBackoff(1_000, 2.0, 60_000);
        ExponentialBackoff pubBackoff = new ExponentialBackoff(1_000, 2.0, 16_000);
        PahoMqttCollectorClient mqttClient =
                new PahoMqttCollectorClient(pahoClient, mqttConfig, connBackoff, pubBackoff);

        IotMessageSerializer serializer = new IotMessageSerializer();
        // TODO: 実センサードライバに置き換える（Raspberry Pi 実装時）
        Clock clock = Clock.systemDefaultZone();
        DataCollector collector = new DataCollector(
                config.getDeviceId(),
                () -> new SensorData(null, null, null, null),
                serializer,
                mqttClient,
                clock
        );

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        CollectionScheduler scheduler =
                new CollectionScheduler(collector, executor, config.getIntervalSeconds());

        CollectorApplication app = new CollectorApplication(mqttClient, scheduler);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        try {
            app.start();
        } catch (MqttAuthException e) {
            LOGGER.severe("MQTT 認証エラー: " + e.getMessage());
            System.exit(1);
        }
    }
}

package com.homesmartfactory.collector.collection;

import com.homesmartfactory.collector.model.IotMessageSerializer;
import com.homesmartfactory.collector.mqtt.MqttCollectorClient;
import com.homesmartfactory.collector.mqtt.MqttPublishException;
import com.homesmartfactory.collector.sensor.SensorData;
import com.homesmartfactory.collector.sensor.SensorReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DataCollectorTest {

    @Mock
    private SensorReader sensorReader;

    @Mock
    private IotMessageSerializer serializer;

    @Mock
    private MqttCollectorClient mqttClient;

    private DataCollector collector;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-01-15T01:00:00Z"),
                ZoneId.of("Asia/Tokyo"));
        collector = new DataCollector("room01", sensorReader, serializer, mqttClient, clock);
    }

    @Test
    @DisplayName("正常系: publish が1回呼ばれること")
    void collect_success_publishesOnce() throws Exception {
        given(sensorReader.read()).willReturn(new SensorData(25.3, 60.1, 1, 120.5));
        given(serializer.serialize(any())).willReturn("{\"device_id\":\"room01\"}");

        collector.collect();

        then(mqttClient).should().publish(eq("home/devices/room01/data"), any(String.class));
    }

    @Test
    @DisplayName("recorded_at が Clock で固定した時刻（+09:00）でシリアライズされること")
    void collect_usesFixedClock_recordedAtIsJst() throws Exception {
        given(sensorReader.read()).willReturn(new SensorData(null, null, null, null));
        given(serializer.serialize(any())).willReturn("{}");

        collector.collect();

        then(serializer).should().serialize(
                argThat(msg -> "2026-01-15T10:00:00+09:00".equals(msg.getRecordedAt())));
    }

    @Test
    @DisplayName("センサー読み取り失敗のとき、publish は呼ばれないこと")
    void collect_sensorReadFails_neverPublishes() throws Exception {
        given(sensorReader.read()).willThrow(new RuntimeException("sensor error"));

        collector.collect();

        then(mqttClient).should(never()).publish(any(), any());
    }

    @Test
    @DisplayName("Publish 失敗のとき、collect() は例外をスローしないこと")
    void collect_publishFails_noExceptionPropagated() throws Exception {
        given(sensorReader.read()).willReturn(new SensorData(null, null, null, null));
        given(serializer.serialize(any())).willReturn("{}");
        willThrow(new MqttPublishException("publish failed"))
                .given(mqttClient).publish(any(), any());

        collector.collect();
        // 例外がスローされなければ OK
    }
}

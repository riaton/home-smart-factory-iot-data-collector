package com.homesmartfactory.collector.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IotMessageSerializerTest {

    private IotMessageSerializer serializer;

    @BeforeEach
    void setup() {
        serializer = new IotMessageSerializer();
    }

    @Test
    @DisplayName("全フィールドに値があるとき、設計書スキーマと一致する JSON を返すこと")
    void serialize_allFields_returnsExpectedJson() {
        IotMessage message = new IotMessage("room01", 25.3, 60.1, 1, 120.5, "2026-01-15T10:00:00+09:00");

        String json = serializer.serialize(message);

        assertThat(json).isEqualTo(
                "{\"device_id\":\"room01\",\"temperature\":25.3,\"humidity\":60.1,"
                + "\"motion\":1,\"power_w\":120.5,\"recorded_at\":\"2026-01-15T10:00:00+09:00\"}");
    }

    @Test
    @DisplayName("センサー未搭載フィールドが null のとき、JSON に null が出力されること")
    void serialize_nullableFields_outputsNullValues() {
        IotMessage message = new IotMessage("room01", null, null, null, null, "2026-01-15T10:00:00+09:00");

        String json = serializer.serialize(message);

        assertThat(json).isEqualTo(
                "{\"device_id\":\"room01\",\"temperature\":null,\"humidity\":null,"
                + "\"motion\":null,\"power_w\":null,\"recorded_at\":\"2026-01-15T10:00:00+09:00\"}");
    }
}

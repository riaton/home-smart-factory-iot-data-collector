package com.homesmartfactory.collector.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IotMessageTest {

    @Test
    @DisplayName("全フィールドを指定して構築したとき、getter が正しい値を返すこと")
    void constructor_allFields_returnsCorrectValues() {
        IotMessage message = new IotMessage("room01", 25.3, 60.1, 1, 120.5, "2026-01-15T10:00:00+09:00");

        assertThat(message.getDeviceId()).isEqualTo("room01");
        assertThat(message.getTemperature()).isEqualTo(25.3);
        assertThat(message.getHumidity()).isEqualTo(60.1);
        assertThat(message.getMotion()).isEqualTo(1);
        assertThat(message.getPowerW()).isEqualTo(120.5);
        assertThat(message.getRecordedAt()).isEqualTo("2026-01-15T10:00:00+09:00");
    }

    @Test
    @DisplayName("センサー未搭載フィールドが null のとき、構築できること")
    void constructor_nullableFields_allowsNull() {
        IotMessage message = new IotMessage("room01", null, null, null, null, "2026-01-15T10:00:00+09:00");

        assertThat(message.getDeviceId()).isEqualTo("room01");
        assertThat(message.getTemperature()).isNull();
        assertThat(message.getHumidity()).isNull();
        assertThat(message.getMotion()).isNull();
        assertThat(message.getPowerW()).isNull();
    }
}

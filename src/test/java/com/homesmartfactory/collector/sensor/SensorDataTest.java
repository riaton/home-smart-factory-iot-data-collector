package com.homesmartfactory.collector.sensor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensorDataTest {

    @Test
    @DisplayName("全フィールドを指定して構築したとき、getter が正しい値を返すこと")
    void constructor_allFields_returnsCorrectValues() {
        SensorData data = new SensorData(25.3, 60.1, 1, 120.5);

        assertThat(data.getTemperature()).isEqualTo(25.3);
        assertThat(data.getHumidity()).isEqualTo(60.1);
        assertThat(data.getMotion()).isEqualTo(1);
        assertThat(data.getPowerW()).isEqualTo(120.5);
    }

    @Test
    @DisplayName("全フィールドが null でも構築できること（センサー未搭載デバイス）")
    void constructor_allNullFields_constructsSuccessfully() {
        SensorData data = new SensorData(null, null, null, null);

        assertThat(data.getTemperature()).isNull();
        assertThat(data.getHumidity()).isNull();
        assertThat(data.getMotion()).isNull();
        assertThat(data.getPowerW()).isNull();
    }
}

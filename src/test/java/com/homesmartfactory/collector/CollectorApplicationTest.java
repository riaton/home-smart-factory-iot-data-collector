package com.homesmartfactory.collector;

import com.homesmartfactory.collector.collection.CollectionScheduler;
import com.homesmartfactory.collector.mqtt.MqttAuthException;
import com.homesmartfactory.collector.mqtt.MqttCollectorClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class CollectorApplicationTest {

    @Mock
    private MqttCollectorClient mqttClient;

    @Mock
    private CollectionScheduler scheduler;

    @Test
    @DisplayName("start() を呼ぶと mqttClient.connect() と scheduler.start() が1回ずつ呼ばれること")
    void start_callsConnectAndSchedulerStart() throws Exception {
        CollectorApplication app = new CollectorApplication(mqttClient, scheduler);

        app.start();

        then(mqttClient).should().connect();
        then(scheduler).should().start();
    }

    @Test
    @DisplayName("stop() を呼ぶと scheduler.stop() → mqttClient.disconnect() の順で呼ばれること")
    void stop_callsSchedulerStopThenDisconnect() {
        CollectorApplication app = new CollectorApplication(mqttClient, scheduler);

        app.stop();

        var order = inOrder(scheduler, mqttClient);
        order.verify(scheduler).stop();
        order.verify(mqttClient).disconnect();
    }

    @Test
    @DisplayName("connect() が MqttAuthException をスローしたとき、例外が伝播して scheduler.start() は呼ばれないこと")
    void start_authError_propagatesExceptionWithoutStartingScheduler() throws Exception {
        willThrow(new MqttAuthException("認証エラー", null))
                .given(mqttClient).connect();
        CollectorApplication app = new CollectorApplication(mqttClient, scheduler);

        assertThatThrownBy(app::start)
                .isInstanceOf(MqttAuthException.class);

        then(scheduler).shouldHaveNoInteractions();
    }
}

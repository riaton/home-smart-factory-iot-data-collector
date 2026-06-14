package com.homesmartfactory.collector.collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CollectionSchedulerTest {

    @Mock
    private DataCollector collector;

    @Mock
    private ScheduledExecutorService scheduler;

    private CollectionScheduler collectionScheduler;

    @BeforeEach
    void setup() {
        collectionScheduler = new CollectionScheduler(collector, scheduler, 60);
    }

    @Test
    @DisplayName("start() を呼ぶと scheduleAtFixedRate が 60秒間隔で登録されること")
    void start_registersScheduledTask() {
        collectionScheduler.start();

        then(scheduler).should().scheduleAtFixedRate(
                any(Runnable.class), eq(0L), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("stop() を呼ぶと scheduler.shutdown() が呼ばれること")
    void stop_shutsDownScheduler() {
        collectionScheduler.stop();

        then(scheduler).should().shutdown();
    }
}

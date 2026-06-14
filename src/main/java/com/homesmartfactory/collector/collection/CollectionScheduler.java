package com.homesmartfactory.collector.collection;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CollectionScheduler {

    private static final Logger LOGGER = Logger.getLogger(CollectionScheduler.class.getName());

    private final DataCollector collector;

    private final ScheduledExecutorService scheduler;

    private final long intervalSeconds;

    public CollectionScheduler(DataCollector collector, ScheduledExecutorService scheduler,
            long intervalSeconds) {
        this.collector = collector;
        this.scheduler = scheduler;
        this.intervalSeconds = intervalSeconds;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                collector.collect();
            } catch (Exception e) {
                LOGGER.severe("収集ループで予期しないエラー: " + e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}

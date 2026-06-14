package com.homesmartfactory.collector.util;

public class ExponentialBackoff {

    private final long initialDelayMs;

    private final double multiplier;

    private final long maxDelayMs;

    private long currentDelayMs;

    public ExponentialBackoff(long initialDelayMs, double multiplier, long maxDelayMs) {
        this.initialDelayMs = initialDelayMs;
        this.multiplier = multiplier;
        this.maxDelayMs = maxDelayMs;
        this.currentDelayMs = initialDelayMs;
    }

    public long next() {
        long delay = currentDelayMs;
        currentDelayMs = Math.min((long) (currentDelayMs * multiplier), maxDelayMs);
        return delay;
    }

    public void reset() {
        currentDelayMs = initialDelayMs;
    }
}

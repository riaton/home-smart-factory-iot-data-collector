package com.homesmartfactory.collector.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExponentialBackoffTest {

    @Test
    @DisplayName("next() を5回呼ぶと 1000, 2000, 4000, 8000, 16000 ms を返すこと")
    void next_fiveTimes_returnsExponentialSequence() {
        ExponentialBackoff backoff = new ExponentialBackoff(1_000, 2.0, 60_000);

        assertThat(backoff.next()).isEqualTo(1_000);
        assertThat(backoff.next()).isEqualTo(2_000);
        assertThat(backoff.next()).isEqualTo(4_000);
        assertThat(backoff.next()).isEqualTo(8_000);
        assertThat(backoff.next()).isEqualTo(16_000);
    }

    @Test
    @DisplayName("maxDelayMs を超えないこと")
    void next_exceedsMax_returnsMaxDelay() {
        ExponentialBackoff backoff = new ExponentialBackoff(1_000, 2.0, 60_000);

        long last = 0;
        for (int i = 0; i < 20; i++) {
            last = backoff.next();
        }

        assertThat(last).isEqualTo(60_000);
    }

    @Test
    @DisplayName("reset() 後に next() を呼ぶと初期値を返すこと")
    void reset_afterMultipleCalls_returnsInitialDelay() {
        ExponentialBackoff backoff = new ExponentialBackoff(1_000, 2.0, 60_000);
        backoff.next();
        backoff.next();
        backoff.next();

        backoff.reset();

        assertThat(backoff.next()).isEqualTo(1_000);
    }
}

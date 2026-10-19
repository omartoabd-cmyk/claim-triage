package com.example.insuranceclaims.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.example.insuranceclaims.exception.RateLimitExceededException;

class AnalysisRateLimiterTest {

    static class MutableClock extends Clock {
        long now = 1_000_000L;

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(now); }
        @Override public long millis() { return now; }
    }

    @Test
    void blocksClientOverPerMinuteLimitThenAllowsAfterWindow() {
        MutableClock clock = new MutableClock();
        AnalysisRateLimiter limiter = new AnalysisRateLimiter(2, 100, clock);

        limiter.acquire("a");
        limiter.acquire("a");
        assertThatThrownBy(() -> limiter.acquire("a"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(e -> assertThat(((RateLimitExceededException) e).getRetryAfterSeconds()).isBetween(1L, 60L));

        clock.now += 61_000;
        assertThatCode(() -> limiter.acquire("a")).doesNotThrowAnyException();
    }

    @Test
    void clientsAreLimitedIndependently() {
        AnalysisRateLimiter limiter = new AnalysisRateLimiter(1, 100, new MutableClock());
        limiter.acquire("a");
        assertThatCode(() -> limiter.acquire("b")).doesNotThrowAnyException();
    }

    @Test
    void globalHourlyCapAppliesAcrossClients() {
        AnalysisRateLimiter limiter = new AnalysisRateLimiter(10, 2, new MutableClock());
        limiter.acquire("a");
        limiter.acquire("b");
        assertThatThrownBy(() -> limiter.acquire("c")).isInstanceOf(RateLimitExceededException.class);
    }
}

package com.example.insuranceclaims.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.insuranceclaims.exception.RateLimitExceededException;

/**
 * Sliding-window limiter protecting the paid OpenAI call: a per-client cap per
 * minute plus a global cap per hour. In-memory, so it is per instance; use a
 * shared store (e.g. Redis) if you run several instances.
 */
@Component
public class AnalysisRateLimiter {

    private static final long MINUTE_MS = 60_000L;
    private static final long HOUR_MS = 3_600_000L;

    private final int perClientPerMinute;
    private final int globalPerHour;
    private final Clock clock;

    private final Map<String, Deque<Long>> perClient = new ConcurrentHashMap<>();
    private final Deque<Long> global = new ArrayDeque<>();

    @Autowired
    public AnalysisRateLimiter(
            @Value("${app.rate-limit.per-client-per-minute:5}") int perClientPerMinute,
            @Value("${app.rate-limit.global-per-hour:100}") int globalPerHour) {
        this(perClientPerMinute, globalPerHour, Clock.systemUTC());
    }

    AnalysisRateLimiter(int perClientPerMinute, int globalPerHour, Clock clock) {
        this.perClientPerMinute = perClientPerMinute;
        this.globalPerHour = globalPerHour;
        this.clock = clock;
    }

    /** Records one AI call for {@code clientId}, or throws if a limit is hit. */
    public synchronized void acquire(String clientId) {
        long now = clock.millis();

        evictOlderThan(global, now - HOUR_MS);
        if (global.size() >= globalPerHour) {
            throw new RateLimitExceededException(
                    "Daily AI analysis capacity reached. Please try again later.",
                    retryAfter(global, now, HOUR_MS));
        }

        Deque<Long> window = perClient.computeIfAbsent(clientId, k -> new ArrayDeque<>());
        evictOlderThan(window, now - MINUTE_MS);
        if (window.size() >= perClientPerMinute) {
            throw new RateLimitExceededException(
                    "Too many analysis requests. Please wait a moment and try again.",
                    retryAfter(window, now, MINUTE_MS));
        }

        window.addLast(now);
        global.addLast(now);

        // Keep the map from growing forever with idle clients.
        if (perClient.size() > 1000) {
            perClient.values().removeIf(d -> {
                evictOlderThan(d, now - MINUTE_MS);
                return d.isEmpty();
            });
        }
    }

    private static void evictOlderThan(Deque<Long> window, long cutoff) {
        while (!window.isEmpty() && window.peekFirst() <= cutoff) {
            window.pollFirst();
        }
    }

    private static long retryAfter(Deque<Long> window, long now, long windowMs) {
        long oldest = window.isEmpty() ? now : window.peekFirst();
        return Math.max(1L, (oldest + windowMs - now + 999) / 1000);
    }
}

package com.bank.notification.service;

import com.bank.notification.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window, in-memory rate limiter for POST /send, keyed by recipient, so a
 * single user cannot be flooded with notifications (e.g. forged "SECURITY" alerts).
 * In-memory is adequate for one instance; a multi-instance deployment would move
 * this to a shared store (Redis). The map is bounded by the number of distinct
 * recipients, which is acceptable here.
 */
@Component
public class SendRateLimiter {

    private static final long WINDOW_MS = 60_000L;

    private final int maxPerWindow;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public SendRateLimiter(@Value("${notification.rate-limit.max-per-minute:20}") int maxPerMinute) {
        this.maxPerWindow = maxPerMinute;
    }

    public void check(String recipientUserId) {
        long now = System.currentTimeMillis();
        Window w = windows.compute(recipientUserId, (key, existing) -> {
            if (existing == null || now - existing.start >= WINDOW_MS) {
                return new Window(now);
            }
            existing.count++;
            return existing;
        });
        if (w.count > maxPerWindow) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many notifications for this recipient; try again shortly");
        }
    }

    private static final class Window {
        final long start;
        int count;

        Window(long start) {
            this.start = start;
            this.count = 1;
        }
    }
}

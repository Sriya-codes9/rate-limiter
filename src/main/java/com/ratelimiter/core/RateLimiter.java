package com.ratelimiter.core;

import com.ratelimiter.exception.RateLimitException;
import com.ratelimiter.model.RateLimitResponse;
import com.ratelimiter.model.UserRequestLog;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-Window Log Rate Limiter.
 *
 * <p>Strategy: each user has a Deque of request timestamps.
 * On every call we evict expired entries, then either permit
 * or reject based on the remaining count.
 *
 * <p>Thread safety: ConcurrentHashMap for the user map +
 * synchronized methods on UserRequestLog cover concurrent access.
 *
 * Default limits: 5 requests per 60 seconds per user.
 */
public class RateLimiter {

    private final int maxRequests;
    private final long windowSizeMs;

    /** userId → sliding-window log */
    private final ConcurrentHashMap<String, UserRequestLog> userLogs = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ //
    //  Constructors
    // ------------------------------------------------------------------ //

    /** Default: 5 requests per minute. */
    public RateLimiter() {
        this(5, 60_000);
    }

    /**
     * Custom limits.
     *
     * @param maxRequests  max allowed requests in the window
     * @param windowSizeMs window duration in milliseconds
     */
    public RateLimiter(int maxRequests, long windowSizeMs) {
        if (maxRequests <= 0)   throw new IllegalArgumentException("maxRequests must be > 0");
        if (windowSizeMs <= 0)  throw new IllegalArgumentException("windowSizeMs must be > 0");
        this.maxRequests  = maxRequests;
        this.windowSizeMs = windowSizeMs;
    }

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    /**
     * Evaluate a request for the given user.
     *
     * @param userId non-null user identifier
     * @return RateLimitResponse indicating whether the request is allowed
     * @throws IllegalArgumentException if userId is null or blank
     */
    public RateLimitResponse allowRequest(String userId) {
        validateUserId(userId);

        long now    = System.currentTimeMillis();
        long cutoff = now - windowSizeMs;

        // Get or create the log atomically
        UserRequestLog log = userLogs.computeIfAbsent(userId, id -> new UserRequestLog());

        synchronized (log) {
            // Step 1 – evict stale entries
            log.evictBefore(cutoff);

            // Step 2 – check count
            int currentCount = log.size();

            if (currentCount < maxRequests) {
                // ALLOWED
                log.addTimestamp(now);
                int remaining = maxRequests - currentCount - 1;
                return RateLimitResponse.allowed(remaining);
            } else {
                // BLOCKED – compute when the oldest slot expires
                long retryAfterMs = log.peekOldest() + windowSizeMs - now;
                retryAfterMs = Math.max(retryAfterMs, 1); // never return 0 or negative
                return RateLimitResponse.blocked(retryAfterMs);
            }
        }
    }

    /**
     * Same as {@link #allowRequest(String)} but throws on rejection
     * instead of returning a response object.
     *
     * @throws RateLimitException if the request is blocked
     */
    public void allowRequestOrThrow(String userId) {
        RateLimitResponse response = allowRequest(userId);
        if (!response.isAllowed()) {
            throw new RateLimitException(userId, response.getRetryAfterMs());
        }
    }

    /**
     * Reset all state for a specific user (useful for testing or admin ops).
     */
    public void resetUser(String userId) {
        userLogs.remove(userId);
    }

    /**
     * Reset all state across all users.
     */
    public void resetAll() {
        userLogs.clear();
    }

    /**
     * How many requests the given user has made in the current window.
     */
    public int getRequestCount(String userId) {
        UserRequestLog log = userLogs.get(userId);
        if (log == null) return 0;
        long cutoff = System.currentTimeMillis() - windowSizeMs;
        log.evictBefore(cutoff);
        return log.size();
    }

    // Getters
    public int   getMaxRequests()  { return maxRequests; }
    public long  getWindowSizeMs() { return windowSizeMs; }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
    }
}

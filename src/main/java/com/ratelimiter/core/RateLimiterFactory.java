package com.ratelimiter.core;

/**
 * Factory for creating pre-configured RateLimiter instances.
 * Useful for common production scenarios.
 */
public final class RateLimiterFactory {

    private RateLimiterFactory() {}

    /** 5 requests per 60 seconds (default). */
    public static RateLimiter defaultLimiter() {
        return new RateLimiter(5, 60_000);
    }

    /** Lenient limiter: 100 requests per minute. */
    public static RateLimiter lenient() {
        return new RateLimiter(100, 60_000);
    }

    /** Strict limiter: 1 request per second. */
    public static RateLimiter strict() {
        return new RateLimiter(1, 1_000);
    }

    /** Custom configuration. */
    public static RateLimiter custom(int maxRequests, long windowSizeMs) {
        return new RateLimiter(maxRequests, windowSizeMs);
    }
}

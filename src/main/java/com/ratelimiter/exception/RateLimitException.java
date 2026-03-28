package com.ratelimiter.exception;

public class RateLimitException extends RuntimeException {
    private final long retryAfterMs;

    public RateLimitException(String userId, long retryAfterMs) {
        super(String.format("Rate limit exceeded for user '%s'. Retry after %d ms.", userId, retryAfterMs));
        this.retryAfterMs = retryAfterMs;
    }

    public long getRetryAfterMs() { return retryAfterMs; }
}

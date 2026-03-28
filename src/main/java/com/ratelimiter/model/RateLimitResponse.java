package com.ratelimiter.model;

/**
 * Immutable response object returned by the rate limiter.
 * Carries the decision (allow/block) along with metadata.
 */
public final class RateLimitResponse {

    private final boolean allowed;
    private final int remainingRequests;
    private final long retryAfterMs;
    private final String message;

    private RateLimitResponse(boolean allowed, int remainingRequests, long retryAfterMs, String message) {
        this.allowed = allowed;
        this.remainingRequests = remainingRequests;
        this.retryAfterMs = retryAfterMs;
        this.message = message;
    }

    public static RateLimitResponse allowed(int remaining) {
        return new RateLimitResponse(true, remaining, 0,
                "Request allowed. Remaining: " + remaining);
    }

    public static RateLimitResponse blocked(long retryAfterMs) {
        return new RateLimitResponse(false, 0, retryAfterMs,
                String.format("Rate limit exceeded. Retry after %d ms (%.2f seconds).",
                        retryAfterMs, retryAfterMs / 1000.0));
    }

    public boolean isAllowed()          { return allowed; }
    public int getRemainingRequests()   { return remainingRequests; }
    public long getRetryAfterMs()       { return retryAfterMs; }
    public String getMessage()          { return message; }

    @Override
    public String toString() {
        return String.format("RateLimitResponse{allowed=%b, remaining=%d, retryAfterMs=%d, message='%s'}",
                allowed, remainingRequests, retryAfterMs, message);
    }
}

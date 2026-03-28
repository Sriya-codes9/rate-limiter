package com.ratelimiter;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.exception.RateLimitException;
import com.ratelimiter.model.RateLimitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimiter Tests")
class RateLimiterTest {

    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new RateLimiter(5, 60_000); // 5 req / 60 s
    }

    // ------------------------------------------------------------------ //
    //  Allow / Block basics
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Should allow exactly maxRequests within the window")
    void shouldAllowMaxRequests() {
        for (int i = 0; i < 5; i++) {
            RateLimitResponse resp = limiter.allowRequest("user1");
            assertTrue(resp.isAllowed(), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should block requests beyond the limit")
    void shouldBlockBeyondLimit() {
        for (int i = 0; i < 5; i++) limiter.allowRequest("user1");

        RateLimitResponse resp = limiter.allowRequest("user1");
        assertFalse(resp.isAllowed(), "6th request should be blocked");
        assertTrue(resp.getRetryAfterMs() > 0, "retryAfterMs should be positive");
    }

    @Test
    @DisplayName("Remaining count should decrement correctly")
    void remainingShouldDecrement() {
        for (int expected = 4; expected >= 0; expected--) {
            RateLimitResponse resp = limiter.allowRequest("user2");
            assertEquals(expected, resp.getRemainingRequests());
        }
    }

    // ------------------------------------------------------------------ //
    //  Multi-user isolation
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Users should have independent counters")
    void usersShouldBeIsolated() {
        for (int i = 0; i < 5; i++) limiter.allowRequest("alice");

        // alice is blocked
        assertFalse(limiter.allowRequest("alice").isAllowed());
        // bob is not affected
        assertTrue(limiter.allowRequest("bob").isAllowed());
    }

    // ------------------------------------------------------------------ //
    //  Reset
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("resetUser should clear the user's window")
    void resetUserShouldWork() {
        for (int i = 0; i < 5; i++) limiter.allowRequest("alice");
        assertFalse(limiter.allowRequest("alice").isAllowed());

        limiter.resetUser("alice");
        assertTrue(limiter.allowRequest("alice").isAllowed(), "Alice should be allowed after reset");
    }

    @Test
    @DisplayName("resetAll should clear all users")
    void resetAllShouldWork() {
        limiter.allowRequest("a");
        limiter.allowRequest("b");
        limiter.resetAll();
        assertEquals(0, limiter.getRequestCount("a"));
        assertEquals(0, limiter.getRequestCount("b"));
    }

    // ------------------------------------------------------------------ //
    //  allowRequestOrThrow
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("allowRequestOrThrow should throw on 6th request")
    void orThrowShouldThrow() {
        for (int i = 0; i < 5; i++) limiter.allowRequestOrThrow("user3");

        RateLimitException ex = assertThrows(RateLimitException.class,
                () -> limiter.allowRequestOrThrow("user3"));
        assertTrue(ex.getRetryAfterMs() > 0);
    }

    // ------------------------------------------------------------------ //
    //  Validation
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Null userId should throw IllegalArgumentException")
    void nullUserIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> limiter.allowRequest(null));
    }

    @Test
    @DisplayName("Blank userId should throw IllegalArgumentException")
    void blankUserIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> limiter.allowRequest("   "));
    }

    @Test
    @DisplayName("Invalid constructor args should throw")
    void invalidConstructorArgs() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(5, 0));
    }

    // ------------------------------------------------------------------ //
    //  Concurrency
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Concurrent requests should never allow more than maxRequests")
    void concurrentRequestsShouldRespectLimit() throws InterruptedException {
        int threads = 20;
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                latch.countDown();
                try { latch.await(); } catch (InterruptedException ignored) {}
                RateLimitResponse r = limiter.allowRequest("concurrent-user");
                if (r.isAllowed()) allowed.incrementAndGet();
                else               blocked.incrementAndGet();
            }));
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(5, allowed.get(), "Exactly 5 requests should be allowed");
        assertEquals(15, blocked.get(), "Remaining 15 should be blocked");
    }
}

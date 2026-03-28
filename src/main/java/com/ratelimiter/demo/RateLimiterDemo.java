package com.ratelimiter.demo;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.exception.RateLimitException;
import com.ratelimiter.model.RateLimitResponse;

/**
 * Demonstrates the rate limiter in action:
 *  1. Single-user burst (expect 5 allowed, then blocked)
 *  2. Multi-user isolation
 *  3. allowRequestOrThrow() usage
 */
public class RateLimiterDemo {

    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";

    public static void main(String[] args) throws InterruptedException {
        demoSingleUserBurst();
        demoMultiUserIsolation();
        demoOrThrow();
        demoConcurrent();
    }

    // ------------------------------------------------------------------ //

    static void demoSingleUserBurst() {
        System.out.println(CYAN + "\n=== Demo 1: Single-User Burst (5 req/min) ===" + RESET);
        RateLimiter limiter = new RateLimiter(5, 60_000);

        for (int i = 1; i <= 8; i++) {
            RateLimitResponse resp = limiter.allowRequest("alice");
            String status = resp.isAllowed()
                    ? GREEN + "✓ ALLOWED" + RESET + "  remaining=" + resp.getRemainingRequests()
                    : RED   + "✗ BLOCKED" + RESET + "  retryAfter=" + resp.getRetryAfterMs() + "ms";
            System.out.printf("  Request #%d → %s  | %s%n", i, status, resp.getMessage());
        }
    }

    // ------------------------------------------------------------------ //

    static void demoMultiUserIsolation() {
        System.out.println(CYAN + "\n=== Demo 2: Multi-User Isolation ===" + RESET);
        RateLimiter limiter = new RateLimiter(3, 60_000);
        String[] users = {"alice", "bob", "charlie"};

        for (String user : users) {
            System.out.println(YELLOW + "  User: " + user + RESET);
            for (int i = 1; i <= 4; i++) {
                RateLimitResponse resp = limiter.allowRequest(user);
                String status = resp.isAllowed()
                        ? GREEN + "✓" + RESET
                        : RED   + "✗" + RESET;
                System.out.printf("    req#%d → %s %s%n", i, status, resp.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ //

    static void demoOrThrow() {
        System.out.println(CYAN + "\n=== Demo 3: allowRequestOrThrow() ===" + RESET);
        RateLimiter limiter = new RateLimiter(2, 60_000);

        for (int i = 1; i <= 4; i++) {
            try {
                limiter.allowRequestOrThrow("dave");
                System.out.println(GREEN + "  Request #" + i + " → processed successfully" + RESET);
            } catch (RateLimitException e) {
                System.out.println(RED + "  Request #" + i + " → caught: " + e.getMessage() + RESET);
            }
        }
    }

    // ------------------------------------------------------------------ //

    static void demoConcurrent() throws InterruptedException {
        System.out.println(CYAN + "\n=== Demo 4: Concurrent Requests (10 threads, 5-req limit) ===" + RESET);
        RateLimiter limiter = new RateLimiter(5, 60_000);
        Thread[] threads = new Thread[10];
        int[] allowed = {0};
        int[] blocked = {0};
        Object lock = new Object();

        for (int i = 0; i < threads.length; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                RateLimitResponse r = limiter.allowRequest("shared-user");
                synchronized (lock) {
                    if (r.isAllowed()) allowed[0]++;
                    else               blocked[0]++;
                    System.out.printf("  Thread-%02d → %s%n", idx,
                            r.isAllowed()
                                    ? GREEN + "ALLOWED" + RESET
                                    : RED + "BLOCKED" + RESET);
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.printf("%n  Summary → Allowed: %d | Blocked: %d (limit was 5)%n",
                allowed[0], blocked[0]);
    }
}

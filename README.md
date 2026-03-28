# Rate Limiter — Java

A **thread-safe, sliding-window log** rate limiter built in pure Java with no external runtime dependencies.

## Features

| Feature | Detail |
|---|---|
| Algorithm | Sliding-Window Log |
| Default limit | 5 requests / 60 seconds per user |
| Thread safety | `ConcurrentHashMap` + `synchronized` per-user log |
| Java version | Java 17+ |

## Project Structure

```
src/
├── main/java/com/ratelimiter/
│   ├── core/
│   │   ├── RateLimiter.java          ← main entry point
│   │   └── RateLimiterFactory.java   ← pre-configured factories
│   ├── model/
│   │   ├── RateLimitResponse.java    ← allow/block result object
│   │   └── UserRequestLog.java       ← per-user timestamp deque
│   ├── exception/
│   │   └── RateLimitException.java   ← thrown by allowRequestOrThrow()
│   └── demo/
│       └── RateLimiterDemo.java      ← runnable demo
└── test/java/com/ratelimiter/
    └── RateLimiterTest.java          ← JUnit 5 tests
```

## Quick Start

### Build & Run Tests
```bash
mvn clean test
```

### Run Demo
```bash
mvn package -DskipTests
java -jar target/rate-limiter.jar
```

## Usage

```java
// 1. Default: 5 requests per minute
RateLimiter limiter = new RateLimiter();

// 2. Custom limits
RateLimiter limiter = new RateLimiter(10, 30_000); // 10 req / 30 s

// 3. Factory shortcuts
RateLimiter limiter = RateLimiterFactory.defaultLimiter();
RateLimiter limiter = RateLimiterFactory.strict();   // 1 req/sec
RateLimiter limiter = RateLimiterFactory.lenient();  // 100 req/min

// Check a request
RateLimitResponse resp = limiter.allowRequest("user-123");
if (resp.isAllowed()) {
    System.out.println("Remaining: " + resp.getRemainingRequests());
} else {
    System.out.println("Blocked. Retry after: " + resp.getRetryAfterMs() + "ms");
}

// Or throw on rejection
try {
    limiter.allowRequestOrThrow("user-123");
    // ... handle request
} catch (RateLimitException e) {
    // respond with 429
}
```

## Algorithm: Sliding-Window Log

```
allowRequest(userId)
       │
       ▼
get/create Deque<Long> for user
       │
       ▼
evict timestamps < (now - windowSize)      ← O(k), k = expired entries
       │
       ▼
deque.size() < maxRequests?
     yes │                 no │
         ▼                    ▼
   addTimestamp(now)     compute retryAfterMs
   return ALLOWED        return BLOCKED
```

## CI / CD

GitHub Actions runs on every push and PR, testing on **Java 17** and **Java 21**.

See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## License

MIT

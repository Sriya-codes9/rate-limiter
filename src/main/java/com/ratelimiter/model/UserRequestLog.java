package com.ratelimiter.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds the sliding-window timestamp log for a single user.
 * All mutation is done through synchronized methods since
 * multiple threads may target the same user simultaneously.
 */
public class UserRequestLog {

    private final Deque<Long> timestamps = new ArrayDeque<>();

    /**
     * Remove all timestamps older than the given cutoff (exclusive).
     *
     * @param cutoff epoch-ms boundary; entries strictly before this are stale
     */
    public synchronized void evictBefore(long cutoff) {
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }
    }

    /**
     * Record a new request at the given timestamp.
     */
    public synchronized void addTimestamp(long timestampMs) {
        timestamps.addLast(timestampMs);
    }

    /**
     * Number of valid (non-evicted) timestamps currently in the log.
     */
    public synchronized int size() {
        return timestamps.size();
    }

    /**
     * Peek at the oldest timestamp without removing it.
     * Returns Long.MAX_VALUE if the log is empty (safe for retry calculation).
     */
    public synchronized long peekOldest() {
        Long oldest = timestamps.peekFirst();
        return oldest != null ? oldest : Long.MAX_VALUE;
    }
}

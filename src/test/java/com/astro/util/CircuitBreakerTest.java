package com.astro.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // Threshold: 2 failures, reset cooldown: 100ms, half-open success: 1
        circuitBreaker = new CircuitBreaker("test-breaker", 2, Duration.ofMillis(100), 1);
    }

    @Test
    @DisplayName("Should execute normally in CLOSED state")
    void testNormalExecutionWhenClosed() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        String result = circuitBreaker.execute(() -> "success", () -> "fallback");
        assertEquals("success", result);
        assertEquals(0, circuitBreaker.getFailureCount());
    }

    @Test
    @DisplayName("Should trip to OPEN after reaching failure threshold")
    void testTripToOpenOnConsecutiveFailures() {
        // 1st failure
        String res1 = circuitBreaker.execute(() -> {
            throw new RuntimeException("fail 1");
        }, () -> "fallback-1");
        assertEquals("fallback-1", res1);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(1, circuitBreaker.getFailureCount());

        // 2nd failure -> reaches threshold 2 -> trips to OPEN
        String res2 = circuitBreaker.execute(() -> {
            throw new RuntimeException("fail 2");
        }, () -> "fallback-2");
        assertEquals("fallback-2", res2);
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // 3rd call while OPEN: short-circuits immediately without calling protected action
        AtomicInteger protectedActionCalls = new AtomicInteger(0);
        String res3 = circuitBreaker.execute(() -> {
            protectedActionCalls.incrementAndGet();
            return "should-not-run";
        }, () -> "fallback-fast");

        assertEquals("fallback-fast", res3);
        assertEquals(0, protectedActionCalls.get(), "Protected action should not be invoked when circuit is OPEN");
    }

    @Test
    @DisplayName("Should transition from OPEN to HALF_OPEN after cooldown and close on success")
    void testHalfOpenRecovery() throws InterruptedException {
        // Trip to OPEN
        circuitBreaker.trip();
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Wait for cooldown of 100ms
        Thread.sleep(150);

        // Next call should probe in HALF_OPEN
        String result = circuitBreaker.execute(() -> "recovered", () -> "fallback");
        assertEquals("recovered", result);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState(), "Successful probe should recover circuit to CLOSED");
    }

    @Test
    @DisplayName("Should manually reset to CLOSED")
    void testManualReset() {
        circuitBreaker.trip();
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        circuitBreaker.reset();
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
    }
}

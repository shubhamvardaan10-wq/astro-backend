package com.astro.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Lightweight, thread-safe Circuit Breaker for optional external services
 * (Elasticsearch, Redis, external Geocoding, AI LLM endpoints).
 *
 * Prevents cascade timeouts, thread starvation, and application degradation
 * when secondary clusters or remote APIs are down or experiencing latency.
 *
 * States:
 *   - CLOSED: Normal operation, requests pass through. Failures increment counter.
 *   - OPEN: Tripped after failure threshold. Calls fail-fast to fallback without network I/O.
 *   - HALF_OPEN: Trial period after cooldown duration. A successful request closes the circuit.
 */
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final String name;
    private final int failureThreshold;
    private final Duration resetTimeout;
    private final int halfOpenSuccessThreshold;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
    private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<Instant> lastStateChangeTime = new AtomicReference<>(Instant.now());
    private final AtomicReference<String> lastFailureReason = new AtomicReference<>("None");

    public CircuitBreaker(String name) {
        this(name, 3, Duration.ofSeconds(30), 2);
    }

    public CircuitBreaker(String name, int failureThreshold, Duration resetTimeout, int halfOpenSuccessThreshold) {
        this.name = name != null ? name : "circuit-breaker";
        this.failureThreshold = Math.max(1, failureThreshold);
        this.resetTimeout = resetTimeout != null ? resetTimeout : Duration.ofSeconds(30);
        this.halfOpenSuccessThreshold = Math.max(1, halfOpenSuccessThreshold);
    }

    /**
     * Executes the protected action. If the circuit is OPEN, immediately executes the fallback.
     * If the action throws an exception, logs the event, updates circuit state, and runs fallback.
     */
    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        if (!allowExecution()) {
            log.debug("[CircuitBreaker:{}] Circuit is OPEN. Short-circuiting to fallback.", name);
            return fallback != null ? fallback.get() : null;
        }

        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (Throwable t) {
            onFailure(t);
            log.warn("[CircuitBreaker:{}] Protected action failed ({}: {}). Executing fallback.",
                    name, t.getClass().getSimpleName(), t.getMessage());
            return fallback != null ? fallback.get() : null;
        }
    }

    /**
     * Executes a void action with a fallback runnable.
     */
    public void executeVoid(Runnable action, Runnable fallback) {
        execute(() -> {
            action.run();
            return null;
        }, () -> {
            if (fallback != null) {
                fallback.run();
            }
            return null;
        });
    }

    /**
     * Determines whether execution is allowed based on current state and elapsed cooldown.
     */
    public boolean allowExecution() {
        State current = state.get();

        if (current == State.CLOSED) {
            return true;
        }

        if (current == State.OPEN) {
            Instant openSince = lastStateChangeTime.get();
            if (Duration.between(openSince, Instant.now()).compareTo(resetTimeout) >= 0) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    lastStateChangeTime.set(Instant.now());
                    consecutiveSuccesses.set(0);
                    log.info("[CircuitBreaker:{}] Reset timeout elapsed. Transitioned from OPEN to HALF_OPEN (probing service).", name);
                    return true;
                }
            }
            return false;
        }

        // In HALF_OPEN state, allow execution for probe requests
        return true;
    }

    public void onSuccess() {
        State current = state.get();
        if (current == State.HALF_OPEN) {
            int successes = consecutiveSuccesses.incrementAndGet();
            if (successes >= halfOpenSuccessThreshold) {
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    failureCount.set(0);
                    consecutiveSuccesses.set(0);
                    lastStateChangeTime.set(Instant.now());
                    log.info("[CircuitBreaker:{}] Probes succeeded. Circuit successfully recovered: CLOSED.", name);
                }
            }
        } else if (current == State.CLOSED) {
            failureCount.set(0);
        }
    }

    public void onFailure(Throwable t) {
        lastFailureTime.set(Instant.now());
        lastFailureReason.set(t != null ? (t.getClass().getSimpleName() + ": " + t.getMessage()) : "Unknown error");

        State current = state.get();
        if (current == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                lastStateChangeTime.set(Instant.now());
                consecutiveSuccesses.set(0);
                log.warn("[CircuitBreaker:{}] Probe failed in HALF_OPEN. Re-opening circuit: OPEN.", name);
            }
        } else if (current == State.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                    lastStateChangeTime.set(Instant.now());
                    log.warn("[CircuitBreaker:{}] Consecutive failure threshold ({}) reached. Circuit tripped: OPEN.",
                            name, failureThreshold);
                }
            }
        }
    }

    /**
     * Manually resets the circuit breaker to CLOSED.
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        consecutiveSuccesses.set(0);
        lastStateChangeTime.set(Instant.now());
        log.info("[CircuitBreaker:{}] Manually reset to CLOSED.", name);
    }

    /**
     * Tripping manual override for testing or emergency isolation.
     */
    public void trip() {
        state.set(State.OPEN);
        lastStateChangeTime.set(Instant.now());
        log.warn("[CircuitBreaker:{}] Manually tripped to OPEN.", name);
    }

    public State getState() {
        return state.get();
    }

    public String getName() {
        return name;
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public String getLastFailureReason() {
        return lastFailureReason.get();
    }

    public Instant getLastFailureTime() {
        return lastFailureTime.get();
    }

    public Instant getLastStateChangeTime() {
        return lastStateChangeTime.get();
    }

    public Map<String, Object> getMetrics() {
        return Map.of(
            "name", name,
            "state", state.get().name(),
            "failureCount", failureCount.get(),
            "failureThreshold", failureThreshold,
            "resetTimeoutSeconds", resetTimeout.toSeconds(),
            "lastStateChangeTime", lastStateChangeTime.get().toString(),
            "lastFailureTime", lastFailureTime.get().toString(),
            "lastFailureReason", lastFailureReason.get()
        );
    }
}

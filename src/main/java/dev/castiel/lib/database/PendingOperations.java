package dev.castiel.lib.database;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

final class PendingOperations {
    private final Object monitor = new Object();
    private final Set<CompletableFuture<?>> pending =
            Collections.newSetFromMap(new IdentityHashMap<CompletableFuture<?>, Boolean>());
    private boolean closing;

    boolean register(CompletableFuture<?> future) {
        synchronized (monitor) {
            if (closing) {
                return false;
            }
            pending.add(future);
        }
        future.whenComplete((result, error) -> complete(future));
        return true;
    }

    void beginClosing() {
        synchronized (monitor) {
            closing = true;
            monitor.notifyAll();
        }
    }

    boolean awaitEmpty(long timeout, TimeUnit unit) {
        if (timeout < 0 || unit == null) {
            throw new IllegalArgumentException("A non-negative timeout and time unit are required.");
        }
        long remaining = unit.toNanos(timeout);
        long deadline = System.nanoTime() + remaining;
        synchronized (monitor) {
            while (!pending.isEmpty()) {
                if (remaining <= 0) {
                    return false;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(monitor, remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                remaining = deadline - System.nanoTime();
            }
            return true;
        }
    }

    boolean isClosing() {
        synchronized (monitor) {
            return closing;
        }
    }

    int size() {
        synchronized (monitor) {
            return pending.size();
        }
    }

    private void complete(CompletableFuture<?> future) {
        synchronized (monitor) {
            pending.remove(future);
            monitor.notifyAll();
        }
    }
}

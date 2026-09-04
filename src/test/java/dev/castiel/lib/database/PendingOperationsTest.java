package dev.castiel.lib.database;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingOperationsTest {
    @Test
    void waitsUntilRegisteredOperationCompletes() {
        PendingOperations operations = new PendingOperations();
        CompletableFuture<Void> future = new CompletableFuture<Void>();

        assertTrue(operations.register(future));
        assertFalse(operations.awaitEmpty(0, TimeUnit.MILLISECONDS));

        future.complete(null);

        assertTrue(operations.awaitEmpty(100, TimeUnit.MILLISECONDS));
        assertEquals(0, operations.size());
    }

    @Test
    void rejectsNewOperationsAfterShutdownBegins() {
        PendingOperations operations = new PendingOperations();

        operations.beginClosing();

        assertTrue(operations.isClosing());
        assertFalse(operations.register(new CompletableFuture<Void>()));
    }
}

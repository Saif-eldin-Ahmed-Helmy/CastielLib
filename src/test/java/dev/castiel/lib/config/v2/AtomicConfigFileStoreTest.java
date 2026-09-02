package dev.castiel.lib.config.v2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicConfigFileStoreTest {
    private static final String SOURCE = "config.yml";

    @TempDir
    Path temp;

    @Test
    void freshWriteCreatesTargetWithoutBackupAndCopiesInput() throws IOException {
        Path target = temp.resolve("nested").resolve("config.yml");
        AtomicConfigFileStore store = new AtomicConfigFileStore(target);
        byte[] bytes = bytes("first");

        AtomicConfigFileStoreResult result = store.write(SOURCE, bytes);
        bytes[0] = 'X';

        assertTrue(result.isSuccessful());
        assertTrue(result.changed());
        assertFalse(result.backupCreated());
        assertArrayEquals(bytes("first"), Files.readAllBytes(target));
        assertFalse(Files.exists(target.resolveSibling("config.yml.bak")));
        assertNoTemps(target.getParent());
    }

    @Test
    void identicalWriteIsNoOpAndChangedWriteBacksUpUntouchedBytes() throws IOException {
        Path target = temp.resolve("config.yml");
        Files.write(target, bytes("old"));
        AtomicConfigFileStore store = new AtomicConfigFileStore(target);

        AtomicConfigFileStoreResult noOp = store.write(SOURCE, bytes("old"));
        assertTrue(noOp.isSuccessful());
        assertFalse(noOp.changed());
        assertFalse(noOp.backupCreated());
        assertFalse(Files.exists(target.resolveSibling("config.yml.bak")));

        AtomicConfigFileStoreResult changed = store.write(SOURCE, bytes("new"));
        assertTrue(changed.isSuccessful());
        assertTrue(changed.changed());
        assertTrue(changed.backupCreated());
        assertArrayEquals(bytes("new"), Files.readAllBytes(target));
        assertArrayEquals(bytes("old"), Files.readAllBytes(target.resolveSibling("config.yml.bak")));
        assertNoTemps(temp);
    }

    @Test
    void rollbackRestoresBackupAndKeepsItForRepeatedRecovery() throws IOException {
        Path target = temp.resolve("config.yml");
        AtomicConfigFileStore store = new AtomicConfigFileStore(target);
        store.write(SOURCE, bytes("old"));
        store.write(SOURCE, bytes("new"));

        AtomicConfigFileStoreResult rollback = store.rollback(SOURCE);
        assertTrue(rollback.isSuccessful());
        assertTrue(rollback.changed());
        assertFalse(rollback.backupCreated());
        assertArrayEquals(bytes("old"), Files.readAllBytes(target));
        assertArrayEquals(bytes("old"), Files.readAllBytes(target.resolveSibling("config.yml.bak")));

        AtomicConfigFileStoreResult repeated = store.rollback(SOURCE);
        assertTrue(repeated.isSuccessful());
        assertFalse(repeated.changed());
        assertArrayEquals(bytes("old"), Files.readAllBytes(target));
    }

    @Test
    void missingBackupAndUnsafeEntriesFailWithoutLeakingDetails() throws IOException {
        Path target = temp.resolve("config.yml");
        AtomicConfigFileStore store = new AtomicConfigFileStore(target);
        AtomicConfigFileStoreResult missing = store.rollback(SOURCE);
        assertFalse(missing.isSuccessful());
        assertEquals("config.file.rollback", missing.report().issues().get(0).id());
        assertEquals("<not-read>", missing.report().issues().get(0).suppliedValue());
        assertFalse(missing.toString().contains("old"));

        Files.createDirectories(target);
        AtomicConfigFileStoreResult directory = store.write(SOURCE, bytes("secret"));
        assertFalse(directory.isSuccessful());
        assertEquals("config.file.write", directory.report().issues().get(0).id());
        assertFalse(directory.report().issues().get(0).path().contains(temp.toString()));
        assertNoTemps(temp);
    }

    @Test
    void resultReportAndIssueCollectionsAreImmutableAndInputsAreValidated() {
        Path target = temp.resolve("config.yml");
        AtomicConfigFileStore store = new AtomicConfigFileStore(target);
        AtomicConfigFileStoreResult result = store.write(SOURCE, bytes("safe"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.report().issues().clear());
        assertThrows(NullPointerException.class, () -> store.write(null, bytes("x")));
        assertThrows(IllegalArgumentException.class, () -> store.write("C:\\secret.yml", bytes("x")));
        assertThrows(NullPointerException.class, () -> store.write(SOURCE, null));
    }

    @Test
    void symlinkTargetOrBackupIsRejectedWhenSupported() throws IOException {
        Path target = temp.resolve("config.yml");
        Path outside = temp.resolve("outside.yml");
        Files.write(outside, bytes("outside"));
        try {
            Files.createSymbolicLink(target, outside);
        } catch (UnsupportedOperationException | IOException | SecurityException ignored) {
            return;
        }
        AtomicConfigFileStoreResult result = new AtomicConfigFileStore(target).write(SOURCE, bytes("new"));
        assertFalse(result.isSuccessful());
        assertArrayEquals(bytes("outside"), Files.readAllBytes(outside));

        Files.deleteIfExists(target);
        Files.write(target, bytes("old"));
        Path backup = target.resolveSibling("config.yml.bak");
        Path outsideBackup = temp.resolve("outside-backup.yml");
        Files.write(outsideBackup, bytes("backup-outside"));
        try {
            Files.createSymbolicLink(backup, outsideBackup);
        } catch (UnsupportedOperationException | IOException | SecurityException ignored) {
            return;
        }
        AtomicConfigFileStoreResult backupResult = new AtomicConfigFileStore(target).write(SOURCE, bytes("new"));
        assertFalse(backupResult.isSuccessful());
        assertArrayEquals(bytes("old"), Files.readAllBytes(target));
        assertArrayEquals(bytes("backup-outside"), Files.readAllBytes(outsideBackup));

        Files.deleteIfExists(backup);
        Path realParent = temp.resolve("real-parent");
        Files.createDirectories(realParent);
        Path linkedParent = temp.resolve("linked-parent");
        try {
            Files.createSymbolicLink(linkedParent, realParent);
        } catch (UnsupportedOperationException | IOException | SecurityException ignored) {
            return;
        }
        AtomicConfigFileStoreResult parentResult = new AtomicConfigFileStore(
                linkedParent.resolve("config.yml")).write(SOURCE, bytes("new"));
        assertFalse(parentResult.isSuccessful());
        assertFalse(Files.exists(realParent.resolve("config.yml")));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertNoTemps(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                assertFalse(name.contains("-candidate-") || name.contains("-backup-")
                        || name.contains("-rollback-"), "temporary file remained: " + name);
            }
        }
    }
}

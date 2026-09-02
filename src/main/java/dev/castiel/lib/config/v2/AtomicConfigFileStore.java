package dev.castiel.lib.config.v2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/**
 * Small, format-neutral file transaction for already-encoded configuration bytes.
 * Operations on one instance are serialized; the caller owns path containment policy.
 */
public final class AtomicConfigFileStore {
    private static final String IMPACT = "configuration file was not changed";
    private static final String NOT_READ = "<not-read>";

    private final Path target;
    private final Path backup;

    /**
     * Creates a store for one absolute, normalized file target.
     *
     * @param target target file, which may not be a directory or symlink when used
     * @throws NullPointerException if target is null
     * @throws IllegalArgumentException if target has no file name
     */
    public AtomicConfigFileStore(Path target) {
        Path checked = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
        if (checked.getFileName() == null || checked.getFileName().toString().trim().isEmpty()) {
            throw new IllegalArgumentException("target must name a file");
        }
        this.target = checked;
        this.backup = checked.resolveSibling(checked.getFileName().toString() + ".bak");
    }

    /**
     * Writes candidate bytes, preserving the previous target in an immediate sibling backup when
     * and only when an existing file actually changes.
     *
     * @param source logical source label used in safe diagnostics
     * @param bytes candidate bytes; copied before any filesystem work
     * @return immutable transaction result
     */
    public synchronized AtomicConfigFileStoreResult write(String source, byte[] bytes) {
        String checkedSource = requireSource(source);
        byte[] candidate = Arrays.copyOf(Objects.requireNonNull(bytes, "bytes"), bytes.length);
        Path candidateTemp = null;
        Path backupTemp = null;
        try {
            ensureDirectoryAndEntries();
            boolean existed = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
            byte[] previous = existed ? Files.readAllBytes(target) : null;
            if (existed && Arrays.equals(previous, candidate)) {
                return AtomicConfigFileStoreResult.success(false, false);
            }

            candidateTemp = Files.createTempFile(parent(), temporaryPrefix("candidate"), ".tmp");
            writeForced(candidateTemp, candidate);

            boolean backupCreated = false;
            if (existed) {
                backupTemp = Files.createTempFile(parent(), temporaryPrefix("backup"), ".tmp");
                writeForced(backupTemp, previous);
                moveReplace(backupTemp, backup);
                backupTemp = null;
                backupCreated = true;
            }
            moveReplace(candidateTemp, target);
            candidateTemp = null;
            return AtomicConfigFileStoreResult.success(true, backupCreated);
        } catch (IOException | RuntimeException ignored) {
            return AtomicConfigFileStoreResult.failure(report("config.file.write", checkedSource,
                    "check the configuration file location and permissions"));
        } finally {
            deleteQuietly(candidateTemp);
            deleteQuietly(backupTemp);
        }
    }

    /**
     * Restores the immediate sibling backup without deleting it, so a repeated rollback remains
     * possible. No backup means a safe no-op failure.
     *
     * @param source logical source label used in safe diagnostics
     * @return immutable transaction result
     */
    public synchronized AtomicConfigFileStoreResult rollback(String source) {
        String checkedSource = requireSource(source);
        Path candidateTemp = null;
        try {
            ensureDirectoryAndEntries();
            if (!Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) {
                return AtomicConfigFileStoreResult.failure(report("config.file.rollback", checkedSource,
                        "create or select an available last-known-good backup"));
            }
            byte[] previous = Files.readAllBytes(backup);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                    && Arrays.equals(previous, Files.readAllBytes(target))) {
                return AtomicConfigFileStoreResult.success(false, false);
            }
            candidateTemp = Files.createTempFile(parent(), temporaryPrefix("rollback"), ".tmp");
            writeForced(candidateTemp, previous);
            moveReplace(candidateTemp, target);
            candidateTemp = null;
            return AtomicConfigFileStoreResult.success(true, false);
        } catch (IOException | RuntimeException ignored) {
            return AtomicConfigFileStoreResult.failure(report("config.file.rollback", checkedSource,
                    "check the backup and target file permissions"));
        } finally {
            deleteQuietly(candidateTemp);
        }
    }

    private void ensureDirectoryAndEntries() throws IOException {
        rejectSymlinkAncestors(parent());
        Files.createDirectories(parent());
        rejectSymlinkAncestors(parent());
        rejectUnsafeEntry(target, "config.file.path");
        rejectUnsafeEntry(backup, "config.file.path");
    }

    private static void rejectSymlinkAncestors(Path directory) throws IOException {
        Path current = directory.getRoot();
        for (Path segment : directory) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw new IOException("config.file.path");
            }
        }
    }

    private void rejectUnsafeEntry(Path entry, String id) throws IOException {
        if (Files.isSymbolicLink(entry)
                || (Files.exists(entry, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException(id);
        }
    }

    private Path parent() {
        return target.getParent();
    }

    private String temporaryPrefix(String kind) {
        String name = target.getFileName().toString();
        StringBuilder safe = new StringBuilder();
        for (int index = 0; index < name.length() && safe.length() < 40; index++) {
            char character = name.charAt(index);
            if ((character >= 'a' && character <= 'z')
                    || (character >= 'A' && character <= 'Z')
                    || (character >= '0' && character <= '9')
                    || character == '-' || character == '_') {
                safe.append(character);
            } else {
                safe.append('-');
            }
        }
        return (safe.length() < 1 ? "config" : safe.toString()) + "-" + kind + "-";
    }

    private static void writeForced(Path file, byte[] bytes) throws IOException {
        OpenOption[] options = new OpenOption[] {
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
        };
        FileChannel channel = FileChannel.open(file, options);
        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } finally {
            channel.close();
        }
    }

    private static void moveReplace(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException | SecurityException ignored) {
            // A failed cleanup is intentionally not exposed as a successful transaction.
        }
    }

    private ConfigValidationReport report(String id, String source, String action) {
        return ConfigValidationReport.of(Collections.singletonList(new ConfigValidationIssue(
                id, ValidationSeverity.ERROR, source, fileLabel(), NOT_READ,
                "a writable regular file", IMPACT, action)));
    }

    private String fileLabel() {
        String name = target.getFileName().toString();
        StringBuilder safe = new StringBuilder(Math.min(name.length(), 64));
        for (int index = 0; index < name.length() && safe.length() < 64; index++) {
            char character = name.charAt(index);
            if ((character >= 'a' && character <= 'z')
                    || (character >= 'A' && character <= 'Z')
                    || (character >= '0' && character <= '9')
                    || character == '.' || character == '_' || character == '-') {
                safe.append(character);
            } else {
                safe.append('?');
            }
        }
        return safe.length() == 0 ? "config-file" : safe.toString();
    }

    private static String requireSource(String source) {
        Objects.requireNonNull(source, "source");
        if (source.trim().isEmpty()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        if (source.startsWith("/") || source.startsWith("\\")
                || (source.length() >= 3 && Character.isLetter(source.charAt(0))
                && source.charAt(1) == ':'
                && (source.charAt(2) == '/' || source.charAt(2) == '\\'))) {
            throw new IllegalArgumentException("source must be a logical label");
        }
        return source;
    }
}

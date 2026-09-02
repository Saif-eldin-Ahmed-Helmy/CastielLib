package dev.castiel.lib.config.v2;

import java.util.Objects;

/** Immutable, fail-closed result of one configuration file transaction. */
public final class AtomicConfigFileStoreResult {
    private final boolean successful;
    private final boolean changed;
    private final boolean backupCreated;
    private final ConfigValidationReport report;

    private AtomicConfigFileStoreResult(boolean successful, boolean changed,
                                        boolean backupCreated, ConfigValidationReport report) {
        this.successful = successful;
        this.changed = changed;
        this.backupCreated = backupCreated;
        this.report = Objects.requireNonNull(report, "report");
    }

    static AtomicConfigFileStoreResult success(boolean changed, boolean backupCreated) {
        return new AtomicConfigFileStoreResult(true, changed, backupCreated,
                ConfigValidationReport.empty());
    }

    static AtomicConfigFileStoreResult failure(ConfigValidationReport report) {
        return new AtomicConfigFileStoreResult(false, false, false,
                Objects.requireNonNull(report, "report"));
    }

    /** @return whether the requested operation completed successfully */
    public boolean isSuccessful() {
        return successful;
    }

    /** @return whether the target bytes changed */
    public boolean changed() {
        return changed;
    }

    /** @return whether this operation created or replaced the immediate backup */
    public boolean backupCreated() {
        return backupCreated;
    }

    /** @return immutable safe validation report */
    public ConfigValidationReport report() {
        return report;
    }

    @Override
    public String toString() {
        return "AtomicConfigFileStoreResult{" +
                "successful=" + successful +
                ", changed=" + changed +
                ", backupCreated=" + backupCreated +
                ", issueCount=" + report.issues().size() +
                '}';
    }
}

package dev.castiel.lib.config.v2;

import java.util.Objects;

/**
 * Immutable, all-or-nothing result of a configuration migration run.
 *
 * <p>Failures intentionally do not retain a partial document, thrown
 * exception, or raw failing value.</p>
 */
public final class ConfigMigrationResult {
    private final boolean successful;
    private final ConfigValidationReport report;
    private final ConfigDocument document;

    private ConfigMigrationResult(boolean successful, ConfigValidationReport report,
                                  ConfigDocument document) {
        this.successful = successful;
        this.report = Objects.requireNonNull(report, "report");
        this.document = document;
    }

    static ConfigMigrationResult success(ConfigDocument document) {
        return new ConfigMigrationResult(true, ConfigValidationReport.empty(),
                Objects.requireNonNull(document, "document"));
    }

    static ConfigMigrationResult failure(ConfigValidationReport report) {
        return new ConfigMigrationResult(false, Objects.requireNonNull(report, "report"), null);
    }

    /**
     * Returns whether the complete migration reached the target version.
     *
     * @return true only when a final document is available
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Returns the immutable success or actionable failure report.
     *
     * @return migration report
     */
    public ConfigValidationReport report() {
        return report;
    }

    /**
     * Returns the final document after a successful run.
     *
     * @return final migrated document
     * @throws IllegalStateException if the migration failed
     */
    public ConfigDocument document() {
        if (!successful) {
            throw new IllegalStateException("configuration migration failed");
        }
        return document;
    }
}

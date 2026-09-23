package dev.castiel.lib.config.v2;

import java.util.Objects;

/** Immutable, fail-closed result of decoding a configuration document. */
public final class ConfigDocumentLoadResult {
    private final boolean successful;
    private final ConfigValidationReport report;
    private final ConfigDocument document;

    private ConfigDocumentLoadResult(boolean successful, ConfigValidationReport report,
                                    ConfigDocument document) {
        this.successful = successful;
        this.report = Objects.requireNonNull(report, "report");
        this.document = document;
    }

    static ConfigDocumentLoadResult success(ConfigDocument document) {
        return new ConfigDocumentLoadResult(true, ConfigValidationReport.empty(),
                Objects.requireNonNull(document, "document"));
    }

    static ConfigDocumentLoadResult failure(ConfigValidationReport report) {
        return new ConfigDocumentLoadResult(false, Objects.requireNonNull(report, "report"), null);
    }

    public boolean isSuccessful() { return successful; }

    public ConfigValidationReport report() { return report; }

    public ConfigDocument document() {
        if (!successful) {
            throw new IllegalStateException("configuration decoding failed");
        }
        return document;
    }

    @Override
    public String toString() {
        return "ConfigDocumentLoadResult{" +
                "successful=" + successful +
                ", issueCount=" + report.issues().size() +
                ", hasDocument=" + (document != null) +
                '}';
    }
}

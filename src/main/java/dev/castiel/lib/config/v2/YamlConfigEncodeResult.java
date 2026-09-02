package dev.castiel.lib.config.v2;

import java.util.Objects;

/** Immutable, fail-closed result of encoding a configuration document. */
public final class YamlConfigEncodeResult {
    private final boolean successful;
    private final ConfigValidationReport report;
    private final String yaml;

    private YamlConfigEncodeResult(boolean successful, ConfigValidationReport report,
                                   String yaml) {
        this.successful = successful;
        this.report = Objects.requireNonNull(report, "report");
        this.yaml = yaml;
    }

    static YamlConfigEncodeResult success(String yaml) {
        return new YamlConfigEncodeResult(true, ConfigValidationReport.empty(),
                Objects.requireNonNull(yaml, "yaml"));
    }

    static YamlConfigEncodeResult failure(ConfigValidationReport report) {
        return new YamlConfigEncodeResult(false, Objects.requireNonNull(report, "report"), null);
    }

    /** @return whether encoding completed successfully */
    public boolean isSuccessful() {
        return successful;
    }

    /** @return immutable validation report */
    public ConfigValidationReport report() {
        return report;
    }

    /**
     * Returns the encoded YAML.
     *
     * @return one deterministic YAML document
     * @throws IllegalStateException when encoding failed
     */
    public String yaml() {
        if (!successful) {
            throw new IllegalStateException("configuration encoding failed");
        }
        return yaml;
    }

    @Override
    public String toString() {
        return "YamlConfigEncodeResult{" +
                "successful=" + successful +
                ", issueCount=" + report.issues().size() +
                ", hasYaml=" + (yaml != null) +
                '}';
    }
}

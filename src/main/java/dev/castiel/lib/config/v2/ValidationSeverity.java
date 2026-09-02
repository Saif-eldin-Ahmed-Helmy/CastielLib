package dev.castiel.lib.config.v2;

/** Severity assigned to a configuration validation issue. */
public enum ValidationSeverity {
    /** A configuration problem that prevents the configuration from being valid. */
    ERROR,
    /** A non-blocking configuration concern. */
    WARNING
}

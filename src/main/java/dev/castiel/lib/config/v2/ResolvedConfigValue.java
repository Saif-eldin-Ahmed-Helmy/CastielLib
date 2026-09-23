package dev.castiel.lib.config.v2;

import java.util.Objects;

/**
 * The result of resolving one typed configuration key.
 *
 * <p>A result is either valid and contains its accepted value, or invalid and
 * contains the actionable validation report. Invalid values are deliberately
 * not retained.</p>
 *
 * @param <T> resolved value type
 */
public final class ResolvedConfigValue<T> {
    private final boolean valid;
    private final boolean usedDefault;
    private final ConfigValidationReport report;
    private final T value;

    private ResolvedConfigValue(boolean valid, boolean usedDefault,
                                ConfigValidationReport report, T value) {
        this.valid = valid;
        this.usedDefault = usedDefault;
        this.report = Objects.requireNonNull(report, "report");
        this.value = value;
    }

    static <T> ResolvedConfigValue<T> valid(T value, boolean usedDefault) {
        return new ResolvedConfigValue<T>(true, usedDefault,
                ConfigValidationReport.empty(), Objects.requireNonNull(value, "value"));
    }

    static <T> ResolvedConfigValue<T> invalid(ConfigValidationReport report) {
        ConfigValidationReport checkedReport = Objects.requireNonNull(report, "report");
        if (!checkedReport.hasErrors()) {
            throw new IllegalArgumentException("invalid result requires an error report");
        }
        return new ResolvedConfigValue<T>(false, false, checkedReport, null);
    }

    /** @return whether this resolution can be consumed safely */
    public boolean isValid() {
        return valid;
    }

    /** @return whether the explicit key default supplied this valid value */
    public boolean usedDefault() {
        return usedDefault;
    }

    /** @return immutable validation report for this resolution */
    public ConfigValidationReport report() {
        return report;
    }

    /**
     * Returns the resolved value only for a valid result.
     *
     * @return accepted or default value
     * @throws IllegalStateException if this result is invalid
     */
    public T value() {
        if (!valid) {
            throw new IllegalStateException("configuration value is invalid");
        }
        return value;
    }
}

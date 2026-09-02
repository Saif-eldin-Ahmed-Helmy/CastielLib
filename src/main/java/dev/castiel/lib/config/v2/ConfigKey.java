package dev.castiel.lib.config.v2;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Immutable definition and resolver for one typed configuration value.
 *
 * <p>Missing input uses the validated explicit default. Supplied input is
 * checked by exact runtime type and then by the supplied predicate; invalid
 * input never falls back to the default.</p>
 *
 * @param <T> configuration value type
 */
public final class ConfigKey<T> {
    private static final Pattern PATH_PATTERN =
            Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");

    private final String path;
    private final Class<T> type;
    private final T defaultValue;
    private final Predicate<T> validator;
    private final String expected;
    private final String impact;
    private final String action;
    private final boolean sensitive;

    /**
     * Defines one typed configuration key with a validated explicit default.
     *
     * @param path stable lowercase segmented configuration path
     * @param type accepted runtime value type
     * @param defaultValue explicit value used only when input is missing
     * @param validator predicate for values of {@code type}
     * @param expected expected value or constraint shown to administrators
     * @param impact affected behavior or subsystem shown to administrators
     * @param action corrective action shown to administrators
     * @param sensitive whether invalid supplied values must be redacted
     * @throws NullPointerException if a required reference is null
     * @throws IllegalArgumentException if the definition is malformed or its
     *     default is the wrong type or fails validation
     */
    public ConfigKey(String path, Class<T> type, T defaultValue,
                     Predicate<T> validator, String expected, String impact,
                     String action, boolean sensitive) {
        this.path = requirePath(path);
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.expected = requireText(expected, "expected");
        this.impact = requireText(impact, "impact");
        this.action = requireText(action, "action");

        if (!type.isInstance(defaultValue)) {
            throw new IllegalArgumentException("defaultValue must be an instance of type");
        }
        if (!validator.test(defaultValue)) {
            throw new IllegalArgumentException("defaultValue fails validator");
        }
        this.sensitive = sensitive;
    }

    /** @return stable configuration path */
    public String path() {
        return path;
    }

    /** @return accepted runtime value type */
    public Class<T> type() {
        return type;
    }

    /** @return validated explicit default value */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Resolves one input without coercion or silent fallback.
     *
     * @param source logical configuration source label, such as
     *     {@code config.yml}
     * @param present whether the source supplied a value
     * @param suppliedValue source value; ignored when absent
     * @return valid value or one actionable validation error
     */
    public ResolvedConfigValue<T> resolve(String source, boolean present,
                                          Object suppliedValue) {
        if (!present) {
            return ResolvedConfigValue.valid(defaultValue, true);
        }

        if (!type.isInstance(suppliedValue)) {
            return ResolvedConfigValue.invalid(report(source, "config.type.invalid", suppliedValue));
        }

        T typedValue = type.cast(suppliedValue);
        if (!validator.test(typedValue)) {
            return ResolvedConfigValue.invalid(report(source, "config.value.invalid", suppliedValue));
        }
        return ResolvedConfigValue.valid(typedValue, false);
    }

    private ConfigValidationReport report(String source, String id, Object suppliedValue) {
        String displayed = sensitive ? "<redacted>" : String.valueOf(suppliedValue);
        ConfigValidationIssue issue = new ConfigValidationIssue(
                id, ValidationSeverity.ERROR, source, path, displayed,
                expected, impact, action);
        return ConfigValidationReport.of(Collections.singletonList(issue));
    }

    private static String requirePath(String value) {
        String path = requireText(value, "path");
        if (!PATH_PATTERN.matcher(path).matches()) {
            throw new IllegalArgumentException("path must match [a-z0-9]+(?:[._-][a-z0-9]+)*");
        }
        return path;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

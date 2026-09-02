package dev.castiel.lib.config.v2;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An immutable, actionable configuration validation problem.
 *
 * <p>{@code suppliedValue} is display-safe text supplied by the caller. Callers
 * must redact secrets and private values (for example, as {@code <redacted>})
 * before constructing an issue. This type deliberately accepts only text and
 * never retains raw objects, throwables, credentials, or player data.</p>
 */
public final class ConfigValidationIssue {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");

    private final String id;
    private final ValidationSeverity severity;
    private final String source;
    private final String path;
    private final String suppliedValue;
    private final String expected;
    private final String impact;
    private final String action;

    /**
     * Creates an issue with the location, safe supplied value, expectation,
     * impact, and corrective action needed by an administrator.
     *
     * @param id stable lowercase machine-readable issue identifier
     * @param severity issue severity
     * @param source logical configuration source label, not an absolute host path
     * @param path typed configuration path
     * @param suppliedValue display-safe supplied value; redact secrets first
     * @param expected expected value or constraint
     * @param impact affected behavior or subsystem
     * @param action specific next corrective action
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if a text argument is blank, the ID is malformed,
     *     or the source is an absolute host path
     */
    public ConfigValidationIssue(String id, ValidationSeverity severity, String source,
                                 String path, String suppliedValue, String expected,
                                 String impact, String action) {
        this.id = requireId(id);
        this.severity = Objects.requireNonNull(severity, "severity");
        this.source = requireLogicalSource(source);
        this.path = requireText(path, "path");
        this.suppliedValue = requireText(suppliedValue, "suppliedValue");
        this.expected = requireText(expected, "expected");
        this.impact = requireText(impact, "impact");
        this.action = requireText(action, "action");
    }

    /** @return stable machine-readable issue identifier */
    public String id() {
        return id;
    }

    /** @return issue severity */
    public ValidationSeverity severity() {
        return severity;
    }

    /** @return logical configuration source label */
    public String source() {
        return source;
    }

    /** @return typed configuration path */
    public String path() {
        return path;
    }

    /** @return caller-supplied display-safe value */
    public String suppliedValue() {
        return suppliedValue;
    }

    /** @return expected value or constraint */
    public String expected() {
        return expected;
    }

    /** @return affected behavior or subsystem */
    public String impact() {
        return impact;
    }

    /** @return corrective action for an administrator */
    public String action() {
        return action;
    }

    /**
     * Returns identifying fields only. The supplied value is intentionally
     * omitted so generic logs cannot accidentally expose it.
     *
     * @return safe issue summary
     */
    @Override
    public String toString() {
        return "ConfigValidationIssue{" +
                "id='" + id + '\'' +
                ", severity=" + severity +
                ", source='" + source + '\'' +
                ", path='" + path + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigValidationIssue)) {
            return false;
        }
        ConfigValidationIssue that = (ConfigValidationIssue) other;
        return id.equals(that.id)
                && severity == that.severity
                && source.equals(that.source)
                && path.equals(that.path)
                && suppliedValue.equals(that.suppliedValue)
                && expected.equals(that.expected)
                && impact.equals(that.impact)
                && action.equals(that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, severity, source, path, suppliedValue, expected, impact, action);
    }

    private static String requireId(String value) {
        String id = requireText(value, "id");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("id must match [a-z0-9]+(?:[._-][a-z0-9]+)*");
        }
        return id;
    }

    private static String requireLogicalSource(String value) {
        String source = requireText(value, "source");
        if (isAbsolutePath(source)) {
            throw new IllegalArgumentException("source must be a logical label, not an absolute host path");
        }
        return source;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static boolean isAbsolutePath(String value) {
        if (value.startsWith("/") || value.startsWith("\\")) {
            return true;
        }
        return value.length() >= 3
                && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':'
                && (value.charAt(2) == '/' || value.charAt(2) == '\\');
    }
}

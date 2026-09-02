package dev.castiel.lib.config.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable ordered collection of configuration validation issues. */
public final class ConfigValidationReport {
    private final List<ConfigValidationIssue> issues;
    private final List<ConfigValidationIssue> errors;
    private final List<ConfigValidationIssue> warnings;

    private ConfigValidationReport(List<ConfigValidationIssue> issues) {
        List<ConfigValidationIssue> copy = new ArrayList<ConfigValidationIssue>(issues.size());
        for (ConfigValidationIssue issue : issues) {
            copy.add(Objects.requireNonNull(issue, "issues element"));
        }
        this.issues = immutable(copy);

        List<ConfigValidationIssue> errorCopy = new ArrayList<ConfigValidationIssue>();
        List<ConfigValidationIssue> warningCopy = new ArrayList<ConfigValidationIssue>();
        for (ConfigValidationIssue issue : copy) {
            if (issue.severity() == ValidationSeverity.ERROR) {
                errorCopy.add(issue);
            } else if (issue.severity() == ValidationSeverity.WARNING) {
                warningCopy.add(issue);
            }
        }
        this.errors = immutable(errorCopy);
        this.warnings = immutable(warningCopy);
    }

    /** @return a valid report containing no issues */
    public static ConfigValidationReport empty() {
        return new ConfigValidationReport(Collections.<ConfigValidationIssue>emptyList());
    }

    /**
     * Creates a report from issues in encounter order.
     *
     * @param issues issues to copy
     * @return immutable validation report
     * @throws NullPointerException if the list or an element is null
     */
    public static ConfigValidationReport of(List<ConfigValidationIssue> issues) {
        return new ConfigValidationReport(new ArrayList<ConfigValidationIssue>(
                Objects.requireNonNull(issues, "issues")));
    }

    /** @return all issues in encounter order */
    public List<ConfigValidationIssue> issues() {
        return issues;
    }

    /** @return error issues in encounter order */
    public List<ConfigValidationIssue> errors() {
        return errors;
    }

    /** @return warning issues in encounter order */
    public List<ConfigValidationIssue> warnings() {
        return warnings;
    }

    /** @return whether at least one error is present */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /** @return whether this report contains no errors */
    public boolean isValid() {
        return !hasErrors();
    }

    private static List<ConfigValidationIssue> immutable(List<ConfigValidationIssue> source) {
        return Collections.unmodifiableList(new ArrayList<ConfigValidationIssue>(source));
    }
}

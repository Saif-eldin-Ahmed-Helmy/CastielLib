package dev.castiel.lib.config.v2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValidationReportTest {
    private static final String SECRET = "super-secret-password";

    @Test
    void issueRequiresEveryFieldAndStableId() {
        assertThrows(NullPointerException.class, () -> issue(null, ValidationSeverity.ERROR, "config.yml", "storage.pool-size", "4", "at least 1", "storage unavailable", "set a positive value"));
        assertThrows(NullPointerException.class, () -> issue("config.invalid", null, "config.yml", "storage.pool-size", "4", "at least 1", "storage unavailable", "set a positive value"));
        assertThrows(NullPointerException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, null, "storage.pool-size", "4", "at least 1", "storage unavailable", "set a positive value"));
        assertThrows(NullPointerException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", null, "4", "at least 1", "storage unavailable", "set a positive value"));
        assertThrows(NullPointerException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "storage.pool-size", null, "at least 1", "storage unavailable", "set a positive value"));
        assertThrows(NullPointerException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "storage.pool-size", "4", null, "storage unavailable", "set a positive value"));
        assertThrows(NullPointerException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "storage.pool-size", "4", "at least 1", null, "set a positive value"));
        assertThrows(NullPointerException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "storage.pool-size", "4", "at least 1", "storage unavailable", null));

        String[] blankValues = {"", "   "};
        for (String blank : blankValues) {
            assertThrows(IllegalArgumentException.class, () -> issue(blank, ValidationSeverity.ERROR, "config.yml", "path", "value", "expected", "impact", "action"));
            assertThrows(IllegalArgumentException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, blank, "path", "value", "expected", "impact", "action"));
            assertThrows(IllegalArgumentException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", blank, "value", "expected", "impact", "action"));
            assertThrows(IllegalArgumentException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "path", blank, "expected", "impact", "action"));
            assertThrows(IllegalArgumentException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "path", "value", blank, "impact", "action"));
            assertThrows(IllegalArgumentException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "path", "value", "expected", blank, "action"));
            assertThrows(IllegalArgumentException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "path", "value", "expected", "impact", blank));
        }

        for (String malformed : Arrays.asList("Config.Invalid", "config..invalid", "config/invalid", "config invalid", "config_")) {
            assertThrows(IllegalArgumentException.class, () -> issue(malformed, ValidationSeverity.ERROR, "config.yml", "path", "value", "expected", "impact", "action"));
        }
        assertThrows(IllegalArgumentException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "/etc/config.yml", "path", "value", "expected", "impact", "action"));
        assertThrows(IllegalArgumentException.class, () -> issue("config.invalid", ValidationSeverity.ERROR, "C:\\private\\config.yml", "path", "value", "expected", "impact", "action"));

        ConfigValidationIssue valid = issue("config.range-invalid", ValidationSeverity.WARNING, "config.yml", "storage.pool-size", "<redacted>", "at least 1", "storage unavailable", "set a positive value");
        assertEquals("config.range-invalid", valid.id());
        assertEquals("<redacted>", valid.suppliedValue());
        assertEquals("config.yml", valid.source());
        assertNotNull(valid.action());
    }

    @Test
    void issueHasValueEqualityAndSafeStringRendering() {
        ConfigValidationIssue first = issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "database.password", SECRET, "a value", "database unavailable", "replace it");
        ConfigValidationIssue same = issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "database.password", SECRET, "a value", "database unavailable", "replace it");
        ConfigValidationIssue different = issue("config.invalid", ValidationSeverity.ERROR, "config.yml", "database.password", "<redacted>", "a value", "database unavailable", "replace it");
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, different);
        assertFalse(first.toString().contains(SECRET));
        assertTrue(first.toString().contains("config.invalid"));
    }

    @Test
    void reportCopiesInputPreservesOrderAndExposesImmutableLists() {
        ConfigValidationIssue warning = issue("config.warning", ValidationSeverity.WARNING, "config.yml", "feature.enabled", "true", "false", "feature enabled", "review setting");
        ConfigValidationIssue error = issue("config.error", ValidationSeverity.ERROR, "config.yml", "storage.pool-size", "0", "at least 1", "storage unavailable", "set a positive value");
        List<ConfigValidationIssue> input = new ArrayList<ConfigValidationIssue>(Arrays.asList(warning, error));
        ConfigValidationReport report = ConfigValidationReport.of(input);
        input.clear();

        assertEquals(Arrays.asList(warning, error), report.issues());
        assertEquals(Arrays.asList(error), report.errors());
        assertEquals(Arrays.asList(warning), report.warnings());
        assertTrue(report.hasErrors());
        assertFalse(report.isValid());
        assertThrows(UnsupportedOperationException.class, () -> report.issues().add(warning));
        assertThrows(UnsupportedOperationException.class, () -> report.errors().clear());
        assertThrows(UnsupportedOperationException.class, () -> report.warnings().remove(warning));
    }

    @Test
    void reportRejectsNullListsAndElementsAndWarningsAreValid() {
        assertThrows(NullPointerException.class, () -> ConfigValidationReport.of(null));
        assertThrows(NullPointerException.class, () -> ConfigValidationReport.of(Arrays.asList((ConfigValidationIssue) null)));

        ConfigValidationReport empty = ConfigValidationReport.empty();
        assertTrue(empty.isValid());
        assertFalse(empty.hasErrors());
        assertTrue(empty.issues().isEmpty());

        ConfigValidationReport warnings = ConfigValidationReport.of(Arrays.asList(
                issue("config.warning-one", ValidationSeverity.WARNING, "config.yml", "one", "x", "y", "impact", "action"),
                issue("config.warning-two", ValidationSeverity.WARNING, "config.yml", "two", "x", "y", "impact", "action")));
        assertTrue(warnings.isValid());
        assertFalse(warnings.hasErrors());
        assertEquals(2, warnings.warnings().size());
        assertTrue(warnings.errors().isEmpty());
    }

    private static ConfigValidationIssue issue(String id, ValidationSeverity severity, String source,
                                                String path, String suppliedValue, String expected,
                                                String impact, String action) {
        return new ConfigValidationIssue(id, severity, source, path, suppliedValue, expected, impact, action);
    }
}

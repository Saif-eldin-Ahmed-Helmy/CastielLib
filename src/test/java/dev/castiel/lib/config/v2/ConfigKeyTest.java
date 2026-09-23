package dev.castiel.lib.config.v2;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigKeyTest {
    private static final String SOURCE = "config.yml";
    private static final String EXPECTED = "at least 1";
    private static final String IMPACT = "storage unavailable";
    private static final String ACTION = "set a positive value";

    @Test
    void constructorRejectsMalformedDefinitionsAndUnsafeDefaults() {
        assertThrows(NullPointerException.class, () -> new ConfigKey<String>(
                null, String.class, "default", value -> true, EXPECTED, IMPACT, ACTION, false));
        assertThrows(NullPointerException.class, () -> new ConfigKey<String>(
                "storage.pool-size", null, "default", value -> true, EXPECTED, IMPACT, ACTION, false));
        assertThrows(NullPointerException.class, () -> new ConfigKey<String>(
                "storage.pool-size", String.class, null, value -> true, EXPECTED, IMPACT, ACTION, false));
        assertThrows(NullPointerException.class, () -> new ConfigKey<String>(
                "storage.pool-size", String.class, "default", null, EXPECTED, IMPACT, ACTION, false));

        for (String blank : Arrays.asList("", "   ")) {
            assertThrows(IllegalArgumentException.class, () -> key(blank, value -> true));
            assertThrows(IllegalArgumentException.class, () -> new ConfigKey<String>(
                    "storage.pool-size", String.class, "default", value -> true,
                    blank, IMPACT, ACTION, false));
            assertThrows(IllegalArgumentException.class, () -> new ConfigKey<String>(
                    "storage.pool-size", String.class, "default", value -> true,
                    EXPECTED, blank, ACTION, false));
            assertThrows(IllegalArgumentException.class, () -> new ConfigKey<String>(
                    "storage.pool-size", String.class, "default", value -> true,
                    EXPECTED, IMPACT, blank, false));
        }

        for (String malformed : Arrays.asList(
                "Storage.pool-size", "storage..pool-size", "storage/pool-size",
                "storage pool-size", "storage_")) {
            assertThrows(IllegalArgumentException.class, () -> key(malformed, value -> true));
        }

        assertThrows(IllegalArgumentException.class, () -> new ConfigKey<String>(
                "storage.pool-size", String.class, "default", value -> false,
                EXPECTED, IMPACT, ACTION, false));

        Predicate<Object> accepts = value -> true;
        assertThrows(IllegalArgumentException.class, () -> new ConfigKey(
                "storage.pool-size", String.class, Integer.valueOf(1), accepts,
                EXPECTED, IMPACT, ACTION, false));
    }

    @Test
    void missingInputUsesOnlyTheValidatedExplicitDefault() {
        String defaultValue = new String("default");
        ConfigKey<String> key = key("storage.pool-size", value -> true, defaultValue);

        ResolvedConfigValue<String> result = key.resolve(SOURCE, false, new Object());

        assertTrue(result.isValid());
        assertTrue(result.usedDefault());
        assertSame(defaultValue, result.value());
        assertTrue(result.report().isValid());
        assertTrue(result.report().issues().isEmpty());
    }

    @Test
    void exactTypeInputIsReturnedUnchangedWithoutTheDefault() {
        String supplied = new String("supplied");
        String defaultValue = new String("default");
        ConfigKey<String> key = key("feature.name", value -> true, defaultValue);

        ResolvedConfigValue<String> result = key.resolve(SOURCE, true, supplied);

        assertTrue(result.isValid());
        assertFalse(result.usedDefault());
        assertSame(supplied, result.value());
        assertTrue(result.report().issues().isEmpty());
    }

    @Test
    void nullAndWrongTypeFailWithTypeIssueWithoutValidationOrFallback() {
        AtomicInteger validations = new AtomicInteger();
        ConfigKey<String> key = key("feature.name", value -> {
            validations.incrementAndGet();
            return true;
        }, "default");

        ResolvedConfigValue<String> nullResult = key.resolve(SOURCE, true, null);
        ResolvedConfigValue<String> wrongTypeResult = key.resolve(SOURCE, true, Integer.valueOf(4));

        assertInvalidTypeResult(nullResult);
        assertInvalidTypeResult(wrongTypeResult);
        assertEquals(1, validations.get());
    }

    @Test
    void predicateRejectionProducesOneActionableValueIssue() {
        ConfigKey<Integer> key = new ConfigKey<Integer>(
                "storage.pool-size", Integer.class, 4, value -> value.intValue() >= 1,
                EXPECTED, IMPACT, ACTION, false);

        ResolvedConfigValue<Integer> result = key.resolve(SOURCE, true, Integer.valueOf(0));

        assertFalse(result.isValid());
        assertFalse(result.usedDefault());
        assertThrows(IllegalStateException.class, result::value);
        assertEquals(1, result.report().issues().size());
        ConfigValidationIssue issue = result.report().issues().get(0);
        assertEquals("config.value.invalid", issue.id());
        assertEquals(SOURCE, issue.source());
        assertEquals("storage.pool-size", issue.path());
        assertEquals("0", issue.suppliedValue());
        assertEquals(EXPECTED, issue.expected());
        assertEquals(IMPACT, issue.impact());
        assertEquals(ACTION, issue.action());
    }

    @Test
    void sensitiveFailuresRedactAndNonSensitiveFailuresDisplaySafely() {
        String secret = "super-secret-password";
        ConfigKey<String> sensitiveKey = key("database.password",
                value -> "safe-default".equals(value), "safe-default", true);

        ResolvedConfigValue<String> sensitiveResult = sensitiveKey.resolve(SOURCE, true, secret);
        ConfigValidationIssue sensitiveIssue = sensitiveResult.report().issues().get(0);

        assertEquals("<redacted>", sensitiveIssue.suppliedValue());
        assertFalse(sensitiveIssue.toString().contains(secret));
        assertFalse(sensitiveResult.report().toString().contains(secret));
        assertFalse(sensitiveResult.toString().contains(secret));
        assertThrows(IllegalStateException.class, sensitiveResult::value);

        ConfigKey<String> nonSensitiveKey = key("feature.name",
                value -> "safe-default".equals(value), "safe-default", false);
        ResolvedConfigValue<String> nonSensitiveResult = nonSensitiveKey.resolve(SOURCE, true, secret);
        assertEquals(secret, nonSensitiveResult.report().issues().get(0).suppliedValue());
    }

    @Test
    void validatorExceptionsPropagateUnchanged() {
        IllegalStateException failure = new IllegalStateException("validator defect");
        ConfigKey<String> key = key("feature.name", value -> {
            if ("supplied".equals(value)) {
                throw failure;
            }
            return true;
        }, "default", true);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> key.resolve(SOURCE, true, "supplied")));
    }

    private static void assertInvalidTypeResult(ResolvedConfigValue<String> result) {
        assertFalse(result.isValid());
        assertFalse(result.usedDefault());
        assertThrows(IllegalStateException.class, result::value);
        assertEquals(1, result.report().issues().size());
        assertEquals("config.type.invalid", result.report().issues().get(0).id());
    }

    private static ConfigKey<String> key(String path, Predicate<String> validator) {
        return key(path, validator, "default", false);
    }

    private static ConfigKey<String> key(String path, Predicate<String> validator, String defaultValue) {
        return key(path, validator, defaultValue, false);
    }

    private static ConfigKey<String> key(String path, Predicate<String> validator,
                                         String defaultValue, boolean sensitive) {
        return new ConfigKey<String>(path, String.class, defaultValue, validator,
                EXPECTED, IMPACT, ACTION, sensitive);
    }
}

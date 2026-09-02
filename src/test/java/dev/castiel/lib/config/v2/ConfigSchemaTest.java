package dev.castiel.lib.config.v2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSchemaTest {
    private static final String SOURCE = "config.yml";
    private static final String EXPECTED = "a positive value";
    private static final String IMPACT = "setting is unavailable";
    private static final String ACTION = "correct the setting";

    @Test
    void schemaCopiesOrderAndRejectsInvalidDefinitions() {
        ConfigKey<String> first = key("first", "first-default");
        ConfigKey<Integer> second = intKey("second", 2);
        List<ConfigKey<?>> input = new ArrayList<ConfigKey<?>>(Arrays.<ConfigKey<?>>asList(first, second));
        ConfigSchema schema = ConfigSchema.of(input);
        input.clear();

        assertEquals(Arrays.<ConfigKey<?>>asList(first, second), schema.keys());
        assertThrows(UnsupportedOperationException.class, () -> schema.keys().clear());
        assertEquals(0, ConfigSchema.of(new ArrayList<ConfigKey<?>>()).keys().size());
        assertThrows(NullPointerException.class, () -> ConfigSchema.of(null));
        assertThrows(NullPointerException.class, () -> ConfigSchema.of(Arrays.<ConfigKey<?>>asList(first, null)));
        assertThrows(IllegalArgumentException.class, () -> ConfigSchema.of(Arrays.<ConfigKey<?>>asList(first,
                key("first", "another-default"))));
    }

    @Test
    void validResolutionReturnsExactValuesDefaultsAndLeavesInputUntouched() {
        String supplied = new String("supplied");
        String defaultName = new String("default-name");
        Integer defaultCount = Integer.valueOf(4);
        ConfigKey<String> name = key("feature.name", defaultName);
        ConfigKey<Integer> count = intKey("feature.count", defaultCount);
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("feature.name", supplied);
        Map<String, Object> before = new LinkedHashMap<String, Object>(input);

        ConfigSnapshot snapshot = ConfigSchema.of(Arrays.<ConfigKey<?>>asList(name, count)).resolve(SOURCE, input);

        assertTrue(snapshot.isValid());
        assertSame(supplied, snapshot.get(name));
        assertFalse(snapshot.usedDefault(name));
        assertSame(defaultCount, snapshot.get(count));
        assertTrue(snapshot.usedDefault(count));
        assertTrue(snapshot.report().issues().isEmpty());
        assertEquals(before, input);
    }

    @Test
    void allDeclaredFailuresAreReportedAndSnapshotFailsClosed() {
        ConfigKey<Integer> count = intKey("feature.count", 4);
        ConfigKey<String> mode = new ConfigKey<String>("feature.mode", String.class, "safe",
                value -> "safe".equals(value) || "fast".equals(value), EXPECTED, IMPACT, ACTION, false);
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("feature.count", "wrong-type");
        input.put("feature.mode", "invalid");
        ConfigSnapshot snapshot = ConfigSchema.of(Arrays.<ConfigKey<?>>asList(count, mode)).resolve(SOURCE, input);

        assertFalse(snapshot.isValid());
        assertEquals(2, snapshot.report().errors().size());
        assertEquals("feature.count", snapshot.report().issues().get(0).path());
        assertEquals("feature.mode", snapshot.report().issues().get(1).path());
        assertThrows(IllegalStateException.class, () -> snapshot.get(count));
        assertThrows(IllegalStateException.class, () -> snapshot.usedDefault(count));
    }

    @Test
    void unknownWarningsAreSortedSafeAndDoNotReadRawValues() {
        RawSentinel first = new RawSentinel();
        RawSentinel second = new RawSentinel();
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("zeta", first);
        input.put("alpha", second);
        Map<String, Object> before = new LinkedHashMap<String, Object>(input);
        ConfigKey<String> known = key("known", "default");

        ConfigSnapshot snapshot = ConfigSchema.of(Arrays.<ConfigKey<?>>asList(known)).resolve(SOURCE, input);

        assertTrue(snapshot.isValid());
        assertSame("default", snapshot.get(known));
        assertEquals(2, snapshot.report().warnings().size());
        assertEquals("alpha", snapshot.report().warnings().get(0).path());
        assertEquals("zeta", snapshot.report().warnings().get(1).path());
        for (ConfigValidationIssue issue : snapshot.report().warnings()) {
            assertEquals("config.key.unknown", issue.id());
            assertEquals("<not-read>", issue.suppliedValue());
        }
        assertEquals(before, input);
        assertEquals(0, first.toStringCalls + second.toStringCalls);
    }

    @Test
    void accessRequiresExactRegisteredIdentityAndValidSnapshot() {
        ConfigKey<String> registered = key("feature.name", "default");
        ConfigKey<String> recreated = key("feature.name", "default");
        ConfigSnapshot snapshot = ConfigSchema.of(Arrays.<ConfigKey<?>>asList(registered))
                .resolve(SOURCE, new HashMap<String, Object>());

        assertSame("default", snapshot.get(registered));
        assertTrue(snapshot.usedDefault(registered));
        assertThrows(NullPointerException.class, () -> snapshot.get(null));
        assertThrows(NullPointerException.class, () -> snapshot.usedDefault(null));
        assertThrows(IllegalStateException.class, () -> snapshot.get(recreated));
        assertThrows(IllegalStateException.class, () -> snapshot.usedDefault(recreated));
    }

    @Test
    void sourceAndMapKeysAreValidatedBeforeResolution() {
        ConfigSchema schema = ConfigSchema.of(Arrays.<ConfigKey<?>>asList(key("known", "default")));
        assertThrows(NullPointerException.class, () -> schema.resolve(null, new HashMap<String, Object>()));
        assertThrows(IllegalArgumentException.class, () -> schema.resolve(" ", new HashMap<String, Object>()));
        assertThrows(IllegalArgumentException.class, () -> schema.resolve("/etc/config.yml", new HashMap<String, Object>()));
        assertThrows(IllegalArgumentException.class, () -> schema.resolve("C:\\config.yml", new HashMap<String, Object>()));
        assertThrows(NullPointerException.class, () -> schema.resolve(SOURCE, null));
        Map<String, Object> nullKey = new HashMap<String, Object>();
        nullKey.put(null, "value");
        assertThrows(NullPointerException.class, () -> schema.resolve(SOURCE, nullKey));
        Map<String, Object> blankKey = new HashMap<String, Object>();
        blankKey.put(" ", "value");
        assertThrows(IllegalArgumentException.class, () -> schema.resolve(SOURCE, blankKey));
    }

    private static ConfigKey<String> key(String path, String defaultValue) {
        return new ConfigKey<String>(path, String.class, defaultValue, value -> true,
                EXPECTED, IMPACT, ACTION, false);
    }

    private static ConfigKey<Integer> intKey(String path, Integer defaultValue) {
        Predicate<Integer> validator = value -> value.intValue() >= 1;
        return new ConfigKey<Integer>(path, Integer.class, defaultValue, validator,
                EXPECTED, IMPACT, ACTION, false);
    }

    private static final class RawSentinel {
        private int toStringCalls;

        @Override
        public String toString() {
            toStringCalls++;
            throw new AssertionError("unknown raw value was stringified");
        }
    }
}

package dev.castiel.lib.config.v2;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDocumentTest {
    private static final String SOURCE = "config.yml";
    private static final String EXPECTED = "a positive value";
    private static final String IMPACT = "feature unavailable";
    private static final String ACTION = "correct the setting";

    @Test
    void factoriesValidateAndCopyEncounterOrderIncludingExplicitNulls() {
        LinkedHashMap<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("known.first", "one");
        input.put("unknown", null);
        input.put("known.last", Integer.valueOf(3));

        ConfigDocument document = ConfigDocument.of(SOURCE, 2, input);
        input.put("later", "not copied");

        assertEquals(Arrays.asList("known.first", "unknown", "known.last"),
                asList(document.values().keySet()));
        assertTrue(document.contains("unknown"));
        assertEquals(null, document.value("unknown"));
        assertFalse(document.contains("later"));
        assertThrows(UnsupportedOperationException.class,
                () -> document.values().put("another", "value"));

        assertThrows(NullPointerException.class, () -> ConfigDocument.fresh(null, 0));
        assertThrows(IllegalArgumentException.class, () -> ConfigDocument.fresh(" ", 0));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigDocument.fresh("/etc/config.yml", 0));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigDocument.fresh("C:\\config.yml", 0));
        assertThrows(IllegalArgumentException.class, () -> ConfigDocument.fresh(SOURCE, -1));
        assertThrows(NullPointerException.class,
                () -> ConfigDocument.of(SOURCE, 0, null));

        LinkedHashMap<String, Object> nullKey = new LinkedHashMap<String, Object>();
        nullKey.put(null, "value");
        assertThrows(NullPointerException.class, () -> ConfigDocument.of(SOURCE, 0, nullKey));
        LinkedHashMap<String, Object> blankKey = new LinkedHashMap<String, Object>();
        blankKey.put(" ", "value");
        assertThrows(IllegalArgumentException.class,
                () -> ConfigDocument.of(SOURCE, 0, blankKey));
    }

    @Test
    void freshDocumentResolvesValidatedDefaultsWithoutRawEntries() {
        ConfigKey<String> name = key("feature.name", "default-name");
        ConfigKey<Integer> count = new ConfigKey<Integer>(
                "feature.count", Integer.class, Integer.valueOf(4),
                value -> value.intValue() >= 1, EXPECTED, IMPACT, ACTION, false);
        ConfigDocument document = ConfigDocument.fresh(SOURCE, 0);

        ConfigSnapshot snapshot = document.resolve(ConfigSchema.of(Arrays.<ConfigKey<?>>asList(name, count)));

        assertTrue(snapshot.isValid());
        assertEquals("default-name", snapshot.get(name));
        assertEquals(Integer.valueOf(4), snapshot.get(count));
        assertTrue(snapshot.usedDefault(name));
        assertTrue(snapshot.usedDefault(count));
        assertTrue(snapshot.report().issues().isEmpty());
        assertTrue(document.values().isEmpty());
    }

    @Test
    void resolutionAggregatesInvalidKnownValuesAndLeavesRawEntriesUntouched() {
        ConfigKey<Integer> count = new ConfigKey<Integer>(
                "feature.count", Integer.class, Integer.valueOf(4),
                value -> value.intValue() >= 1, EXPECTED, IMPACT, ACTION, false);
        ConfigKey<String> mode = new ConfigKey<String>(
                "feature.mode", String.class, "safe",
                value -> "safe".equals(value), EXPECTED, IMPACT, ACTION, false);
        LinkedHashMap<String, Object> raw = new LinkedHashMap<String, Object>();
        raw.put("feature.count", Integer.valueOf(0));
        raw.put("feature.mode", "invalid");
        ConfigDocument document = ConfigDocument.of(SOURCE, 3, raw);

        ConfigSnapshot snapshot = document.resolve(ConfigSchema.of(
                Arrays.<ConfigKey<?>>asList(count, mode)));

        assertFalse(snapshot.isValid());
        assertEquals(2, snapshot.report().errors().size());
        assertEquals(Integer.valueOf(0), document.value("feature.count"));
        assertEquals("invalid", document.value("feature.mode"));
        assertThrows(IllegalStateException.class, () -> snapshot.get(count));
        assertThrows(IllegalStateException.class, () -> snapshot.get(mode));
    }

    @Test
    void unknownValuesRemainExactAndAreNeverStringified() {
        ThrowingValue unknown = new ThrowingValue();
        ConfigDocument document = ConfigDocument.fresh(SOURCE, 0)
                .withValue("unknown", unknown);
        ConfigSnapshot snapshot = document.resolve(ConfigSchema.of(Arrays.<ConfigKey<?>>asList(
                key("known", "default"))));

        assertSame(unknown, document.value("unknown"));
        assertSame(unknown, document.withValue("known", "value").value("unknown"));
        assertSame(unknown, document.advanceTo(1).value("unknown"));
        assertSame(unknown, document.withValue("known", "value")
                .withoutValue("known").value("unknown"));
        assertEquals("<not-read>", snapshot.report().warnings().get(0).suppliedValue());
        assertFalse(document.toString().contains("unknown-value"));
        assertFalse(snapshot.toString().contains("unknown-value"));
        assertFalse(snapshot.report().toString().contains("unknown-value"));
    }

    @Test
    void copyUpdatesPreserveOrderAndOriginalDocument() {
        ThrowingValue unknown = new ThrowingValue();
        ConfigDocument original = ConfigDocument.of(SOURCE, 1,
                ordered("first", "value", "unknown", unknown, "last", "end"));

        ConfigDocument replaced = original.withValue("unknown", "replacement");
        ConfigDocument appended = replaced.withValue("new", null);
        ConfigDocument removed = appended.withoutValue("first");

        assertEquals(Arrays.asList("first", "unknown", "last"), asList(original.values().keySet()));
        assertSame(unknown, original.value("unknown"));
        assertEquals(Arrays.asList("first", "unknown", "last"), asList(replaced.values().keySet()));
        assertEquals("replacement", replaced.value("unknown"));
        assertEquals(Arrays.asList("first", "unknown", "last", "new"), asList(appended.values().keySet()));
        assertTrue(appended.contains("new"));
        assertEquals(Arrays.asList("unknown", "last", "new"), asList(removed.values().keySet()));
        assertFalse(removed.contains("first"));
        assertThrows(IllegalArgumentException.class, () -> original.withoutValue("missing"));
        assertThrows(NullPointerException.class, () -> original.withValue(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> original.withValue(" ", "value"));
        assertThrows(NullPointerException.class, () -> original.withoutValue(null));
    }

    @Test
    void advanceToRequiresExactlyOneForwardVersionAndPreservesIdentity() {
        ThrowingValue unknown = new ThrowingValue();
        ConfigDocument original = ConfigDocument.of(SOURCE, 4,
                ordered("unknown", unknown, "known", "value"));
        ConfigDocument advanced = original.advanceTo(5);

        assertEquals(4, original.schemaVersion());
        assertEquals(5, advanced.schemaVersion());
        assertEquals(SOURCE, advanced.source());
        assertEquals(Arrays.asList("unknown", "known"), asList(advanced.values().keySet()));
        assertSame(unknown, advanced.value("unknown"));
        assertThrows(IllegalArgumentException.class, () -> original.advanceTo(6));
        assertThrows(IllegalArgumentException.class, () -> original.advanceTo(4));
        assertThrows(IllegalArgumentException.class, () -> original.advanceTo(3));
        assertThrows(IllegalArgumentException.class, () -> original.advanceTo(-1));
        assertEquals(4, original.schemaVersion());
    }

    @Test
    void pathsAreValidatedAndToStringIsStructuralOnly() {
        ConfigDocument document = ConfigDocument.fresh(SOURCE, 7)
                .withValue("database.password", "super-secret-password");

        assertThrows(NullPointerException.class, () -> document.contains(null));
        assertThrows(IllegalArgumentException.class, () -> document.contains(" "));
        assertThrows(NullPointerException.class, () -> document.value(null));
        assertThrows(IllegalArgumentException.class, () -> document.value(" "));
        assertTrue(document.toString().contains(SOURCE));
        assertTrue(document.toString().contains("schemaVersion=7"));
        assertTrue(document.toString().contains("keyCount=1"));
        assertFalse(document.toString().contains("database.password"));
        assertFalse(document.toString().contains("super-secret-password"));
    }

    private static ConfigKey<String> key(String path, String defaultValue) {
        return new ConfigKey<String>(path, String.class, defaultValue,
                value -> true, EXPECTED, IMPACT, ACTION, false);
    }

    private static LinkedHashMap<String, Object> ordered(String firstPath, Object firstValue,
                                                          String secondPath, Object secondValue,
                                                          String thirdPath, Object thirdValue) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put(firstPath, firstValue);
        values.put(secondPath, secondValue);
        values.put(thirdPath, thirdValue);
        return values;
    }

    private static LinkedHashMap<String, Object> ordered(String firstPath, Object firstValue,
                                                          String secondPath, Object secondValue) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put(firstPath, firstValue);
        values.put(secondPath, secondValue);
        return values;
    }

    private static List<String> asList(Iterable<String> values) {
        java.util.ArrayList<String> result = new java.util.ArrayList<String>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    private static final class ThrowingValue {
        @Override
        public String toString() {
            throw new AssertionError("unknown raw value was stringified");
        }
    }
}

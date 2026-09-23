package dev.castiel.lib.config.v2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {
    private static final String SOURCE = "config.yml";

    @Test
    void constructorValidatesCompletePlanAndSnapshotsCallerCollections() {
        MutableMigration zero = new MutableMigration(0, set("known"),
                document -> document.withValue("known", "one").advanceTo(1));
        MutableMigration one = new MutableMigration(1, set("known"),
                document -> document.withValue("known", "two").advanceTo(2));
        List<ConfigMigration> plan = new ArrayList<ConfigMigration>(Arrays.<ConfigMigration>asList(one, zero));
        Set<String> mutablePaths = zero.paths;
        ConfigMigrator migrator = new ConfigMigrator(2, plan);
        plan.clear();
        mutablePaths.clear();

        ConfigMigrationResult result = migrator.migrate(ConfigDocument.fresh(SOURCE, 0));

        assertTrue(result.isSuccessful());
        assertEquals("two", result.document().value("known"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.document().values().put("new", "value"));

        assertThrows(IllegalArgumentException.class,
                () -> new ConfigMigrator(-1, Collections.<ConfigMigration>emptyList()));
        assertThrows(NullPointerException.class, () -> new ConfigMigrator(0, null));
        assertThrows(NullPointerException.class,
                () -> new ConfigMigrator(1, Arrays.<ConfigMigration>asList((ConfigMigration) null)));
        assertThrows(NullPointerException.class,
                () -> new ConfigMigrator(1, Arrays.<ConfigMigration>asList(
                        new MutableMigration(0, null, null))));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigMigrator(2, Collections.<ConfigMigration>singletonList(zero)));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigMigrator(1, Arrays.<ConfigMigration>asList(zero, zero)));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigMigrator(1, Collections.<ConfigMigration>singletonList(
                        new MutableMigration(-1, set("known"), null))));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigMigrator(1, Collections.<ConfigMigration>singletonList(
                        new MutableMigration(1, set("known"), null))));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigMigrator(1, Collections.<ConfigMigration>singletonList(
                        new MutableMigration(0, set("bad/path"), null))));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigMigrator(0, Collections.<ConfigMigration>singletonList(zero)));
    }

    @Test
    void targetIsNoOpAndFutureVersionFailsWithoutCallingSteps() {
        AtomicInteger calls = new AtomicInteger();
        ConfigMigration migration = migration(0, set("known"), document -> {
            calls.incrementAndGet();
            return document.advanceTo(1);
        });
        ConfigMigrator migrator = new ConfigMigrator(1,
                Collections.singletonList(migration));
        ConfigDocument current = ConfigDocument.fresh(SOURCE, 1);

        ConfigMigrationResult currentResult = migrator.migrate(current);
        ConfigMigrationResult futureResult = migrator.migrate(ConfigDocument.fresh(SOURCE, 2));

        assertTrue(currentResult.isSuccessful());
        assertSame(current, currentResult.document());
        assertTrue(currentResult.report().issues().isEmpty());
        assertEquals(0, calls.get());
        assertFalse(futureResult.isSuccessful());
        assertEquals("config.version.future", futureResult.report().issues().get(0).id());
        assertEquals("schema-version", futureResult.report().issues().get(0).path());
        assertEquals("2", futureResult.report().issues().get(0).suppliedValue());
        assertTrue(futureResult.report().issues().get(0).action().contains("downgrade"));
        assertThrows(IllegalStateException.class, futureResult::document);
        assertEquals(0, calls.get());
    }

    @Test
    void multiStepRunPreservesUnknownIdentityAndAllowsOwnedOperations() {
        ThrowingValue unknown = new ThrowingValue();
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("unknown.first", unknown);
        values.put("old-name", "legacy");
        values.put("unknown.last", unknown);
        ConfigDocument original = ConfigDocument.of(SOURCE, 0, values);
        List<Integer> calls = new ArrayList<Integer>();
        ConfigMigration first = migration(0, set("old-name", "new-name"), document -> {
            calls.add(Integer.valueOf(0));
            return document.withValue("new-name", document.value("old-name"))
                    .withoutValue("old-name").advanceTo(1);
        });
        ConfigMigration second = migration(1, set("new-name", "new-setting"), document -> {
            calls.add(Integer.valueOf(1));
            return document.withValue("new-setting", "enabled")
                    .withoutValue("new-name").advanceTo(2);
        });

        ConfigMigrationResult result = new ConfigMigrator(2,
                Arrays.asList(second, first)).migrate(original);

        assertTrue(result.isSuccessful());
        assertEquals(Arrays.asList("unknown.first", "unknown.last", "new-setting"),
                new ArrayList<String>(result.document().values().keySet()));
        assertSame(unknown, result.document().value("unknown.first"));
        assertSame(unknown, result.document().value("unknown.last"));
        assertEquals(Arrays.asList(Integer.valueOf(0), Integer.valueOf(1)), calls);
        assertEquals(0, original.schemaVersion());
        assertTrue(original.contains("old-name"));
        assertFalse(original.contains("new-setting"));
    }

    @Test
    void invalidOutputsFailClosedAndStopLaterSteps() {
        List<String> cases = Arrays.asList("null", "source", "version", "add", "change", "remove", "reorder");
        for (String kind : cases) {
            AtomicInteger laterCalls = new AtomicInteger();
            ConfigMigration first = migration(0, set("owned"), document -> invalid(kind, document));
            ConfigMigration later = migration(1, set("owned"), document -> {
                laterCalls.incrementAndGet();
                return document.advanceTo(2);
            });
            ConfigDocument original = ConfigDocument.of(SOURCE, 0,
                    ordered("unknown.one", new Object(), "owned", "old", "unknown.two", new Object()));

            ConfigMigrationResult result = new ConfigMigrator(2, Arrays.asList(later, first))
                    .migrate(original);

            assertFalse(result.isSuccessful(), kind);
            assertEquals("config.migration.invalid-output", result.report().issues().get(0).id());
            assertThrows(IllegalStateException.class, result::document);
            assertEquals(0, laterCalls.get(), kind);
            assertEquals(0, original.schemaVersion(), kind);
            assertEquals("old", original.value("owned"), kind);
        }
    }

    @Test
    void undeclaredValueReplacementIsRejectedByIdentity() {
        Object unknown = new Object();
        ConfigDocument original = ConfigDocument.of(SOURCE, 0,
                ordered("unknown", unknown, "owned", "old"));
        ConfigMigration replacement = migration(0, set("owned"), document ->
                ConfigDocument.of(SOURCE, 1, ordered("unknown", new Object(), "owned", "new")));

        ConfigMigrationResult result = new ConfigMigrator(1,
                Collections.singletonList(replacement)).migrate(original);

        assertFalse(result.isSuccessful());
        assertEquals("config.migration.invalid-output", result.report().issues().get(0).id());
        assertSame(unknown, original.value("unknown"));
    }

    @Test
    void thrownMigrationFailsSafelyWithoutLeakingExceptionDetails() {
        String secret = "private migration failure details";
        ConfigMigration throwing = migration(0, set("owned"), document -> {
            throw new IllegalStateException(secret);
        });
        ConfigMigration later = migration(1, set("owned"), document ->
                document.advanceTo(2));
        ConfigMigrationResult result = new ConfigMigrator(2,
                Arrays.asList(later, throwing)).migrate(ConfigDocument.fresh(SOURCE, 0));

        assertFalse(result.isSuccessful());
        assertEquals("config.migration.failed", result.report().issues().get(0).id());
        assertFalse(result.report().issues().get(0).toString().contains(secret));
        assertFalse(result.report().toString().contains(secret));
        assertFalse(result.toString().contains(secret));
        assertThrows(IllegalStateException.class, result::document);
    }

    @Test
    void resultAndReportsAreImmutableAndFailureHasNoDocument() {
        ConfigMigrationResult success = new ConfigMigrator(0,
                Collections.<ConfigMigration>emptyList()).migrate(ConfigDocument.fresh(SOURCE, 0));

        assertTrue(success.isSuccessful());
        assertTrue(success.report().isValid());
        assertThrows(UnsupportedOperationException.class,
                () -> success.report().issues().clear());

        ConfigMigrationResult failure = new ConfigMigrator(1,
                Collections.singletonList(migration(0, set("owned"), document -> null)))
                .migrate(ConfigDocument.fresh(SOURCE, 0));
        assertFalse(failure.isSuccessful());
        assertFalse(failure.report().issues().isEmpty());
        assertThrows(IllegalStateException.class, failure::document);
    }

    private static ConfigDocument invalid(String kind, ConfigDocument document) {
        if ("null".equals(kind)) {
            return null;
        }
        if ("source".equals(kind)) {
            return ConfigDocument.fresh("other.yml", 1);
        }
        if ("version".equals(kind)) {
            return ConfigDocument.fresh(SOURCE, 3);
        }
        if ("add".equals(kind)) {
            return document.withValue("unowned", "value").advanceTo(1);
        }
        if ("change".equals(kind)) {
            return document.withValue("unknown.one", "changed").advanceTo(1);
        }
        if ("remove".equals(kind)) {
            return document.withoutValue("unknown.one").advanceTo(1);
        }
        LinkedHashMap<String, Object> reordered = new LinkedHashMap<String, Object>();
        reordered.put("unknown.two", document.value("unknown.two"));
        reordered.put("unknown.one", document.value("unknown.one"));
        reordered.put("owned", document.value("owned"));
        return ConfigDocument.of(SOURCE, 1, reordered);
    }

    private static ConfigMigration migration(int fromVersion, Set<String> paths,
                                             MigrationFunction function) {
        return new MutableMigration(fromVersion, paths, function);
    }

    private static Set<String> set(String... paths) {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<String>();
        result.addAll(Arrays.asList(paths));
        return result;
    }

    private static LinkedHashMap<String, Object> ordered(String firstPath, Object firstValue,
                                                          String secondPath, Object secondValue,
                                                          String thirdPath, Object thirdValue) {
        LinkedHashMap<String, Object> values = ordered(firstPath, firstValue, secondPath, secondValue);
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

    private interface MigrationFunction {
        ConfigDocument apply(ConfigDocument document);
    }

    private static final class MutableMigration implements ConfigMigration {
        private final int version;
        private final Set<String> paths;
        private final MigrationFunction function;

        private MutableMigration(int version, Set<String> paths, MigrationFunction function) {
            this.version = version;
            this.paths = paths;
            this.function = function;
        }

        @Override
        public int fromVersion() {
            return version;
        }

        @Override
        public Set<String> changedPaths() {
            return paths;
        }

        @Override
        public ConfigDocument migrate(ConfigDocument document) {
            return function.apply(document);
        }
    }

    private static final class ThrowingValue {
        @Override
        public String toString() {
            throw new AssertionError("unknown value was stringified");
        }
    }
}

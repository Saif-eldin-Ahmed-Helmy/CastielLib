package dev.castiel.lib.config.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigChangePlannerTest {
    private static final String SOURCE = "config.yml";

    @Test
    void reportsAddedRemovedChangedAndOrderInStableUnionOrder() {
        LinkedHashMap<String, Object> before = new LinkedHashMap<String, Object>();
        before.put("first", "one");
        before.put("removed", Boolean.TRUE);
        before.put("changed", Integer.valueOf(1));
        LinkedHashMap<String, Object> after = new LinkedHashMap<String, Object>();
        after.put("changed", Integer.valueOf(2));
        after.put("first", "one");
        after.put("added", null);

        ConfigChangePlan plan = new ConfigChangePlanner().plan(
                ConfigDocument.of(SOURCE, 1, before), ConfigDocument.of(SOURCE, 1, after));

        assertEquals(Arrays.asList(ConfigChange.Kind.ORDER_CHANGED, ConfigChange.Kind.REMOVED,
                ConfigChange.Kind.CHANGED, ConfigChange.Kind.ADDED), kinds(plan.changes()));
        assertEquals(1, plan.addedCount());
        assertEquals(1, plan.removedCount());
        assertEquals(1, plan.changedCount());
        assertEquals(1, plan.orderChangedCount());
        assertEquals("<null>", plan.changes().get(3).afterDescriptor());
    }

    @Test
    void distinguishesNullUnknownEmptyAndNestedSupportedValues() {
        LinkedHashMap<String, Object> nestedBefore = new LinkedHashMap<String, Object>();
        nestedBefore.put("empty", new ArrayList<Object>());
        nestedBefore.put("nested", Arrays.<Object>asList("a", null));
        LinkedHashMap<String, Object> before = new LinkedHashMap<String, Object>();
        before.put("optional", null);
        before.put("unknown", nestedBefore);

        LinkedHashMap<String, Object> nestedAfter = new LinkedHashMap<String, Object>();
        nestedAfter.put("empty", new ArrayList<Object>());
        nestedAfter.put("nested", Arrays.<Object>asList("a", "b"));
        LinkedHashMap<String, Object> after = new LinkedHashMap<String, Object>();
        after.put("optional", null);
        after.put("unknown", nestedAfter);

        ConfigChangePlan plan = new ConfigChangePlanner().plan(
                ConfigDocument.of(SOURCE, 1, before), ConfigDocument.of(SOURCE, 1, after));

        assertEquals(1, plan.changes().size());
        assertEquals(ConfigChange.Kind.CHANGED, plan.changes().get(0).kind());
        assertEquals("<map>", plan.changes().get(0).beforeDescriptor());
        assertEquals("<map>", plan.changes().get(0).afterDescriptor());
    }

    @Test
    void schemaVersionChangesAreVisibleAsTheFirstPreviewEntry() {
        ConfigChangePlan plan = new ConfigChangePlanner().plan(
                ConfigDocument.fresh(SOURCE, 1), ConfigDocument.fresh(SOURCE, 2));

        assertEquals(1, plan.changes().size());
        assertEquals("schema-version", plan.changes().get(0).path());
        assertEquals(ConfigChange.Kind.CHANGED, plan.changes().get(0).kind());
        assertEquals("<number>", plan.changes().get(0).beforeDescriptor());
        assertEquals("<number>", plan.changes().get(0).afterDescriptor());
    }

    @Test
    void unsupportedAndCyclicValuesAreSafeAndNeverRendered() {
        ThrowingValue unsupported = new ThrowingValue();
        LinkedHashMap<String, Object> cyclic = new LinkedHashMap<String, Object>();
        cyclic.put("self", cyclic);
        LinkedHashMap<String, Object> before = new LinkedHashMap<String, Object>();
        before.put("secret", unsupported);
        before.put("cycle", cyclic);
        LinkedHashMap<String, Object> after = new LinkedHashMap<String, Object>();
        after.put("secret", unsupported);
        after.put("cycle", cyclic);

        ConfigChangePlan plan = new ConfigChangePlanner().plan(
                ConfigDocument.of(SOURCE, 1, before), ConfigDocument.of(SOURCE, 1, after));

        assertEquals(2, plan.changes().size());
        assertEquals("<unsupported>", plan.changes().get(0).beforeDescriptor());
        assertEquals("<unsupported>", plan.changes().get(0).afterDescriptor());
        assertEquals(ConfigChange.Kind.CHANGED, plan.changes().get(1).kind());
        assertEquals("<unsupported>", plan.changes().get(1).beforeDescriptor());
        assertFalse(plan.toString().contains("secret-value"));
    }

    @Test
    void pathsAreSanitizedAndPlanIsImmutableAndDeterministic() {
        LinkedHashMap<String, Object> before = new LinkedHashMap<String, Object>();
        before.put("unsafe\npath", "secret-value");
        ConfigDocument oldDocument = ConfigDocument.of(SOURCE, 1, before);
        ConfigDocument newDocument = ConfigDocument.of(SOURCE, 1,
                new LinkedHashMap<String, Object>());

        ConfigChangePlanner planner = new ConfigChangePlanner();
        ConfigChangePlan first = planner.plan(oldDocument, newDocument);
        ConfigChangePlan second = planner.plan(oldDocument, newDocument);

        assertEquals(first.toString(), second.toString());
        assertEquals("unsafe?path", first.changes().get(0).path());
        assertFalse(first.toString().contains("secret-value"));
        assertThrows(UnsupportedOperationException.class, () -> first.changes().clear());
        assertThrows(NullPointerException.class, () -> planner.plan(null, newDocument));
        assertThrows(NullPointerException.class, () -> planner.plan(oldDocument, null));
    }

    @Test
    void sameDocumentProducesEmptyPlanAndThrowingCustomCollectionsBecomeUnsupported() {
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("stable", "value");
        ConfigDocument stableDocument = ConfigDocument.of(SOURCE, 1,
                new LinkedHashMap<String, Object>(values));
        values.put("custom", new ThrowingList());
        ConfigDocument document = ConfigDocument.of(SOURCE, 1, values);

        ConfigChangePlan same = new ConfigChangePlanner().plan(stableDocument, stableDocument);
        assertTrue(same.isEmpty());

        LinkedHashMap<String, Object> changed = new LinkedHashMap<String, Object>(values);
        changed.put("custom", new ArrayList<Object>());
        ConfigChangePlan plan = new ConfigChangePlanner().plan(document,
                ConfigDocument.of(SOURCE, 1, changed));
        assertEquals(1, plan.changes().size());
        assertEquals("<unsupported>", plan.changes().get(0).beforeDescriptor());
        assertEquals("<list>", plan.changes().get(0).afterDescriptor());
    }

    private static List<ConfigChange.Kind> kinds(List<ConfigChange> changes) {
        List<ConfigChange.Kind> result = new ArrayList<ConfigChange.Kind>();
        for (ConfigChange change : changes) {
            result.add(change.kind());
        }
        return result;
    }

    private static final class ThrowingValue {
        @Override
        public String toString() {
            throw new AssertionError("must not render");
        }

        @Override
        public boolean equals(Object other) {
            throw new AssertionError("must not compare");
        }
    }

    private static final class ThrowingList extends ArrayList<Object> {
        @Override
        public int size() {
            throw new AssertionError("must not inspect custom list");
        }
    }
}

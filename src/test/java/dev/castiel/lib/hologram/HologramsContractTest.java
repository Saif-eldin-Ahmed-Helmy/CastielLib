package dev.castiel.lib.hologram;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HologramsContractTest {
    @Test
    void namespaceMatchingDoesNotCrossPluginBoundaries() {
        assertTrue(HologramNamespaces.owns("awesomegenerators", "awesomegenerators:one"));
        assertTrue(HologramNamespaces.owns("awesomegenerators:", "awesomegenerators:one:item"));
        assertFalse(HologramNamespaces.owns("awesomegenerators", "awesomegenerators2:one"));
        assertFalse(HologramNamespaces.owns("", "awesomegenerators:one"));
    }

    @Test
    void restartDiscoveryRemovesOnlyStaleNamespacedDisplays() {
        FakeProvider provider = new FakeProvider("awesomegenerators:keep", "awesomegenerators:keep:item",
                "awesomegenerators:stale", "other:untouched");
        HologramsAPI api = new HologramsAPI(provider);

        HologramResult result = api.reconcile("awesomegenerators",
                Collections.singleton("awesomegenerators:keep"));

        assertTrue(result.successful());
        assertEquals(Collections.singleton("awesomegenerators:stale"), provider.removed);
    }

    @Test
    void removeNamespaceCanCleanRestartStateWithoutExpectedIds() {
        FakeProvider provider = new FakeProvider("awesomegenerators:one", "other:two");
        HologramsAPI api = new HologramsAPI(provider);

        assertTrue(api.removeNamespace("awesomegenerators").successful());
        assertEquals(Collections.singleton("awesomegenerators:one"), provider.removed);
    }

    @Test
    void resultReportsVisualFallbackAndAppliedCapabilities() {
        HologramCapabilities capabilities = new HologramCapabilities(true, false, false, true);
        HologramResult result = HologramResult.success("fancy", capabilities, true, "text only");

        assertTrue(result.successful());
        assertTrue(result.fallback());
        assertTrue(result.capabilities().text());
        assertFalse(result.capabilities().item());
        assertEquals("text only", result.reason());
    }

    @Test
    void selectionExposesProviderNeutralFallbackDetails() {
        HologramCapabilities capabilities = new HologramCapabilities(true, false, false, false);
        HologramSelection selection = new HologramSelection("fancy", "native", "fancy unavailable", capabilities);

        assertTrue(selection.fallback());
        assertEquals("fancy", selection.requested());
        assertEquals("native", selection.selected());
        assertEquals("fancy unavailable", selection.fallbackReason());
        assertTrue(selection.capabilities().text());
    }

    private static final class FakeProvider implements HologramProvider {
        private final Set<String> discovered;
        private final Set<String> removed = new LinkedHashSet<String>();

        private FakeProvider(String... discovered) {
            this.discovered = new LinkedHashSet<String>(Arrays.asList(discovered));
        }

        public String id() { return "fake"; }
        public ProviderAvailability availability() { return ProviderAvailability.available(id()); }
        public HologramCapabilities capabilities() { return new HologramCapabilities(true, true, true, true); }
        public HologramResult show(String displayId, HologramDisplay display) { return HologramResult.success(id()); }
        public HologramResult remove(String displayId) { removed.add(displayId); return HologramResult.success(id()); }
        public HologramResult removeAll(Set<String> displayIds) { removed.addAll(displayIds); return HologramResult.success(id()); }
        public Set<String> ownedIds(String namespace) {
            Set<String> result = new LinkedHashSet<String>();
            for (String id : discovered) if (HologramNamespaces.owns(namespace, id)) result.add(id);
            return result;
        }
        public void shutdown() { }
    }
}

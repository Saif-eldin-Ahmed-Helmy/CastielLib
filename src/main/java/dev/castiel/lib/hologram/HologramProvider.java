package dev.castiel.lib.hologram;

import java.util.Set;
import java.util.Collections;

public interface HologramProvider {
    String id();

    ProviderAvailability availability();

    /** Returns capabilities detected for the currently installed provider version. */
    default HologramCapabilities capabilities() {
        return HologramCapabilities.none();
    }

    HologramResult show(String displayId, HologramDisplay display);

    HologramResult remove(String displayId);

    HologramResult removeAll(Set<String> displayIds);

    /** Returns IDs currently owned by this provider for this plugin namespace. */
    default Set<String> ownedIds() {
        return Collections.emptySet();
    }

    /** Discovers IDs in a namespace when the provider exposes a safe listing API. */
    default Set<String> ownedIds(String namespace) {
        Set<String> result = new java.util.LinkedHashSet<String>();
        for (String id : ownedIds()) {
            if (HologramNamespaces.owns(namespace, id)) {
                result.add(id);
            }
        }
        return result;
    }

    void shutdown();
}

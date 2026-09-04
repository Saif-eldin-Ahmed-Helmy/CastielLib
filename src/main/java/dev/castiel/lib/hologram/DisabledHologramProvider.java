package dev.castiel.lib.hologram;

import java.util.Collections;
import java.util.Set;

public final class DisabledHologramProvider implements HologramProvider {
    public String id() {
        return "disabled";
    }

    public ProviderAvailability availability() {
        return ProviderAvailability.available(id());
    }

    public HologramCapabilities capabilities() {
        return HologramCapabilities.none();
    }

    public HologramResult show(String displayId, HologramDisplay display) {
        return HologramResult.success(id());
    }

    public HologramResult remove(String displayId) {
        return HologramResult.success(id());
    }

    public HologramResult removeAll(Set<String> displayIds) {
        return HologramResult.success(id());
    }

    public void shutdown() {
        // Disabled provider owns no runtime state.
    }
}

package dev.castiel.lib.hologram;

import dev.castiel.lib.util.Placeholders;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Uses CastielLib's TextDisplay/ArmorStand implementation as a safe fallback. */
public final class NativeHologramProvider implements HologramProvider {
    private final HologramManager manager;
    private final Map<String, Hologram> handles = new LinkedHashMap<String, Hologram>();

    public NativeHologramProvider(HologramManager manager) {
        this.manager = manager;
    }

    public String id() {
        return "native";
    }

    public ProviderAvailability availability() {
        return ProviderAvailability.available(id());
    }

    public HologramCapabilities capabilities() {
        return new HologramCapabilities(true, true, true, false);
    }

    public HologramResult show(String displayId, HologramDisplay display) {
        Location location = display == null ? null : display.location();
        if (location == null) {
            return HologramResult.failure(id(), "display location is missing");
        }
        HologramOptions options = new HologramOptions(true, display.lines(), 0.0, 0.28, 1.0f);
        HologramItem item = display.item() == null ? null : new HologramItem(display.item(), display.itemOffsetY());
        Hologram handle = manager.show(displayId, location, options, Placeholders.empty(), item, true);
        handles.put(displayId, handle);
        return HologramResult.success(id(), capabilities(), false, "");
    }

    public HologramResult remove(String displayId) {
        manager.remove(displayId);
        handles.remove(displayId);
        return HologramResult.success(id());
    }

    public HologramResult removeAll(Set<String> displayIds) {
        if (displayIds != null) {
            for (String displayId : displayIds) {
                remove(displayId);
            }
        }
        return HologramResult.success(id());
    }

    public Set<String> ownedIds() {
        return new java.util.LinkedHashSet<String>(handles.keySet());
    }

    public void shutdown() {
        handles.clear();
    }
}

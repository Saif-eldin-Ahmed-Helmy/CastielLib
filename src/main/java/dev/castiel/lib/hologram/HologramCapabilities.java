package dev.castiel.lib.hologram;

/** Provider-neutral description of visuals and lifecycle operations a provider supports. */
public final class HologramCapabilities {
    private final boolean text;
    private final boolean item;
    private final boolean itemOffset;
    private final boolean namespaceDiscovery;

    public HologramCapabilities(boolean text, boolean item, boolean itemOffset, boolean namespaceDiscovery) {
        this.text = text;
        this.item = item;
        this.itemOffset = itemOffset;
        this.namespaceDiscovery = namespaceDiscovery;
    }

    public static HologramCapabilities none() {
        return new HologramCapabilities(false, false, false, false);
    }

    /** Returns whether text displays are supported. */
    public boolean text() {
        return text;
    }

    /** Returns whether item displays are supported. */
    public boolean item() {
        return item;
    }

    /** Returns whether an item display can honor its requested vertical offset. */
    public boolean itemOffset() {
        return itemOffset;
    }

    /** Returns whether stale provider displays can be discovered after restart. */
    public boolean namespaceDiscovery() {
        return namespaceDiscovery;
    }
}

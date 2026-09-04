package dev.castiel.lib.hologram;

/** Provider-neutral result of provider selection and fallback. */
public final class HologramSelection {
    private final String requested;
    private final String selected;
    private final String fallbackReason;
    private final HologramCapabilities capabilities;

    HologramSelection(String requested, String selected, String fallbackReason,
                      HologramCapabilities capabilities) {
        this.requested = requested;
        this.selected = selected;
        this.fallbackReason = fallbackReason == null ? "" : fallbackReason;
        this.capabilities = capabilities == null ? HologramCapabilities.none() : capabilities;
    }

    /** Returns the configured provider mode. */
    public String requested() { return requested; }
    /** Returns the provider that was actually selected. */
    public String selected() { return selected; }
    /** Returns whether selection required a fallback. */
    public boolean fallback() { return !fallbackReason.isEmpty(); }
    /** Returns a concise fallback reason, or an empty string. */
    public String fallbackReason() { return fallbackReason; }
    /** Returns capabilities for the selected provider. */
    public HologramCapabilities capabilities() { return capabilities; }
}

package dev.castiel.lib.hologram;

public final class HologramResult {
    private final boolean successful;
    private final String provider;
    private final String reason;
    private final HologramCapabilities capabilities;
    private final boolean fallback;

    private HologramResult(boolean successful, String provider, String reason,
                           HologramCapabilities capabilities, boolean fallback) {
        this.successful = successful;
        this.provider = provider;
        this.reason = reason;
        this.capabilities = capabilities == null ? HologramCapabilities.none() : capabilities;
        this.fallback = fallback;
    }

    public static HologramResult success(String provider) {
        return success(provider, HologramCapabilities.none(), false, "");
    }

    public static HologramResult success(String provider, HologramCapabilities capabilities,
                                         boolean fallback, String reason) {
        return new HologramResult(true, provider, reason == null ? "" : reason, capabilities, fallback);
    }

    public static HologramResult failure(String provider, String reason) {
        return new HologramResult(false, provider, reason == null ? "unknown failure" : reason,
                HologramCapabilities.none(), false);
    }

    public boolean successful() {
        return successful;
    }

    public String provider() {
        return provider;
    }

    public String reason() {
        return reason;
    }

    public HologramCapabilities capabilities() {
        return capabilities;
    }

    public boolean fallback() {
        return fallback;
    }
}

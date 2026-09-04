package dev.castiel.lib.hologram;

public final class ProviderAvailability {
    private final boolean available;
    private final String provider;
    private final String reason;

    private ProviderAvailability(boolean available, String provider, String reason) {
        this.available = available;
        this.provider = provider;
        this.reason = reason;
    }

    public static ProviderAvailability available(String provider) {
        return new ProviderAvailability(true, provider, "");
    }

    public static ProviderAvailability unavailable(String provider, String reason) {
        return new ProviderAvailability(false, provider, reason);
    }

    public boolean available() {
        return available;
    }

    public String provider() {
        return provider;
    }

    public String reason() {
        return reason;
    }
}

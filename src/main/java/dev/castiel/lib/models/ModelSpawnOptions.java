package dev.castiel.lib.models;

/**
 * Runtime options for spawning a vanilla model.
 */
public final class ModelSpawnOptions {
    private final boolean persistent;
    private final boolean allowArmorStandFallback;
    private final float viewRange;
    private final float shadowRadius;
    private final float shadowStrength;
    private final int interpolationDuration;
    private final int teleportDuration;

    public ModelSpawnOptions(
            boolean persistent,
            boolean allowArmorStandFallback,
            float viewRange,
            float shadowRadius,
            float shadowStrength,
            int interpolationDuration,
            int teleportDuration) {
        this.persistent = persistent;
        this.allowArmorStandFallback = allowArmorStandFallback;
        this.viewRange = Math.max(0.1f, viewRange);
        this.shadowRadius = Math.max(0.0f, shadowRadius);
        this.shadowStrength = Math.max(0.0f, shadowStrength);
        this.interpolationDuration = Math.max(0, interpolationDuration);
        this.teleportDuration = Math.max(0, Math.min(59, teleportDuration));
    }

    public static ModelSpawnOptions defaults() {
        return new ModelSpawnOptions(false, false, 1.0f, 0.0f, 0.0f, 0, 0);
    }

    public ModelSpawnOptions persistent(boolean value) {
        return new ModelSpawnOptions(value, allowArmorStandFallback, viewRange, shadowRadius, shadowStrength, interpolationDuration, teleportDuration);
    }

    public ModelSpawnOptions allowArmorStandFallback(boolean value) {
        return new ModelSpawnOptions(persistent, value, viewRange, shadowRadius, shadowStrength, interpolationDuration, teleportDuration);
    }

    public boolean persistent() {
        return persistent;
    }

    public boolean allowArmorStandFallback() {
        return allowArmorStandFallback;
    }

    public float viewRange() {
        return viewRange;
    }

    public float shadowRadius() {
        return shadowRadius;
    }

    public float shadowStrength() {
        return shadowStrength;
    }

    public int interpolationDuration() {
        return interpolationDuration;
    }

    public int teleportDuration() {
        return teleportDuration;
    }
}

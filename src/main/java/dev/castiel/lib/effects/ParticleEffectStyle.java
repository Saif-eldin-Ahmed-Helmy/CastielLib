package dev.castiel.lib.effects;

import java.util.Locale;

public enum ParticleEffectStyle {
    HALO,
    RINGS,
    SPIRAL,
    DOUBLE_SPIRAL,
    HELIX,
    DNA,
    VORTEX,
    FOUNTAIN,
    ORBIT,
    GALAXY,
    CROWN,
    BEACON,
    COMET,
    PULSE,
    AURORA,
    STAR,
    HEART,
    CUBE,
    PYRAMID,
    NOVA,
    RAIN,
    SNOWGLOBE,
    FLAME_SWIRL,
    PORTAL,
    ENCHANTED,
    TOTEM_BURST,
    DRIP,
    WAVE,
    CORKSCREW,
    FIREWORK;

    public static ParticleEffectStyle match(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return GALAXY;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return GALAXY;
        }
    }
}

package dev.castiel.lib.effects;

import org.bukkit.configuration.ConfigurationSection;

public final class ParticleEffectConfig {
    private final boolean enabled;
    private final ParticleEffectStyle style;
    private final String particle;
    private final String secondaryParticle;
    private final int density;
    private final int intervalTicks;
    private final double radius;
    private final double height;
    private final double speed;
    private final double yOffset;

    public ParticleEffectConfig(boolean enabled, ParticleEffectStyle style, String particle, String secondaryParticle, int density,
                                int intervalTicks, double radius, double height, double speed, double yOffset) {
        this.enabled = enabled;
        this.style = style == null ? ParticleEffectStyle.GALAXY : style;
        this.particle = particle == null || particle.trim().isEmpty() ? "ENCHANTMENT_TABLE" : particle;
        this.secondaryParticle = secondaryParticle == null || secondaryParticle.trim().isEmpty() ? this.particle : secondaryParticle;
        this.density = Math.max(1, density);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.radius = Math.max(0.1, radius);
        this.height = Math.max(0.1, height);
        this.speed = speed;
        this.yOffset = yOffset;
    }

    public static ParticleEffectConfig from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        return new ParticleEffectConfig(
                section.getBoolean("enabled", section.getBoolean("Enabled", true)),
                ParticleEffectStyle.match(section.getString("style", section.getString("Style", "GALAXY"))),
                section.getString("particle", section.getString("Particle", "ENCHANTMENT_TABLE")),
                section.getString("secondary-particle", section.getString("Secondary-Particle", "END_ROD")),
                section.getInt("density", section.getInt("Density", 28)),
                section.getInt("interval", section.getInt("Interval", 2)),
                section.getDouble("radius", section.getDouble("Radius", 1.15)),
                section.getDouble("height", section.getDouble("Height", 1.75)),
                section.getDouble("speed", section.getDouble("Speed", 0.01)),
                section.getDouble("y-offset", section.getDouble("Y-Offset", 0.55))
        );
    }

    public static ParticleEffectConfig defaults() {
        return new ParticleEffectConfig(true, ParticleEffectStyle.GALAXY, "ENCHANTMENT_TABLE", "END_ROD", 28, 2, 1.15, 1.75, 0.01, 0.55);
    }

    public boolean enabled() {
        return enabled;
    }

    public ParticleEffectStyle style() {
        return style;
    }

    public String particle() {
        return particle;
    }

    public String secondaryParticle() {
        return secondaryParticle;
    }

    public int density() {
        return density;
    }

    public int intervalTicks() {
        return intervalTicks;
    }

    public double radius() {
        return radius;
    }

    public double height() {
        return height;
    }

    public double speed() {
        return speed;
    }

    public double yOffset() {
        return yOffset;
    }
}

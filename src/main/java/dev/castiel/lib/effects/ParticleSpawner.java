package dev.castiel.lib.effects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.Locale;

/** Dispatches particles without linking CastielLib to post-1.8 Bukkit types. */
public final class ParticleSpawner {
    private static final ModernParticles MODERN = ModernParticles.discover();

    private ParticleSpawner() {
    }

    public static void spawn(World world, Location location, String name, int count,
                             double offsetX, double offsetY, double offsetZ, double extra) {
        if (world == null || location == null || name == null) {
            return;
        }
        String normalized = normalize(name);
        if (MODERN.spawn(world, location, normalized, count, offsetX, offsetY, offsetZ, extra)) {
            return;
        }
        Effect effect = legacyEffect(normalized);
        if (effect != null) {
            world.playEffect(location, effect, 0);
        }
    }

    static String normalize(String raw) {
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        if ("ENCHANT".equals(normalized) || "ENCHANTED".equals(normalized)) {
            return "ENCHANTMENT_TABLE";
        }
        if ("DUST".equals(normalized)) {
            return "REDSTONE";
        }
        if ("WITCH".equals(normalized)) {
            return "SPELL_WITCH";
        }
        if ("FIREWORK".equals(normalized)) {
            return "FIREWORKS_SPARK";
        }
        return normalized;
    }

    private static Effect legacyEffect(String particle) {
        String effectName;
        if ("FLAME".equals(particle)) {
            effectName = "MOBSPAWNER_FLAMES";
        } else if ("SMOKE_NORMAL".equals(particle) || "SMOKE_LARGE".equals(particle)) {
            effectName = "SMOKE";
        } else if ("FIREWORKS_SPARK".equals(particle)) {
            effectName = "FIREWORKS_SPARK";
        } else {
            return null;
        }
        try {
            return Effect.valueOf(effectName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static final class ModernParticles {
        private final Class<? extends Enum> particleType;
        private final Method spawn;

        private ModernParticles(Class<? extends Enum> particleType, Method spawn) {
            this.particleType = particleType;
            this.spawn = spawn;
        }

        private static ModernParticles discover() {
            try {
                Class<?> rawType = Class.forName("org.bukkit.Particle");
                Method method = World.class.getMethod("spawnParticle", rawType, double.class, double.class,
                        double.class, int.class, double.class, double.class, double.class, double.class);
                return new ModernParticles(rawType.asSubclass(Enum.class), method);
            } catch (ReflectiveOperationException ignored) {
                return new ModernParticles(null, null);
            }
        }

        private boolean spawn(World world, Location location, String name, int count,
                              double offsetX, double offsetY, double offsetZ, double extra) {
            if (particleType == null || spawn == null) {
                return false;
            }
            Object particle = enumValue(name);
            if (particle == null && "FIREWORKS_SPARK".equals(name)) {
                particle = enumValue("FIREWORK");
            }
            if (particle == null) {
                return false;
            }
            try {
                spawn.invoke(world, particle, location.getX(), location.getY(), location.getZ(), count,
                        offsetX, offsetY, offsetZ, extra);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"}) // Enum.valueOf requires the runtime particle enum type.
        private Object enumValue(String name) {
            try {
                return Enum.valueOf((Class) particleType, name);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}

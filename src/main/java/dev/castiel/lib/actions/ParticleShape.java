package dev.castiel.lib.actions;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Locale;

public final class ParticleShape {
    private ParticleShape() {
    }

    public static void spawn(Location center, String payload) {
        String[] parts = payload.split("\\s+");
        Particle particle = match(parts.length > 0 ? parts[0] : "FLAME");
        String shape = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "point";
        if (particle == null || center.getWorld() == null) {
            return;
        }
        if ("ring".equals(shape)) {
            ring(center, particle, 1.2, 48);
        } else if ("sphere".equals(shape)) {
            sphere(center, particle, 1.0, 64);
        } else {
            center.getWorld().spawnParticle(particle, center, 1);
        }
    }

    private static void ring(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            world.spawnParticle(particle, center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
        }
    }

    private static void sphere(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1)) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = Math.PI * (3 - Math.sqrt(5)) * i;
            world.spawnParticle(particle, center.clone().add(Math.cos(theta) * r * radius, y * radius, Math.sin(theta) * r * radius), 1, 0, 0, 0, 0);
        }
    }

    private static Particle match(String raw) {
        try {
            return Particle.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

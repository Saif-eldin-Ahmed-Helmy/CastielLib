package dev.castiel.lib.actions;

import org.bukkit.Location;
import org.bukkit.World;
import dev.castiel.lib.effects.ParticleSpawner;

import java.util.Locale;

public final class ParticleShape {
    private ParticleShape() {
    }

    public static void spawn(Location center, String payload) {
        String[] parts = payload.split("\\s+");
        String particle = parts.length > 0 ? parts[0] : "FLAME";
        String shape = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "point";
        if (center.getWorld() == null) {
            return;
        }
        if ("ring".equals(shape)) {
            ring(center, particle, 1.2, 48);
        } else if ("sphere".equals(shape)) {
            sphere(center, particle, 1.0, 64);
        } else {
            ParticleSpawner.spawn(center.getWorld(), center, particle, 1, 0, 0, 0, 0);
        }
    }

    private static void ring(Location center, String particle, double radius, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            ParticleSpawner.spawn(world, center.clone().add(Math.cos(angle) * radius, 0.1,
                    Math.sin(angle) * radius), particle, 1, 0, 0, 0, 0);
        }
    }

    private static void sphere(Location center, String particle, double radius, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1)) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = Math.PI * (3 - Math.sqrt(5)) * i;
            ParticleSpawner.spawn(world, center.clone().add(Math.cos(theta) * r * radius, y * radius,
                    Math.sin(theta) * r * radius), particle, 1, 0, 0, 0, 0);
        }
    }

}

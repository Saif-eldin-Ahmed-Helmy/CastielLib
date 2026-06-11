package dev.castiel.lib.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ParticleEffectEngine {
    private final JavaPlugin plugin;
    private final Map<String, Target> targets = new LinkedHashMap<String, Target>();
    private BukkitTask task;
    private int tick;

    public ParticleEffectEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void put(String id, Location blockLocation, ParticleEffectConfig config) {
        if (id == null || blockLocation == null || config == null || !config.enabled()) {
            return;
        }
        Location center = blockLocation.clone().add(0.5, config.yOffset(), 0.5);
        targets.put(id, new Target(center, config));
        start();
    }

    public void remove(String id) {
        targets.remove(id);
    }

    public void clear() {
        targets.clear();
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                renderAll();
            }
        }, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        targets.clear();
    }

    private void renderAll() {
        tick++;
        if (targets.isEmpty()) {
            return;
        }
        for (Target target : targets.values()) {
            if (tick % target.config.intervalTicks() == 0) {
                render(target);
            }
        }
    }

    private void render(Target target) {
        Particle primary = match(target.config.particle());
        Particle secondary = match(target.config.secondaryParticle());
        if (primary == null) {
            primary = match("FLAME");
        }
        if (secondary == null) {
            secondary = primary;
        }
        if (primary == null || target.center.getWorld() == null) {
            return;
        }
        if (!isRenderable(target.center)) {
            return;
        }
        switch (target.config.style()) {
            case HALO:
                circle(target.center, primary, target.config.radius(), target.config.density(), 0.9, tick * 0.04);
                break;
            case RINGS:
                circle(target.center, primary, target.config.radius(), target.config.density(), 0.25, tick * 0.03);
                circle(target.center, secondary, target.config.radius() * 0.72, target.config.density(), 1.05, -tick * 0.04);
                break;
            case SPIRAL:
                spiral(target.center, primary, target.config.radius(), target.config.height(), target.config.density(), tick * 0.12, 1);
                break;
            case DOUBLE_SPIRAL:
                spiral(target.center, primary, target.config.radius(), target.config.height(), target.config.density(), tick * 0.1, 1);
                spiral(target.center, secondary, target.config.radius(), target.config.height(), target.config.density(), tick * 0.1 + Math.PI, 1);
                break;
            case HELIX:
                spiral(target.center, primary, target.config.radius() * 0.85, target.config.height(), target.config.density(), tick * 0.09, 2);
                break;
            case DNA:
                spiral(target.center, primary, target.config.radius() * 0.75, target.config.height(), target.config.density(), tick * 0.1, 2);
                spiral(target.center, secondary, target.config.radius() * 0.75, target.config.height(), target.config.density(), tick * 0.1 + Math.PI, 2);
                ladder(target.center, primary, target.config.radius() * 0.75, target.config.height(), 6, tick * 0.1);
                break;
            case VORTEX:
                vortex(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case FOUNTAIN:
                fountain(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case ORBIT:
                orbit(target.center, primary, secondary, target.config.radius(), target.config.height());
                break;
            case GALAXY:
                galaxy(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case CROWN:
                crown(target.center, primary, secondary, target.config.radius(), target.config.density());
                break;
            case BEACON:
                beacon(target.center, primary, secondary, target.config.height(), target.config.density());
                break;
            case COMET:
                comet(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case PULSE:
                pulse(target.center, primary, secondary, target.config.radius(), target.config.density());
                break;
            case AURORA:
                aurora(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case STAR:
                star(target.center, primary, secondary, target.config.radius(), target.config.density());
                break;
            case HEART:
                heart(target.center, primary, target.config.radius(), target.config.density());
                break;
            case CUBE:
                cube(target.center, primary, secondary, target.config.radius(), target.config.height());
                break;
            case PYRAMID:
                pyramid(target.center, primary, secondary, target.config.radius(), target.config.height());
                break;
            case NOVA:
                nova(target.center, primary, secondary, target.config.radius(), target.config.density());
                break;
            case RAIN:
                rain(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case SNOWGLOBE:
                snowglobe(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case FLAME_SWIRL:
                spiral(target.center, primary, target.config.radius() * 0.8, target.config.height(), target.config.density(), tick * 0.16, 1);
                fountain(target.center, secondary, primary, target.config.radius() * 0.45, target.config.height() * 0.75, Math.max(6, target.config.density() / 2));
                break;
            case PORTAL:
                portal(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case ENCHANTED:
                enchanted(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case TOTEM_BURST:
                totemBurst(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case DRIP:
                drip(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case WAVE:
                wave(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            case CORKSCREW:
                spiral(target.center, primary, target.config.radius(), target.config.height(), target.config.density(), tick * 0.2, 3);
                break;
            case FIREWORK:
                firework(target.center, primary, secondary, target.config.radius(), target.config.height(), target.config.density());
                break;
            default:
                circle(target.center, primary, target.config.radius(), target.config.density(), 0.8, tick * 0.04);
        }
    }

    private void circle(Location center, Particle particle, double radius, int points, double y, double phase) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + phase;
            spawn(world, center, particle, Math.cos(angle) * radius, y, Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private void spiral(Location center, Particle particle, double radius, double height, int points, double phase, int turns) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double progress = i / (double) Math.max(1, points - 1);
            double angle = progress * Math.PI * 2 * turns + phase;
            spawn(world, center, particle, Math.cos(angle) * radius, progress * height, Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private void ladder(Location center, Particle particle, double radius, double height, int bars, double phase) {
        World world = center.getWorld();
        for (int i = 0; i < bars; i++) {
            double progress = i / (double) Math.max(1, bars - 1);
            double angle = progress * Math.PI * 4 + phase;
            spawn(world, center, particle, Math.cos(angle) * radius, progress * height, Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            spawn(world, center, particle, -Math.cos(angle) * radius, progress * height, -Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private void vortex(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double progress = i / (double) points;
            double localRadius = radius * (1.0 - progress * 0.75);
            double angle = tick * 0.18 + progress * Math.PI * 8;
            spawn(world, center, i % 3 == 0 ? secondary : primary, Math.cos(angle) * localRadius, progress * height, Math.sin(angle) * localRadius, 1, 0, 0, 0, 0);
        }
    }

    private void fountain(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + tick * 0.04;
            double progress = (Math.sin(tick * 0.08 + i) + 1) / 2;
            spawn(world, center, i % 4 == 0 ? secondary : primary, Math.cos(angle) * radius * progress, progress * height, Math.sin(angle) * radius * progress, 1, 0.02, 0.04, 0.02, 0);
        }
    }

    private void orbit(Location center, Particle primary, Particle secondary, double radius, double height) {
        World world = center.getWorld();
        for (int i = 0; i < 4; i++) {
            double angle = tick * 0.12 + i * Math.PI / 2;
            double y = 0.55 + (Math.sin(tick * 0.08 + i) + 1) * height * 0.18;
            spawn(world, center, i % 2 == 0 ? primary : secondary, Math.cos(angle) * radius, y, Math.sin(angle) * radius, 4, 0.03, 0.03, 0.03, 0);
        }
    }

    private void galaxy(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        circle(center, primary, radius, points, height * 0.45, tick * 0.04);
        spiral(center, secondary, radius * 0.8, height, Math.max(8, points / 2), tick * 0.09, 2);
        orbit(center, primary, secondary, radius * 0.55, height);
    }

    private void crown(Location center, Particle primary, Particle secondary, double radius, int points) {
        circle(center, primary, radius, points, 1.15, tick * 0.025);
        World world = center.getWorld();
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + tick * 0.025;
            spawn(world, center, secondary, Math.cos(angle) * radius, 1.45 + Math.sin(i) * 0.1, Math.sin(angle) * radius, 3, 0.02, 0.03, 0.02, 0);
        }
    }

    private void beacon(Location center, Particle primary, Particle secondary, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double y = i * height / points;
            double wave = Math.sin(tick * 0.1 + y * 4) * 0.08;
            spawn(world, center, i % 5 == 0 ? secondary : primary, wave, y, -wave, 1, 0, 0, 0, 0);
        }
    }

    private void comet(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        double head = tick * 0.12;
        for (int i = 0; i < points; i++) {
            double tail = i * 0.12;
            double angle = head - tail;
            double fade = 1.0 - i / (double) points;
            spawn(world, center, i == 0 ? secondary : primary, Math.cos(angle) * radius * fade, 0.45 + fade * height * 0.55, Math.sin(angle) * radius * fade, 1, 0, 0, 0, 0);
        }
    }

    private void pulse(Location center, Particle primary, Particle secondary, double radius, int points) {
        double pulse = (Math.sin(tick * 0.12) + 1) / 2;
        circle(center, pulse > 0.65 ? secondary : primary, radius * (0.45 + pulse * 0.65), points, 0.75, 0);
    }

    private void aurora(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double x = -radius + (radius * 2 * i / Math.max(1, points - 1));
            double z = Math.sin(tick * 0.07 + i * 0.55) * radius * 0.25;
            double y = height * 0.65 + Math.sin(tick * 0.1 + i * 0.35) * 0.22;
            spawn(world, center, i % 2 == 0 ? primary : secondary, x, y, z, 1, 0, 0.015, 0, 0);
        }
    }

    private void star(Location center, Particle primary, Particle secondary, double radius, int points) {
        World world = center.getWorld();
        for (int i = 0; i < 5; i++) {
            double a1 = tick * 0.035 + i * Math.PI * 2 / 5;
            double a2 = a1 + Math.PI;
            for (int j = 0; j < Math.max(3, points / 8); j++) {
                double p = j / (double) Math.max(1, points / 8);
                spawn(world, center, j % 2 == 0 ? primary : secondary, Math.cos(a1) * radius * p + Math.cos(a2) * radius * 0.35 * (1 - p), 0.9, Math.sin(a1) * radius * p + Math.sin(a2) * radius * 0.35 * (1 - p), 1, 0, 0, 0, 0);
            }
        }
    }

    private void heart(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double t = Math.PI * 2 * i / points;
            double x = 16 * Math.pow(Math.sin(t), 3) / 16.0 * radius;
            double y = (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 18.0 * radius + 1.05;
            spawn(world, center, particle, x, y, 0, 1, 0, 0, 0, 0);
        }
    }

    private void cube(Location center, Particle primary, Particle secondary, double radius, double height) {
        World world = center.getWorld();
        int steps = 8;
        double y1 = 0.35;
        double y2 = height;
        for (int i = 0; i <= steps; i++) {
            double p = -radius + radius * 2 * i / steps;
            spawn(world, center, primary, p, y1, radius, 1, 0, 0, 0, 0);
            spawn(world, center, primary, p, y1, -radius, 1, 0, 0, 0, 0);
            spawn(world, center, primary, radius, y1, p, 1, 0, 0, 0, 0);
            spawn(world, center, primary, -radius, y1, p, 1, 0, 0, 0, 0);
            spawn(world, center, secondary, p, y2, radius, 1, 0, 0, 0, 0);
            spawn(world, center, secondary, p, y2, -radius, 1, 0, 0, 0, 0);
            spawn(world, center, secondary, radius, y2, p, 1, 0, 0, 0, 0);
            spawn(world, center, secondary, -radius, y2, p, 1, 0, 0, 0, 0);
        }
    }

    private void pyramid(Location center, Particle primary, Particle secondary, double radius, double height) {
        World world = center.getWorld();
        int steps = 12;
        for (int i = 0; i <= steps; i++) {
            double p = i / (double) steps;
            double x = -radius + radius * 2 * p;
            spawn(world, center, primary, x, 0.35, radius, 1, 0, 0, 0, 0);
            spawn(world, center, primary, x, 0.35, -radius, 1, 0, 0, 0, 0);
            spawn(world, center, secondary, x * (1 - p), 0.35 + height * p, radius * (1 - p), 1, 0, 0, 0, 0);
            spawn(world, center, secondary, -x * (1 - p), 0.35 + height * p, -radius * (1 - p), 1, 0, 0, 0, 0);
        }
    }

    private void nova(Location center, Particle primary, Particle secondary, double radius, int points) {
        World world = center.getWorld();
        double pulse = 0.25 + ((tick % 24) / 24.0) * radius;
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) Math.max(1, points - 1)) * 2;
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double theta = Math.PI * (3 - Math.sqrt(5)) * i + tick * 0.03;
            spawn(world, center, i % 4 == 0 ? secondary : primary, Math.cos(theta) * r * pulse, 0.9 + y * pulse, Math.sin(theta) * r * pulse, 1, 0, 0, 0, 0);
        }
    }

    private void rain(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = i * 2.399963 + tick * 0.01;
            double local = radius * ((i % 7) + 1) / 7.0;
            double y = height - ((tick * 0.08 + i * 0.37) % height);
            spawn(world, center, i % 5 == 0 ? secondary : primary, Math.cos(angle) * local, y, Math.sin(angle) * local, 1, 0, -0.04, 0, 0);
        }
    }

    private void snowglobe(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        nova(center, primary, secondary, radius, Math.max(8, points / 2));
        circle(center, secondary, radius, Math.max(12, points / 2), height * 0.5, -tick * 0.02);
    }

    private void portal(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = tick * 0.08 + i * Math.PI * 2 / points;
            double y = 0.2 + (i % 9) * height / 9.0;
            spawn(world, center, i % 2 == 0 ? primary : secondary, Math.cos(angle) * radius, y, Math.sin(angle) * radius, 1, 0.02, 0.02, 0.02, 0.02);
        }
    }

    private void enchanted(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = i * 2.399963 + tick * 0.035;
            double y = 0.4 + Math.abs(Math.sin(tick * 0.05 + i)) * height;
            spawn(world, center, i % 3 == 0 ? secondary : primary, Math.cos(angle) * radius * 0.65, y, Math.sin(angle) * radius * 0.65, 1, 0, 0.02, 0, 0);
        }
    }

    private void totemBurst(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        double stage = (tick % 20) / 20.0;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            spawn(world, center, i % 2 == 0 ? primary : secondary, Math.cos(angle) * radius * stage, 0.35 + height * stage, Math.sin(angle) * radius * stage, 1, 0, 0.02, 0, 0);
        }
    }

    private void drip(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = i * 2.399963;
            double y = height - ((tick * 0.04 + i * 0.23) % height);
            spawn(world, center, i % 4 == 0 ? secondary : primary, Math.cos(angle) * radius * 0.55, y, Math.sin(angle) * radius * 0.55, 1, 0, -0.02, 0, 0);
        }
    }

    private void wave(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            double y = height * 0.55 + Math.sin(angle * 3 + tick * 0.12) * height * 0.18;
            spawn(world, center, i % 3 == 0 ? secondary : primary, Math.cos(angle) * radius, y, Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private void firework(Location center, Particle primary, Particle secondary, double radius, double height, int points) {
        World world = center.getWorld();
        double stage = (tick % 18) / 18.0;
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) Math.max(1, points - 1)) * 2;
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double theta = Math.PI * (3 - Math.sqrt(5)) * i;
            spawn(world, center, i % 5 == 0 ? secondary : primary, Math.cos(theta) * r * radius * stage, height * 0.55 + y * radius * stage, Math.sin(theta) * r * radius * stage, 1, 0, 0, 0, 0);
        }
    }

    private void spawn(World world, Location center, Particle particle, double x, double y, double z, int count, double ox, double oy, double oz, double extra) {
        if (world == null || particle == null) {
            return;
        }
        try {
            world.spawnParticle(particle, center.getX() + x, center.getY() + y, center.getZ() + z, count, ox, oy, oz, extra);
        } catch (Throwable ignored) {
        }
    }

    private boolean isRenderable(Location center) {
        World world = center.getWorld();
        if (world == null || !world.isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4)) {
            return false;
        }
        double rangeSquared = 96.0 * 96.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= rangeSquared) {
                return true;
            }
        }
        return false;
    }

    private Particle match(String raw) {
        String normalized = raw == null ? "" : raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        if ("ENCHANT".equals(normalized) || "ENCHANTED".equals(normalized)) {
            normalized = "ENCHANTMENT_TABLE";
        } else if ("DUST".equals(normalized)) {
            normalized = "REDSTONE";
        } else if ("WITCH".equals(normalized)) {
            normalized = "SPELL_WITCH";
        } else if ("FIREWORK".equals(normalized)) {
            normalized = "FIREWORKS_SPARK";
        }
        try {
            return Particle.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            if ("FIREWORKS_SPARK".equals(normalized)) {
                try {
                    return Particle.valueOf("FIREWORK");
                } catch (IllegalArgumentException ignoredAgain) {
                    return null;
                }
            }
            return null;
        }
    }

    private static final class Target {
        private final Location center;
        private final ParticleEffectConfig config;

        private Target(Location center, ParticleEffectConfig config) {
            this.center = center;
            this.config = config;
        }
    }
}

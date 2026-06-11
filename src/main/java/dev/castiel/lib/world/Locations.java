package dev.castiel.lib.world;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Locale;

public final class Locations {
    private Locations() {
    }

    public static String blockKey(Block block) {
        return block == null ? "" : blockKey(block.getLocation());
    }

    public static String blockKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public static String chunkKey(Chunk chunk) {
        return chunk == null ? "" : chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    public static String chunkKey(Location location) {
        return location == null ? "" : chunkKey(location.getChunk());
    }

    public static String precise(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%s:%.4f:%.4f:%.4f:%.2f:%.2f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    public static Location parseBlockKey(String key) {
        String[] parts = key == null ? new String[0] : key.split(":");
        if (parts.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Location parsePrecise(String key) {
        String[] parts = key == null ? new String[0] : key.split(":");
        if (parts.length < 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), yaw, pitch);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

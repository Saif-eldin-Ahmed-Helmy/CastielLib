package dev.castiel.lib.world;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.lang.reflect.Method;
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

    public static String keyedBlockKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return worldKey(location.getWorld()) + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public static String chunkKey(Chunk chunk) {
        return chunk == null ? "" : chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    public static String keyedChunkKey(Chunk chunk) {
        return chunk == null ? "" : worldKey(chunk.getWorld()) + ":" + chunk.getX() + ":" + chunk.getZ();
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

    public static String keyedPrecise(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%s:%.4f:%.4f:%.4f:%.2f:%.2f",
                worldKey(location.getWorld()),
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

    public static Location parseFlexibleBlockKey(String key) {
        ParsedLocationKey parsed = parseLocationKey(key, 3);
        if (parsed == null || parsed.values.length != 3) {
            return null;
        }
        try {
            return new Location(parsed.world, Integer.parseInt(parsed.values[0]), Integer.parseInt(parsed.values[1]), Integer.parseInt(parsed.values[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Location parseFlexiblePrecise(String key) {
        ParsedLocationKey parsed = parseLocationKey(key, 3);
        if (parsed == null || parsed.values.length < 3) {
            return null;
        }
        try {
            float yaw = parsed.values.length > 3 ? Float.parseFloat(parsed.values[3]) : 0f;
            float pitch = parsed.values.length > 4 ? Float.parseFloat(parsed.values[4]) : 0f;
            return new Location(parsed.world,
                    Double.parseDouble(parsed.values[0]),
                    Double.parseDouble(parsed.values[1]),
                    Double.parseDouble(parsed.values[2]),
                    yaw,
                    pitch);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String worldKey(World world) {
        if (world == null) {
            return "";
        }
        try {
            Method method = world.getClass().getMethod("getKey");
            Object key = method.invoke(world);
            return String.valueOf(key);
        } catch (ReflectiveOperationException ignored) {
            return world.getName();
        }
    }

    public static World worldByNameOrKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        World byName = Bukkit.getWorld(value);
        if (byName != null) {
            return byName;
        }
        if (value.indexOf(':') >= 0) {
            try {
                World byKey = Bukkit.getWorld(NamespacedKey.fromString(value));
                if (byKey != null) {
                    return byKey;
                }
            } catch (Throwable ignored) {
            }
        }
        for (World world : Bukkit.getWorlds()) {
            if (value.equalsIgnoreCase(worldKey(world))) {
                return world;
            }
        }
        return null;
    }

    private static ParsedLocationKey parseLocationKey(String key, int minValues) {
        String[] parts = key == null ? new String[0] : key.split(":");
        if (parts.length < minValues + 1) {
            return null;
        }
        for (int worldParts = parts.length - minValues; worldParts >= 1; worldParts--) {
            StringBuilder worldId = new StringBuilder(parts[0]);
            for (int i = 1; i < worldParts; i++) {
                worldId.append(':').append(parts[i]);
            }
            World world = worldByNameOrKey(worldId.toString());
            if (world == null) {
                continue;
            }
            String[] values = new String[parts.length - worldParts];
            System.arraycopy(parts, worldParts, values, 0, values.length);
            return new ParsedLocationKey(world, values);
        }
        return null;
    }

    private static final class ParsedLocationKey {
        private final World world;
        private final String[] values;

        private ParsedLocationKey(World world, String[] values) {
            this.world = world;
            this.values = values;
        }
    }
}

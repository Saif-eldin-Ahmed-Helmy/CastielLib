package dev.castiel.lib.hologram;

import dev.castiel.lib.util.Placeholders;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HologramManager implements Listener {
    private static final double ACTIVE_RANGE = 56.0;
    private final JavaPlugin plugin;
    private final Map<String, Hologram> holograms = new LinkedHashMap<String, Hologram>();
    private final Map<String, Record> records = new LinkedHashMap<String, Record>();
    private final BukkitTask refreshTask;

    public HologramManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                refreshLoaded(false);
            }
        }, 100L, 100L);
    }

    public Hologram show(String id, Location base, HologramOptions options, Placeholders placeholders) {
        return show(id, base, options, placeholders, false);
    }

    public Hologram show(String id, Location base, HologramOptions options, Placeholders placeholders, boolean force) {
        records.put(id, new Record(base == null ? null : base.clone(), options, placeholders == null ? Placeholders.empty() : placeholders));
        Hologram hologram = holograms.get(id);
        if (hologram == null) {
            hologram = new Hologram(plugin, id);
            holograms.put(id, hologram);
        }
        refresh(id, force);
        return hologram;
    }

    public void remove(String id) {
        records.remove(id);
        Hologram hologram = holograms.remove(id);
        if (hologram != null) {
            hologram.remove();
        }
    }

    public void clear() {
        for (Hologram hologram : holograms.values()) {
            hologram.remove();
        }
        holograms.clear();
        records.clear();
    }

    public void shutdown() {
        refreshTask.cancel();
        clear();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                refreshChunk(chunk, false);
            }
        }, 2L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleRefreshNear(event.getPlayer().getLocation(), false);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null) {
            scheduleRefreshNear(event.getTo(), false);
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        scheduleRefreshNear(event.getPlayer().getLocation(), false);
    }

    private void scheduleRefreshNear(final Location location, final boolean force) {
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                refreshNear(location, 6, force);
            }
        }, 10L);
    }

    private void refreshLoaded(boolean force) {
        for (String id : new java.util.ArrayList<String>(records.keySet())) {
            refresh(id, force);
        }
    }

    private void refreshChunk(Chunk chunk, boolean force) {
        for (Map.Entry<String, Record> entry : new java.util.ArrayList<Map.Entry<String, Record>>(records.entrySet())) {
            Location base = entry.getValue().base;
            if (base == null || base.getWorld() == null || !base.getWorld().equals(chunk.getWorld())) {
                continue;
            }
            if (base.getBlockX() >> 4 == chunk.getX() && base.getBlockZ() >> 4 == chunk.getZ()) {
                refresh(entry.getKey(), force);
            }
        }
    }

    private void refreshNear(Location center, int chunkRadius, boolean force) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        int centerX = center.getBlockX() >> 4;
        int centerZ = center.getBlockZ() >> 4;
        for (Map.Entry<String, Record> entry : new java.util.ArrayList<Map.Entry<String, Record>>(records.entrySet())) {
            Location base = entry.getValue().base;
            if (base == null || base.getWorld() == null || !base.getWorld().equals(world)) {
                continue;
            }
            int x = base.getBlockX() >> 4;
            int z = base.getBlockZ() >> 4;
            if (Math.abs(x - centerX) <= chunkRadius && Math.abs(z - centerZ) <= chunkRadius) {
                refresh(entry.getKey(), force);
            }
        }
    }

    private void refresh(String id, boolean force) {
        Record record = records.get(id);
        if (record == null || !isLoaded(record.base)) {
            return;
        }
        Hologram hologram = holograms.get(id);
        if (hologram == null) {
            hologram = new Hologram(plugin, id);
            holograms.put(id, hologram);
        }
        if (!hasNearbyViewer(record.base)) {
            if (hologram.hasLiveEntities()) {
                hologram.remove();
            }
            return;
        }
        if (force || !hologram.hasLiveEntities()) {
            hologram.spawn(record.base, record.options, record.placeholders);
        }
    }

    private boolean isLoaded(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private boolean hasNearbyViewer(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        double rangeSquared = ACTIVE_RANGE * ACTIVE_RANGE;
        for (Player player : location.getWorld().getPlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(location) <= rangeSquared) {
                return true;
            }
        }
        return false;
    }

    private static final class Record {
        private final Location base;
        private final HologramOptions options;
        private final Placeholders placeholders;

        private Record(Location base, HologramOptions options, Placeholders placeholders) {
            this.base = base;
            this.options = options;
            this.placeholders = placeholders;
        }
    }
}

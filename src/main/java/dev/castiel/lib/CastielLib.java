package dev.castiel.lib;

import dev.castiel.lib.actions.ActionRegistry;
import dev.castiel.lib.commands.CommandRegistry;
import dev.castiel.lib.config.ConfigManager;
import dev.castiel.lib.database.DatabaseManager;
import dev.castiel.lib.effects.ParticleEffectEngine;
import dev.castiel.lib.hologram.HologramManager;
import dev.castiel.lib.inventory.InventoryManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CastielLib {
    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final ActionRegistry actions;
    private final InventoryManager inventories;
    private final CommandRegistry commands;
    private final ParticleEffectEngine particles;
    private final HologramManager holograms;
    private DatabaseManager database;

    private CastielLib(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configs = new ConfigManager(plugin);
        this.actions = ActionRegistry.defaults(plugin);
        this.inventories = new InventoryManager(plugin, actions);
        this.commands = new CommandRegistry(plugin);
        this.particles = new ParticleEffectEngine(plugin);
        this.holograms = new HologramManager(plugin);
    }

    public static CastielLib bind(JavaPlugin plugin) {
        return new CastielLib(plugin);
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public ConfigManager configs() {
        return configs;
    }

    public ActionRegistry actions() {
        return actions;
    }

    public InventoryManager inventories() {
        return inventories;
    }

    public CommandRegistry commands() {
        return commands;
    }

    public ParticleEffectEngine particles() {
        return particles;
    }

    public HologramManager holograms() {
        return holograms;
    }

    public DatabaseManager database() {
        if (database == null) {
            throw new IllegalStateException("Database has not been configured.");
        }
        return database;
    }

    public CastielLib database(DatabaseManager database) {
        this.database = Objects.requireNonNull(database, "database");
        return this;
    }

    public void shutdown() {
        particles.stop();
        holograms.shutdown();
        if (database != null) {
            database.close();
        }
    }
}

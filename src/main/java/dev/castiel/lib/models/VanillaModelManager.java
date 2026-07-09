package dev.castiel.lib.models;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Objects;

/**
 * Loads and spawns vanilla-only custom models without resource packs.
 */
public final class VanillaModelManager {
    private final JavaPlugin plugin;
    private final ServerCapabilities capabilities;

    public VanillaModelManager(JavaPlugin plugin) {
        this(plugin, ServerCapabilities.runtime());
    }

    public VanillaModelManager(JavaPlugin plugin, ServerCapabilities capabilities) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    public ServerCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Loads the CustomMobs YAML bone format.
     */
    public ModelDefinition loadCustomMobs(File file) {
        return ModelLoaders.customMobs(file);
    }

    /**
     * Loads the CustomExtras Treasure flat model-part format.
     */
    public ModelDefinition loadTreasure(File file) {
        return ModelLoaders.treasure(file);
    }

    /**
     * Loads Blockbench .bbmodel cube geometry using vanilla material names.
     */
    public ModelDefinition loadBlockbench(File file, Map<String, String> materials, String defaultMaterial) {
        return ModelLoaders.blockbench(file, materials, defaultMaterial);
    }

    /**
     * Spawns a model at a living entity's current location. Call
     * {@link SpawnedModel#syncToDriver(LivingEntity)} from your own tick loop
     * when the driver moves.
     */
    public SpawnedModel spawnDriven(ModelDefinition definition, LivingEntity driver, ModelSpawnOptions options) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null.");
        }
        return spawn(definition, driver.getLocation(), options);
    }

    /**
     * Spawns a static vanilla model at a world location.
     */
    public SpawnedModel spawn(ModelDefinition definition, Location location, ModelSpawnOptions options) {
        ModelSpawnOptions safeOptions = options == null ? ModelSpawnOptions.defaults() : options;
        if (capabilities.supportsDisplayEntities()) {
            return DisplayEntitySpawner.spawn(definition, location, safeOptions);
        }
        if (safeOptions.allowArmorStandFallback()) {
            return ArmorStandModelSpawner.spawn(definition, location, safeOptions);
        }
        throw new UnsupportedOperationException("Display entities are not supported on this server version.");
    }

    public JavaPlugin plugin() {
        return plugin;
    }
}

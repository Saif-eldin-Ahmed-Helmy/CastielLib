package dev.castiel.lib.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Handle for a spawned model's managed visual entities.
 */
public final class SpawnedModel {
    private final ModelDefinition definition;
    private final List<ModelEntity> entities;
    private final boolean armorStandFallback;

    SpawnedModel(ModelDefinition definition, List<ModelEntity> entities, boolean armorStandFallback) {
        this.definition = definition;
        this.entities = Collections.unmodifiableList(new ArrayList<ModelEntity>(entities));
        this.armorStandFallback = armorStandFallback;
    }

    public ModelDefinition definition() {
        return definition;
    }

    public List<UUID> entityIds() {
        List<UUID> ids = new ArrayList<UUID>();
        for (ModelEntity entity : entities) {
            ids.add(entity.entityId);
        }
        return Collections.unmodifiableList(ids);
    }

    public boolean armorStandFallback() {
        return armorStandFallback;
    }

    public boolean isValid() {
        for (ModelEntity entity : entities) {
            Entity bukkitEntity = Bukkit.getEntity(entity.entityId);
            if (bukkitEntity == null || !bukkitEntity.isValid()) {
                return false;
            }
        }
        return true;
    }

    public void remove() {
        for (ModelEntity entity : entities) {
            Entity bukkitEntity = Bukkit.getEntity(entity.entityId);
            if (bukkitEntity != null) {
                bukkitEntity.remove();
            }
        }
    }

    public void syncToDriver(LivingEntity driver) {
        if (driver != null) {
            syncTo(driver.getLocation());
        }
    }

    public void syncTo(Location baseLocation) {
        if (baseLocation == null || baseLocation.getWorld() == null) {
            return;
        }
        for (ModelEntity entity : entities) {
            Entity bukkitEntity = Bukkit.getEntity(entity.entityId);
            if (bukkitEntity != null && bukkitEntity.isValid()) {
                bukkitEntity.teleport(ModelTransforms.partLocation(baseLocation, definition, entity.part));
            }
        }
    }

    static final class ModelEntity {
        private final ModelPart part;
        private final UUID entityId;

        ModelEntity(ModelPart part, UUID entityId) {
            this.part = part;
            this.entityId = entityId;
        }
    }
}

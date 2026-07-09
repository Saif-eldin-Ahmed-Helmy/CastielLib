package dev.castiel.lib.models;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class ArmorStandModelSpawner {
    private ArmorStandModelSpawner() {
    }

    static SpawnedModel spawn(ModelDefinition definition, Location location, ModelSpawnOptions options) {
        requireWorld(location);
        List<SpawnedModel.ModelEntity> entities = new ArrayList<SpawnedModel.ModelEntity>();
        try {
            for (ModelPart part : definition.parts()) {
                entities.add(spawnPart(definition, part, location, options));
            }
            return new SpawnedModel(definition, entities, true);
        } catch (RuntimeException ex) {
            new SpawnedModel(definition, entities, true).remove();
            throw ex;
        }
    }

    private static SpawnedModel.ModelEntity spawnPart(ModelDefinition definition, ModelPart part, Location base, ModelSpawnOptions options) {
        Location location = ModelTransforms.partLocation(base, definition, part);
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setVisible(false);
        invokeBoolean(stand, "setGravity", false);
        invokeBoolean(stand, "setSilent", true);
        invokeBoolean(stand, "setMarker", true);
        invokeBoolean(stand, "setPersistent", options.persistent());
        stand.getEquipment().setHelmet(new ItemStack(part.material()));
        return new SpawnedModel.ModelEntity(part, stand.getUniqueId());
    }

    private static void invokeBoolean(ArmorStand stand, String methodName, boolean value) {
        try {
            Method method = stand.getClass().getMethod(methodName, Boolean.TYPE);
            method.invoke(stand, value);
        } catch (NoSuchMethodException ignored) {
            return;
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Failed to call ArmorStand#" + methodName + ".", ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("ArmorStand#" + methodName + " rejected the fallback model part.", ex);
        }
    }

    private static void requireWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Model spawn location must have a world.");
        }
    }
}

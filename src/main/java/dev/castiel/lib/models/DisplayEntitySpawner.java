package dev.castiel.lib.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class DisplayEntitySpawner {
    private DisplayEntitySpawner() {
    }

    static SpawnedModel spawn(ModelDefinition definition, Location location, ModelSpawnOptions options) {
        requireWorld(location);
        List<SpawnedModel.ModelEntity> entities = new ArrayList<SpawnedModel.ModelEntity>();
        try {
            for (ModelPart part : definition.parts()) {
                entities.add(spawnPart(definition, part, location, options));
            }
            return new SpawnedModel(definition, entities, false);
        } catch (RuntimeException ex) {
            new SpawnedModel(definition, entities, false).remove();
            throw ex;
        }
    }

    private static SpawnedModel.ModelEntity spawnPart(ModelDefinition definition, ModelPart part, Location base, ModelSpawnOptions options) {
        Class<? extends Entity> type = displayClass(part.displayType());
        Entity entity = spawnEntity(base.getWorld(), ModelTransforms.partLocation(base, definition, part), type);
        configureCommon(entity, options);
        configureVisual(entity, part);
        setMatrix(entity, definition, part);
        return new SpawnedModel.ModelEntity(part, entity.getUniqueId());
    }

    private static Class<? extends Entity> displayClass(ModelDisplayType type) {
        String name = type == ModelDisplayType.ITEM ? "org.bukkit.entity.ItemDisplay" : "org.bukkit.entity.BlockDisplay";
        try {
            return Class.forName(name).asSubclass(Entity.class);
        } catch (ClassNotFoundException ex) {
            throw new UnsupportedOperationException(name + " is not available.", ex);
        }
    }

    private static Entity spawnEntity(World world, Location location, Class<? extends Entity> type) {
        return world.spawn(location, type);
    }

    private static void configureCommon(Entity entity, ModelSpawnOptions options) {
        invokeCompatible(entity, "setGravity", Boolean.FALSE);
        invokeCompatible(entity, "setSilent", Boolean.TRUE);
        invokeCompatible(entity, "setInvulnerable", Boolean.TRUE);
        invoke(entity, "setPersistent", new Class<?>[]{boolean.class}, options.persistent());
        invoke(entity, "setInterpolationDelay", new Class<?>[]{int.class}, 0);
        invoke(entity, "setInterpolationDuration", new Class<?>[]{int.class}, options.interpolationDuration());
        invoke(entity, "setTeleportDuration", new Class<?>[]{int.class}, options.teleportDuration());
        invoke(entity, "setViewRange", new Class<?>[]{float.class}, options.viewRange());
        invoke(entity, "setShadowRadius", new Class<?>[]{float.class}, options.shadowRadius());
        invoke(entity, "setShadowStrength", new Class<?>[]{float.class}, options.shadowStrength());
        setBillboard(entity);
    }

    private static void configureVisual(Entity entity, ModelPart part) {
        if (part.displayType() == ModelDisplayType.ITEM) {
            invoke(entity, "setItemStack", new Class<?>[]{ItemStack.class}, new ItemStack(part.material()));
            setItemTransform(entity);
            return;
        }
        try {
            Object data = part.material().getClass().getMethod("createBlockData").invoke(part.material());
            invokeCompatible(entity, "setBlock", data);
        } catch (ReflectiveOperationException ignored) {
            throw new UnsupportedOperationException("Block display data is unavailable.");
        }
    }

    private static void setBillboard(Entity entity) {
        Object fixed = enumValue("org.bukkit.entity.Display$Billboard", "FIXED");
        if (fixed != null) {
            invoke(entity, "setBillboard", new Class<?>[]{fixed.getClass()}, fixed);
        }
    }

    private static void setItemTransform(Entity entity) {
        Object fixed = enumValue("org.bukkit.entity.ItemDisplay$ItemDisplayTransform", "FIXED");
        if (fixed != null) {
            invoke(entity, "setItemDisplayTransform", new Class<?>[]{fixed.getClass()}, fixed);
        }
    }

    private static Object enumValue(String className, String name) {
        try {
            Class<?> type = Class.forName(className);
            return Enum.valueOf(type.asSubclass(Enum.class), name);
        } catch (ClassNotFoundException | IllegalArgumentException ex) {
            return null;
        }
    }

    private static void setMatrix(Entity entity, ModelDefinition definition, ModelPart part) {
        try {
            Class<?> matrixClass = Class.forName("org.joml.Matrix4f");
            Object matrix = matrixClass.getDeclaredConstructor().newInstance();
            matrix(matrixClass, matrix, part, definition.yawOffset());
            Method method = entity.getClass().getMethod("setTransformationMatrix", matrixClass);
            method.invoke(entity, matrix);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not configure display transformation.", ex);
        }
    }

    private static void matrix(Class<?> type, Object matrix, ModelPart part, double yawOffset) throws ReflectiveOperationException {
        invokeMatrix(type, matrix, "translate", (float) part.localX(), (float) part.localY(), (float) part.localZ());
        invokeMatrix(type, matrix, "rotateXYZ", radians(part.pitch()), radians(part.yaw() + yawOffset), radians(part.roll()));
        invokeMatrix(type, matrix, "scale", (float) part.scaleX(), (float) part.scaleY(), (float) part.scaleZ());
        invokeMatrix(type, matrix, "translate", -0.5f, -0.5f, -0.5f);
    }

    private static void invokeMatrix(Class<?> type, Object matrix, String name, float x, float y, float z) throws ReflectiveOperationException {
        type.getMethod(name, float.class, float.class, float.class).invoke(matrix, x, y, z);
    }

    private static float radians(double degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... values) {
        try {
            target.getClass().getMethod(name, types).invoke(target, values);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void invokeCompatible(Object target, String name, Object value) {
        for (Method method : target.getClass().getMethods()) {
            if (!name.equals(method.getName()) || method.getParameterTypes().length != 1
                    || value == null || !method.getParameterTypes()[0].isAssignableFrom(value.getClass())) continue;
            try {
                method.invoke(target, value);
                return;
            } catch (ReflectiveOperationException ignored) {
                return;
            }
        }
    }

    private static void requireWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Model spawn location must have a world.");
        }
    }
}

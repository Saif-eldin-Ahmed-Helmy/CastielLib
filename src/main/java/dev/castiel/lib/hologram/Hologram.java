package dev.castiel.lib.hologram;

import dev.castiel.lib.text.Colors;
import dev.castiel.lib.util.Placeholders;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class Hologram {
    private final JavaPlugin plugin;
    private final String id;
    private final List<Entity> entities = new ArrayList<Entity>();

    Hologram(JavaPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public void spawn(Location base, HologramOptions options, Placeholders placeholders) {
        remove();
        if (base == null || base.getWorld() == null || options == null || !options.enabled()) {
            return;
        }
        List<String> lines = options.lines();
        double spacing = options.lineSpacing() * options.scale();
        for (int i = 0; i < lines.size(); i++) {
            String line = Colors.color(placeholders.apply(lines.get(i)));
            Location lineLocation = base.clone().add(0, options.yOffset() + (lines.size() - 1 - i) * spacing, 0);
            Entity entity = spawnTextDisplay(lineLocation, line, options.scale());
            if (entity == null) {
                entity = spawnArmorStand(lineLocation, line);
            }
            if (entity != null) {
                entities.add(entity);
            }
        }
    }

    public void remove() {
        for (Entity entity : entities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        entities.clear();
    }

    public boolean hasLiveEntities() {
        if (entities.isEmpty()) {
            return false;
        }
        for (Entity entity : entities) {
            if (entity == null || entity.isDead() || !entity.isValid()) {
                return false;
            }
        }
        return true;
    }

    public String id() {
        return id;
    }

    private Entity spawnTextDisplay(Location location, String line, float scale) {
        try {
            EntityType type = EntityType.valueOf("TEXT_DISPLAY");
            Entity entity = location.getWorld().spawnEntity(location, type);
            invoke(entity, "setText", new Class<?>[]{String.class}, new Object[]{line});
            invokeOptional(entity, "setPersistent", new Class<?>[]{boolean.class}, new Object[]{Boolean.FALSE});
            invokeOptional(entity, "setGravity", new Class<?>[]{boolean.class}, new Object[]{Boolean.FALSE});
            invokeOptional(entity, "setInvulnerable", new Class<?>[]{boolean.class}, new Object[]{Boolean.TRUE});
            invokeOptional(entity, "setViewRange", new Class<?>[]{float.class}, new Object[]{Float.valueOf(96f)});
            setBillboard(entity);
            setScale(entity, scale);
            return entity;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Entity spawnArmorStand(Location location, String line) {
        try {
            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
            stand.setCustomName(line);
            stand.setCustomNameVisible(true);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            try {
                stand.setMarker(true);
            } catch (Throwable ignored) {
            }
            try {
                stand.setInvulnerable(true);
            } catch (Throwable ignored) {
            }
            return stand;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Unable to spawn hologram '" + id + "': " + throwable.getMessage());
            return null;
        }
    }

    private void setBillboard(Entity entity) {
        try {
            Class<?> billboardClass = Class.forName("org.bukkit.entity.Display$Billboard");
            Object center = Enum.valueOf((Class<Enum>) billboardClass.asSubclass(Enum.class), "CENTER");
            invoke(entity, "setBillboard", new Class<?>[]{billboardClass}, new Object[]{center});
        } catch (Throwable ignored) {
        }
    }

    private void setScale(Entity entity, float scale) {
        try {
            Class<?> vectorClass = Class.forName("org.joml.Vector3f");
            Class<?> angleClass = Class.forName("org.joml.AxisAngle4f");
            Class<?> transformationClass = Class.forName("org.bukkit.util.Transformation");
            Constructor<?> vector = vectorClass.getConstructor(float.class, float.class, float.class);
            Constructor<?> angle = angleClass.getConstructor(float.class, float.class, float.class, float.class);
            Object translation = vector.newInstance(0f, 0f, 0f);
            Object leftRotation = angle.newInstance(0f, 0f, 0f, 1f);
            Object scaleVector = vector.newInstance(scale, scale, scale);
            Object rightRotation = angle.newInstance(0f, 0f, 0f, 1f);
            Constructor<?> transformation = transformationClass.getConstructor(vectorClass, angleClass, vectorClass, angleClass);
            Object value = transformation.newInstance(translation, leftRotation, scaleVector, rightRotation);
            invoke(entity, "setTransformation", new Class<?>[]{transformationClass}, new Object[]{value});
        } catch (Throwable ignored) {
        }
    }

    private void invoke(Object target, String method, Class<?>[] types, Object[] args) throws ReflectiveOperationException {
        Method reflected = target.getClass().getMethod(method, types);
        reflected.invoke(target, args);
    }

    private void invokeOptional(Object target, String method, Class<?>[] types, Object[] args) {
        try {
            invoke(target, method, types, args);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}

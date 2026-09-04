package dev.castiel.lib.pdc;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Persistent string tags with no Java 1.13+ Bukkit linkage. On older servers
 * these helpers safely no-op, allowing callers to retain one source path.
 */
public final class PdcTags {
    private PdcTags() {
    }

    @SuppressWarnings("unchecked")
    public static <K> K key(JavaPlugin plugin, String name) {
        try {
            Class<?> keyType = Class.forName("org.bukkit.NamespacedKey");
            Constructor<?> constructor = keyType.getConstructor(JavaPlugin.class, String.class);
            return (K) constructor.newInstance(plugin, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void setString(Object holder, Object key, String value) {
        if (holder == null || key == null) return;
        try {
            Object container = holder.getClass().getMethod("getPersistentDataContainer").invoke(holder);
            Class<?> keyType = key.getClass();
            Class<?> dataType = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = dataType.getField("STRING").get(null);
            if (value == null) invoke(container, "remove", keyType, key);
            else invoke(container, "set", keyType, dataType, key, stringType, value);
        } catch (Throwable ignored) {
        }
    }

    public static String getString(Object holder, Object key) {
        if (holder == null || key == null) return null;
        try {
            Object container = holder.getClass().getMethod("getPersistentDataContainer").invoke(holder);
            Class<?> dataType = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = dataType.getField("STRING").get(null);
            Object value = invoke(container, "get", key.getClass(), dataType, key, stringType);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean hasString(Object holder, Object key) {
        if (holder == null || key == null) return false;
        try {
            Object container = holder.getClass().getMethod("getPersistentDataContainer").invoke(holder);
            Class<?> dataType = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = dataType.getField("STRING").get(null);
            Object value = invoke(container, "has", key.getClass(), dataType, key, stringType);
            return Boolean.TRUE.equals(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setBoolean(Object holder, Object key, boolean value) {
        setString(holder, key, Boolean.toString(value));
    }

    public static boolean getBoolean(Object holder, Object key, boolean fallback) {
        String value = getString(holder, key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static Object invoke(Object target, String name, Class<?> keyType, Object key) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (name.equals(method.getName()) && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].isAssignableFrom(keyType)) {
                return method.invoke(target, key);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Object invoke(Object target, String name, Class<?> keyType, Class<?> dataType,
                                 Object key, Object type, Object value) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (!name.equals(method.getName()) || method.getParameterTypes().length != 3) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters[0].isAssignableFrom(keyType) && parameters[1].isAssignableFrom(dataType)) {
                return method.invoke(target, key, type, value);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Object invoke(Object target, String name, Class<?> keyType, Class<?> dataType,
                                 Object key, Object type) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (!name.equals(method.getName()) || method.getParameterTypes().length != 2) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters[0].isAssignableFrom(keyType) && parameters[1].isAssignableFrom(dataType)) {
                return method.invoke(target, key, type);
            }
        }
        throw new NoSuchMethodException(name);
    }
}

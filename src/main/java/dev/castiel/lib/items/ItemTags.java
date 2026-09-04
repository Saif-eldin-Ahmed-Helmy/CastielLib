package dev.castiel.lib.items;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Cross-version item metadata without linking Java 1.13+ PDC types. */
public final class ItemTags {
    private ItemTags() {
    }

    public static ItemStack setString(JavaPlugin plugin, ItemStack source, String name, String value) {
        if (source == null || name == null || value == null) return source;
        ItemStack result = source.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null || !setPersistent(meta, plugin, name, value)) {
            List<String> lore = meta == null || meta.getLore() == null
                    ? new ArrayList<String>() : new ArrayList<String>(meta.getLore());
            lore.add(marker(plugin, name, value));
            if (meta != null) {
                meta.setLore(lore);
                result.setItemMeta(meta);
            }
            return result;
        }
        result.setItemMeta(meta);
        return result;
    }

    public static String getString(ItemStack source, JavaPlugin plugin, String name) {
        if (source == null || name == null) return null;
        ItemMeta meta = source.getItemMeta();
        if (meta == null) return null;
        String persistent = getPersistent(meta, plugin, name);
        if (persistent != null) return persistent;
        String prefix = "§0castiel:" + name + "=";
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        for (String line : lore) {
            if (line == null || !line.startsWith(prefix)) continue;
            String payload = line.substring(prefix.length());
            int separator = payload.lastIndexOf('|');
            if (separator <= 0) continue;
            String value = payload.substring(0, separator);
            String signature = payload.substring(separator + 1);
            if (ItemIdentitySigner.verifies(plugin.getDataFolder().toPath(), name, value, signature)) return value;
        }
        return null;
    }

    private static String marker(JavaPlugin plugin, String name, String value) {
        return "§0castiel:" + name + "=" + value + "|"
                + ItemIdentitySigner.sign(plugin.getDataFolder().toPath(), name, value);
    }

    private static boolean setPersistent(ItemMeta meta, JavaPlugin plugin, String name, String value) {
        try {
            Class<?> keyType = Class.forName("org.bukkit.NamespacedKey");
            Constructor<?> keyConstructor = keyType.getConstructor(JavaPlugin.class, String.class);
            Object key = keyConstructor.newInstance(plugin, name);
            Object container = meta.getClass().getMethod("getPersistentDataContainer").invoke(meta);
            Class<?> typeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = typeClass.getField("STRING").get(null);
            invokeContainer(container, "set", keyType, typeClass, key, stringType, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String getPersistent(ItemMeta meta, JavaPlugin plugin, String name) {
        try {
            Class<?> keyType = Class.forName("org.bukkit.NamespacedKey");
            Object key = keyType.getConstructor(JavaPlugin.class, String.class).newInstance(plugin, name);
            Object container = meta.getClass().getMethod("getPersistentDataContainer").invoke(meta);
            Class<?> typeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = typeClass.getField("STRING").get(null);
            Object value = invokeContainer(container, "get", keyType, typeClass, key, stringType);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeContainer(Object container, String name, Class<?> keyType, Class<?> dataType,
                                          Object key, Object type, Object... value) throws ReflectiveOperationException {
        for (Method method : container.getClass().getMethods()) {
            if (!name.equals(method.getName())) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if ("set".equals(name) && parameters.length == 3) {
                return method.invoke(container, key, type, value.length == 0 ? null : value[0]);
            }
            if ("get".equals(name) && parameters.length == 2) {
                return method.invoke(container, key, type);
            }
        }
        throw new NoSuchMethodException(name);
    }
}

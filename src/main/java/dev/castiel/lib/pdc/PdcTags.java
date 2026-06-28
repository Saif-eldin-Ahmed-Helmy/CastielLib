package dev.castiel.lib.pdc;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class PdcTags {
    private PdcTags() {
    }

    public static NamespacedKey key(JavaPlugin plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    public static void setString(PersistentDataHolder holder, NamespacedKey key, String value) {
        if (holder == null || key == null) {
            return;
        }
        PersistentDataContainer container = holder.getPersistentDataContainer();
        if (value == null) {
            container.remove(key);
            return;
        }
        container.set(key, PersistentDataType.STRING, value);
    }

    public static String getString(PersistentDataHolder holder, NamespacedKey key) {
        if (holder == null || key == null) {
            return null;
        }
        return holder.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static boolean hasString(PersistentDataHolder holder, NamespacedKey key) {
        return holder != null && key != null && holder.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public static void setBoolean(PersistentDataHolder holder, NamespacedKey key, boolean value) {
        setString(holder, key, Boolean.toString(value));
    }

    public static boolean getBoolean(PersistentDataHolder holder, NamespacedKey key, boolean fallback) {
        String value = getString(holder, key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}

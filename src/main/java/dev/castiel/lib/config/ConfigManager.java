package dev.castiel.lib.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public final class ConfigManager {
    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public <T> T load(String fileName, Class<T> type) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            File file = ensureFile(fileName);
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            boolean changed = false;
            for (Field field : type.getDeclaredFields()) {
                ConfigNode node = field.getAnnotation(ConfigNode.class);
                if (node == null) {
                    continue;
                }
                field.setAccessible(true);
                Object defaultValue = field.get(instance);
                if (!yaml.contains(node.value())) {
                    yaml.set(node.value(), defaultValue);
                    changed = true;
                } else {
                    field.set(instance, yaml.get(node.value()));
                }
            }
            if (changed) {
                yaml.save(file);
            }
            return instance;
        } catch (ReflectiveOperationException | IOException e) {
            throw new IllegalStateException("Unable to load config " + fileName, e);
        }
    }

    public FileConfiguration yaml(String fileName) {
        return YamlConfiguration.loadConfiguration(ensureFile(fileName));
    }

    public void save(String fileName, FileConfiguration configuration) {
        try {
            configuration.save(ensureFile(fileName));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save config " + fileName, e);
        }
    }

    private File ensureFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IllegalStateException("Unable to create config directory " + file.getParent());
        }
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new IOException("createNewFile returned false");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create config " + fileName, e);
            }
        }
        return file;
    }

    @SuppressWarnings("unchecked")
    public static List<String> stringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof List ? (List<String>) value : java.util.Collections.emptyList();
    }
}

package dev.castiel.lib.models;

import dev.castiel.lib.items.ItemStacks;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class YamlModelLoader {
    private YamlModelLoader() {
    }

    static ModelDefinition customMobs(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("id", stripExtension(file));
        double yawOffset = yaml.getDouble("model.yaw-offset", 0.0);
        ConfigurationSection bones = section(yaml, "bones");
        List<ModelPart> parts = new ArrayList<ModelPart>();
        for (String name : bones.getKeys(false)) {
            parts.add(customMobPart(name, section(bones, name)));
        }
        return new ModelDefinition(id, yawOffset, parts);
    }

    static ModelDefinition treasure(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection model = section(yaml, "model");
        List<ModelPart> parts = new ArrayList<ModelPart>();
        for (Map<?, ?> raw : model.getMapList("parts")) {
            parts.add(treasurePart(raw));
        }
        return new ModelDefinition(stripExtension(file), 0.0, parts);
    }

    private static ModelPart customMobPart(String name, ConfigurationSection section) {
        ModelDisplayType type = displayType(section.getString("display", "BLOCK"));
        Material material = material(section.getString("material", "STONE"));
        VectorData offset = vector(section.getConfigurationSection("offset"), 0.0);
        VectorData scale = scale(section.get("scale"), section.getConfigurationSection("scale"));
        ConfigurationSection rotation = section.getConfigurationSection("rotation");
        return new ModelPart(name, type, material, offset.x, offset.y, offset.z, scale.x, scale.y, scale.z, value(rotation, "pitch"), value(rotation, "yaw"), value(rotation, "roll"));
    }

    private static ModelPart treasurePart(Map<?, ?> raw) {
        String id = string(raw.get("id"), "part");
        Material material = material(string(raw.get("material"), "STONE"));
        double sx = number(raw.get("scale-x"), 1.0);
        double sy = number(raw.get("scale-y"), 1.0);
        double sz = number(raw.get("scale-z"), 1.0);
        double y = number(raw.get("offset-y"), 0.0) + sy * 0.5;
        return new ModelPart(id, ModelDisplayType.BLOCK, material, number(raw.get("offset-x"), 0.0), y, number(raw.get("offset-z"), 0.0), sx, sy, sz, number(raw.get("rotation-x"), 0.0), number(raw.get("rotation-y"), 0.0), number(raw.get("rotation-z"), 0.0));
    }

    private static ModelDisplayType displayType(String raw) {
        return "ITEM".equalsIgnoreCase(raw) ? ModelDisplayType.ITEM : ModelDisplayType.BLOCK;
    }

    static Material material(String raw) {
        Material material = ItemStacks.resolveMaterial(raw, Material.STONE);
        return material == null || material == Material.AIR ? Material.STONE : material;
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing section: " + path);
        }
        return section;
    }

    private static VectorData scale(Object raw, ConfigurationSection section) {
        if (raw instanceof Number) {
            double value = ((Number) raw).doubleValue();
            return new VectorData(value, value, value);
        }
        return vector(section, 1.0);
    }

    private static VectorData vector(ConfigurationSection section, double fallback) {
        if (section == null) {
            return new VectorData(fallback, fallback, fallback);
        }
        return new VectorData(section.getDouble("x", fallback), section.getDouble("y", fallback), section.getDouble("z", fallback));
    }

    private static double value(ConfigurationSection section, String key) {
        return section == null ? 0.0 : section.getDouble(key, 0.0);
    }

    static double number(Object value, double fallback) {
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    static String stripExtension(File file) {
        String name = file == null ? "model" : file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 1 ? name : name.substring(0, dot);
    }

    static final class VectorData {
        final double x;
        final double y;
        final double z;

        VectorData(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}

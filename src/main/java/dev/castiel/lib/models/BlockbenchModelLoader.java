package dev.castiel.lib.models;

import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BlockbenchModelLoader {
    private BlockbenchModelLoader() {
    }

    static ModelDefinition load(File file, Map<String, String> materials, String defaultMaterial) {
        Map<String, Object> root = SimpleJson.parseObject(read(file));
        Map<String, String> groups = new LinkedHashMap<String, String>();
        collectGroups(root.get("outliner"), "", groups);
        List<ModelPart> parts = parts(root, groups, normalizeMap(materials), defaultMaterial);
        return new ModelDefinition(YamlModelLoader.string(root.get("name"), YamlModelLoader.stripExtension(file)), 0.0, parts);
    }

    private static List<ModelPart> parts(Map<String, Object> root, Map<String, String> groups, Map<String, String> materials, String fallback) {
        List<ModelPart> parts = new ArrayList<ModelPart>();
        Map<String, String> textures = textures(root.get("textures"));
        for (Object value : list(root.get("elements"))) {
            if (value instanceof Map) {
                parts.add(part(castMap(value), groups, textures, materials, fallback));
            }
        }
        return parts;
    }

    private static ModelPart part(Map<String, Object> cube, Map<String, String> groups, Map<String, String> textures, Map<String, String> materials, String fallback) {
        String id = YamlModelLoader.string(cube.get("name"), YamlModelLoader.string(cube.get("uuid"), "cube"));
        VectorData from = vector(list(cube.get("from")), 0.0);
        VectorData to = vector(list(cube.get("to")), 0.0);
        VectorData center = from.midpoint(to).divide(16.0);
        VectorData size = from.sizeTo(to).divide(16.0);
        VectorData origin = vector(list(cube.get("origin")), 0.0).divide(16.0);
        VectorData rotation = vector(list(cube.get("rotation")), 0.0);
        String group = groups.get(YamlModelLoader.string(cube.get("uuid"), ""));
        String texture = firstTexture(cube.get("faces"), textures);
        Material material = YamlModelLoader.material(materialName(id, group, texture, materials, fallback));
        return new ModelPart(id, ModelDisplayType.BLOCK, material, origin.x, origin.y, origin.z, center.x - origin.x, center.y - origin.y, center.z - origin.z, size.x, size.y, size.z, rotation.x, rotation.y, rotation.z);
    }

    private static String materialName(String cube, String group, String texture, Map<String, String> materials, String fallback) {
        String found = firstMapped(materials, cube, group, texture);
        return found == null ? fallback : found;
    }

    private static String firstMapped(Map<String, String> materials, String... keys) {
        for (String key : keys) {
            String value = key == null ? null : materials.get(key.toLowerCase(Locale.ROOT));
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static Map<String, String> textures(Object raw) {
        Map<String, String> textures = new LinkedHashMap<String, String>();
        int index = 0;
        for (Object value : list(raw)) {
            if (value instanceof Map) {
                Map<String, Object> map = castMap(value);
                String name = YamlModelLoader.string(map.get("name"), YamlModelLoader.string(map.get("id"), String.valueOf(index)));
                textures.put(String.valueOf(index), name);
                textures.put("#" + index, name);
            }
            index++;
        }
        return textures;
    }

    private static String firstTexture(Object rawFaces, Map<String, String> textures) {
        if (!(rawFaces instanceof Map)) {
            return null;
        }
        for (Object face : ((Map<?, ?>) rawFaces).values()) {
            if (face instanceof Map) {
                String texture = YamlModelLoader.string(((Map<?, ?>) face).get("texture"), null);
                return textures.containsKey(texture) ? textures.get(texture) : texture;
            }
        }
        return null;
    }

    private static void collectGroups(Object node, String group, Map<String, String> groups) {
        for (Object child : list(node)) {
            if (child instanceof String) {
                groups.put((String) child, group);
            } else if (child instanceof Map) {
                Map<String, Object> map = castMap(child);
                String name = YamlModelLoader.string(map.get("name"), group);
                collectGroups(map.get("children"), name, groups);
            }
        }
    }

    private static VectorData vector(List<Object> list, double fallback) {
        return new VectorData(numberAt(list, 0, fallback), numberAt(list, 1, fallback), numberAt(list, 2, fallback));
    }

    private static double numberAt(List<Object> values, int index, double fallback) {
        return index < values.size() ? YamlModelLoader.number(values.get(index), fallback) : fallback;
    }

    private static List<Object> list(Object value) {
        return value instanceof List ? (List<Object>) value : Collections.<Object>emptyList();
    }

    private static Map<String, String> normalizeMap(Map<String, String> values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (values == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return result;
    }

    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static String read(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read model file: " + file, ex);
        }
    }

    private static final class VectorData {
        private final double x;
        private final double y;
        private final double z;

        private VectorData(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private VectorData divide(double divisor) {
            return new VectorData(x / divisor, y / divisor, z / divisor);
        }

        private VectorData midpoint(VectorData other) {
            return new VectorData((x + other.x) * 0.5, (y + other.y) * 0.5, (z + other.z) * 0.5);
        }

        private VectorData sizeTo(VectorData other) {
            return new VectorData(Math.abs(other.x - x), Math.abs(other.y - y), Math.abs(other.z - z));
        }
    }
}

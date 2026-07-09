package dev.castiel.lib.models;

import java.io.File;
import java.util.Map;

final class ModelLoaders {
    private ModelLoaders() {
    }

    static ModelDefinition customMobs(File file) {
        return YamlModelLoader.customMobs(file);
    }

    static ModelDefinition treasure(File file) {
        return YamlModelLoader.treasure(file);
    }

    static ModelDefinition blockbench(File file, Map<String, String> materials, String defaultMaterial) {
        return BlockbenchModelLoader.load(file, materials, defaultMaterial);
    }
}

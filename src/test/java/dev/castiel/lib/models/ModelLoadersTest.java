package dev.castiel.lib.models;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelLoadersTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsCustomMobsBones() throws Exception {
        File file = write("elephant.yml",
                "id: elephant\n" +
                        "model:\n" +
                        "  yaw-offset: 180.0\n" +
                        "bones:\n" +
                        "  head:\n" +
                        "    display: ITEM\n" +
                        "    material: ZOMBIE_HEAD\n" +
                        "    offset: { x: 0.0, y: 2.0, z: 1.0 }\n" +
                        "    scale: { x: 1.5, y: 1.5, z: 1.5 }\n" +
                        "    rotation: { pitch: 0.0, yaw: 180.0, roll: 0.0 }\n");

        ModelDefinition model = ModelLoaders.customMobs(file);
        ModelPart part = model.parts().get(0);

        assertEquals("elephant", model.id());
        assertEquals(180.0, model.yawOffset());
        assertEquals(ModelDisplayType.ITEM, part.displayType());
        assertEquals(Material.ZOMBIE_HEAD, part.material());
        assertEquals(2.0, part.offsetY());
        assertEquals(1.5, part.scaleX());
        assertEquals(180.0, part.yaw());
    }

    @Test
    void loadsTreasurePartsWithBottomAnchoredY() throws Exception {
        File file = write("geode.yml",
                "model:\n" +
                        "  parts:\n" +
                        "    - id: shell\n" +
                        "      material: STONE\n" +
                        "      offset-y: 0.1\n" +
                        "      scale-x: 1.2\n" +
                        "      scale-y: 0.8\n" +
                        "      scale-z: 1.1\n");

        ModelPart part = ModelLoaders.treasure(file).parts().get(0);

        assertEquals("shell", part.id());
        assertEquals(Material.STONE, part.material());
        assertEquals(0.5, part.offsetY(), 0.0001);
    }

    @Test
    void loadsBlockbenchCubesWithMaterialPrecedence() throws Exception {
        File file = write("model.bbmodel",
                "{\n" +
                        "  \"name\": \"rock\",\n" +
                        "  \"textures\": [{\"name\":\"stone_texture\"}],\n" +
                        "  \"outliner\": [{\"name\":\"outer_shell\",\"children\":[\"cube-1\"]}],\n" +
                        "  \"elements\": [{\n" +
                        "    \"uuid\": \"cube-1\",\n" +
                        "    \"name\": \"crystal\",\n" +
                        "    \"from\": [0, 0, 0],\n" +
                        "    \"to\": [16, 8, 16],\n" +
                        "    \"origin\": [8, 0, 8],\n" +
                        "    \"rotation\": [5, 10, 15],\n" +
                        "    \"faces\": {\"north\":{\"texture\":\"#0\"}}\n" +
                        "  }]\n" +
                        "}\n");
        Map<String, String> materials = new HashMap<String, String>();
        materials.put("stone_texture", "STONE");
        materials.put("outer_shell", "DEEPSLATE");
        materials.put("crystal", "EMERALD_BLOCK");

        ModelPart part = ModelLoaders.blockbench(file, materials, "DIRT").parts().get(0);

        assertEquals(Material.EMERALD_BLOCK, part.material());
        assertEquals(0.5, part.offsetX(), 0.0001);
        assertEquals(0.25, part.localY(), 0.0001);
        assertEquals(5.0, part.pitch());
        assertEquals(10.0, part.yaw());
        assertEquals(15.0, part.roll());
    }

    private File write(String name, String text) throws Exception {
        Path path = tempDir.resolve(name);
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
        return path.toFile();
    }
}

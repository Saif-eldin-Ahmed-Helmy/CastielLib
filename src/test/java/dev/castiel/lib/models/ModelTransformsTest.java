package dev.castiel.lib.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelTransformsTest {
    @Test
    void partLocationKeepsBaseYawWithoutDoubleApplyingModelYawOffset() {
        ModelPart part = new ModelPart("body", ModelDisplayType.BLOCK, Material.STONE, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        ModelDefinition definition = new ModelDefinition("mob", 180.0, Collections.singletonList(part));
        Location base = new Location(null, 0.0, 0.0, 0.0, 90.0f, 0.0f);

        Location result = ModelTransforms.partLocation(base, definition, part);

        assertEquals(90.0f, result.getYaw());
        assertEquals(0.0, result.getX(), 0.0001);
        assertEquals(1.0, result.getZ(), 0.0001);
    }
}

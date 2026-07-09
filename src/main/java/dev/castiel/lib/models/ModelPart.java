package dev.castiel.lib.models;

import org.bukkit.Material;

import java.util.Objects;

/**
 * One cuboid or item part in a vanilla model.
 */
public final class ModelPart {
    private final String id;
    private final ModelDisplayType displayType;
    private final Material material;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double localX;
    private final double localY;
    private final double localZ;
    private final double scaleX;
    private final double scaleY;
    private final double scaleZ;
    private final double pitch;
    private final double yaw;
    private final double roll;

    public ModelPart(
            String id,
            ModelDisplayType displayType,
            Material material,
            double offsetX,
            double offsetY,
            double offsetZ,
            double scaleX,
            double scaleY,
            double scaleZ,
            double pitch,
            double yaw,
            double roll) {
        this(id, displayType, material, offsetX, offsetY, offsetZ, 0.0, 0.0, 0.0, scaleX, scaleY, scaleZ, pitch, yaw, roll);
    }

    public ModelPart(
            String id,
            ModelDisplayType displayType,
            Material material,
            double offsetX,
            double offsetY,
            double offsetZ,
            double localX,
            double localY,
            double localZ,
            double scaleX,
            double scaleY,
            double scaleZ,
            double pitch,
            double yaw,
            double roll) {
        this.id = requireId(id);
        this.displayType = Objects.requireNonNull(displayType, "displayType");
        this.material = Objects.requireNonNull(material, "material");
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.localX = localX;
        this.localY = localY;
        this.localZ = localZ;
        this.scaleX = positive(scaleX, "scaleX");
        this.scaleY = positive(scaleY, "scaleY");
        this.scaleZ = positive(scaleZ, "scaleZ");
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public String id() {
        return id;
    }

    public ModelDisplayType displayType() {
        return displayType;
    }

    public Material material() {
        return material;
    }

    public double offsetX() {
        return offsetX;
    }

    public double offsetY() {
        return offsetY;
    }

    public double offsetZ() {
        return offsetZ;
    }

    public double localX() {
        return localX;
    }

    public double localY() {
        return localY;
    }

    public double localZ() {
        return localZ;
    }

    public double scaleX() {
        return scaleX;
    }

    public double scaleY() {
        return scaleY;
    }

    public double scaleZ() {
        return scaleZ;
    }

    public double pitch() {
        return pitch;
    }

    public double yaw() {
        return yaw;
    }

    public double roll() {
        return roll;
    }

    private static String requireId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Model part id cannot be blank.");
        }
        return id;
    }

    private static double positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
        return value;
    }
}

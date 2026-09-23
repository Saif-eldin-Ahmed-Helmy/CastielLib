package dev.castiel.lib.models;

import org.bukkit.Location;

final class ModelTransforms {
    private ModelTransforms() {
    }

    static Location partLocation(Location base, ModelDefinition definition, ModelPart part) {
        double radians = Math.toRadians(base.getYaw());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = part.offsetX() * cos - part.offsetZ() * sin;
        double z = part.offsetX() * sin + part.offsetZ() * cos;
        Location location = base.clone().add(x, part.offsetY(), z);
        location.setYaw(base.getYaw());
        location.setPitch(0.0f);
        return location;
    }
}

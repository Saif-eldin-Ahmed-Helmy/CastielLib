package dev.castiel.lib.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable vanilla model definition made from named display parts.
 */
public final class ModelDefinition {
    private final String id;
    private final double yawOffset;
    private final List<ModelPart> parts;

    public ModelDefinition(String id, double yawOffset, List<ModelPart> parts) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Model id cannot be blank.");
        }
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Model must contain at least one part.");
        }
        this.id = id;
        this.yawOffset = yawOffset;
        this.parts = Collections.unmodifiableList(new ArrayList<ModelPart>(parts));
    }

    public String id() {
        return id;
    }

    public double yawOffset() {
        return yawOffset;
    }

    public List<ModelPart> parts() {
        return parts;
    }
}

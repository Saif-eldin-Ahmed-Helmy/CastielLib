package dev.castiel.lib.hologram;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HologramOptions {
    private final boolean enabled;
    private final List<String> lines;
    private final double yOffset;
    private final double lineSpacing;
    private final float scale;

    public HologramOptions(boolean enabled, List<String> lines, double yOffset, double lineSpacing, float scale) {
        this.enabled = enabled;
        this.lines = lines == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<String>(lines));
        this.yOffset = yOffset;
        this.lineSpacing = lineSpacing <= 0 ? 0.28 : lineSpacing;
        this.scale = scale <= 0 ? 1.0f : scale;
    }

    public static HologramOptions from(ConfigurationSection section) {
        if (section == null) {
            return new HologramOptions(false, Collections.<String>emptyList(), 1.65, 0.28, 1.0f);
        }
        return new HologramOptions(
                section.getBoolean("enabled", section.getBoolean("Enabled", true)),
                section.getStringList("lines").isEmpty() ? section.getStringList("Lines") : section.getStringList("lines"),
                section.getDouble("y-offset", section.getDouble("Y-Offset", 1.65)),
                section.getDouble("line-spacing", section.getDouble("Line-Spacing", 0.28)),
                (float) section.getDouble("scale", section.getDouble("Scale", 1.0))
        );
    }

    public boolean enabled() {
        return enabled;
    }

    public List<String> lines() {
        return lines;
    }

    public double yOffset() {
        return yOffset;
    }

    public double lineSpacing() {
        return lineSpacing;
    }

    public float scale() {
        return scale;
    }
}

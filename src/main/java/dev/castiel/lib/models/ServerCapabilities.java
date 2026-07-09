package dev.castiel.lib.models;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime Minecraft capability checks used before spawning modern model
 * entities.
 */
public final class ServerCapabilities {
    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private final int major;
    private final int minor;
    private final int patch;
    private final boolean displayEntities;

    public ServerCapabilities(int major, int minor, int patch, boolean displayEntities) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.displayEntities = displayEntities;
    }

    public static ServerCapabilities runtime() {
        ServerCapabilities parsed = fromVersion(Bukkit.getBukkitVersion());
        return new ServerCapabilities(parsed.major, parsed.minor, parsed.patch, hasDisplayClasses(parsed.displayEntities));
    }

    public static ServerCapabilities fromVersion(String version) {
        Matcher matcher = VERSION.matcher(version == null ? "" : version);
        if (!matcher.find()) {
            return new ServerCapabilities(0, 0, 0, false);
        }
        int major = integer(matcher.group(1));
        int minor = integer(matcher.group(2));
        int patch = integer(matcher.group(3));
        return new ServerCapabilities(major, minor, patch, supportsDisplayVersion(major, minor, patch));
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public boolean supportsDisplayEntities() {
        return displayEntities;
    }

    public boolean canSpawnModels(ModelSpawnOptions options) {
        return displayEntities || (options != null && options.allowArmorStandFallback());
    }

    private static boolean supportsDisplayVersion(int major, int minor, int patch) {
        return major > 1 || minor > 19 || (minor == 19 && patch >= 4);
    }

    private static boolean hasDisplayClasses(boolean versionAllows) {
        return versionAllows && hasClass("org.bukkit.entity.BlockDisplay") && hasClass("org.bukkit.entity.ItemDisplay");
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static int integer(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}

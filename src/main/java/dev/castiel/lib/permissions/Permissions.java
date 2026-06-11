package dev.castiel.lib.permissions;

import dev.castiel.lib.text.Colors;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Permissions {
    private Permissions() {
    }

    public static boolean has(CommandSender sender, String permission) {
        return hasManual(sender, permission, false);
    }

    public static boolean hasWildcard(CommandSender sender, String permission) {
        return hasManual(sender, permission, true);
    }

    public static boolean hasManual(CommandSender sender, String permission, boolean wildcard) {
        String node = normalize(permission);
        if (node.isEmpty()) {
            return true;
        }
        if (sender == null) {
            return false;
        }
        if (!(sender instanceof Player)) {
            return true;
        }
        Map<String, Boolean> effective = effectivePermissions(sender);
        Boolean exact = effective.get(node);
        if (exact != null) {
            return exact.booleanValue();
        }
        if (wildcard) {
            Boolean global = effective.get("*");
            if (global != null) {
                return global.booleanValue();
            }
            String parent = node;
            while (parent.contains(".")) {
                parent = parent.substring(0, parent.lastIndexOf('.'));
                Boolean inherited = effective.get(parent + ".*");
                if (inherited != null) {
                    return inherited.booleanValue();
                }
            }
        }
        return sender.isOp();
    }

    public static boolean hasWithAdmin(CommandSender sender, String permission, String adminPermission) {
        return hasWildcard(sender, permission) || hasWildcard(sender, adminPermission);
    }

    public static boolean hasAny(CommandSender sender, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        for (String permission : permissions) {
            if (hasWildcard(sender, permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAll(CommandSender sender, String... permissions) {
        if (permissions == null) {
            return true;
        }
        for (String permission : permissions) {
            if (!hasWildcard(sender, permission)) {
                return false;
            }
        }
        return true;
    }

    public static boolean require(CommandSender sender, String permission, String deniedMessage) {
        if (hasWildcard(sender, permission)) {
            return true;
        }
        deny(sender, deniedMessage);
        return false;
    }

    public static void deny(CommandSender sender, String message) {
        if (sender != null && message != null && !message.trim().isEmpty()) {
            sender.sendMessage(Colors.color(message));
        }
    }

    public static int highestNumbered(CommandSender sender, String prefix, int min, int max, int fallback) {
        int safeMin = Math.min(min, max);
        int safeMax = Math.max(min, max);
        for (int value = safeMax; value >= safeMin; value--) {
            if (hasWildcard(sender, prefix + "." + value)) {
                return value;
            }
        }
        return fallback;
    }

    private static Map<String, Boolean> effectivePermissions(CommandSender sender) {
        Map<String, Boolean> permissions = new HashMap<String, Boolean>();
        for (PermissionAttachmentInfo info : sender.getEffectivePermissions()) {
            permissions.put(normalize(info.getPermission()), Boolean.valueOf(info.getValue()));
        }
        return permissions;
    }

    private static String normalize(String permission) {
        return permission == null ? "" : permission.trim().toLowerCase(Locale.ROOT);
    }
}

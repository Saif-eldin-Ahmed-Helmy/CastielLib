package dev.castiel.lib.permissions;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface PermissionResolver {
    boolean hasPermission(CommandSender sender, String permission);
}

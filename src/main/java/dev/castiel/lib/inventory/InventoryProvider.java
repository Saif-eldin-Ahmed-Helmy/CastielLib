package dev.castiel.lib.inventory;

import dev.castiel.lib.util.Placeholders;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface InventoryProvider {
    Placeholders placeholders(Player player, String menuId, String itemId);
}

package dev.castiel.lib.inventory;

import dev.castiel.lib.actions.ActionRegistry;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class MenuHolder implements InventoryHolder {
    final String menuId;
    final ActionRegistry actions;

    MenuHolder(String menuId, ActionRegistry actions) {
        this.menuId = menuId;
        this.actions = actions;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

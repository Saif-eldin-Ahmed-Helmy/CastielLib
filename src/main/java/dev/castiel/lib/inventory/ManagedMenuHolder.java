package dev.castiel.lib.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public final class ManagedMenuHolder implements InventoryHolder {
    private final String menuId;
    private final Map<Integer, MenuButton> buttons = new HashMap<>();
    private Inventory inventory;

    ManagedMenuHolder(String menuId) {
        this.menuId = menuId;
    }

    void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    void button(int slot, MenuButton button) {
        buttons.put(slot, button);
    }

    public String menuId() {
        return menuId;
    }

    public void handle(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }
        event.setCancelled(true);
        MenuButton button = buttons.get(event.getRawSlot());
        if (button != null) {
            button.handle((Player) event.getWhoClicked(), event);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

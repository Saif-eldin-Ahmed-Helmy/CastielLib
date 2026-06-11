package dev.castiel.lib.inventory;

import dev.castiel.lib.text.Colors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class ManagedMenu {
    private final String id;
    private final int size;
    private final String title;
    private final Map<Integer, MenuButton> buttons = new HashMap<>();

    private ManagedMenu(String id, int size, String title) {
        this.id = id;
        this.size = Math.max(9, Math.min(54, ((size + 8) / 9) * 9));
        this.title = title == null ? id : title;
    }

    public static ManagedMenu create(String id, int size, String title) {
        return new ManagedMenu(id, size, title);
    }

    public ManagedMenu item(int slot, ItemStack item) {
        return button(slot, MenuButton.decorative(item));
    }

    public ManagedMenu button(int slot, MenuButton button) {
        if (slot >= 0 && slot < size && button != null) {
            buttons.put(slot, button);
        }
        return this;
    }

    public ManagedMenu fill(Iterable<Integer> slots, ItemStack item) {
        for (int slot : slots) {
            item(slot, item);
        }
        return this;
    }

    public Inventory inventory() {
        ManagedMenuHolder holder = new ManagedMenuHolder(id);
        Inventory inventory = Bukkit.createInventory(holder, size, Colors.color(title));
        holder.bind(inventory);
        for (Map.Entry<Integer, MenuButton> entry : buttons.entrySet()) {
            holder.button(entry.getKey(), entry.getValue());
            inventory.setItem(entry.getKey(), entry.getValue().item());
        }
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory());
    }
}

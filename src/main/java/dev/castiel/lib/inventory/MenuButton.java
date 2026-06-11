package dev.castiel.lib.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class MenuButton {
    private final ItemStack item;
    private final BiConsumer<Player, InventoryClickEvent> click;

    public MenuButton(ItemStack item, BiConsumer<Player, InventoryClickEvent> click) {
        this.item = item == null ? null : item.clone();
        this.click = click;
    }

    public static MenuButton decorative(ItemStack item) {
        return new MenuButton(item, null);
    }

    public static MenuButton clickable(ItemStack item, BiConsumer<Player, InventoryClickEvent> click) {
        return new MenuButton(item, Objects.requireNonNull(click, "click"));
    }

    public ItemStack item() {
        return item == null ? null : item.clone();
    }

    void handle(Player player, InventoryClickEvent event) {
        if (click != null) {
            click.accept(player, event);
        }
    }
}

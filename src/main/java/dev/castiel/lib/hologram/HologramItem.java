package dev.castiel.lib.hologram;

import org.bukkit.inventory.ItemStack;

/** Provider-neutral optional item line for hologram displays. */
public final class HologramItem {
    private final ItemStack item;
    private final double offsetY;

    public HologramItem(ItemStack item, double offsetY) {
        this.item = item == null ? null : item.clone();
        this.offsetY = offsetY;
    }

    public ItemStack item() { return item == null ? null : item.clone(); }
    public double offsetY() { return offsetY; }
}

package dev.castiel.lib.hologram;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Provider-neutral hologram display request. */
public final class HologramDisplay {
    private final Location location;
    private final List<String> lines;
    private final ItemStack item;
    private final double itemOffsetY;

    public HologramDisplay(Location location, List<String> lines, ItemStack item, double itemOffsetY) {
        this.location = location == null ? null : location.clone();
        this.lines = lines == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(lines));
        this.item = item == null ? null : item.clone();
        this.itemOffsetY = itemOffsetY;
    }

    public HologramDisplay(Location location, List<String> lines, HologramItem item) {
        this(location, lines, item == null ? null : item.item(), item == null ? 0.0 : item.offsetY());
    }

    public Location location() {
        return location == null ? null : location.clone();
    }

    public List<String> lines() {
        return lines;
    }

    public ItemStack item() {
        return item == null ? null : item.clone();
    }

    public double itemOffsetY() {
        return itemOffsetY;
    }
}

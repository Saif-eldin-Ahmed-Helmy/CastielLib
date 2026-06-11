package dev.castiel.lib.inventory;

import dev.castiel.lib.actions.ActionRegistry;
import dev.castiel.lib.items.ItemStacks;
import dev.castiel.lib.text.Colors;
import dev.castiel.lib.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CastielMenu {
    private final String id;
    private final boolean enabled;
    private final int size;
    private final String title;
    private final Map<Integer, MenuItem> slots = new HashMap<>();

    CastielMenu(String id, ConfigurationSection section) {
        this.id = id;
        this.enabled = section.getBoolean("Enabled", true);
        this.size = section.getInt("Size", 27);
        this.title = section.getString("Title", id);
        readItems(section.getConfigurationSection("DecorationItems"), false);
        readItems(section.getConfigurationSection("Items"), true);
    }

    public Inventory create(Player player, ActionRegistry actions, InventoryProvider provider) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(id, actions), size, Colors.color(title));
        for (Map.Entry<Integer, MenuItem> entry : slots.entrySet()) {
            Placeholders placeholders = provider == null ? Placeholders.empty() : provider.placeholders(player, id, entry.getValue().id);
            inventory.setItem(entry.getKey(), entry.getValue().stack(placeholders));
        }
        return inventory;
    }

    public MenuItem item(int slot) {
        return slots.get(slot);
    }

    public boolean enabled() {
        return enabled;
    }

    private void readItems(ConfigurationSection section, boolean dynamic) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }
            MenuItem item = new MenuItem(key, itemSection, dynamic);
            for (int slot : item.slots) {
                if (slot >= 0 && slot < size) {
                    slots.put(slot, item);
                }
            }
        }
    }

    public static final class MenuItem {
        final String id;
        final boolean dynamic;
        final List<Integer> slots;
        final String material;
        final int customModelData;
        final String name;
        final List<String> lore;
        final boolean glow;
        final List<String> actions;

        MenuItem(String id, ConfigurationSection section, boolean dynamic) {
            this.id = id;
            this.dynamic = dynamic;
            this.material = section.getString("Material", "STONE");
            this.customModelData = section.getInt("Custom-Model-Data", 0);
            this.name = section.getString("Name", id);
            this.lore = section.getStringList("Lore");
            this.glow = section.getBoolean("Glow", false);
            this.actions = section.getStringList("Actions");
            if (section.contains("Slot")) {
                this.slots = Collections.singletonList(section.getInt("Slot"));
            } else {
                this.slots = section.getIntegerList("Slots");
            }
        }

        ItemStack stack(Placeholders placeholders) {
            ItemStack stack = new ItemStack(resolveMaterial(material));
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Colors.color(placeholders.apply(name)));
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(Colors.color(placeholders.apply(line)));
                }
                meta.setLore(coloredLore);
                if (customModelData > 0) {
                    try {
                        meta.getClass().getMethod("setCustomModelData", Integer.class).invoke(meta, customModelData);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
                if (glow) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                stack.setItemMeta(meta);
            }
            return stack;
        }

        public List<String> actions() {
            return actions;
        }

        private static Material resolveMaterial(String raw) {
            return ItemStacks.resolveMaterial(raw, Material.STONE);
        }
    }
}

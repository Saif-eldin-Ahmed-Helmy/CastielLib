package dev.castiel.lib.items;

import dev.castiel.lib.text.Colors;
import dev.castiel.lib.util.Placeholders;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigItem {
    private final String material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final int customModelData;
    private final boolean glow;
    private final boolean hideAttributes;

    public ConfigItem(String material, int amount, String name, List<String> lore, int customModelData, boolean glow) {
        this(material, amount, name, lore, customModelData, glow, false);
    }

    public ConfigItem(String material, int amount, String name, List<String> lore, int customModelData, boolean glow, boolean hideAttributes) {
        this.material = material == null || material.trim().isEmpty() ? "STONE" : material;
        this.amount = Math.max(1, amount);
        this.name = name;
        this.lore = lore == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<String>(lore));
        this.customModelData = customModelData;
        this.glow = glow;
        this.hideAttributes = hideAttributes;
    }

    public static ConfigItem from(ConfigurationSection section, String defaultMaterial, String defaultName) {
        if (section == null) {
            return new ConfigItem(defaultMaterial, 1, defaultName, Collections.<String>emptyList(), 0, false);
        }
        String material = section.getString("Material", section.getString("material", defaultMaterial));
        int amount = section.getInt("Amount", section.getInt("amount", 1));
        String name = section.getString("Name", section.getString("Display-Name", section.getString("display-name", defaultName)));
        List<String> lore = section.getStringList("Lore");
        if (lore.isEmpty()) {
            lore = section.getStringList("display-lore");
        }
        int customModelData = section.getInt("Custom-Model-Data", section.getInt("custom-model-data", 0));
        boolean glow = section.getBoolean("Glow", section.getBoolean("glow", false));
        boolean hideAttributes = section.getBoolean("Hide-Attributes", section.getBoolean("hide-attributes", false));
        return new ConfigItem(material, amount, name, lore, customModelData, glow, hideAttributes);
    }

    public ItemStack build() {
        return build(Placeholders.empty());
    }

    public ItemStack build(Placeholders placeholders) {
        ItemStack stack = ItemStacks.fromMaterialOrTexture(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null && !name.trim().isEmpty()) {
                meta.setDisplayName(Colors.color(placeholders.apply(name)));
            }
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<String>();
                for (String line : lore) {
                    coloredLore.add(Colors.color(placeholders.apply(line)));
                }
                meta.setLore(coloredLore);
            }
            if (customModelData > 0) {
                try {
                    meta.getClass().getMethod("setCustomModelData", Integer.class).invoke(meta, Integer.valueOf(customModelData));
                } catch (ReflectiveOperationException ignored) {
                }
            }
            if (glow) {
                addGlow(meta);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            if (hideAttributes) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            stack.setItemMeta(meta);
            if (glow) {
                addGlow(stack);
            }
        }
        return stack;
    }

    public boolean visuallyMatches(ItemStack stack, Placeholders placeholders) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemStack expected = build(placeholders);
        if (stack.getType() != expected.getType()) {
            return false;
        }
        ItemMeta actualMeta = stack.getItemMeta();
        ItemMeta expectedMeta = expected.getItemMeta();
        if (expectedMeta == null) {
            return true;
        }
        if (expectedMeta.hasDisplayName()) {
            if (actualMeta == null || !actualMeta.hasDisplayName() || !expectedMeta.getDisplayName().equals(actualMeta.getDisplayName())) {
                return false;
            }
        }
        if (expectedMeta.hasLore()) {
            if (actualMeta == null || !actualMeta.hasLore() || !expectedMeta.getLore().equals(actualMeta.getLore())) {
                return false;
            }
        }
        if (customModelData > 0 && !customModelDataMatches(actualMeta, expectedMeta)) {
            return false;
        }
        return true;
    }

    public String material() {
        return material;
    }

    public int amount() {
        return amount;
    }

    public String name() {
        return name;
    }

    public List<String> lore() {
        return lore;
    }

    public int customModelData() {
        return customModelData;
    }

    public boolean glow() {
        return glow;
    }

    public boolean hideAttributes() {
        return hideAttributes;
    }

    private static boolean customModelDataMatches(ItemMeta actual, ItemMeta expected) {
        try {
            Method has = expected.getClass().getMethod("hasCustomModelData");
            Method get = expected.getClass().getMethod("getCustomModelData");
            boolean expectedHas = Boolean.TRUE.equals(has.invoke(expected));
            boolean actualHas = actual != null && Boolean.TRUE.equals(has.invoke(actual));
            if (!expectedHas) {
                return true;
            }
            if (!actualHas) {
                return false;
            }
            return get.invoke(expected).equals(get.invoke(actual));
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static void addGlow(ItemMeta meta) {
        Enchantment enchantment = glowEnchantment();
        if (enchantment == null) {
            return;
        }
        try {
            meta.addEnchant(enchantment, 1, true);
        } catch (Throwable ignored) {
        }
    }

    private static void addGlow(ItemStack stack) {
        Enchantment enchantment = glowEnchantment();
        if (enchantment == null) {
            return;
        }
        try {
            stack.addUnsafeEnchantment(enchantment, 1);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private static Enchantment glowEnchantment() {
        Enchantment enchantment = null;
        try {
            enchantment = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        } catch (Throwable ignored) {
        }
        if (enchantment != null) {
            return enchantment;
        }
        try {
            enchantment = Enchantment.getByName("UNBREAKING");
        } catch (Throwable ignored) {
        }
        if (enchantment != null) {
            return enchantment;
        }
        try {
            enchantment = Enchantment.getByName("DURABILITY");
        } catch (Throwable ignored) {
        }
        return enchantment;
    }
}

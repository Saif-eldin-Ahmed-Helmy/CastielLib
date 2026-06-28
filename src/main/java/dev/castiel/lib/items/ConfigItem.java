package dev.castiel.lib.items;

import dev.castiel.lib.text.Colors;
import dev.castiel.lib.util.Placeholders;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ConfigItem {
    private final String material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final int customModelData;
    private final boolean glow;
    private final boolean hideAttributes;
    private final List<String> itemFlagNames;
    private final boolean hideItemFlags;
    private final boolean unbreakable;
    private final int damage;
    private final List<String> enchantments;
    private final ConfiguredPotion basePotion;
    private final Color potionColor;
    private final List<PotionEffect> potionEffects;
    private final List<Color> fireworkColors;
    private final List<Color> fireworkFadeColors;
    private final FireworkEffect.Type fireworkType;
    private final boolean fireworkTrail;
    private final boolean fireworkFlicker;
    private final int fireworkPower;

    public ConfigItem(String material, int amount, String name, List<String> lore, int customModelData, boolean glow) {
        this(material, amount, name, lore, customModelData, glow, false);
    }

    public ConfigItem(String material, int amount, String name, List<String> lore, int customModelData, boolean glow, boolean hideAttributes) {
        this(
                material,
                amount,
                name,
                lore,
                customModelData,
                glow,
                hideAttributes,
                Collections.<String>emptyList(),
                false,
                false,
                0,
                Collections.<String>emptyList(),
                null,
                null,
                Collections.<PotionEffect>emptyList(),
                Collections.<Color>emptyList(),
                Collections.<Color>emptyList(),
                FireworkEffect.Type.BALL,
                false,
                false,
                1
        );
    }

    private ConfigItem(String material,
                       int amount,
                       String name,
                       List<String> lore,
                       int customModelData,
                       boolean glow,
                       boolean hideAttributes,
                       List<String> itemFlagNames,
                       boolean hideItemFlags,
                       boolean unbreakable,
                       int damage,
                       List<String> enchantments,
                       ConfiguredPotion basePotion,
                       Color potionColor,
                       List<PotionEffect> potionEffects,
                       List<Color> fireworkColors,
                       List<Color> fireworkFadeColors,
                       FireworkEffect.Type fireworkType,
                       boolean fireworkTrail,
                       boolean fireworkFlicker,
                       int fireworkPower) {
        this.material = material == null || material.trim().isEmpty() ? "STONE" : material;
        this.amount = Math.max(1, amount);
        this.name = name;
        this.lore = immutableStringList(lore);
        this.customModelData = customModelData;
        this.glow = glow;
        this.hideAttributes = hideAttributes;
        this.itemFlagNames = immutableStringList(itemFlagNames);
        this.hideItemFlags = hideItemFlags;
        this.unbreakable = unbreakable;
        this.damage = Math.max(0, damage);
        this.enchantments = immutableStringList(enchantments);
        this.basePotion = basePotion;
        this.potionColor = potionColor;
        this.potionEffects = potionEffects == null ? Collections.<PotionEffect>emptyList() : Collections.unmodifiableList(new ArrayList<PotionEffect>(potionEffects));
        this.fireworkColors = fireworkColors == null ? Collections.<Color>emptyList() : Collections.unmodifiableList(new ArrayList<Color>(fireworkColors));
        this.fireworkFadeColors = fireworkFadeColors == null ? Collections.<Color>emptyList() : Collections.unmodifiableList(new ArrayList<Color>(fireworkFadeColors));
        this.fireworkType = fireworkType == null ? FireworkEffect.Type.BALL : fireworkType;
        this.fireworkTrail = fireworkTrail;
        this.fireworkFlicker = fireworkFlicker;
        this.fireworkPower = Math.max(0, Math.min(3, fireworkPower));
    }

    public static ConfigItem from(ConfigurationSection section, String defaultMaterial, String defaultName) {
        if (section == null) {
            return new ConfigItem(defaultMaterial, 1, defaultName, Collections.<String>emptyList(), 0, false);
        }
        String material = getConfigString(section, defaultMaterial, "Material", "material", "Item", "item");
        int amount = getConfigInt(section, 1, "Amount", "amount");
        String name = getConfigString(section, defaultName, "Name", "name", "Display-Name", "display-name");
        List<String> lore = getConfigStringList(section, "Lore", "lore", "display-lore");
        int customModelData = getConfigInt(section, 0, "Custom-Model-Data", "custom-model-data", "CustomModelData", "customModelData");
        boolean glow = getConfigBoolean(section, false, "Glow", "glow", "Glowing", "glowing");
        boolean hideAttributes = getConfigBoolean(section, false, "Hide-Attributes", "hide-attributes");
        List<String> itemFlags = getConfigStringList(section, "ItemFlags", "Item-Flags", "item-flags", "itemFlags");
        boolean hideItemFlags = getConfigBoolean(section, false, "Hide-ItemFlags", "Hide-All-Flags", "hide-itemflags", "hide-flags");
        boolean unbreakable = getConfigBoolean(section, false, "Unbreakable", "unbreakable");
        int damage = getConfigInt(section, 0, "Damage", "damage", "Durability", "durability");
        List<String> enchantments = getConfigStringList(section, "Enchantments", "enchantments");

        ConfiguredPotion basePotion = parsePotionType(getConfigString(section, "", "Potion-Type", "potion-type"));
        Color potionColor = parseColor(getConfigString(section, "", "Potion-Color", "potion-color"));
        List<PotionEffect> potionEffects = new ArrayList<PotionEffect>();
        for (String rawEffect : getConfigStringList(section, "Potion-Effects", "potion-effects")) {
            PotionEffect effect = parsePotionEffect(rawEffect);
            if (effect != null) {
                potionEffects.add(effect);
            }
        }

        List<Color> fireworkColors = getConfigColors(section, "Firework-Colors", "Firework-Color", "firework-colors", "firework-color");
        Color nestedColor = getNestedColor(section, "colors", "Colors");
        if (fireworkColors.isEmpty() && nestedColor != null) {
            fireworkColors.add(nestedColor);
        }
        List<Color> fireworkFadeColors = getConfigColors(section, "Firework-Fade-Colors", "Firework-Fade-Color", "firework-fade-colors", "firework-fade-color");
        FireworkEffect.Type fireworkType = parseFireworkType(getConfigString(section, "BALL", "Firework-Type", "firework-type"));
        boolean fireworkTrail = getConfigBoolean(section, false, "Firework-Trail", "firework-trail", "Trail", "trail");
        boolean fireworkFlicker = getConfigBoolean(section, false, "Firework-Flicker", "firework-flicker", "Flicker", "flicker");
        int fireworkPower = getConfigInt(section, 1, "Firework-Power", "firework-power", "Power", "power");

        return new ConfigItem(
                material,
                amount,
                name,
                lore,
                customModelData,
                glow,
                hideAttributes,
                itemFlags,
                hideItemFlags,
                unbreakable,
                damage,
                enchantments,
                basePotion,
                potionColor,
                potionEffects,
                fireworkColors,
                fireworkFadeColors,
                fireworkType,
                fireworkTrail,
                fireworkFlicker,
                fireworkPower
        );
    }

    public ItemStack build() {
        return build(Placeholders.empty());
    }

    public ItemStack build(Placeholders placeholders) {
        MaterialSpec materialSpec = MaterialSpec.parse(material);
        ItemStack stack = ItemStacks.fromMaterialOrTexture(materialSpec.material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (damage > 0 || materialSpec.damage > 0) {
                applyDamage(meta, stack.getType(), damage > 0 ? damage : materialSpec.damage);
            }
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
            int modelData = customModelData > 0 ? customModelData : materialSpec.customModelData;
            if (modelData > 0) {
                try {
                    meta.getClass().getMethod("setCustomModelData", Integer.class).invoke(meta, Integer.valueOf(modelData));
                } catch (ReflectiveOperationException ignored) {
                }
            }
            applyEnchantments(meta, enchantments);
            if (glow) {
                addGlow(meta);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            if (hideAttributes) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            for (String itemFlagName : itemFlagNames) {
                ItemFlag flag = parseItemFlag(itemFlagName);
                if (flag != null) {
                    meta.addItemFlags(flag);
                }
            }
            if (hideItemFlags) {
                meta.addItemFlags(ItemFlag.values());
            }
            if (unbreakable) {
                try {
                    meta.setUnbreakable(true);
                } catch (Throwable ignored) {
                }
            }
            applyPotionMeta(meta, materialSpec);
            applyFireworkMeta(meta, stack.getType(), materialSpec);
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

    private void applyPotionMeta(ItemMeta meta, MaterialSpec materialSpec) {
        if (!(meta instanceof PotionMeta)) {
            return;
        }
        PotionMeta potionMeta = (PotionMeta) meta;
        ConfiguredPotion configuredBase = basePotion != null ? basePotion : materialSpec.basePotion;
        Color configuredColor = potionColor != null ? potionColor : materialSpec.color;
        if (configuredBase != null) {
            applyBasePotion(potionMeta, configuredBase);
        }
        for (PotionEffect effect : potionEffects) {
            potionMeta.addCustomEffect(effect, true);
        }
        if (configuredColor != null) {
            try {
                potionMeta.setColor(configuredColor);
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyFireworkMeta(ItemMeta meta, Material materialType, MaterialSpec materialSpec) {
        boolean fireworkStar = materialType != null && "FIREWORK_STAR".equals(materialType.name());
        boolean fireworkRocket = materialType != null && ("FIREWORK_ROCKET".equals(materialType.name()) || "FIREWORK".equals(materialType.name()));
        if (!fireworkStar && !fireworkRocket) {
            return;
        }
        List<Color> colors = !fireworkColors.isEmpty() ? fireworkColors : materialSpec.fireworkColors;
        if (colors.isEmpty() && materialSpec.color != null) {
            colors = Collections.singletonList(materialSpec.color);
        }
        if (colors.isEmpty()) {
            return;
        }
        FireworkEffect effect = createFireworkEffect(colors);
        if (fireworkStar && meta instanceof FireworkEffectMeta) {
            ((FireworkEffectMeta) meta).setEffect(effect);
        }
        if (fireworkRocket && meta instanceof FireworkMeta) {
            FireworkMeta fireworkMeta = (FireworkMeta) meta;
            fireworkMeta.clearEffects();
            fireworkMeta.addEffect(effect);
            fireworkMeta.setPower(fireworkPower);
        }
    }

    private FireworkEffect createFireworkEffect(List<Color> colors) {
        FireworkEffect.Builder builder = FireworkEffect.builder()
                .with(fireworkType)
                .withColor(colors);
        if (!fireworkFadeColors.isEmpty()) {
            builder.withFade(fireworkFadeColors);
        }
        if (fireworkTrail) {
            builder.trail(true);
        }
        if (fireworkFlicker) {
            builder.flicker(true);
        }
        return builder.build();
    }

    private static void applyDamage(ItemMeta meta, Material material, int damage) {
        if (!(meta instanceof Damageable) || material == null || damage <= 0) {
            return;
        }
        short maxDurability = material.getMaxDurability();
        int safeDamage = maxDurability > 0 ? Math.min(damage, maxDurability) : damage;
        ((Damageable) meta).setDamage(safeDamage);
    }

    private static void applyEnchantments(ItemMeta meta, List<String> rawEnchantments) {
        for (String raw : rawEnchantments) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String[] parts = raw.split(":", 2);
            Enchantment enchantment = parseEnchantment(parts[0]);
            if (enchantment == null) {
                continue;
            }
            int level = 1;
            if (parts.length > 1) {
                level = Math.max(1, parseInt(parts[1], 1));
            }
            try {
                meta.addEnchant(enchantment, level, true);
            } catch (Throwable ignored) {
            }
        }
    }

    private static Enchantment parseEnchantment(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        try {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(normalized));
            if (enchantment != null) {
                return enchantment;
            }
        } catch (Throwable ignored) {
        }
        return Enchantment.getByName(normalized.toUpperCase(Locale.ROOT));
    }

    private static void applyBasePotion(PotionMeta potionMeta, ConfiguredPotion configuredPotion) {
        if (configuredPotion == null || configuredPotion.type == null) {
            return;
        }
        if (invokeBasePotionType(potionMeta, configuredPotion.type)) {
            return;
        }
        invokeBasePotionData(potionMeta, configuredPotion);
    }

    private static boolean invokeBasePotionType(PotionMeta potionMeta, PotionType type) {
        try {
            Method method = potionMeta.getClass().getMethod("setBasePotionType", PotionType.class);
            method.invoke(potionMeta, type);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean invokeBasePotionData(PotionMeta potionMeta, ConfiguredPotion configuredPotion) {
        try {
            Class<?> potionDataClass = Class.forName("org.bukkit.potion.PotionData");
            Constructor<?> constructor = potionDataClass.getConstructor(PotionType.class, boolean.class, boolean.class);
            Object potionData = constructor.newInstance(configuredPotion.type, Boolean.valueOf(configuredPotion.extended), Boolean.valueOf(configuredPotion.upgraded));
            Method method = potionMeta.getClass().getMethod("setBasePotionData", potionDataClass);
            method.invoke(potionMeta, potionData);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
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

    private static ItemFlag parseItemFlag(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        try {
            return ItemFlag.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static PotionEffect parsePotionEffect(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String[] parts = raw.split(":");
        PotionEffectType type = parsePotionEffectType(parts[0]);
        if (type == null) {
            return null;
        }
        int durationSeconds = parts.length > 1 ? parseInt(parts[1], 30) : 30;
        int amplifier = parts.length > 2 ? parseInt(parts[2], 0) : 0;
        return new PotionEffect(type, Math.max(1, durationSeconds) * 20, Math.max(0, amplifier));
    }

    private static PotionEffectType parsePotionEffectType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        PotionEffectType type = PotionEffectType.getByName(normalized);
        if (type != null) {
            return type;
        }
        String[] aliases = potionEffectAliases(normalized);
        for (String alias : aliases) {
            type = PotionEffectType.getByName(alias);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    private static String[] potionEffectAliases(String normalized) {
        if ("HASTE".equals(normalized)) return new String[] {"FAST_DIGGING"};
        if ("MINING_FATIGUE".equals(normalized)) return new String[] {"SLOW_DIGGING"};
        if ("STRENGTH".equals(normalized)) return new String[] {"INCREASE_DAMAGE"};
        if ("INSTANT_HEALTH".equals(normalized) || "HEALING".equals(normalized)) return new String[] {"HEAL"};
        if ("INSTANT_DAMAGE".equals(normalized) || "HARMING".equals(normalized)) return new String[] {"HARM"};
        if ("JUMP_BOOST".equals(normalized) || "LEAPING".equals(normalized)) return new String[] {"JUMP"};
        if ("NAUSEA".equals(normalized)) return new String[] {"CONFUSION"};
        if ("RESISTANCE".equals(normalized)) return new String[] {"DAMAGE_RESISTANCE"};
        return new String[0];
    }

    private static ConfiguredPotion parsePotionType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        boolean extended = false;
        boolean upgraded = false;
        String base = normalized;
        if (base.startsWith("LONG_")) {
            extended = true;
            base = base.substring("LONG_".length());
        } else if (base.startsWith("STRONG_")) {
            upgraded = true;
            base = base.substring("STRONG_".length());
        }

        PotionType direct = resolvePotionType(normalized);
        if (direct != null) {
            return new ConfiguredPotion(direct, extended, upgraded);
        }

        PotionType aliased = resolvePotionType(base, potionTypeAliases(base));
        if (aliased != null) {
            return new ConfiguredPotion(aliased, extended, upgraded);
        }
        return null;
    }

    private static PotionType resolvePotionType(String first, String... more) {
        PotionType type = resolvePotionTypeName(first);
        if (type != null) {
            return type;
        }
        for (String candidate : more) {
            type = resolvePotionTypeName(candidate);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    private static PotionType resolvePotionTypeName(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return PotionType.valueOf(raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String[] potionTypeAliases(String base) {
        if ("HEALING".equals(base)) return new String[] {"INSTANT_HEAL"};
        if ("INSTANT_HEAL".equals(base)) return new String[] {"HEALING"};
        if ("HARMING".equals(base)) return new String[] {"INSTANT_DAMAGE"};
        if ("INSTANT_DAMAGE".equals(base)) return new String[] {"HARMING"};
        if ("SWIFTNESS".equals(base)) return new String[] {"SPEED"};
        if ("SPEED".equals(base)) return new String[] {"SWIFTNESS"};
        if ("LEAPING".equals(base)) return new String[] {"JUMP"};
        if ("JUMP".equals(base)) return new String[] {"LEAPING"};
        if ("STRENGTH".equals(base)) return new String[] {"INCREASE_DAMAGE"};
        if ("INCREASE_DAMAGE".equals(base)) return new String[] {"STRENGTH"};
        if ("SLOW_FALLING".equals(base)) return new String[] {"SLOW_FALL"};
        if ("SLOW_FALL".equals(base)) return new String[] {"SLOW_FALLING"};
        return new String[0];
    }

    private static FireworkEffect.Type parseFireworkType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return FireworkEffect.Type.BALL;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        if ("LARGE_BALL".equals(normalized)) {
            normalized = "BALL_LARGE";
        }
        try {
            return FireworkEffect.Type.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return FireworkEffect.Type.BALL;
        }
    }

    private static List<Color> getConfigColors(ConfigurationSection section, String... keys) {
        List<Color> colors = new ArrayList<Color>();
        for (String key : keys) {
            if (section.isList(key)) {
                for (Object raw : section.getList(key, Collections.emptyList())) {
                    Color color = parseColor(raw);
                    if (color != null) {
                        colors.add(color);
                    }
                }
                if (!colors.isEmpty()) {
                    return colors;
                }
            }
            if (section.isString(key)) {
                Color color = parseColor(section.getString(key, ""));
                if (color != null) {
                    colors.add(color);
                    return colors;
                }
            }
            if (section.isConfigurationSection(key)) {
                Color color = readRgbSection(section.getConfigurationSection(key));
                if (color != null) {
                    colors.add(color);
                    return colors;
                }
            }
        }
        return colors;
    }

    private static Color getNestedColor(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (section.isConfigurationSection(key)) {
                Color color = readRgbSection(section.getConfigurationSection(key));
                if (color != null) {
                    return color;
                }
            }
        }
        return null;
    }

    private static Color readRgbSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        int red = section.getInt("r", section.getInt("R", -1));
        int green = section.getInt("g", section.getInt("G", -1));
        int blue = section.getInt("b", section.getInt("B", -1));
        if (red < 0 || green < 0 || blue < 0) {
            return null;
        }
        return Color.fromRGB(clampColor(red), clampColor(green), clampColor(blue));
    }

    @SuppressWarnings("unchecked")
    private static Color parseColor(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof ConfigurationSection) {
            return readRgbSection((ConfigurationSection) raw);
        }
        if (raw instanceof java.util.Map) {
            java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) raw;
            int red = mapInt(map, "r", mapInt(map, "R", -1));
            int green = mapInt(map, "g", mapInt(map, "G", -1));
            int blue = mapInt(map, "b", mapInt(map, "B", -1));
            if (red >= 0 && green >= 0 && blue >= 0) {
                return Color.fromRGB(clampColor(red), clampColor(green), clampColor(blue));
            }
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.contains(",")) {
            String[] rgb = value.split(",");
            if (rgb.length == 3) {
                return Color.fromRGB(clampColor(parseInt(rgb[0], 0)), clampColor(parseInt(rgb[1], 0)), clampColor(parseInt(rgb[2], 0)));
            }
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        if (normalized.length() == 6 && normalized.matches("[0-9a-fA-F]{6}")) {
            int rgb = Integer.parseInt(normalized, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
        String name = normalized.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        if ("WHITE".equals(name)) return Color.WHITE;
        if ("SILVER".equals(name)) return Color.SILVER;
        if ("GRAY".equals(name) || "GREY".equals(name)) return Color.GRAY;
        if ("BLACK".equals(name)) return Color.BLACK;
        if ("RED".equals(name)) return Color.RED;
        if ("MAROON".equals(name)) return Color.MAROON;
        if ("YELLOW".equals(name)) return Color.YELLOW;
        if ("OLIVE".equals(name)) return Color.OLIVE;
        if ("LIME".equals(name)) return Color.LIME;
        if ("GREEN".equals(name)) return Color.GREEN;
        if ("AQUA".equals(name) || "CYAN".equals(name)) return Color.AQUA;
        if ("TEAL".equals(name)) return Color.TEAL;
        if ("BLUE".equals(name)) return Color.BLUE;
        if ("NAVY".equals(name)) return Color.NAVY;
        if ("FUCHSIA".equals(name) || "MAGENTA".equals(name) || "PINK".equals(name)) return Color.FUCHSIA;
        if ("PURPLE".equals(name)) return Color.PURPLE;
        if ("ORANGE".equals(name)) return Color.ORANGE;
        return null;
    }

    private static int mapInt(java.util.Map<Object, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return parseInt(String.valueOf(value), fallback);
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static boolean hasConfigValue(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (section.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static String getConfigString(ConfigurationSection section, String fallback, String... keys) {
        for (String key : keys) {
            if (section.contains(key)) {
                return section.getString(key, fallback);
            }
        }
        return fallback;
    }

    private static int getConfigInt(ConfigurationSection section, int fallback, String... keys) {
        for (String key : keys) {
            if (section.contains(key)) {
                return section.getInt(key, fallback);
            }
        }
        return fallback;
    }

    private static boolean getConfigBoolean(ConfigurationSection section, boolean fallback, String... keys) {
        for (String key : keys) {
            if (section.contains(key)) {
                return section.getBoolean(key, fallback);
            }
        }
        return fallback;
    }

    private static List<String> getConfigStringList(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (section.isList(key)) {
                return section.getStringList(key);
            }
            if (section.isString(key)) {
                return Collections.singletonList(section.getString(key, ""));
            }
        }
        return Collections.emptyList();
    }

    private static List<String> immutableStringList(List<String> source) {
        return source == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<String>(source));
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static final class ConfiguredPotion {
        private final PotionType type;
        private final boolean extended;
        private final boolean upgraded;

        private ConfiguredPotion(PotionType type, boolean extended, boolean upgraded) {
            this.type = type;
            this.extended = extended;
            this.upgraded = upgraded;
        }
    }

    private static final class MaterialSpec {
        private final String material;
        private final int customModelData;
        private final int damage;
        private final Color color;
        private final ConfiguredPotion basePotion;
        private final List<Color> fireworkColors;

        private MaterialSpec(String material, int customModelData, int damage, Color color, ConfiguredPotion basePotion, List<Color> fireworkColors) {
            this.material = material;
            this.customModelData = customModelData;
            this.damage = damage;
            this.color = color;
            this.basePotion = basePotion;
            this.fireworkColors = fireworkColors == null ? Collections.<Color>emptyList() : fireworkColors;
        }

        private static MaterialSpec parse(String raw) {
            if (raw == null || raw.trim().isEmpty() || ItemStacks.looksLikeTexture(raw)) {
                return new MaterialSpec(raw, 0, 0, null, null, Collections.<Color>emptyList());
            }
            String material = raw.trim();
            int customModelData = 0;
            int damage = 0;
            Color color = null;
            ConfiguredPotion basePotion = null;
            List<Color> fireworkColors = Collections.emptyList();

            int modelIndex = material.indexOf('#');
            if (modelIndex >= 0) {
                customModelData = parseInt(material.substring(modelIndex + 1), 0);
                material = material.substring(0, modelIndex);
            }

            int metadataIndex = material.indexOf(':');
            if (metadataIndex >= 0) {
                String metadata = material.substring(metadataIndex + 1);
                material = material.substring(0, metadataIndex);
                if (isInteger(metadata)) {
                    damage = parseInt(metadata, 0);
                } else {
                    color = parseColor(metadata);
                    if (color != null) {
                        fireworkColors = Collections.singletonList(color);
                    } else {
                        basePotion = parsePotionType(metadata);
                    }
                }
            }
            return new MaterialSpec(material, customModelData, damage, color, basePotion, fireworkColors);
        }

        private static boolean isInteger(String raw) {
            if (raw == null || raw.trim().isEmpty()) {
                return false;
            }
            try {
                Integer.parseInt(raw.trim());
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
    }
}

package dev.castiel.lib.items;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public final class ItemStacks {
    private ItemStacks() {
    }

    public static ItemStack fromMaterialOrTexture(String raw, int amount) {
        if (looksLikeTexture(raw)) {
            return playerHead(raw, amount);
        }
        return new ItemStack(resolveMaterial(raw, Material.STONE), Math.max(1, amount));
    }

    public static Material resolveMaterial(String raw, Material fallback) {
        String normalized = raw == null ? "" : raw.trim();
        if (normalized.isEmpty()) {
            return fallback;
        }
        Optional<XMaterial> matched = XMaterial.matchXMaterial(normalized);
        if (matched.isPresent()) {
            Material material = matched.get().parseMaterial();
            if (material != null) {
                return material;
            }
        }
        try {
            return Material.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public static boolean looksLikeTexture(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim();
        if (value.startsWith("texture:") || value.startsWith("base64:") || value.startsWith("head:")) {
            return true;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return true;
        }
        if (value.length() < 80) {
            return false;
        }
        try {
            String decoded = decodeTexture(value);
            return decoded.contains("textures") && decoded.contains("SKIN");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static ItemStack playerHead(String texture, int amount) {
        ItemStack stack = new ItemStack(resolveMaterial("PLAYER_HEAD", Material.PLAYER_HEAD), Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof SkullMeta) {
            applyTexture((SkullMeta) meta, normalizeTexture(texture));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static String normalizeTexture(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("texture:")) {
            value = value.substring("texture:".length());
        } else if (value.startsWith("base64:")) {
            value = value.substring("base64:".length());
        } else if (value.startsWith("head:")) {
            value = value.substring("head:".length());
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + value + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes());
        }
        return value;
    }

    public static void applyTexture(SkullMeta meta, String texture) {
        if (texture == null || texture.trim().isEmpty()) {
            return;
        }
        if (applySkullTexture(meta, texture)) {
            return;
        }
        tryGameProfileField(meta, texture);
    }

    public static boolean applySkullTexture(Object target, String texture) {
        if (target == null || texture == null || texture.trim().isEmpty()) {
            return false;
        }
        String normalized = normalizeTexture(texture);
        Object profile = createProfile(normalized);
        if (profile != null && invokeProfileSetter(target, profile)) {
            return true;
        }
        return tryGameProfileField(target, normalized);
    }

    private static Object createProfile(String texture) {
        try {
            UUID id = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
            Object profile = createBukkitProfile(id);
            Method getTextures = profile.getClass().getMethod("getTextures");
            Object textures = getTextures.invoke(profile);
            String decoded = decodeTexture(texture);
            String url = extractUrl(decoded);
            if (url == null) {
                return null;
            }
            textures.getClass().getMethod("setSkin", java.net.URL.class).invoke(textures, new java.net.URL(url));
            try {
                profile.getClass().getMethod("setTextures", textures.getClass()).invoke(profile, textures);
            } catch (NoSuchMethodException ignored) {
                invokeCompatibleSetter(profile, "setTextures", textures);
            }
            return profile;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean tryGameProfileField(Object target, String texture) {
        try {
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            Constructor<?> profileConstructor = profileClass.getConstructor(UUID.class, String.class);
            Object profile = profileConstructor.newInstance(UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)), "CastielHead");
            Object properties = profileClass.getMethod("getProperties").invoke(profile);
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
            Object property = propertyConstructor.newInstance("textures", texture);
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);
            Field profileField = target.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(target, profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object createBukkitProfile(UUID id) throws ReflectiveOperationException {
        try {
            return Bukkit.class.getMethod("createProfile", UUID.class).invoke(null, id);
        } catch (NoSuchMethodException ignored) {
            return Bukkit.class.getMethod("createProfile", UUID.class, String.class).invoke(null, id, "CastielHead");
        }
    }

    private static boolean invokeProfileSetter(Object target, Object profile) {
        return invokeCompatibleSetter(target, "setPlayerProfile", profile) || invokeCompatibleSetter(target, "setOwnerProfile", profile);
    }

    private static boolean invokeCompatibleSetter(Object target, String name, Object value) {
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!name.equals(method.getName()) || parameterTypes.length != 1) {
                continue;
            }
            if (!parameterTypes[0].isAssignableFrom(value.getClass())) {
                continue;
            }
            try {
                method.invoke(target, value);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static String decodeTexture(String texture) {
        String value = texture.trim();
        int padding = (4 - value.length() % 4) % 4;
        StringBuilder padded = new StringBuilder(value);
        for (int i = 0; i < padding; i++) {
            padded.append('=');
        }
        return new String(Base64.getDecoder().decode(padded.toString()), StandardCharsets.UTF_8);
    }

    private static String extractUrl(String decoded) {
        int key = decoded.indexOf("\"url\"");
        if (key < 0) {
            return null;
        }
        int colon = decoded.indexOf(':', key);
        int firstQuote = decoded.indexOf('"', colon + 1);
        int secondQuote = decoded.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) {
            return null;
        }
        return decoded.substring(firstQuote + 1, secondQuote);
    }
}

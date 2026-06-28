package dev.castiel.lib.text;

import org.bukkit.ChatColor;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Colors {
    private static final Pattern OPEN_NO_BRACKET = Pattern.compile("(?i)(?<![</])GRADIENT:");
    private static final Pattern OPEN_MISSING_GT = Pattern.compile("(?i)<GRADIENT:([0-9a-f]{6})(?![A-Za-z0-9_#>])");
    private static final Pattern CLOSE_NO_BRACKET = Pattern.compile("(?i)(?<!<)/GRADIENT:");
    private static final Pattern CLOSE_MISSING_GT = Pattern.compile("(?i)</GRADIENT:([0-9a-f]{6})(?![A-Za-z0-9_#>])");
    private static final Pattern BROKEN_CLOSE = Pattern.compile("(?i)</<GRADIENT:([A-Za-z0-9_#]{3,32})>");
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})|#\\{([A-Fa-f0-9]{6})}");
    private static final Pattern SOLID = Pattern.compile("<SOLID:([A-Za-z0-9_#]{3,32})>(.*?)</SOLID(?::\\1)?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SOLID_INSERT = Pattern.compile("<SOLID:#?([A-Fa-f0-9]{6})>", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRADIENT = Pattern.compile("<GRADIENT:([A-Za-z0-9_#]{3,32})>(.*?)</GRADIENT:([A-Za-z0-9_#]{3,32})>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RAINBOW = Pattern.compile("<RAINBOW(?:(\\d{1,3})|:([0-9.]+))?>(.*?)</RAINBOW>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Method OF_COLOR_METHOD = findOfMethod(Color.class);
    private static final Method OF_STRING_METHOD = findOfMethod(String.class);
    private static final LegacyColor[] LEGACY_COLORS = new LegacyColor[] {
            new LegacyColor(new Color(0x000000), ChatColor.BLACK),
            new LegacyColor(new Color(0x0000AA), ChatColor.DARK_BLUE),
            new LegacyColor(new Color(0x00AA00), ChatColor.DARK_GREEN),
            new LegacyColor(new Color(0x00AAAA), ChatColor.DARK_AQUA),
            new LegacyColor(new Color(0xAA0000), ChatColor.DARK_RED),
            new LegacyColor(new Color(0xAA00AA), ChatColor.DARK_PURPLE),
            new LegacyColor(new Color(0xFFAA00), ChatColor.GOLD),
            new LegacyColor(new Color(0xAAAAAA), ChatColor.GRAY),
            new LegacyColor(new Color(0x555555), ChatColor.DARK_GRAY),
            new LegacyColor(new Color(0x5555FF), ChatColor.BLUE),
            new LegacyColor(new Color(0x55FF55), ChatColor.GREEN),
            new LegacyColor(new Color(0x55FFFF), ChatColor.AQUA),
            new LegacyColor(new Color(0xFF5555), ChatColor.RED),
            new LegacyColor(new Color(0xFF55FF), ChatColor.LIGHT_PURPLE),
            new LegacyColor(new Color(0xFFFF55), ChatColor.YELLOW),
            new LegacyColor(new Color(0xFFFFFF), ChatColor.WHITE)
    };

    private Colors() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        String text = normalizeGradientTags(input);
        text = applyRainbows(text);
        text = applyGradients(text);
        text = applySolids(text);
        text = applySolidInserts(text);
        text = applyHex(text);
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String normalizeGradientTags(String input) {
        if (input == null) {
            return null;
        }
        String text = BROKEN_CLOSE.matcher(input).replaceAll("</GRADIENT:$1>");
        text = OPEN_NO_BRACKET.matcher(text).replaceAll("<GRADIENT:");
        text = OPEN_MISSING_GT.matcher(text).replaceAll("<GRADIENT:$1>");
        text = CLOSE_NO_BRACKET.matcher(text).replaceAll("</GRADIENT:");
        return CLOSE_MISSING_GT.matcher(text).replaceAll("</GRADIENT:$1>");
    }

    private static String applyRainbows(String input) {
        Matcher matcher = RAINBOW.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            float saturation = parseRainbowSaturation(matcher.group(1), matcher.group(2));
            String content = inheritFormatting(input, matcher.start(), matcher.group(3));
            matcher.appendReplacement(out, Matcher.quoteReplacement(rainbow(content, saturation)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String applyGradients(String input) {
        Matcher matcher = GRADIENT.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            Color from = parseColor(matcher.group(1));
            Color to = parseColor(matcher.group(3));
            if (from == null || to == null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(2)));
            } else {
                String content = inheritFormatting(input, matcher.start(), matcher.group(2));
                matcher.appendReplacement(out, Matcher.quoteReplacement(gradient(content, from, to)));
            }
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String applySolids(String input) {
        Matcher matcher = SOLID.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            Color color = parseColor(matcher.group(1));
            String content = inheritFormatting(input, matcher.start(), matcher.group(2));
            String replacement = color == null ? matcher.group(2) : solid(content, color);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String applySolidInserts(String input) {
        Matcher matcher = SOLID_INSERT.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(hex(matcher.group(1))));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String applyHex(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(out, Matcher.quoteReplacement(hex(color)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String solid(String text, Color color) {
        return colorizeCharacters(text, new ColorSupplier() {
            @Override
            public Color color(int index, int total) {
                return color;
            }
        });
    }

    private static String gradient(String text, final Color from, final Color to) {
        return colorizeCharacters(text, new ColorSupplier() {
            @Override
            public Color color(int index, int total) {
                float t = total <= 1 ? 0 : (float) index / (float) (total - 1);
                int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * t);
                int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t);
                int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t);
                return new Color(r, g, b);
            }
        });
    }

    private static String rainbow(String text, final float saturation) {
        return colorizeCharacters(text, new ColorSupplier() {
            @Override
            public Color color(int index, int total) {
                float hue = total <= 1 ? 0 : (float) index / (float) total;
                return Color.getHSBColor(hue, saturation, 1.0f);
            }
        });
    }

    private static String colorizeCharacters(String text, ColorSupplier supplier) {
        StringBuilder builder = new StringBuilder();
        int visible = visibleCharacters(text);
        int colorIndex = 0;
        StringBuilder activeFormatting = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (isLegacyCode(text, i)) {
                char code = text.charAt(i + 1);
                if (isResetOrLegacyColor(code)) {
                    activeFormatting.setLength(0);
                } else if (!containsFormatting(activeFormatting, code)) {
                    activeFormatting.append(current).append(code);
                }
                i++;
                continue;
            }
            builder.append(hex(toHex(supplier.color(colorIndex++, visible)))).append(activeFormatting).append(current);
        }
        return builder.toString();
    }

    private static int visibleCharacters(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isLegacyCode(text, i)) {
                i++;
            } else {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private static boolean isLegacyCode(String text, int index) {
        return index + 1 < text.length() && (text.charAt(index) == '&' || text.charAt(index) == '§') && isColorCode(text.charAt(index + 1));
    }

    private static boolean isColorCode(char code) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(code) >= 0;
    }

    private static String inheritFormatting(String input, int endIndex, String content) {
        String formatting = activeFormatting(input, endIndex);
        return formatting.isEmpty() ? content : formatting + content;
    }

    private static String activeFormatting(String input, int endIndex) {
        StringBuilder formatting = new StringBuilder();
        for (int i = 0; i < endIndex; i++) {
            if (!isLegacyCode(input, i)) {
                continue;
            }
            char marker = input.charAt(i);
            char code = input.charAt(++i);
            if (isResetOrLegacyColor(code)) {
                formatting.setLength(0);
            } else if (!containsFormatting(formatting, code)) {
                formatting.append(marker).append(code);
            }
        }
        return formatting.toString();
    }

    private static boolean isResetOrLegacyColor(char code) {
        char normalized = Character.toLowerCase(code);
        return normalized == 'r' || "0123456789abcdef".indexOf(normalized) >= 0;
    }

    private static boolean containsFormatting(StringBuilder formatting, char code) {
        char normalized = Character.toLowerCase(code);
        for (int i = 1; i < formatting.length(); i += 2) {
            if (Character.toLowerCase(formatting.charAt(i)) == normalized) {
                return true;
            }
        }
        return false;
    }

    private static Color parseColor(String raw) {
        String normalized = raw.replace("#", "").toUpperCase(Locale.ROOT);
        if (normalized.matches("[A-F0-9]{6}")) {
            return Color.decode("#" + normalized);
        }
        if ("WHITE".equals(normalized)) return Color.WHITE;
        if ("BLACK".equals(normalized)) return Color.BLACK;
        if ("RED".equals(normalized)) return Color.RED;
        if ("GREEN".equals(normalized)) return Color.GREEN;
        if ("BLUE".equals(normalized)) return Color.BLUE;
        if ("YELLOW".equals(normalized)) return Color.YELLOW;
        if ("CYAN".equals(normalized) || "AQUA".equals(normalized)) return Color.CYAN;
        if ("MAGENTA".equals(normalized) || "PINK".equals(normalized)) return Color.MAGENTA;
        if ("ORANGE".equals(normalized) || "GOLD".equals(normalized)) return Color.ORANGE;
        if ("GRAY".equals(normalized) || "GREY".equals(normalized)) return Color.GRAY;
        if ("DARK_GRAY".equals(normalized) || "DARK_GREY".equals(normalized)) return Color.DARK_GRAY;
        return null;
    }

    private static String toHex(Color color) {
        return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String hex(String value) {
        Color color = Color.decode("#" + value);
        if (OF_COLOR_METHOD != null) {
            try {
                return String.valueOf(OF_COLOR_METHOD.invoke(null, color));
            } catch (Exception ignored) {
            }
        }
        if (OF_STRING_METHOD != null) {
            try {
                return String.valueOf(OF_STRING_METHOD.invoke(null, "#" + value));
            } catch (Exception ignored) {
            }
        }
        return closestLegacyColor(color).toString();
    }

    private static float parseRainbowSaturation(String iridiumValue, String castielValue) {
        String raw = castielValue != null ? castielValue : iridiumValue;
        if (raw == null || raw.trim().isEmpty()) {
            return 1.0f;
        }
        try {
            float value = Float.parseFloat(raw);
            if (castielValue == null && value > 1.0f) {
                value = value / 100.0f;
            }
            return clamp(value, 0.0f, 1.0f);
        } catch (NumberFormatException ignored) {
            return 1.0f;
        }
    }

    private static ChatColor closestLegacyColor(Color color) {
        LegacyColor nearest = LEGACY_COLORS[0];
        double nearestDistance = Double.MAX_VALUE;
        for (LegacyColor legacyColor : LEGACY_COLORS) {
            double distance = Math.pow(color.getRed() - legacyColor.color.getRed(), 2)
                    + Math.pow(color.getGreen() - legacyColor.color.getGreen(), 2)
                    + Math.pow(color.getBlue() - legacyColor.color.getBlue(), 2);
            if (distance < nearestDistance) {
                nearest = legacyColor;
                nearestDistance = distance;
            }
        }
        return nearest.chatColor;
    }

    private static Method findOfMethod(Class<?> parameterType) {
        try {
            Class<?> clazz = Class.forName("net.md_5.bungee.api.ChatColor");
            return clazz.getMethod("of", parameterType);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private interface ColorSupplier {
        Color color(int index, int total);
    }

    private static final class LegacyColor {
        private final Color color;
        private final ChatColor chatColor;

        private LegacyColor(Color color, ChatColor chatColor) {
            this.color = color;
            this.chatColor = chatColor;
        }
    }
}

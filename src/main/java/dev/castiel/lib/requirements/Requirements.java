package dev.castiel.lib.requirements;

import dev.castiel.lib.permissions.Permissions;
import dev.castiel.lib.util.Placeholders;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Requirements {
    private static final Pattern COMPARISON = Pattern.compile("(.+?)\\s*(>=|<=|==|!=|>|<|contains|starts-with|ends-with)\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private Requirements() {
    }

    public static boolean test(Player player, Iterable<String> rawRequirements) {
        return test(player, rawRequirements, Placeholders.empty());
    }

    public static boolean test(Player player, Iterable<String> rawRequirements, Placeholders placeholders) {
        RequirementContext context = new RequirementContext(player, placeholders);
        for (Requirement requirement : parseAll(rawRequirements)) {
            if (!requirement.test(context)) {
                return false;
            }
        }
        return true;
    }

    public static List<Requirement> parseAll(Iterable<String> rawRequirements) {
        List<Requirement> requirements = new ArrayList<Requirement>();
        if (rawRequirements == null) {
            return requirements;
        }
        for (String raw : rawRequirements) {
            Requirement requirement = parse(raw);
            if (requirement != null) {
                requirements.add(requirement);
            }
        }
        return requirements;
    }

    public static Requirement parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String expression = raw.trim();
        boolean inverted = expression.startsWith("!");
        if (inverted) {
            expression = expression.substring(1).trim();
        }
        String[] parts = expression.split(":", 2);
        final String type = parts[0].trim().toLowerCase(Locale.ROOT);
        final String value = parts.length > 1 ? parts[1].trim() : "";
        final boolean negate = inverted;
        return new Requirement() {
            @Override
            public boolean test(RequirementContext context) {
                boolean result = evaluate(type, value, context);
                return negate ? !result : result;
            }
        };
    }

    private static boolean evaluate(String type, String value, RequirementContext context) {
        Player player = context.player();
        if ("permission".equals(type) || "perm".equals(type)) {
            return player != null && Permissions.hasWildcard(player, value);
        }
        if ("any-permission".equals(type) || "any-perm".equals(type)) {
            return player != null && Permissions.hasAny(player, splitCsv(value));
        }
        if ("world".equals(type)) {
            return player != null && player.getWorld().getName().equalsIgnoreCase(value);
        }
        if ("gamemode".equals(type) || "game-mode".equals(type)) {
            return player != null && player.getGameMode() == parseGameMode(value);
        }
        if ("op".equals(type)) {
            return player != null && player.isOp() == Boolean.parseBoolean(value);
        }
        if ("placeholder".equals(type) || "compare".equals(type)) {
            return compare(context.placeholders().apply(value));
        }
        return compare(context.placeholders().apply(type + (value.isEmpty() ? "" : ":" + value)));
    }

    private static String[] splitCsv(String value) {
        String[] raw = value.split(",");
        for (int i = 0; i < raw.length; i++) {
            raw[i] = raw[i].trim();
        }
        return raw;
    }

    private static GameMode parseGameMode(String value) {
        try {
            return GameMode.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean compare(String expression) {
        Matcher matcher = COMPARISON.matcher(expression);
        if (!matcher.matches()) {
            return truthy(expression);
        }
        String left = stripQuotes(matcher.group(1).trim());
        String operator = matcher.group(2).toLowerCase(Locale.ROOT);
        String right = stripQuotes(matcher.group(3).trim());
        Double leftNumber = number(left);
        Double rightNumber = number(right);
        if (leftNumber != null && rightNumber != null) {
            int compared = Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
            return compareResult(compared, operator);
        }
        int compared = left.compareToIgnoreCase(right);
        if ("contains".equals(operator)) return left.toLowerCase(Locale.ROOT).contains(right.toLowerCase(Locale.ROOT));
        if ("starts-with".equals(operator)) return left.toLowerCase(Locale.ROOT).startsWith(right.toLowerCase(Locale.ROOT));
        if ("ends-with".equals(operator)) return left.toLowerCase(Locale.ROOT).endsWith(right.toLowerCase(Locale.ROOT));
        return compareResult(compared, operator);
    }

    private static boolean compareResult(int compared, String operator) {
        if (">=".equals(operator)) return compared >= 0;
        if ("<=".equals(operator)) return compared <= 0;
        if (">".equals(operator)) return compared > 0;
        if ("<".equals(operator)) return compared < 0;
        if ("!=".equals(operator)) return compared != 0;
        return compared == 0;
    }

    private static boolean truthy(String value) {
        String normalized = value == null ? "" : value.trim();
        return !normalized.isEmpty()
                && !"false".equalsIgnoreCase(normalized)
                && !"no".equalsIgnoreCase(normalized)
                && !"0".equals(normalized);
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static Double number(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

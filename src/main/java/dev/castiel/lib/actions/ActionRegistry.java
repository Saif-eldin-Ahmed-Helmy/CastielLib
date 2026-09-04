package dev.castiel.lib.actions;

import dev.castiel.lib.text.Colors;
import dev.castiel.lib.sounds.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ActionRegistry {
    private final JavaPlugin plugin;
    private final Map<String, ActionHandler> handlers = new LinkedHashMap<>();

    public ActionRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ActionRegistry defaults(JavaPlugin plugin) {
        ActionRegistry registry = new ActionRegistry(plugin);
        registry.register("console", (ctx, payload) -> requireDispatched(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), apply(ctx, payload)), payload));
        registry.register("player", (ctx, payload) -> requireDispatched(Bukkit.dispatchCommand(ctx.player, apply(ctx, payload)), payload));
        registry.register("message", (ctx, payload) -> ctx.player.sendMessage(Colors.color(apply(ctx, payload))));
        registry.register("broadcast", (ctx, payload) -> Bukkit.broadcastMessage(Colors.color(apply(ctx, payload))));
        registry.register("title", ActionRegistry::title);
        registry.register("action", ActionRegistry::actionBar);
        registry.register("sound", ActionRegistry::sound);
        registry.register("close", (ctx, payload) -> ctx.player.closeInventory());
        registry.register("particle", ActionRegistry::particle);
        return registry;
    }

    public ActionRegistry register(String tag, ActionHandler handler) {
        handlers.put(tag.toLowerCase(Locale.ROOT), handler);
        return this;
    }

    public void run(Player player, List<String> actions, dev.castiel.lib.util.Placeholders placeholders) {
        runChecked(player, actions, placeholders);
    }

    /** Executes a sequence and reports malformed, unsupported, or failed actions. */
    public ActionRunResult runChecked(Player player, List<String> actions, dev.castiel.lib.util.Placeholders placeholders) {
        if (player == null) return ActionRunResult.failure("Player is missing");
        ActionContext context = new ActionContext(plugin, player, placeholders);
        if (actions == null || actions.isEmpty()) return ActionRunResult.failure("Action sequence is empty");
        for (String action : actions) {
            String validation = validate(action);
            if (validation != null) return ActionRunResult.failure(validation);
        }
        for (String action : actions) {
            try {
                run(context, action);
            } catch (RuntimeException failure) {
                return ActionRunResult.failure("Action failed: " + failure.getMessage());
            }
        }
        return ActionRunResult.success();
    }

    public void run(ActionContext context, String raw) {
        if (raw == null || !raw.startsWith("{")) {
            return;
        }
        int close = raw.indexOf('}');
        if (close < 0) {
            return;
        }
        String tag = raw.substring(1, close).toLowerCase(Locale.ROOT);
        String payload = raw.substring(close + 1).trim();
        ActionHandler handler = handlers.get(tag);
        if (handler != null) {
            handler.execute(context, payload);
        }
    }

    private String validate(String raw) {
        if (raw == null || !raw.startsWith("{")) return "Malformed action: " + raw;
        int close = raw.indexOf('}');
        if (close <= 1) return "Malformed action: " + raw;
        String tag = raw.substring(1, close).toLowerCase(Locale.ROOT);
        return handlers.containsKey(tag) ? null : "Unsupported action tag: " + tag;
    }

    private static void requireDispatched(boolean dispatched, String payload) {
        if (!dispatched) throw new IllegalStateException("Command was rejected: " + payload);
    }

    private static String apply(ActionContext ctx, String payload) {
        return ctx.placeholders.apply(payload).replace("%player%", ctx.player.getName());
    }

    private static void title(ActionContext ctx, String payload) {
        String[] parts = apply(ctx, payload).split(";", 2);
        ctx.player.sendTitle(Colors.color(parts[0]), Colors.color(parts.length > 1 ? parts[1] : ""));
    }

    private static void actionBar(ActionContext ctx, String payload) {
        try {
            Method spigot = Player.class.getMethod("spigot");
            Object playerSpigot = spigot.invoke(ctx.player);
            Class<?> component = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object[] components = (Object[]) component.getMethod("fromLegacyText", String.class).invoke(null, Colors.color(apply(ctx, payload)));
            Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBar = Enum.valueOf((Class<Enum>) chatMessageType.asSubclass(Enum.class), "ACTION_BAR");
            playerSpigot.getClass().getMethod("sendMessage", chatMessageType, Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;")).invoke(playerSpigot, actionBar, components);
        } catch (ReflectiveOperationException ignored) {
            ctx.player.sendMessage(Colors.color(apply(ctx, payload)));
        }
    }

    private static void sound(ActionContext ctx, String payload) {
        String name = apply(ctx, payload).split("\\s+")[0];
        Sounds.play(ctx.player, name, 1f, 1f);
    }

    private static void particle(ActionContext ctx, String payload) {
        ParticleShape.spawn(ctx.player.getLocation(), payload);
    }
}

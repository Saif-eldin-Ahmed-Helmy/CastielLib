package dev.castiel.lib.actions;

import dev.castiel.lib.util.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ActionContext {
    public final JavaPlugin plugin;
    public final Player player;
    public final Placeholders placeholders;

    public ActionContext(JavaPlugin plugin, Player player, Placeholders placeholders) {
        this.plugin = plugin;
        this.player = player;
        this.placeholders = placeholders == null ? Placeholders.empty() : placeholders;
    }
}

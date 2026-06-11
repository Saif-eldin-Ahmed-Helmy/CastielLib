package dev.castiel.lib.requirements;

import dev.castiel.lib.util.Placeholders;
import org.bukkit.entity.Player;

public final class RequirementContext {
    private final Player player;
    private final Placeholders placeholders;

    public RequirementContext(Player player, Placeholders placeholders) {
        this.player = player;
        this.placeholders = placeholders == null ? Placeholders.empty() : placeholders;
    }

    public Player player() {
        return player;
    }

    public Placeholders placeholders() {
        return placeholders;
    }
}

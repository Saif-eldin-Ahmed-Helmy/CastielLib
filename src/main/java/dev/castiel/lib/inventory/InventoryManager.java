package dev.castiel.lib.inventory;

import dev.castiel.lib.actions.ActionRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryManager implements Listener {
    private final JavaPlugin plugin;
    private final ActionRegistry actions;
    private final Map<String, CastielMenu> menus = new LinkedHashMap<>();
    private InventoryProvider provider = (player, menuId, itemId) -> dev.castiel.lib.util.Placeholders.empty();

    public InventoryManager(JavaPlugin plugin, ActionRegistry actions) {
        this.plugin = plugin;
        this.actions = actions;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public InventoryManager placeholders(InventoryProvider provider) {
        this.provider = provider;
        return this;
    }

    public void load(File file) {
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            if (yaml.isConfigurationSection(key)) {
                menus.put(key, new CastielMenu(key, yaml.getConfigurationSection(key)));
            }
        }
    }

    public void open(Player player, String menuId) {
        CastielMenu menu = menus.get(menuId);
        if (menu != null && menu.enabled()) {
            player.openInventory(menu.create(player, actions, provider));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ManagedMenuHolder) {
            ((ManagedMenuHolder) event.getInventory().getHolder()).handle(event);
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MenuHolder) || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        event.setCancelled(true);
        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        CastielMenu menu = menus.get(holder.menuId);
        if (menu == null) {
            return;
        }
        CastielMenu.MenuItem item = menu.item(event.getRawSlot());
        if (item != null) {
            actions.run((Player) event.getWhoClicked(), item.actions(), provider.placeholders((Player) event.getWhoClicked(), holder.menuId, item.id));
        }
    }
}

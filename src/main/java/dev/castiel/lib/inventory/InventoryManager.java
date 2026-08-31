package dev.castiel.lib.inventory;

import dev.castiel.lib.actions.ActionRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

public final class InventoryManager implements Listener {
    private final JavaPlugin plugin;
    private final ActionRegistry actions;
    private final Map<String, CastielMenu> menus = new LinkedHashMap<>();
    private final ProtectionRegistry protectedInventories = new ProtectionRegistry();
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

    /**
     * Marks a custom inventory as protected by this manager's shutdown handling.
     * CastielLib menus are recognized by their holders and need no registration.
     */
    public void protect(Inventory inventory) {
        if (inventory != null) {
            protectedInventories.add(inventory);
        }
    }

    /** Removes a custom inventory from shutdown handling. */
    public void unprotect(Inventory inventory) {
        if (inventory != null) {
            protectedInventories.remove(inventory);
        }
    }

    /**
     * Closes protected views before Bukkit unregisters their click listeners.
     * Must be called on the server thread.
     */
    public void closeOpenMenus() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Inventory top = null;
            try {
                top = player.getOpenInventory().getTopInventory();
                if (isProtected(top)) {
                    player.closeInventory();
                }
            } catch (Throwable closeFailure) {
                plugin.getLogger().log(Level.WARNING,
                        "Could not close a protected inventory during shutdown", closeFailure);
                if (top != null && isProtected(top)) {
                    try {
                        top.clear();
                    } catch (Throwable clearFailure) {
                        plugin.getLogger().log(Level.SEVERE,
                                "Could not make a protected inventory safe during shutdown", clearFailure);
                    }
                }
            }
        }
        protectedInventories.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            closeOpenMenus();
        }
    }

    boolean isProtected(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        return protectedInventories.contains(inventory);
    }

    static final class ProtectionRegistry {
        private final Set<Inventory> inventories = Collections.newSetFromMap(new WeakHashMap<Inventory, Boolean>());

        void add(Inventory inventory) {
            inventories.add(inventory);
        }

        void remove(Inventory inventory) {
            inventories.remove(inventory);
        }

        boolean contains(Inventory inventory) {
            if (inventory == null) {
                return false;
            }
            InventoryHolder holder = inventory.getHolder();
            return holder instanceof ManagedMenuHolder
                    || holder instanceof MenuHolder
                    || inventories.contains(inventory);
        }

        void clear() {
            inventories.clear();
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

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
     * Call on the server thread.
     */
    public void protect(Inventory inventory) {
        if (inventory != null) {
            protectedInventories.add(inventory);
        }
    }

    /** Removes a custom inventory from shutdown handling. Call on the server thread. */
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
            try {
                closeProtectedView(protectedInventories, new ViewControl() {
                    @Override
                    public Inventory topInventory() {
                        return player.getOpenInventory().getTopInventory();
                    }

                    @Override
                    public void close() {
                        player.closeInventory();
                    }
                });
            } catch (Throwable closeFailure) {
                plugin.getLogger().log(Level.WARNING,
                        "Could not close a protected inventory during shutdown", closeFailure);
            }
        }
        try {
            protectedInventories.clearRegisteredContents();
        } catch (Throwable clearFailure) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not make every protected inventory safe during shutdown", clearFailure);
        }
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

    static void closeProtectedView(ProtectionRegistry registry, ViewControl view) {
        Inventory top;
        try {
            top = view.topInventory();
        } catch (Throwable lookupFailure) {
            // We cannot classify the view, so closing it is the only safe fallback.
            view.close();
            return;
        }
        if (!registry.contains(top)) {
            return;
        }
        try {
            view.close();
        } catch (Throwable closeFailure) {
            top.clear();
        }
    }

    interface ViewControl {
        Inventory topInventory();

        void close();
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

        void clearRegisteredContents() {
            Throwable firstFailure = null;
            for (Inventory inventory : new java.util.ArrayList<Inventory>(inventories)) {
                try {
                    inventory.clear();
                } catch (Throwable failure) {
                    if (firstFailure == null) {
                        firstFailure = failure;
                    }
                }
            }
            inventories.clear();
            if (firstFailure != null) {
                throw new IllegalStateException("Could not clear a protected inventory", firstFailure);
            }
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

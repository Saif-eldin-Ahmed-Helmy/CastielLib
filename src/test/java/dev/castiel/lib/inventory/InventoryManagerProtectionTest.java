package dev.castiel.lib.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryManagerProtectionTest {

    @Test
    void customInventoriesCanBeProtectedAndReleased() {
        InventoryManager.ProtectionRegistry registry = new InventoryManager.ProtectionRegistry();
        Inventory inventory = inventory(null);

        assertFalse(registry.contains(inventory));
        registry.add(inventory);
        assertTrue(registry.contains(inventory));
        registry.remove(inventory);
        assertFalse(registry.contains(inventory));
    }

    @Test
    void managedMenuHoldersAreProtectedWithoutRegistration() {
        InventoryManager.ProtectionRegistry registry = new InventoryManager.ProtectionRegistry();

        assertTrue(registry.contains(inventory(new ManagedMenuHolder("test"))));
        assertTrue(registry.contains(inventory(new MenuHolder("test", null))));
    }

    @Test
    void protectedViewClosesNormally() {
        InventoryManager.ProtectionRegistry registry = new InventoryManager.ProtectionRegistry();
        Inventory inventory = inventory(null);
        registry.add(inventory);
        AtomicBoolean closed = new AtomicBoolean();

        InventoryManager.closeProtectedView(registry, view(inventory, closed, false, false));

        assertTrue(closed.get());
    }

    @Test
    void failedCloseClearsProtectedContents() {
        AtomicBoolean cleared = new AtomicBoolean();
        Inventory inventory = inventory(null, cleared);
        InventoryManager.ProtectionRegistry registry = new InventoryManager.ProtectionRegistry();
        registry.add(inventory);

        InventoryManager.closeProtectedView(registry, view(inventory, new AtomicBoolean(), false, true));

        assertTrue(cleared.get());
    }

    @Test
    void failedViewLookupStillAttemptsClose() {
        AtomicBoolean closed = new AtomicBoolean();

        InventoryManager.closeProtectedView(new InventoryManager.ProtectionRegistry(),
                view(null, closed, true, false));

        assertTrue(closed.get());
    }

    @Test
    void registryCleanupClearsCustomInventories() {
        AtomicBoolean cleared = new AtomicBoolean();
        Inventory inventory = inventory(null, cleared);
        InventoryManager.ProtectionRegistry registry = new InventoryManager.ProtectionRegistry();
        registry.add(inventory);

        registry.clearRegisteredContents();

        assertTrue(cleared.get());
        assertFalse(registry.contains(inventory));
    }

    private static Inventory inventory(InventoryHolder holder) {
        return inventory(holder, new AtomicBoolean());
    }

    private static Inventory inventory(InventoryHolder holder, AtomicBoolean cleared) {
        return (Inventory) Proxy.newProxyInstance(
                Inventory.class.getClassLoader(),
                new Class<?>[]{Inventory.class},
                (proxy, method, arguments) -> {
                    if ("getHolder".equals(method.getName())) {
                        return holder;
                    }
                    if ("clear".equals(method.getName())) {
                        cleared.set(true);
                        return null;
                    }
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    if (type == double.class) return 0D;
                    if (type == float.class) return 0F;
                    if (type == short.class) return (short) 0;
                    if (type == byte.class) return (byte) 0;
                    if (type == char.class) return (char) 0;
                    return null;
                }
        );
    }

    private static InventoryManager.ViewControl view(Inventory inventory, AtomicBoolean closed,
                                                     boolean failLookup, boolean failClose) {
        return new InventoryManager.ViewControl() {
            @Override
            public Inventory topInventory() {
                if (failLookup) throw new IllegalStateException("lookup failed");
                return inventory;
            }

            @Override
            public void close() {
                if (failClose) throw new IllegalStateException("close failed");
                closed.set(true);
            }
        };
    }
}

package dev.castiel.lib.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

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
    }

    private static Inventory inventory(InventoryHolder holder) {
        return (Inventory) Proxy.newProxyInstance(
                Inventory.class.getClassLoader(),
                new Class<?>[]{Inventory.class},
                (proxy, method, arguments) -> {
                    if ("getHolder".equals(method.getName())) {
                        return holder;
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
}

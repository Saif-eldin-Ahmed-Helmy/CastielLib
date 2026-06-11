package dev.castiel.lib.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

public final class InventoryMath {
    private InventoryMath() {
    }

    public static boolean canFit(PlayerInventory inventory, ItemStack sample, int amount) {
        if (sample == null || sample.getType() == Material.AIR || amount <= 0) {
            return true;
        }
        int remaining = amount;
        int maxStack = Math.max(1, sample.getMaxStackSize());
        for (ItemStack content : inventory.getStorageContents()) {
            if (content != null && content.isSimilar(sample)) {
                remaining -= Math.max(0, maxStack - content.getAmount());
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) {
                remaining -= maxStack;
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int countSimilar(PlayerInventory inventory, ItemStack sample) {
        int count = 0;
        if (sample == null) {
            return count;
        }
        for (ItemStack content : inventory.getStorageContents()) {
            if (content != null && content.isSimilar(sample)) {
                count += content.getAmount();
            }
        }
        return count;
    }

    public static int removeSimilar(PlayerInventory inventory, ItemStack sample, int amount) {
        if (sample == null || amount <= 0) {
            return 0;
        }
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack content = contents[i];
            if (content == null || !content.isSimilar(sample)) {
                continue;
            }
            int taking = Math.min(remaining, content.getAmount());
            content.setAmount(content.getAmount() - taking);
            remaining -= taking;
            if (content.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        inventory.setStorageContents(contents);
        return amount - remaining;
    }

    public static Map<Integer, ItemStack> give(Inventory inventory, ItemStack sample, int amount) {
        Map<Integer, ItemStack> leftovers = new HashMap<Integer, ItemStack>();
        if (inventory == null || sample == null || sample.getType() == Material.AIR || amount <= 0) {
            return leftovers;
        }
        int maxStack = Math.max(1, sample.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            ItemStack chunk = sample.clone();
            int requested = Math.min(maxStack, remaining);
            chunk.setAmount(requested);
            Map<Integer, ItemStack> chunkLeftovers = inventory.addItem(chunk);
            int leftoverAmount = amount(chunkLeftovers);
            remaining -= requested - leftoverAmount;
            leftovers.putAll(chunkLeftovers);
            if (leftoverAmount > 0) {
                break;
            }
        }
        return leftovers;
    }

    public static boolean giveAtomic(PlayerInventory inventory, ItemStack sample, int amount) {
        ItemStack[] backup = cloneContents(inventory.getStorageContents());
        Map<Integer, ItemStack> leftovers = give(inventory, sample, amount);
        if (!leftovers.isEmpty()) {
            inventory.setStorageContents(backup);
            return false;
        }
        return true;
    }

    private static int amount(Map<Integer, ItemStack> items) {
        int amount = 0;
        for (ItemStack item : items.values()) {
            if (item != null) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = contents[i] == null ? null : contents[i].clone();
        }
        return clone;
    }
}

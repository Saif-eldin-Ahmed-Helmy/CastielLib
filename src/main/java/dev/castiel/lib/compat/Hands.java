package dev.castiel.lib.compat;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.Method;

/**
 * Tells main-hand interactions apart from off-hand ones across every
 * supported server line.
 *
 * From 1.9 onwards a single right-click fires {@link PlayerInteractEvent}
 * twice -- once per hand -- so a listener that does not check which hand it is
 * looking at runs its effect twice for one player action. 1.8.8 has no off
 * hand and no {@code getHand()} at all, so the check cannot be compiled
 * against directly; it is resolved once, reflectively, and treated as
 * "main hand" where the concept does not exist.
 */
public final class Hands {
    private static final Method GET_HAND = findGetHand();
    private static final String MAIN_HAND = "HAND";
    private static final Method SET_OFF_HAND = findSetOffHand();

    private Hands() {
    }

    /**
     * Whether this event is the main-hand half of the interaction, and so the
     * one a listener should act on. Always true on 1.8.8.
     */
    public static boolean isMainHand(PlayerInteractEvent event) {
        if (event == null) return false;
        if (GET_HAND == null) return true;
        try {
            Object hand = GET_HAND.invoke(event);
            // A null hand comes from synthetic interactions (physical
            // triggers such as pressure plates); those fire once, so
            // treating them as main-hand keeps them working.
            return hand == null || MAIN_HAND.equals(((Enum<?>) hand).name());
        } catch (ReflectiveOperationException | ClassCastException unavailable) {
            return true;
        }
    }

    /**
     * Replaces what the player is holding in one specific hand.
     *
     * Consuming an item with {@code setItemInHand} is only correct when the
     * item came from the main hand; used on an off-hand interaction it
     * destroys whatever the player happened to be holding in the other hand
     * instead. 1.8.8 has no off hand, so there the main-hand path is the only
     * one that can be reached.
     */
    public static void setItemInHand(Player player, boolean mainHand, ItemStack item) {
        if (player == null) return;
        PlayerInventory inventory = player.getInventory();
        if (mainHand || SET_OFF_HAND == null) {
            inventory.setItemInHand(item);
            return;
        }
        try {
            SET_OFF_HAND.invoke(inventory, item);
        } catch (ReflectiveOperationException unavailable) {
            inventory.setItemInHand(item);
        }
    }

    private static Method findGetHand() {
        try {
            return PlayerInteractEvent.class.getMethod("getHand");
        } catch (NoSuchMethodException absent) {
            return null;
        }
    }

    private static Method findSetOffHand() {
        try {
            return PlayerInventory.class.getMethod("setItemInOffHand", ItemStack.class);
        } catch (NoSuchMethodException absent) {
            return null;
        }
    }
}

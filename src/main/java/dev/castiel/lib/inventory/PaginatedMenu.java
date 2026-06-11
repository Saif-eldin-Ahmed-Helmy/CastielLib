package dev.castiel.lib.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class PaginatedMenu<T> {
    private final String id;
    private final int size;
    private final String title;
    private final List<Integer> contentSlots;
    private final Function<T, ItemStack> renderer;
    private final PagedClick<T> click;
    private final Map<Integer, MenuButton> staticButtons = new HashMap<>();
    private int previousSlot = -1;
    private int nextSlot = -1;
    private ItemStack previousItem;
    private ItemStack nextItem;

    public PaginatedMenu(String id, int size, String title, List<Integer> contentSlots, Function<T, ItemStack> renderer, BiConsumer<Player, T> click) {
        this(id, size, title, contentSlots, renderer, (player, entry, event) -> click.accept(player, entry));
    }

    public PaginatedMenu(String id, int size, String title, List<Integer> contentSlots, Function<T, ItemStack> renderer, PagedClick<T> click) {
        this.id = id;
        this.size = size;
        this.title = title;
        this.contentSlots = Collections.unmodifiableList(new ArrayList<>(contentSlots));
        this.renderer = renderer;
        this.click = click;
    }

    public PaginatedMenu<T> navigation(int previousSlot, ItemStack previousItem, int nextSlot, ItemStack nextItem) {
        this.previousSlot = previousSlot;
        this.previousItem = previousItem == null ? null : previousItem.clone();
        this.nextSlot = nextSlot;
        this.nextItem = nextItem == null ? null : nextItem.clone();
        return this;
    }

    public PaginatedMenu<T> item(int slot, ItemStack item) {
        return button(slot, MenuButton.decorative(item));
    }

    public PaginatedMenu<T> button(int slot, MenuButton button) {
        if (slot >= 0 && button != null) {
            staticButtons.put(slot, button);
        }
        return this;
    }

    public void open(Player player, List<T> entries, int page) {
        List<T> safeEntries = entries == null ? Collections.emptyList() : entries;
        int pages = pageCount(safeEntries.size(), contentSlots.size());
        int current = normalizePage(page, pages);
        ManagedMenu menu = ManagedMenu.create(id, size, title(current, pages));
        for (Map.Entry<Integer, MenuButton> entry : staticButtons.entrySet()) {
            menu.button(entry.getKey(), entry.getValue());
        }
        int start = current * contentSlots.size();
        int end = Math.min(safeEntries.size(), start + contentSlots.size());
        for (int i = start; i < end; i++) {
            final T entry = safeEntries.get(i);
            menu.button(contentSlots.get(i - start), MenuButton.clickable(renderer.apply(entry), (p, event) -> click.accept(p, entry, event)));
        }
        if (previousSlot >= 0 && current > 0) {
            menu.button(previousSlot, MenuButton.clickable(previousItem, (p, event) -> open(p, safeEntries, current - 1)));
        }
        if (nextSlot >= 0 && current + 1 < pages) {
            menu.button(nextSlot, MenuButton.clickable(nextItem, (p, event) -> open(p, safeEntries, current + 1)));
        }
        menu.open(player);
    }

    public static int pageCount(int entries, int pageSize) {
        if (pageSize <= 0) {
            return 1;
        }
        return Math.max(1, (entries + pageSize - 1) / pageSize);
    }

    public static int normalizePage(int page, int pages) {
        return Math.max(0, Math.min(Math.max(1, pages) - 1, page));
    }

    public static List<Integer> rectangle(int fromSlot, int rows, int columns) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            int base = fromSlot + row * 9;
            for (int column = 0; column < columns; column++) {
                slots.add(base + column);
            }
        }
        return slots;
    }

    private String title(int page, int pages) {
        return title.replace("%page%", String.valueOf(page + 1)).replace("%pages%", String.valueOf(pages));
    }

    @FunctionalInterface
    public interface PagedClick<T> {
        void accept(Player player, T entry, InventoryClickEvent event);
    }
}

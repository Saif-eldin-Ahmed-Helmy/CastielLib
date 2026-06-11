package dev.castiel.lib.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SlotParser {
    private SlotParser() {
    }

    public static List<Integer> parse(Collection<String> entries) {
        List<Integer> slots = new ArrayList<Integer>();
        if (entries == null) {
            return slots;
        }
        for (String entry : entries) {
            slots.addAll(parse(entry));
        }
        return slots;
    }

    public static List<Integer> parse(String entry) {
        List<Integer> slots = new ArrayList<Integer>();
        if (entry == null || entry.trim().isEmpty()) {
            return slots;
        }
        String[] parts = entry.split(",");
        for (String part : parts) {
            addPart(slots, part.trim());
        }
        return slots;
    }

    public static List<Integer> rectangle(int fromSlot, int rows, int columns) {
        List<Integer> slots = new ArrayList<Integer>();
        for (int row = 0; row < rows; row++) {
            int base = fromSlot + row * 9;
            for (int column = 0; column < columns; column++) {
                slots.add(base + column);
            }
        }
        return slots;
    }

    private static void addPart(List<Integer> slots, String part) {
        if (part.isEmpty()) {
            return;
        }
        String delimiter = part.contains("..") ? "\\.\\." : "-";
        if (part.contains("-") || part.contains("..")) {
            String[] bounds = part.split(delimiter, 2);
            int start = Integer.parseInt(bounds[0].trim());
            int end = Integer.parseInt(bounds[1].trim());
            int step = start <= end ? 1 : -1;
            for (int slot = start; slot != end + step; slot += step) {
                slots.add(slot);
            }
            return;
        }
        slots.add(Integer.parseInt(part));
    }
}

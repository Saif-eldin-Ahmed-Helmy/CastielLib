package dev.castiel.lib.random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class WeightedTable<T> {
    private final List<Entry<T>> entries = new ArrayList<Entry<T>>();
    private final Random random;

    public WeightedTable() {
        this(new Random());
    }

    public WeightedTable(Random random) {
        this.random = random == null ? new Random() : random;
    }

    public WeightedTable<T> add(T value, double weight) {
        if (value != null && weight > 0) {
            entries.add(new Entry<T>(value, weight));
        }
        return this;
    }

    public T roll() {
        double total = totalWeight();
        if (total <= 0) {
            return null;
        }
        double cursor = random.nextDouble() * total;
        for (Entry<T> entry : entries) {
            cursor -= entry.weight;
            if (cursor <= 0) {
                return entry.value;
            }
        }
        return entries.isEmpty() ? null : entries.get(entries.size() - 1).value;
    }

    public double totalWeight() {
        double total = 0;
        for (Entry<T> entry : entries) {
            total += Math.max(0, entry.weight);
        }
        return total;
    }

    public List<Entry<T>> entries() {
        return Collections.unmodifiableList(entries);
    }

    public static <T extends Weighted> WeightedTable<T> fromWeighted(Iterable<T> values) {
        WeightedTable<T> table = new WeightedTable<T>();
        if (values != null) {
            for (T value : values) {
                table.add(value, value.weight());
            }
        }
        return table;
    }

    public static final class Entry<T> {
        private final T value;
        private final double weight;

        private Entry(T value, double weight) {
            this.value = value;
            this.weight = weight;
        }

        public T value() {
            return value;
        }

        public double weight() {
            return weight;
        }
    }
}

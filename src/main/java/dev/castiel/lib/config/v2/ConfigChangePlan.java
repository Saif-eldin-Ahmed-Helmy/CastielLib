package dev.castiel.lib.config.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable, safe dry-run configuration change plan. */
public final class ConfigChangePlan {
    private final List<ConfigChange> changes;
    private final int added;
    private final int removed;
    private final int changed;
    private final int orderChanged;

    ConfigChangePlan(List<ConfigChange> changes) {
        List<ConfigChange> copy = new ArrayList<ConfigChange>(
                Objects.requireNonNull(changes, "changes").size());
        int addedCount = 0;
        int removedCount = 0;
        int changedCount = 0;
        int orderChangedCount = 0;
        for (ConfigChange change : changes) {
            ConfigChange checked = Objects.requireNonNull(change, "changes element");
            copy.add(checked);
            switch (checked.kind()) {
                case ADDED:
                    addedCount++;
                    break;
                case REMOVED:
                    removedCount++;
                    break;
                case CHANGED:
                    changedCount++;
                    break;
                case ORDER_CHANGED:
                    orderChangedCount++;
                    break;
                default:
                    throw new IllegalStateException("unhandled change kind");
            }
        }
        this.changes = Collections.unmodifiableList(copy);
        this.added = addedCount;
        this.removed = removedCount;
        this.changed = changedCount;
        this.orderChanged = orderChangedCount;
    }

    static ConfigChangePlan of(List<ConfigChange> changes) {
        return new ConfigChangePlan(changes);
    }

    /** @return whether no change is needed */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /** @return changes in deterministic encounter order */
    public List<ConfigChange> changes() {
        return changes;
    }

    /** @return number of added paths */
    public int addedCount() {
        return added;
    }

    /** @return number of removed paths */
    public int removedCount() {
        return removed;
    }

    /** @return number of value changes */
    public int changedCount() {
        return changed;
    }

    /** @return number of order-only changes */
    public int orderChangedCount() {
        return orderChanged;
    }

    @Override
    public String toString() {
        return "ConfigChangePlan{" +
                "changeCount=" + changes.size() +
                ", added=" + added +
                ", removed=" + removed +
                ", changed=" + changed +
                ", orderChanged=" + orderChanged +
                '}';
    }
}

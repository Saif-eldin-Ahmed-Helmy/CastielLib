package dev.castiel.lib.config.v2;

import java.util.Objects;

/** One safe, value-free configuration preview entry. */
public final class ConfigChange {
    /** Kind of change represented by one entry. */
    public enum Kind {
        ADDED,
        REMOVED,
        CHANGED,
        ORDER_CHANGED
    }

    private final String path;
    private final Kind kind;
    private final String beforeDescriptor;
    private final String afterDescriptor;

    ConfigChange(String path, Kind kind, String beforeDescriptor, String afterDescriptor) {
        this.path = requireText(path, "path");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.beforeDescriptor = requireText(beforeDescriptor, "beforeDescriptor");
        this.afterDescriptor = requireText(afterDescriptor, "afterDescriptor");
    }

    /** @return bounded, sanitized configuration path */
    public String path() {
        return path;
    }

    /** @return stable change kind */
    public Kind kind() {
        return kind;
    }

    /** @return safe type/shape descriptor before the change */
    public String beforeDescriptor() {
        return beforeDescriptor;
    }

    /** @return safe type/shape descriptor after the change */
    public String afterDescriptor() {
        return afterDescriptor;
    }

    @Override
    public String toString() {
        return "ConfigChange{" +
                "path='" + path + '\'' +
                ", kind=" + kind +
                ", before='" + beforeDescriptor + '\'' +
                ", after='" + afterDescriptor + '\'' +
                '}';
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

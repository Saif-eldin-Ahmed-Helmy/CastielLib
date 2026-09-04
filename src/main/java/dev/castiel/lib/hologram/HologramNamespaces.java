package dev.castiel.lib.hologram;

final class HologramNamespaces {
    private HologramNamespaces() {
    }

    static String normalize(String namespace) {
        if (namespace == null) {
            return "";
        }
        String value = namespace.trim();
        while (value.endsWith(":")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    static boolean owns(String namespace, String displayId) {
        String normalized = normalize(namespace);
        return !normalized.isEmpty() && displayId != null
                && (displayId.equals(normalized) || displayId.startsWith(normalized + ":"));
    }

    static String namespaceOf(String displayId) {
        if (displayId == null) {
            return "";
        }
        int separator = displayId.indexOf(':');
        return separator <= 0 ? "" : displayId.substring(0, separator);
    }
}

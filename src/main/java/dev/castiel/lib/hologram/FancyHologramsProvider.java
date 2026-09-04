package dev.castiel.lib.hologram;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

/** Optional FancyHolograms adapter. No FancyHolograms type is linked directly. */
public final class FancyHologramsProvider implements HologramProvider {
    private final JavaPlugin plugin;
    private final ProviderClasses classes;
    private final Map<String, Object> handles = new LinkedHashMap<String, Object>();

    public FancyHologramsProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.classes = new ProviderClasses(plugin, "FancyHolograms");
    }

    public String id() {
        return "fancy";
    }

    public ProviderAvailability availability() {
        if (classes.plugin() == null) {
            return ProviderAvailability.unavailable(id(), "FancyHolograms is not installed");
        }
        try {
            classes.load("de.oliver.fancyholograms.api.FancyHologramsPlugin");
            classes.load("de.oliver.fancyholograms.api.data.TextHologramData");
            return ProviderAvailability.available(id());
        } catch (Throwable throwable) {
            return ProviderAvailability.unavailable(id(), "FancyHolograms requires an unsupported runtime");
        }
    }

    public HologramCapabilities capabilities() {
        if (!availability().available()) return HologramCapabilities.none();
        return new HologramCapabilities(true, supportsItem(), supportsItem(), supportsDiscovery());
    }

    public HologramResult show(String displayId, HologramDisplay display) {
        ProviderAvailability status = availability();
        if (!status.available()) {
            return HologramResult.failure(id(), status.reason());
        }
        if (display == null || display.location() == null) {
            return HologramResult.failure(id(), "display location is missing");
        }
        try {
            Object manager = manager();
            removeFromManager(manager, displayId);
            Class<?> dataType = classes.load("de.oliver.fancyholograms.api.data.TextHologramData");
            Constructor<?> constructor = dataType.getConstructor(String.class, Location.class);
            Object data = constructor.newInstance(displayId, display.location());
            setText(data, display.lines());
            Object hologram = invoke(manager, "create", data);
            invokeOptional(hologram, "setPersistent", Boolean.FALSE);
            invoke(manager, "addHologram", hologram);
            handles.put(displayId, hologram);
            boolean fallback = false;
            String reason = "";
            if (display.item() != null) {
                if (supportsItem()) {
                    String itemId = displayId + ":item";
                    remove(itemId);
                    Object itemData = newData("de.oliver.fancyholograms.api.data.ItemHologramData",
                            itemId, display.location().clone().add(0, display.itemOffsetY(), 0));
                    setItem(itemData, display.item());
                    Object itemHologram = invoke(manager, "create", itemData);
                    invokeOptional(itemHologram, "setPersistent", Boolean.FALSE);
                    invoke(manager, "addHologram", itemHologram);
                    handles.put(itemId, itemHologram);
                } else {
                    fallback = true;
                    reason = "installed FancyHolograms version does not expose item holograms; rendered text only";
                }
            }
            return HologramResult.success(id(), capabilities(), fallback, reason);
        } catch (Throwable throwable) {
            return HologramResult.failure(id(), message(throwable));
        }
    }

    public HologramResult remove(String displayId) {
        try {
            removeFromManager(manager(), displayId);
        } catch (Throwable throwable) {
            return HologramResult.failure(id(), message(throwable));
        }
        handles.remove(displayId);
        if (!displayId.endsWith(":item")) {
            try {
                removeFromManager(manager(), displayId + ":item");
            } catch (Throwable throwable) {
                return HologramResult.failure(id(), message(throwable));
            }
            handles.remove(displayId + ":item");
        }
        return HologramResult.success(id());
    }

    public HologramResult removeAll(Set<String> displayIds) {
        StringBuilder failures = new StringBuilder();
        if (displayIds != null) {
            for (String displayId : displayIds) {
                HologramResult result = remove(displayId);
                if (!result.successful()) {
                    if (failures.length() > 0) failures.append("; ");
                    failures.append(displayId).append(": ").append(result.reason());
                }
            }
        }
        if (failures.length() > 0) return HologramResult.failure(id(), failures.toString());
        return HologramResult.success(id());
    }

    public Set<String> ownedIds() {
        return new java.util.LinkedHashSet<String>(handles.keySet());
    }

    public Set<String> ownedIds(String namespace) {
        Set<String> result = HologramProvider.super.ownedIds(namespace);
        try {
            Object values = invoke(manager(), "getHolograms");
            collectNames(values, namespace, result);
        } catch (Throwable ignored) {
            // Some versions expose lookup/removal but no safe global listing.
        }
        return result;
    }

    public void shutdown() {
        removeAll(new java.util.HashSet<String>(handles.keySet()));
    }

    private Object manager() throws Exception {
        Class<?> pluginType = classes.load("de.oliver.fancyholograms.api.FancyHologramsPlugin");
        Object fancyPlugin = pluginType.getMethod("get").invoke(null);
        return fancyPlugin.getClass().getMethod("getHologramManager").invoke(fancyPlugin);
    }

    private static void removeFromManager(Object manager, String id) throws Exception {
        invoke(manager, "removeHologram", id);
    }

    private static void setText(Object data, java.util.List<String> lines) throws Exception {
        for (Method method : data.getClass().getMethods()) {
            if (!method.getName().equals("setText") || method.getParameterTypes().length != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            if (java.util.List.class.isAssignableFrom(parameter)) {
                method.invoke(data, lines);
                return;
            }
            if (String.class.equals(parameter)) {
                method.invoke(data, lines.isEmpty() ? "" : lines.get(0));
                return;
            }
        }
        throw new NoSuchMethodException("setText");
    }

    private Object newData(String typeName, String name, Location location) throws Exception {
        Class<?> type = classes.load(typeName);
        Constructor<?> constructor = type.getConstructor(String.class, Location.class);
        return constructor.newInstance(name, location);
    }

    private static void setItem(Object data, Object item) throws Exception {
        try {
            invoke(data, "setItemStack", item);
        } catch (NoSuchMethodException ignored) {
            invoke(data, "setItem", item);
        }
    }

    private static Object invoke(Object target, String name, Object... args) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                continue;
            }
            boolean compatible = true;
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !method.getParameterTypes()[i].isAssignableFrom(args[i].getClass())) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static void invokeOptional(Object target, String name, Object... args) {
        try {
            invoke(target, name, args);
        } catch (Throwable ignored) {
        }
    }

    private static String message(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private boolean supportsItem() {
        try {
            Class<?> type = classes.load("de.oliver.fancyholograms.api.data.ItemHologramData");
            for (Method method : type.getMethods()) {
                if (("setItemStack".equals(method.getName()) || "setItem".equals(method.getName()))
                        && method.getParameterTypes().length == 1) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean supportsDiscovery() {
        try {
            Class<?> type = manager().getClass();
            type.getMethod("getHolograms");
            type.getMethod("removeHologram", String.class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void collectNames(Object values, String namespace, Set<String> target) throws Exception {
        Collection<?> collection;
        if (values instanceof Map) collection = ((Map<?, ?>) values).values();
        else if (values instanceof Collection) collection = (Collection<?>) values;
        else return;
        for (Object hologram : collection) {
            Object name;
            try {
                name = invoke(hologram, "getName");
            } catch (NoSuchMethodException ignored) {
                Object data = invoke(hologram, "getData");
                name = invoke(data, "getName");
            }
            if (name instanceof String && HologramNamespaces.owns(namespace, (String) name)) {
                target.add((String) name);
            }
        }
    }
}

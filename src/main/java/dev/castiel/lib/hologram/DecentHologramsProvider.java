package dev.castiel.lib.hologram;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

/** Optional DecentHolograms adapter kept behind reflection for legacy servers. */
public final class DecentHologramsProvider implements HologramProvider {
    private final JavaPlugin plugin;
    private final ProviderClasses classes;
    private final Map<String, Object> handles = new LinkedHashMap<String, Object>();

    public DecentHologramsProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.classes = new ProviderClasses(plugin, "DecentHolograms");
    }

    public String id() {
        return "decent";
    }

    public ProviderAvailability availability() {
        if (classes.plugin() == null) {
            return ProviderAvailability.unavailable(id(), "DecentHolograms is not installed");
        }
        try {
            classes.load("eu.decentsoftware.holograms.api.DHAPI");
            return ProviderAvailability.available(id());
        } catch (Throwable throwable) {
            return ProviderAvailability.unavailable(id(), "DecentHolograms API is unavailable");
        }
    }

    public HologramCapabilities capabilities() {
        if (!availability().available()) return HologramCapabilities.none();
        return new HologramCapabilities(true, true, true, supportsDiscovery());
    }

    public HologramResult show(String displayId, HologramDisplay display) {
        ProviderAvailability status = availability();
        if (!status.available()) {
            return HologramResult.failure(id(), status.reason());
        }
        if (display == null || display.location() == null) {
            return HologramResult.failure(id(), "display location is missing");
        }
        remove(displayId);
        try {
            Class<?> api = classes.load("eu.decentsoftware.holograms.api.DHAPI");
            Method create = api.getMethod("createHologram", String.class, Location.class);
            Object hologram = create.invoke(null, displayId, display.location());
            invokeOptional(hologram, "setSaveToFile", Boolean.FALSE);
            for (String line : display.lines()) {
                invokeStaticCompatible(api, "addHologramLine", hologram, line);
            }
            if (display.item() != null) {
                String itemId = displayId + ":item";
                remove(itemId);
                Object itemHologram = create.invoke(null, itemId,
                        display.location().clone().add(0, display.itemOffsetY(), 0));
                invokeOptional(itemHologram, "setSaveToFile", Boolean.FALSE);
                invokeStaticCompatible(api, "addHologramLine", itemHologram, display.item());
                handles.put(itemId, itemHologram);
            }
            handles.put(displayId, hologram);
            return HologramResult.success(id(), capabilities(), false, "");
        } catch (Throwable throwable) {
            return HologramResult.failure(id(), message(throwable));
        }
    }

    public HologramResult remove(String displayId) {
        Object hologram = handles.get(displayId);
        try {
            if (hologram == null && availability().available()) {
                Class<?> api = classes.load("eu.decentsoftware.holograms.api.DHAPI");
                hologram = api.getMethod("getHologram", String.class).invoke(null, displayId);
            }
            if (hologram != null) invokeRequired(hologram, "delete");
            handles.remove(displayId);
        } catch (Throwable throwable) {
            return HologramResult.failure(id(), message(throwable));
        }
        if (!displayId.endsWith(":item")) {
            HologramResult itemResult = remove(displayId + ":item");
            if (!itemResult.successful()) return itemResult;
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
            Class<?> hologramType = classes.load("eu.decentsoftware.holograms.api.holograms.Hologram");
            Object values = hologramType.getMethod("getCachedHolograms").invoke(null);
            collectNames(values, namespace, result);
        } catch (Throwable ignored) {
            // Older versions expose lookup/removal but no safe global discovery.
        }
        return result;
    }

    public void shutdown() {
        removeAll(new java.util.HashSet<String>(handles.keySet()));
    }

    private static void invokeStaticCompatible(Class<?> type, String name, Object... args) throws Exception {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                continue;
            }
            Object[] values = new Object[args.length];
            boolean compatible = true;
            for (int i = 0; i < values.length; i++) {
                values[i] = args[i];
                if (values[i] != null && !method.getParameterTypes()[i].isAssignableFrom(values[i].getClass())) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                method.invoke(null, values);
                return;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static void invokeOptional(Object target, String name, Object... args) {
        if (target == null) {
            return;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                continue;
            }
            try {
                method.invoke(target, args);
                return;
            } catch (Throwable ignored) {
                return;
            }
        }
    }

    private static void invokeRequired(Object target, String name, Object... args) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == args.length) {
                method.invoke(target, args);
                return;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static String message(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private boolean supportsDiscovery() {
        try {
            return classes.load("eu.decentsoftware.holograms.api.holograms.Hologram")
                    .getMethod("getCachedHolograms") != null;
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
            Object name = invoke(hologram, "getName");
            if (name instanceof String && HologramNamespaces.owns(namespace, (String) name)) {
                target.add((String) name);
            }
        }
    }

    private static Object invoke(Object target, String name) throws Exception {
        return target.getClass().getMethod(name).invoke(target);
    }
}

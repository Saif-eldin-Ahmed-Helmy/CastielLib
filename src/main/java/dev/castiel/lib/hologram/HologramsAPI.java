package dev.castiel.lib.hologram;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Reusable provider-neutral hologram lifecycle API for CastielLib consumers. */
public final class HologramsAPI {
    private final JavaPlugin plugin;
    private final HologramProvider nativeProvider;
    private final Set<String> ownedDisplayIds = new LinkedHashSet<String>();
    private HologramProvider selected;
    private String configuredProvider;
    private String fallbackReason = "";

    public HologramsAPI(JavaPlugin plugin) {
        this(plugin, new HologramManager(plugin));
    }

    public HologramsAPI(JavaPlugin plugin, HologramManager manager) {
        this.plugin = plugin;
        this.nativeProvider = new NativeHologramProvider(manager);
        configure("auto");
    }

    public synchronized void configure(String provider) {
        if (selected != null) {
            Set<String> owned = new LinkedHashSet<String>(ownedDisplayIds);
            owned.addAll(selected.ownedIds());
            HologramResult cleanup = selected.removeAll(owned);
            if (!cleanup.successful()) {
                plugin.getLogger().severe("CastielLib hologram provider switch rejected: " + cleanup.reason());
                return;
            }
            selected.shutdown();
            ownedDisplayIds.clear();
        }
        configuredProvider = provider == null || provider.trim().isEmpty()
                ? "auto" : provider.trim().toLowerCase(java.util.Locale.ROOT);
        selected = choose(configuredProvider);
        plugin.getLogger().info("CastielLib hologram provider: " + selected.id());
        if (!fallbackReason.isEmpty()) {
            plugin.getLogger().warning("CastielLib hologram provider fallback: " + fallbackReason);
        }
    }

    public synchronized String provider() {
        return selected.id();
    }

    public synchronized ProviderAvailability availability() {
        return selected.availability();
    }

    /** Returns the selected provider's detected visual and reconciliation capabilities. */
    public synchronized HologramCapabilities capabilities() {
        return selected.capabilities();
    }

    /** Describes the requested provider, selected provider, capabilities, and fallback reason. */
    public synchronized HologramSelection selection() {
        return new HologramSelection(configuredProvider, selected.id(), fallbackReason, selected.capabilities());
    }

    public synchronized HologramResult show(String displayId, HologramDisplay display) {
        HologramResult result = selected.show(displayId, display);
        if (result.successful()) {
            ownedDisplayIds.add(displayId);
        }
        return result;
    }

    public synchronized HologramResult remove(String displayId) {
        HologramResult result = selected.remove(displayId);
        if (result.successful()) {
            ownedDisplayIds.remove(displayId);
        }
        return result;
    }

    public synchronized HologramResult reconcile(Set<String> expectedDisplayIds) {
        Set<String> expected = expectedDisplayIds == null
                ? Collections.<String>emptySet()
                : new LinkedHashSet<String>(expectedDisplayIds);
        Set<String> stale = new LinkedHashSet<String>(ownedDisplayIds);
        Set<String> namespaces = new LinkedHashSet<String>();
        for (String id : expected) {
            String namespace = HologramNamespaces.namespaceOf(id);
            if (!namespace.isEmpty()) namespaces.add(namespace);
        }
        for (String namespace : namespaces) stale.addAll(selected.ownedIds(namespace));
        stale.removeAll(expected);
        for (String id : expected) stale.remove(id + ":item");
        HologramResult result = selected.removeAll(stale);
        if (result.successful()) {
            ownedDisplayIds.removeAll(stale);
        }
        return result;
    }

    /** Reconciles one namespace, including provider objects surviving a previous process. */
    public synchronized HologramResult reconcile(String namespace, Set<String> expectedDisplayIds) {
        String normalized = HologramNamespaces.normalize(namespace);
        if (normalized.isEmpty()) return HologramResult.failure(selected.id(), "namespace is missing");
        Set<String> expected = expectedDisplayIds == null
                ? Collections.<String>emptySet() : new LinkedHashSet<String>(expectedDisplayIds);
        for (String id : expected) {
            if (!HologramNamespaces.owns(normalized, id)) {
                return HologramResult.failure(selected.id(), "display ID is outside namespace: " + id);
            }
        }
        Set<String> stale = new LinkedHashSet<String>();
        for (String id : ownedDisplayIds) if (HologramNamespaces.owns(normalized, id)) stale.add(id);
        stale.addAll(selected.ownedIds(normalized));
        stale.removeAll(expected);
        for (String id : expected) stale.remove(id + ":item");
        HologramResult result = selected.removeAll(stale);
        if (result.successful()) ownedDisplayIds.removeAll(stale);
        return result;
    }

    /** Removes all discoverable displays belonging to one namespace. */
    public synchronized HologramResult removeNamespace(String namespace) {
        return reconcile(namespace, Collections.<String>emptySet());
    }

    public synchronized void shutdown() {
        selected.shutdown();
        ownedDisplayIds.clear();
        if (selected != nativeProvider) {
            nativeProvider.shutdown();
        }
    }

    HologramsAPI(HologramProvider provider) {
        this.plugin = null;
        this.nativeProvider = provider;
        this.selected = provider;
        this.configuredProvider = provider.id();
        this.fallbackReason = "";
    }

    private HologramProvider choose(String provider) {
        fallbackReason = "";
        if ("disabled".equals(provider)) {
            return new DisabledHologramProvider();
        }
        if ("native".equals(provider)) {
            return nativeProvider;
        }
        if ("fancy".equals(provider)) {
            HologramProvider fancy = new FancyHologramsProvider(plugin);
            ProviderAvailability availability = fancy.availability();
            if (availability.available()) return fancy;
            fallbackReason = availability.reason();
            return new DisabledHologramProvider();
        }
        if ("decent".equals(provider)) {
            HologramProvider decent = new DecentHologramsProvider(plugin);
            ProviderAvailability availability = decent.availability();
            if (availability.available()) return decent;
            fallbackReason = availability.reason();
            return new DisabledHologramProvider();
        }
        HologramProvider fancy = new FancyHologramsProvider(plugin);
        if (fancy.availability().available()) {
            return fancy;
        }
        HologramProvider decent = new DecentHologramsProvider(plugin);
        if (decent.availability().available()) {
            return decent;
        }
        if (!"auto".equals(provider)) fallbackReason = "unknown provider '" + provider + "'; used auto selection";
        return nativeProvider;
    }
}

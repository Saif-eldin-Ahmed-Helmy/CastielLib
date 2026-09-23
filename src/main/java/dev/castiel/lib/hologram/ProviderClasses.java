package dev.castiel.lib.hologram;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

final class ProviderClasses {
    private final JavaPlugin owner;
    private final String providerPlugin;

    ProviderClasses(JavaPlugin owner, String providerPlugin) {
        this.owner = owner;
        this.providerPlugin = providerPlugin;
    }

    Plugin plugin() {
        return owner.getServer().getPluginManager().getPlugin(providerPlugin);
    }

    Class<?> load(String name) throws ClassNotFoundException {
        Plugin provider = plugin();
        ClassLoader loader = provider == null ? owner.getClass().getClassLoader()
                : provider.getClass().getClassLoader();
        return Class.forName(name, false, loader);
    }
}

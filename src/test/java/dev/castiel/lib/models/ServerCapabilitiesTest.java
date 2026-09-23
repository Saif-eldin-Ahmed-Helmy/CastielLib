package dev.castiel.lib.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerCapabilitiesTest {
    @Test
    void detectsDisplayEntityVersions() {
        assertFalse(ServerCapabilities.fromVersion("1.8.8-R0.1-SNAPSHOT").supportsDisplayEntities());
        assertFalse(ServerCapabilities.fromVersion("1.19.3-R0.1-SNAPSHOT").supportsDisplayEntities());
        assertTrue(ServerCapabilities.fromVersion("1.19.4-R0.1-SNAPSHOT").supportsDisplayEntities());
        assertTrue(ServerCapabilities.fromVersion("1.21.10-R0.1-SNAPSHOT").supportsDisplayEntities());
    }

    @Test
    void oldServersRequireExplicitArmorStandFallback() {
        ServerCapabilities old = ServerCapabilities.fromVersion("1.8.8-R0.1-SNAPSHOT");

        assertFalse(old.canSpawnModels(ModelSpawnOptions.defaults()));
        assertTrue(old.canSpawnModels(ModelSpawnOptions.defaults().allowArmorStandFallback(true)));
    }
}

package dev.castiel.lib.effects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleSpawnerTest {
    @Test
    void normalizesConfiguredAliasesWithoutBukkitParticleLinkage() {
        assertEquals("ENCHANTMENT_TABLE", ParticleSpawner.normalize("enchanted"));
        assertEquals("REDSTONE", ParticleSpawner.normalize("dust"));
        assertEquals("FIREWORKS_SPARK", ParticleSpawner.normalize("firework"));
    }
}

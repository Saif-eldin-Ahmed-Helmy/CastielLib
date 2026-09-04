package dev.castiel.lib.items;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemIdentitySignerTest {
    @Test
    void rejectsUnsignedAndTamperedMarkersAndPersistsKey() throws Exception {
        Path directory = Files.createTempDirectory("castiellib-item-identity");
        String signature = ItemIdentitySigner.sign(directory, "reward-id", "gems");
        assertTrue(ItemIdentitySigner.verifies(directory, "reward-id", "gems", signature));
        assertFalse(ItemIdentitySigner.verifies(directory, "reward-id", "mobcoins", signature));
        assertFalse(ItemIdentitySigner.verifies(directory, "reward-id", "gems", "unsigned"));

        ItemIdentitySigner.clearCachedKey(directory);
        assertTrue(ItemIdentitySigner.verifies(directory, "reward-id", "gems", signature));
    }
}

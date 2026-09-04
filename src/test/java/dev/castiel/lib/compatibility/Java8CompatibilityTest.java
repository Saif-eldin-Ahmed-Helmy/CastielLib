package dev.castiel.lib.compatibility;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;

class Java8CompatibilityTest {
    @Test
    void productionSourcesDoNotDirectlyImportPostOneEightTypes() throws Exception {
        File source = new File("src/main/java");
        List<File> files = Files.walk(source.toPath())
                .filter(path -> path.toString().endsWith(".java"))
                .map(java.nio.file.Path::toFile)
                .collect(Collectors.toList());
        for (File file : files) {
            String contents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            assertFalse(contents.contains("import org.bukkit.NamespacedKey"), file.getPath());
            assertFalse(contents.contains("import org.bukkit.persistence."), file.getPath());
            assertFalse(contents.contains("import org.bukkit.block.data."), file.getPath());
            assertFalse(contents.contains("import org.bukkit.entity.Display"), file.getPath());
            assertFalse(contents.contains("import org.bukkit.Particle"), file.getPath());
            assertFalse(contents.contains(".getStorageContents("), file.getPath());
            assertFalse(contents.contains(".setStorageContents("), file.getPath());
            assertFalse(contents.contains(".spawnParticle("), file.getPath());
        }
    }
}

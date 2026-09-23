package dev.castiel.lib.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSectionInstallerTest {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String DEFAULTS = String.join("\n",
            "# top of file",
            "Server-Name: \"Survival\"",
            "",
            "# ── Sync ──",
            "# Explains the section.",
            "Sync:",
            "  Mode: none",
            "",
            "  Redis:",
            "    Port: 6379",
            "",
            "Other: true",
            "Config-Version: 1.7",
            "");

    /** An existing install: same file, but the Sync section was never there. */
    private static final String EXISTING = String.join("\n",
            "# top of file",
            "Server-Name: \"MyServer\"   # admin renamed this",
            "",
            "Other: true",
            "Config-Version: 1.7",
            "");

    private File write(Path dir, String name, String content) throws Exception {
        File file = dir.resolve(name).toFile();
        Files.write(file.toPath(), content.getBytes(UTF8));
        return file;
    }

    private ByteArrayInputStream defaults() {
        return new ByteArrayInputStream(DEFAULTS.getBytes(UTF8));
    }

    @Test
    void aMissingSectionIsInstalledBelowItsAnchor(@TempDir Path dir) throws Exception {
        File config = write(dir, "config.yml", EXISTING);

        ConfigSectionInstaller.Result result =
                ConfigSectionInstaller.install(config, defaults(), "Sync", "Server-Name");

        assertEquals(ConfigSectionInstaller.Result.INSTALLED, result);
        List<String> lines = Files.readAllLines(config.toPath(), UTF8);
        int serverName = indexOfPrefix(lines, "Server-Name:");
        int header = indexOfContaining(lines, "── Sync ──");
        int sync = indexOfPrefix(lines, "Sync:");
        int other = indexOfPrefix(lines, "Other:");

        assertTrue(header > serverName, "the section goes below its anchor");
        assertTrue(sync < other, "and above what followed the anchor, not at the end");
        assertTrue(header < sync, "its comments come with it");
    }

    @Test
    void everythingElseIsLeftExactlyAsItWas(@TempDir Path dir) throws Exception {
        File config = write(dir, "config.yml", EXISTING);

        ConfigSectionInstaller.install(config, defaults(), "Sync", "Server-Name");

        String after = new String(Files.readAllBytes(config.toPath()), UTF8);
        assertTrue(after.contains("Server-Name: \"MyServer\"   # admin renamed this"),
                "quoting and inline comment spacing must survive untouched");
        assertTrue(after.contains("Other: true"));
        assertTrue(after.contains("Config-Version: 1.7"));
    }

    @Test
    void theSectionBringsItsWholeBodyIncludingNestedKeys(@TempDir Path dir) throws Exception {
        File config = write(dir, "config.yml", EXISTING);

        ConfigSectionInstaller.install(config, defaults(), "Sync", "Server-Name");

        String after = new String(Files.readAllBytes(config.toPath()), UTF8);
        assertTrue(after.contains("  Mode: none"));
        assertTrue(after.contains("  Redis:"));
        assertTrue(after.contains("    Port: 6379"));
    }

    @Test
    void anAlreadyPresentSectionIsLeftAlone(@TempDir Path dir) throws Exception {
        File config = write(dir, "config.yml", DEFAULTS);
        byte[] before = Files.readAllBytes(config.toPath());

        ConfigSectionInstaller.Result result =
                ConfigSectionInstaller.install(config, defaults(), "Sync", "Server-Name");

        assertEquals(ConfigSectionInstaller.Result.ALREADY_PRESENT, result);
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(config.toPath())),
                "a second run must not touch the file at all");
    }

    @Test
    void runningItTwiceInstallsOnlyOnce(@TempDir Path dir) throws Exception {
        File config = write(dir, "config.yml", EXISTING);

        ConfigSectionInstaller.install(config, defaults(), "Sync", "Server-Name");
        ConfigSectionInstaller.install(config, defaults(), "Sync", "Server-Name");

        List<String> lines = Files.readAllLines(config.toPath(), UTF8);
        int occurrences = 0;
        for (String line : lines) {
            if (line.startsWith("Sync:")) {
                occurrences++;
            }
        }
        assertEquals(1, occurrences);
    }

    @Test
    void anUnknownAnchorAppendsRatherThanLosingTheSection(@TempDir Path dir) throws Exception {
        File config = write(dir, "config.yml", EXISTING);

        ConfigSectionInstaller.Result result =
                ConfigSectionInstaller.install(config, defaults(), "Sync", "No-Such-Key");

        assertEquals(ConfigSectionInstaller.Result.INSTALLED, result);
        assertTrue(new String(Files.readAllBytes(config.toPath()), UTF8).contains("Sync:"));
    }

    @Test
    void aSectionMissingFromTheDefaultsIsReported(@TempDir Path dir) throws Exception {
        File config = write(dir, "config.yml", EXISTING);

        assertEquals(ConfigSectionInstaller.Result.NOT_IN_DEFAULTS,
                ConfigSectionInstaller.install(config, defaults(), "Nope", "Server-Name"));
    }

    @Test
    void aMissingConfigFileIsReportedRatherThanCreated(@TempDir Path dir) {
        File missing = dir.resolve("nothing-here.yml").toFile();

        assertEquals(ConfigSectionInstaller.Result.NO_CONFIG_FILE,
                ConfigSectionInstaller.install(missing, defaults(), "Sync", "Server-Name"));
    }

    private int indexOfPrefix(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfContaining(List<String> lines, String needle) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(needle)) {
                return i;
            }
        }
        return -1;
    }
}

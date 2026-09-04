package dev.castiel.lib.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds a section that a plugin has started shipping into a config file that
 * predates it, in the right place and with its comments.
 *
 * <p>Bukkit's {@code config.set(...)} plus {@code save(...)} appends a bare,
 * comment-free block to the very end of the file and rewrites everything else
 * on the way, which loses quoting and comment spacing. This copies the section
 * out of the plugin's own bundled defaults instead and splices it in as text, so
 * the rest of the file is untouched byte for byte.
 */
public final class ConfigSectionInstaller {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public enum Result {
        /** The section was already there; nothing was written. */
        ALREADY_PRESENT,
        /** The section was copied in. */
        INSTALLED,
        /** There is no config file yet, so the bundled defaults will be used as-is. */
        NO_CONFIG_FILE,
        /** The bundled defaults do not contain that section. */
        NOT_IN_DEFAULTS,
        /** The file could not be read or written. */
        FAILED
    }

    private ConfigSectionInstaller() {
    }

    /**
     * Installs a top-level section into one of the plugin's config files.
     *
     * @param sectionKey the top-level key, without the colon, such as "Sync"
     * @param afterKey   the top-level key to place it below, such as
     *                   "Server-Name"; appended at the end when absent
     */
    public static Result install(JavaPlugin plugin, String fileName, String sectionKey, String afterKey) {
        if (plugin == null || fileName == null || sectionKey == null) {
            return Result.FAILED;
        }
        File configFile = new File(plugin.getDataFolder(), fileName);
        InputStream defaults = plugin.getResource(fileName);
        try {
            return install(configFile, defaults, sectionKey, afterKey);
        } finally {
            closeQuietly(defaults);
        }
    }

    /** The same thing without a plugin, so it can be tested against real files. */
    public static Result install(File configFile, InputStream bundledDefaults,
                                 String sectionKey, String afterKey) {
        if (configFile == null || !configFile.isFile()) {
            return Result.NO_CONFIG_FILE;
        }
        if (bundledDefaults == null) {
            return Result.NOT_IN_DEFAULTS;
        }
        try {
            List<String> current = Files.readAllLines(configFile.toPath(), UTF8);
            if (indexOfKey(current, sectionKey) >= 0) {
                return Result.ALREADY_PRESENT;
            }
            List<String> section = extractSection(readLines(bundledDefaults), sectionKey);
            if (section.isEmpty()) {
                return Result.NOT_IN_DEFAULTS;
            }
            int insertAt = insertionPoint(current, afterKey);
            if (insertAt < 0) {
                current.addAll(section);
            } else {
                current.addAll(insertAt, section);
            }
            Files.write(configFile.toPath(), current, UTF8);
            return Result.INSTALLED;
        } catch (IOException failure) {
            return Result.FAILED;
        }
    }

    /**
     * The section plus the comment block written directly above it.
     *
     * <p>The section runs until the next line that starts in column zero and is
     * not blank, which is where the next top-level key begins.
     */
    private static List<String> extractSection(List<String> defaults, String sectionKey) {
        List<String> section = new ArrayList<String>();
        int start = indexOfKey(defaults, sectionKey);
        if (start < 0) {
            return section;
        }
        int commentStart = start;
        while (commentStart > 0 && defaults.get(commentStart - 1).startsWith("#")) {
            commentStart--;
        }
        int end = start + 1;
        while (end < defaults.size()) {
            String line = defaults.get(end);
            if (line.trim().isEmpty() || line.startsWith(" ") || line.startsWith("\t")) {
                end++;
            } else {
                break;
            }
        }
        section.addAll(defaults.subList(commentStart, end));
        while (!section.isEmpty() && section.get(section.size() - 1).trim().isEmpty()) {
            section.remove(section.size() - 1);
        }
        section.add("");
        return section;
    }

    /** Just past the anchor key and any blank lines after it, or -1. */
    private static int insertionPoint(List<String> lines, String afterKey) {
        if (afterKey == null) {
            return -1;
        }
        int anchor = indexOfKey(lines, afterKey);
        if (anchor < 0) {
            return -1;
        }
        int insertAt = anchor + 1;
        while (insertAt < lines.size() && lines.get(insertAt).trim().isEmpty()) {
            insertAt++;
        }
        return insertAt;
    }

    private static int indexOfKey(List<String> lines, String key) {
        String prefix = key + ":";
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> readLines(InputStream stream) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF8));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // Nothing useful to do with a stream that will not close.
        }
    }
}

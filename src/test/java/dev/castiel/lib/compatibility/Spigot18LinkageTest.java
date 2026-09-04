package dev.castiel.lib.compatibility;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Spigot18LinkageTest {
    @Test
    void everyCastielLibClassLoadsAgainstSpigot18Api() throws Exception {
        String classesProperty = System.getProperty("castiellib.main.classes", "");
        String linkageProperty = System.getProperty("castiellib.spigot18.classpath", "");
        List<URL> urls = new ArrayList<URL>();
        for (String entry : (classesProperty + File.pathSeparator + linkageProperty).split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!entry.trim().isEmpty()) urls.add(new File(entry).toURI().toURL());
        }
        List<String> failures = new ArrayList<String>();
        URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
        try {
            for (String root : classesProperty.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (root.trim().isEmpty()) continue;
                Path base = new File(root).toPath();
                Files.walk(base).filter(path -> path.toString().endsWith(".class")).forEach(path -> load(base, path, loader, failures));
            }
        } finally {
            loader.close();
        }
        assertTrue(failures.isEmpty(), failures.toString());
    }

    private static void load(Path base, Path classFile, ClassLoader loader, List<String> failures) {
        String name = base.relativize(classFile).toString().replace(File.separatorChar, '.');
        name = name.substring(0, name.length() - ".class".length());
        try {
            Class.forName(name, false, loader);
        } catch (Throwable failure) {
            failures.add(name + " -> " + failure);
        }
    }
}

package dev.castiel.lib.items;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class ItemIdentitySigner {
    private static final Map<Path, byte[]> KEYS = new HashMap<Path, byte[]>();

    private ItemIdentitySigner() {
    }

    static String sign(Path directory, String name, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key(directory), "HmacSHA256"));
            return Base64.getEncoder().withoutPadding().encodeToString(
                    mac.doFinal((name + "\u0000" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException("Unable to sign CastielLib item identity", failure);
        }
    }

    static boolean verifies(Path directory, String name, String value, String signature) {
        try {
            return MessageDigest.isEqual(Base64.getDecoder().decode(signature),
                    Base64.getDecoder().decode(sign(directory, name, value)));
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    static synchronized void clearCachedKey(Path directory) {
        KEYS.remove(directory.toAbsolutePath().normalize());
    }

    private static synchronized byte[] key(Path rawDirectory) throws java.io.IOException {
        Path directory = rawDirectory.toAbsolutePath().normalize();
        byte[] cached = KEYS.get(directory);
        if (cached != null) return cached;
        Path file = directory.resolve(".castiel-item-key");
        Files.createDirectories(directory);
        if (Files.isRegularFile(file)) {
            try {
                cached = Base64.getDecoder().decode(new String(Files.readAllBytes(file), StandardCharsets.US_ASCII).trim());
            } catch (IllegalArgumentException ignored) {
                cached = null;
            }
        }
        if (cached == null || cached.length < 32) {
            cached = new byte[32];
            new SecureRandom().nextBytes(cached);
            Path temporary = directory.resolve(".castiel-item-key.tmp");
            Files.write(temporary, Base64.getEncoder().encode(cached));
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        KEYS.put(directory, cached);
        return cached;
    }
}

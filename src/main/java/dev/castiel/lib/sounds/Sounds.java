package dev.castiel.lib.sounds;

import com.cryptomorin.xseries.XSound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

public final class Sounds {
    private Sounds() {
    }

    public static Sound match(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim();
        Optional<XSound> matched = XSound.matchXSound(normalized);
        if (matched.isPresent()) {
            Sound sound = matched.get().parseSound();
            if (sound != null) {
                return sound;
            }
        }
        try {
            return Sound.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean play(Player player, String raw, float volume, float pitch) {
        if (player == null) {
            return false;
        }
        Sound sound = match(raw);
        if (sound == null) {
            return false;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
        return true;
    }
}

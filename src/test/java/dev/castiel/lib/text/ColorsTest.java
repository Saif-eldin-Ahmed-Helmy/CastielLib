package dev.castiel.lib.text;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorsTest {
    @Test
    void supportsLegacyColors() {
        assertEquals(ChatColor.RED + "Legacy", Colors.color("&cLegacy"));
    }

    @Test
    void supportsHexColors() {
        String result = Colors.color("&#00F5FFHex");

        assertEquals("Hex", ChatColor.stripColor(result));
        assertTrue(result.startsWith("\u00a7x\u00a70\u00a70\u00a7f\u00a75\u00a7f\u00a7f"));
    }

    @Test
    void supportsSolidColors() {
        String result = Colors.color("<SOLID:00F5FF>Solid</SOLID>");

        assertEquals("Solid", ChatColor.stripColor(result));
        assertFalse(result.contains("<SOLID"));
    }

    @Test
    void supportsGradientColorsWithoutRewritingTheClosingTag() {
        String result = Colors.color("<GRADIENT:00F5FF>CustomShop</GRADIENT:80FF72>");

        assertEquals("CustomShop", ChatColor.stripColor(result));
        assertFalse(result.contains("GRADIENT"));
        assertTrue(result.startsWith("\u00a7x\u00a70\u00a70\u00a7f\u00a75\u00a7f\u00a7f"));
        assertTrue(result.endsWith("\u00a7x\u00a78\u00a70\u00a7f\u00a7f\u00a77\u00a72p"));
    }

    @Test
    void supportsRainbowColors() {
        String result = Colors.color("<RAINBOW>Rainbow</RAINBOW>");

        assertEquals("Rainbow", ChatColor.stripColor(result));
        assertFalse(result.contains("RAINBOW"));
    }
}

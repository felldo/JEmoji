package net.fellbaum.jemoji;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FitzpatrickTest {

    private static final String fitzPatrickEmoji = "👋🏻";
    private static final String nonFitzPatrickEmoji = "👋";

    @Test
    public void isFitzpatrickEmoji() {
        assertTrue(Fitzpatrick.isFitzpatrickEmoji(fitzPatrickEmoji));
        assertFalse(Fitzpatrick.isFitzpatrickEmoji(nonFitzPatrickEmoji));
    }

    @Test
    public void removeFitzpatrick() {
        assertEquals("👋", Fitzpatrick.removeFitzpatrick(fitzPatrickEmoji));
        assertEquals("👋", Fitzpatrick.removeFitzpatrick(nonFitzPatrickEmoji));
    }

    // ===== All 5 skin tone constants =====

    @Test
    public void isFitzpatrickEmojiAllSkinTones() {
        assertTrue(Fitzpatrick.isFitzpatrickEmoji("👋🏼")); // MEDIUM_LIGHT_SKIN
        assertTrue(Fitzpatrick.isFitzpatrickEmoji("👋🏽")); // MEDIUM_SKIN
        assertTrue(Fitzpatrick.isFitzpatrickEmoji("👋🏾")); // MEDIUM_DARK_SKIN
        assertTrue(Fitzpatrick.isFitzpatrickEmoji("👋🏿")); // DARK_SKIN
    }

    @Test
    public void removeFitzpatrickAllSkinTones() {
        assertEquals("👋", Fitzpatrick.removeFitzpatrick("👋🏼")); // MEDIUM_LIGHT_SKIN
        assertEquals("👋", Fitzpatrick.removeFitzpatrick("👋🏽")); // MEDIUM_SKIN
        assertEquals("👋", Fitzpatrick.removeFitzpatrick("👋🏾")); // MEDIUM_DARK_SKIN
        assertEquals("👋", Fitzpatrick.removeFitzpatrick("👋🏿")); // DARK_SKIN
    }

    @Test
    public void fitzpatrickGetUnicodeNonNullNonEmpty() {
        for (Fitzpatrick f : Fitzpatrick.values()) {
            assertNotNull(f.getUnicode(), f.name() + " unicode should not be null");
            assertFalse(f.getUnicode().isEmpty(), f.name() + " unicode should not be empty");
        }
    }

    // ===== Standalone modifier edge cases =====

    @Test
    public void isFitzpatrickEmojiForStandaloneModifier() {
        // A standalone skin tone modifier is not a Fitzpatrick compound emoji
        assertFalse(Fitzpatrick.isFitzpatrickEmoji(Fitzpatrick.LIGHT_SKIN.getUnicode()));
        assertFalse(Fitzpatrick.isFitzpatrickEmoji(Fitzpatrick.MEDIUM_SKIN.getUnicode()));
        assertFalse(Fitzpatrick.isFitzpatrickEmoji(Fitzpatrick.DARK_SKIN.getUnicode()));
    }

    @Test
    public void removeFitzpatrickFromPlainText() {
        assertEquals("Hello World", Fitzpatrick.removeFitzpatrick("Hello World"));
    }
}
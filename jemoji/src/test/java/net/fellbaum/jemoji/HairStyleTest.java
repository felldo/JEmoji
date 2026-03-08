package net.fellbaum.jemoji;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HairStyleTest {

    private static final String RED_HAIR = "👨‍🦰";
    private static final String CURLY_HAIR = "👨‍🦱";
    private static final String WHITE_HAIR = "👨‍🦳";
    private static final String BALD = "👨‍🦲";
    private static final String noHair = "👨";

    @Test
    public void isHairStyleEmoji() {
        assertTrue(HairStyle.isHairStyleEmoji(RED_HAIR));
        assertFalse(HairStyle.isHairStyleEmoji(noHair));
    }

    @Test
    public void removeHairStyle() {
        assertEquals(noHair, HairStyle.removeHairStyle(RED_HAIR));
        assertEquals(noHair, HairStyle.removeHairStyle(noHair));
    }

    // ===== All 4 hair style constants =====

    @Test
    public void isHairStyleEmojiAllStyles() {
        assertTrue(HairStyle.isHairStyleEmoji(CURLY_HAIR));
        assertTrue(HairStyle.isHairStyleEmoji(WHITE_HAIR));
        assertTrue(HairStyle.isHairStyleEmoji(BALD));
    }

    @Test
    public void removeHairStyleAllStyles() {
        assertEquals(noHair, HairStyle.removeHairStyle(CURLY_HAIR));
        assertEquals(noHair, HairStyle.removeHairStyle(WHITE_HAIR));
        assertEquals(noHair, HairStyle.removeHairStyle(BALD));
    }

    @Test
    public void hairStyleGetUnicodeNonNullNonEmpty() {
        for (HairStyle hairStyle : HairStyle.values()) {
            assertNotNull(hairStyle.getUnicode(), hairStyle.name() + " unicode should not be null");
            assertFalse(hairStyle.getUnicode().isEmpty(), hairStyle.name() + " unicode should not be empty");
        }
    }

    // ===== Standalone modifier edge cases =====

    @Test
    public void isHairStyleEmojiForStandaloneModifier() {
        assertFalse(HairStyle.isHairStyleEmoji(HairStyle.BALD.getUnicode()));
        assertFalse(HairStyle.isHairStyleEmoji(HairStyle.RED_HAIR.getUnicode()));
    }

    @Test
    public void removeHairStyleFromPlainText() {
        assertEquals("Hello World", HairStyle.removeHairStyle("Hello World"));
    }
}
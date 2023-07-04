package net.fellbaum.jemoji;

import org.junit.Test;

import static org.junit.Assert.*;

public class HairStyleTest {

    private static final String RED_HAIR = "\uD83D\uDC68\u200D\uD83E\uDDB0";
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
}
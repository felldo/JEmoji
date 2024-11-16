package net.fellbaum.jemoji;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmojiTest {

    @Test
    public void testUnicode() {
        assertEquals("\\uD83D\\uDC4D", Emojis.THUMBS_UP.getUnicode());
    }

    @Test
    public void testHtmlDec() {
        assertEquals("&#128104;&#8205;&#128105;&#8205;&#128103;&#8205;&#128102;", Emojis.FAMILY_MAN_WOMAN_GIRL_BOY.getHtmlDecimalCode());
    }

    @Test
    public void testHtmlHex() {
        assertEquals("&#x1F468;&#x200D;&#x1F469;&#x200D;&#x1F467;&#x200D;&#x1F466;", Emojis.FAMILY_MAN_WOMAN_GIRL_BOY.getHtmlHexadecimalCode());
    }

}

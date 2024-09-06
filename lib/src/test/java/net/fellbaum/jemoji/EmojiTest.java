package net.fellbaum.jemoji;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmojiTest {

    @Test
    public void testUnicode() {
        assertEquals("\\uD83D\\uDC4D", Emojis.THUMBS_UP.getUnicode());
    }

}

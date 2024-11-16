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
}
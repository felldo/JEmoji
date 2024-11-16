package jemoji;

import net.fellbaum.jemoji.EmojiLanguage;
import net.fellbaum.jemoji.Emojis;
import org.junit.Test;

import static org.junit.Assert.*;

public class EmojiManagerTest {

    @Test
    public void testIfEmojiAlternativeLanguageIsLoaded() {
        assertEquals("Daumen hoch", Emojis.THUMBS_UP.getDescription(EmojiLanguage.DE).orElseThrow(RuntimeException::new));
    }

}
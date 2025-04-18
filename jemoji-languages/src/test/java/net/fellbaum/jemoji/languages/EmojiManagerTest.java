package net.fellbaum.jemoji.languages;

import net.fellbaum.jemoji.EmojiLanguage;
import net.fellbaum.jemoji.Emojis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmojiManagerTest {

    @Test
    public void testIfEmojiAlternativeLanguageIsLoaded() {
        assertEquals("Daumen hoch", Emojis.THUMBS_UP.getDescription(EmojiLanguage.DE).orElseThrow(RuntimeException::new));
    }

}
package net.fellbaum.jemoji.languages;

import net.fellbaum.jemoji.EmojiLanguage;
import net.fellbaum.jemoji.Emojis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmojiManagerTest {

    @Test
    public void testIfEmojiAlternativeLanguageIsLoaded() {
        assertEquals("Daumen hoch", Emojis.THUMBS_UP.getDescription(EmojiLanguage.DE).orElseThrow(RuntimeException::new));
        assertEquals("pouce vers le haut", Emojis.THUMBS_UP.getDescription(EmojiLanguage.FR).orElseThrow(RuntimeException::new));
        assertEquals("pollice in su", Emojis.THUMBS_UP.getDescription(EmojiLanguage.IT).orElseThrow(RuntimeException::new));
        assertEquals("pulgar hacia arriba", Emojis.THUMBS_UP.getDescription(EmojiLanguage.ES).orElseThrow(RuntimeException::new));
    }

}
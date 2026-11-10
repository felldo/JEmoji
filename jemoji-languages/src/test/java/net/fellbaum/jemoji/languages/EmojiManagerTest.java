package net.fellbaum.jemoji.languages;

import net.fellbaum.jemoji.EmojiLanguage;
import net.fellbaum.jemoji.EmojiLoader;
import net.fellbaum.jemoji.Emojis;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmojiManagerTest {

    @Test
    public void testIfEmojiAlternativeLanguageIsLoaded() {
        assertEquals("Daumen hoch", Emojis.THUMBS_UP.getDescription(EmojiLanguage.DE).orElseThrow(RuntimeException::new));
        assertEquals("pouce vers le haut", Emojis.THUMBS_UP.getDescription(EmojiLanguage.FR).orElseThrow(RuntimeException::new));
        assertEquals("pollice in su", Emojis.THUMBS_UP.getDescription(EmojiLanguage.IT).orElseThrow(RuntimeException::new));
        assertEquals("pulgar hacia arriba", Emojis.THUMBS_UP.getDescription(EmojiLanguage.ES).orElseThrow(RuntimeException::new));
    }

    @Test
    public void testIfEmojiAlternativeLanguageKeywordsAreLoaded() {
        final List<String> keywords = Emojis.THUMBS_UP.getKeywords(EmojiLanguage.DE).orElseThrow(RuntimeException::new);
        assertTrue(keywords.contains("Daumen hoch"), keywords::toString);
        assertTrue(keywords.contains("super"), keywords::toString);
    }

    @Test
    public void testIfAllLanguageFilesCanBeLoaded() {
        assertDoesNotThrow(EmojiLoader::loadAllEmojiDescriptions);
        assertDoesNotThrow(EmojiLoader::loadAllEmojiKeywords);
    }

}
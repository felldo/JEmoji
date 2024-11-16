package net.fellbaum.jemoji;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EmojiManagerTest {

    public static final String ALL_EMOJIS_STRING = EmojiManager.getAllEmojisLengthDescending().stream().map(Emoji::getEmoji).collect(Collectors.joining());
    private static final String SIMPLE_EMOJI_STRING = "Hello ❤️ ❤ ❤❤️ World";
    private static final String SIMPLE_POSITION_EMOJI_STRING = "Hello ❤️ ❤ 👩🏻‍🤝‍👨🏼 ❤❤️ World";
    private static final String EMOJI_VARIATION_STRING = "♎️";

    @Test
    public void testIfAllEmojisAreUnique() {
        final List<String> unicodeEmojis = EmojiManager.getAllEmojis().stream().map(Emoji::getEmoji).collect(Collectors.toList());
        assertTrue(EmojiManager.getAllEmojis().stream().allMatch(emoji -> unicodeEmojis.contains(emoji.getEmoji())));
    }

    @Test
    public void testEmojiLanguageIsLoaded() {
        assertEquals("thumbs up", Emojis.THUMBS_UP.getDescription(EmojiLanguage.EN).orElseThrow(RuntimeException::new));
    }

    @Test
    public void extractEmojisInOrder() {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrder(ALL_EMOJIS_STRING + ALL_EMOJIS_STRING);

        assertEquals(EmojiManager.getAllEmojisLengthDescending().size() * 2, emojis.size());

        List<Emoji> allEmojis = new ArrayList<>(EmojiManager.getAllEmojisLengthDescending());
        allEmojis.addAll(EmojiManager.getAllEmojisLengthDescending());
        assertEquals(allEmojis, emojis);
    }

    @Test
    public void extractEmojisInOrderWithIndex() {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(ALL_EMOJIS_STRING + ALL_EMOJIS_STRING)
                .stream()
                .map(IndexedEmoji::getEmoji)
                .collect(Collectors.toList());

        assertEquals(EmojiManager.getAllEmojisLengthDescending().size() * 2, emojis.size());
        List<Emoji> allEmojis = new ArrayList<>(EmojiManager.getAllEmojisLengthDescending());
        allEmojis.addAll(EmojiManager.getAllEmojisLengthDescending());

        assertEquals(allEmojis, emojis);
    }

    @Test
    public void extractEmojisInOrderWithIndexCheckPosition() {
        List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(SIMPLE_POSITION_EMOJI_STRING);
        assertEquals(5, emojis.size());

        checkIndexedEmoji(emojis.get(0), 6, 6);
        checkIndexedEmoji(emojis.get(1), 9, 9);
        checkIndexedEmoji(emojis.get(2), 11, 11);
        checkIndexedEmoji(emojis.get(3), 24, 19);
        checkIndexedEmoji(emojis.get(4), 25, 20);
    }

    private void checkIndexedEmoji(IndexedEmoji indexedEmoji, int expectedCharIndex, int expectedCodePointIndex) {
        assertEquals(expectedCharIndex, indexedEmoji.getCharIndex());
        assertEquals(expectedCodePointIndex, indexedEmoji.getCodePointIndex());
    }

    @Test
    public void extractEmojis() {
        Set<Emoji> emojis = EmojiManager.extractEmojis(ALL_EMOJIS_STRING + ALL_EMOJIS_STRING);

        assertEquals(EmojiManager.getAllEmojisLengthDescending().size(), emojis.size());
        Set<Emoji> allEmojis = EmojiManager.getAllEmojis();
        assertEquals(allEmojis, emojis);
    }

    @Test
    public void getEmoji() {
        String emojiString = "👍";

        Optional<Emoji> emoji = EmojiManager.getEmoji(emojiString);
        assertTrue(emoji.isPresent());
        assertEquals(emojiString, emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void getEmojiWithVariation() {
        Optional<Emoji> emoji = EmojiManager.getEmoji(EMOJI_VARIATION_STRING);
        assertTrue(emoji.isPresent());
        assertEquals("♎", emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void isEmoji() {
        String emojiString = "\uD83D\uDC4D";

        assertTrue(EmojiManager.isEmoji(emojiString));
    }

    @Test
    public void getByAlias() {
        String alias = "smile";

        Optional<Emoji> emoji = EmojiManager.getByAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals("😄", emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void getByAliasWithColon() {
        String alias = ":smile:";

        Optional<Emoji> emoji = EmojiManager.getByAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals("😄", emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void containsEmoji() {
        assertTrue(EmojiManager.containsEmoji(SIMPLE_EMOJI_STRING));
    }

    @Test
    public void removeEmojis() {
        assertEquals("Hello    World", EmojiManager.removeAllEmojis(SIMPLE_EMOJI_STRING));
    }

    @Test
    public void removeAllEmojisExcept() {
        assertEquals("Hello ❤️  ❤️ World", EmojiManager.removeAllEmojisExcept(SIMPLE_EMOJI_STRING + "👍", Emojis.RED_HEART));
    }

    @Test
    public void replaceEmojis() {
        assertEquals("Hello :heart: ❤ ❤:heart: World", EmojiManager.replaceEmojis(SIMPLE_EMOJI_STRING, ":heart:", Emojis.RED_HEART));
    }

    @Test
    public void replaceOnlyUnqualifiedEmoji() {
        assertEquals("Hello ❤️ :heart: :heart:❤️ World", EmojiManager.replaceEmojis(SIMPLE_EMOJI_STRING, ":heart:", Emojis.RED_HEART_UNQUALIFIED));
    }

    @Test
    public void replaceAllEmojis() {
        assertEquals("Hello something something somethingsomething World something something something", EmojiManager.replaceAllEmojis(SIMPLE_EMOJI_STRING + " 👍 👨🏿‍🦱 😊", "something"));
    }

    @Test
    public void replaceAllEmojisFunction() {
        assertEquals("Hello SMILEYS_AND_EMOTION SMILEYS_AND_EMOTION SMILEYS_AND_EMOTIONSMILEYS_AND_EMOTION World PEOPLE_AND_BODY PEOPLE_AND_BODY SMILEYS_AND_EMOTION", EmojiManager.replaceAllEmojis(SIMPLE_EMOJI_STRING + " 👍 👨🏿‍🦱 😊", emoji -> emoji.getGroup().toString()));
    }

    @Test
    public void testEmojiPattern() {
        for (Emoji emoji : EmojiManager.getAllEmojis()) {
            assertTrue(EmojiManager.getEmojiPattern().matcher(emoji.getEmoji()).matches());
        }
        assertFalse(EmojiManager.getEmojiPattern().matcher("a").matches());
        assertFalse(EmojiManager.getEmojiPattern().matcher("ä").matches());
        assertFalse(EmojiManager.getEmojiPattern().matcher("1").matches());
        assertFalse(EmojiManager.getEmojiPattern().matcher("/").matches());
    }

}
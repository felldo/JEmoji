package net.fellbaum.jemoji;

import org.junit.jupiter.api.Test;

import java.util.*;
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
    public void testEmojiLanguageIsNotLoaded() {
        assertThrowsExactly(IllegalStateException.class, () -> Emojis.THUMBS_UP.getDescription(EmojiLanguage.EN));
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
        List<IndexedEmoji> indexedEmojis = EmojiManager.extractEmojisInOrderWithIndex(ALL_EMOJIS_STRING + ALL_EMOJIS_STRING);

        assertEquals(EmojiManager.getAllEmojisLengthDescending().size() * 2, indexedEmojis.size());

        assertEquals(52596, indexedEmojis.get(indexedEmojis.size() - 2).getCharIndex());
        assertEquals(33554, indexedEmojis.get(indexedEmojis.size() - 2).getCodePointIndex());

        assertEquals(52597, indexedEmojis.get(indexedEmojis.size() - 1).getCharIndex());
        assertEquals(33555, indexedEmojis.get(indexedEmojis.size() - 1).getCodePointIndex());

        List<Emoji> allEmojis = new ArrayList<>(EmojiManager.getAllEmojisLengthDescending());
        allEmojis.addAll(EmojiManager.getAllEmojisLengthDescending());

        assertEquals(allEmojis, indexedEmojis.stream().map(IndexedEmoji::getEmoji).collect(Collectors.toList()));
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

    @Test
    public void extractMixedEmojisInOrderWithIndexCheckPosition() {
        List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex("&#128077;&#x1F44D; &#x1F468;&#x1F3FF;&#x200D;&#x2764;&#x200D;&#x1F468;&#x1F3FD;&#x1F448;&#x1F3FE;" + SIMPLE_POSITION_EMOJI_STRING + "&#128104;&#127999;&#8205;&#10084;&#8205;&#128104;&#127997;&#128072;&#127998;", EnumSet.of(EmojiType.UNICODE, EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL));
        assertEquals(11, emojis.size());

        checkIndexedEmoji(emojis.get(0), 0, 0);
        checkIndexedEmoji(emojis.get(1), 9, 9);
        checkIndexedEmoji(emojis.get(2), 19, 19);
        checkIndexedEmoji(emojis.get(3), 79, 79);

        checkIndexedEmoji(emojis.get(4), 103, 103);
        checkIndexedEmoji(emojis.get(5), 106, 106);
        checkIndexedEmoji(emojis.get(6), 108, 108);
        checkIndexedEmoji(emojis.get(7), 121, 116);
        checkIndexedEmoji(emojis.get(8), 122, 117);

        checkIndexedEmoji(emojis.get(9), 130, 125);
        checkIndexedEmoji(emojis.get(10), 188, 183);
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

        Optional<List<Emoji>> emoji = EmojiManager.getByAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals("😄", emoji.orElseThrow(RuntimeException::new).get(0).getEmoji());
    }

    @Test
    public void getByDiscordAlias() {
        String alias = "smile";

        Optional<Emoji> emoji = EmojiManager.getByDiscordAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals("😄", emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void getByAliasWithColon() {
        String alias = ":smile:";

        Optional<List<Emoji>> emoji = EmojiManager.getByAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals("😄", emoji.orElseThrow(RuntimeException::new).get(0).getEmoji());
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
    public void testEmojiHtmlReplacementScenarios() {
        Object[][] testCases = {
                // {input, expectedOutput, emojiTypes}
                {"&#128129;&#127997;&#8205;&#9794;&#65039;", "<replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#128129;&#127997;&#8205;&#9794;&#65039;&#128129;&#127997;&#8205;&#9794;&#65039;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;", "<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#000128077;&#x0001F44E;&#x000FE0F;", "<replaced><replaced>&#x000FE0F;", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#;", "&#;", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F44E;&#;", "<replaced>&#;", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;", "<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077;", "<replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F469;&#x1F3FB;&#x200D;&#x2764;&#xFE0F;&#x200D;&#x1F468;&#x1F3FC;", "<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077;&#x1F44E;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"Hello &#128077; world &#x1F44E;!", "Hello <replaced> world <replaced>!", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"Just some text.", "Just some text.", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#xXYZ;", "&#xXYZ;", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F481; Start of text.", "<replaced> Start of text.", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"End of text &#x1F44E;", "End of text <replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"Start &#128077; middle &#x1F44E; end.", "Start <replaced> middle <replaced> end.", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E; Invalid &#xXYZ; &#128077;", "<replaced> Invalid &#xXYZ; <replaced>", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077;&#128169;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F44E;&#128077;", "&#x1F44E;&#128077;", EnumSet.noneOf(EmojiType.class)},
                {"&#x1F44E", "&#x1F44E", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#128077;&#;", "<replaced><replaced>&#;", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"This is not &#xemoji;", "This is not &#xemoji;", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077;&#invalid;&#x1F44E;", "<replaced>&#invalid;<replaced>", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1f44e;&#X1F44E;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077; some text &#128077;", "<replaced> some text <replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F44E;&#x1F44E;&#x1F44E;&#x1F44E;", "<replaced><replaced><replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#128077;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44;", "&#x1F44;", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"No emojis here!", "No emojis here!", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#128077; at start &#x1F44E; at end.", "<replaced> at start <replaced> at end.", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"Some text &#x03A9; &#128077;", "Some text &#x03A9; <replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"Text between &#x1F44E; and &#128077;.", "Text between <replaced> and <replaced>.", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#128077; and something else.", "<replaced> and something else.", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"Hello &#x1F44E; world!", "Hello <replaced> world!", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#invalid;&#x1F44E;", "<replaced>&#invalid;<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E; mixed &#x1F44E;", "<replaced> mixed <replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;Invalid&#x1F44E;", "<replaced>Invalid<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
        };

        for (Object[] testCase : testCases) {
            String input = (String) testCase[0];
            String expectedOutput = (String) testCase[1];
            EnumSet<EmojiType> emojiTypes = (EnumSet<EmojiType>) testCase[2];

            assertEquals(expectedOutput, EmojiManager.replaceAllEmojis(input, "<replaced>", emojiTypes),
                    "Failed for input: " + input);
        }
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
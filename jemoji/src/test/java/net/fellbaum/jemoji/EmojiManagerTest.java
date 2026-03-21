package net.fellbaum.jemoji;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static java.util.Collections.singletonList;

public class EmojiManagerTest {

    public static final String ALL_EMOJIS_STRING = EmojiManager.getAllEmojisLengthDescending().stream().map(Emoji::getEmoji).collect(Collectors.joining());
    private static final String SIMPLE_EMOJI_STRING = "Hello ❤️ ❤ ❤❤️ World";
    private static final String SIMPLE_POSITION_EMOJI_STRING = "Hello ❤️ ❤ 👩🏻‍🤝‍👨🏼 ❤❤️ World";
    private static final String EMOJI_VARIATION_STRING = "♎️";


    @Test
    public void testIfEmojisRetrievalWorks() {
        assertNotSame(Emojis.RECYCLING_SYMBOL, Emojis.RECYCLING_SYMBOL_UNQUALIFIED);
    }

    @Test
    public void testIfTextVariantIsHandled() {
        // Text variation (for this emoji) is not an emoji, which is included in the emoji.json. Let's default back to the emoji because the variation ending only signalizes how the symbol should be displayed
        assertEquals(EmojiManager.getEmoji(Emojis.LIBRA.getEmoji()).orElseThrow(IllegalStateException::new), EmojiManager.getEmoji(Emojis.LIBRA.getTextVariation().orElseThrow(IllegalStateException::new)).orElseThrow(IllegalStateException::new));
        assertEquals(EmojiManager.getEmoji(Emojis.LIBRA.getEmoji()).orElseThrow(IllegalStateException::new), EmojiManager.getEmoji(Emojis.LIBRA.getEmojiVariation().orElseThrow(IllegalStateException::new)).orElseThrow(IllegalStateException::new));

        assertNotEquals(Emojis.A_BUTTON_BLOOD_TYPE, Emojis.A_BUTTON_BLOOD_TYPE_UNQUALIFIED);

        // emojis.json only contains fully qualified version \\u2B1B
        assertEquals(Emojis.BLACK_LARGE_SQUARE, EmojiManager.getEmoji(Emojis.BLACK_LARGE_SQUARE.getEmojiVariation().orElseThrow(IllegalStateException::new)).orElseThrow(IllegalStateException::new));
        assertEquals(Emojis.BLACK_LARGE_SQUARE, EmojiManager.getEmoji(Emojis.BLACK_LARGE_SQUARE.getTextVariation().orElseThrow(IllegalStateException::new)).orElseThrow(IllegalStateException::new));

        // emojis.json only contains fully qualified and unqualified version version \\u2B1B
        assertNotEquals(Emojis.A_BUTTON_BLOOD_TYPE, Emojis.A_BUTTON_BLOOD_TYPE_UNQUALIFIED);
        assertEquals(Emojis.A_BUTTON_BLOOD_TYPE_UNQUALIFIED, EmojiManager.getEmoji(Emojis.A_BUTTON_BLOOD_TYPE_UNQUALIFIED.getTextVariation().orElseThrow(IllegalStateException::new)).orElseThrow(IllegalStateException::new));
        assertEquals(Emojis.A_BUTTON_BLOOD_TYPE, EmojiManager.getEmoji(Emojis.A_BUTTON_BLOOD_TYPE_UNQUALIFIED.getEmojiVariation().orElseThrow(IllegalStateException::new)).orElseThrow(IllegalStateException::new));
    }

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
    public void getEmojiForInvalidString() {
        String invalidEmojiString = "notAnEmoji";

        Optional<Emoji> emoji = EmojiManager.getEmoji(invalidEmojiString);
        assertFalse(emoji.isPresent());
    }

    @Test
    public void getEmojiForNullInput() {
        Optional<Emoji> emoji = EmojiManager.getEmoji(null);
        assertFalse(emoji.isPresent());
    }

    @Test
    public void getEmojiForEmptyString() {
        Optional<Emoji> emoji = EmojiManager.getEmoji("");
        assertFalse(emoji.isPresent());
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
        assertTrue(EmojiManager.containsAnyEmoji(SIMPLE_EMOJI_STRING));
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
    public void testUrlEncodedEmojiScenarios() {
        Object[][] testCases = {
                {"abc2%EF%B8%8F%E2%83%A3xyz", "abc<replaced>xyz", EnumSet.of(EmojiType.URL_ENCODED)},
                {"!!3%E2%83%A3%EF%B8%8F??", "!!<replaced>%EF%B8%8F??", EnumSet.of(EmojiType.URL_ENCODED)},
                {"random*%EF%B8%8F%E2%83%A3text", "random<replaced>text", EnumSet.of(EmojiType.URL_ENCODED)},
                {"1%E2%83%A3*%EF%B8%8F!!", "<replaced>*%EF%B8%8F!!", EnumSet.of(EmojiType.URL_ENCODED)}
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

    @Test
    public void replaceDiscordAliases() {
        assertEquals(
                Emojis.THUMBS_UP.getEmoji(),
                EmojiManager.replaceAliases(
                        Emojis.THUMBS_UP.getDiscordAliases().get(0),
                        (alias, emojis) -> emojis.stream()
                                .filter(emoji -> emoji.getDiscordAliases().contains(alias))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new).getEmoji()));

        assertEquals(
                "Hello World " + Emojis.THUMBS_UP.getEmoji(),
                EmojiManager.replaceAliases(
                        "Hello World " + Emojis.THUMBS_UP.getDiscordAliases().get(0),
                        (alias, emojis) -> emojis.stream()
                                .filter(emoji -> emoji.getDiscordAliases().contains(alias))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new).getEmoji()));

        assertEquals(
                Emojis.THUMBS_UP.getEmoji() + " Hello World",
                EmojiManager.replaceAliases(
                        Emojis.THUMBS_UP.getDiscordAliases().get(0) + " Hello World",
                        (alias, emojis) -> emojis.stream()
                                .filter(emoji -> emoji.getDiscordAliases().contains(alias))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new).getEmoji()));

        assertEquals(
                "Hello " + Emojis.THUMBS_UP.getEmoji() + " World",
                EmojiManager.replaceAliases(
                        "Hello " + Emojis.THUMBS_UP.getDiscordAliases().get(0) + " World",
                        (alias, emojis) -> emojis.stream()
                                .filter(emoji -> emoji.getDiscordAliases().contains(alias))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new).getEmoji()));
    }

    @Test
    public void extractAliases() {
        assertEquals(2, EmojiManager.extractAliases(Emojis.THUMBS_UP.getDiscordAliases().get(0) + " test 1 2 3 " + Emojis.THUMBS_DOWN.getDiscordAliases().get(0)).size());
    }

    @Test
    public void extractAliasesInOrder() {
        assertEquals(Arrays.asList(":thumbsup:", ":thumbsdown:"), EmojiManager.extractAliasesInOrder(Emojis.THUMBS_UP.getDiscordAliases().get(0) + " test 1 2 3 " + Emojis.THUMBS_DOWN.getDiscordAliases().get(0)));
    }

    @Test
    public void testIsEmojiWithValidEmoji() {
        assertTrue(EmojiManager.isEmoji("👍")); // Standard emoji
        assertTrue(EmojiManager.isEmoji("❤️")); // Heart emoji
        assertTrue(EmojiManager.isEmoji("👩‍🚀")); // Emoji with ZWJ sequence
    }

    @Test
    public void testIsEmojiWithInvalidString() {
        assertFalse(EmojiManager.isEmoji("notAnEmoji")); // Random string
        assertFalse(EmojiManager.isEmoji("a")); // Single letter
        assertFalse(EmojiManager.isEmoji("1")); // Single digit
        assertFalse(EmojiManager.isEmoji("" + (char) 0x20)); // Single space
    }

    @Test
    public void testIsEmojiForEmptyAndNullInput() {
        assertFalse(EmojiManager.isEmoji(null)); // Null input
        assertFalse(EmojiManager.isEmoji("")); // Empty string
    }

    @Test
    public void testGetAllEmojisNotEmpty() {
        Set<Emoji> emojis = EmojiManager.getAllEmojis();
        assertNotNull(emojis, "The set of emojis should not be null.");
        assertFalse(emojis.isEmpty(), "The set of emojis should not be empty.");
    }

    @Test
    public void testGetAllEmojisContainsCertainEmoji() {
        Set<Emoji> emojis = EmojiManager.getAllEmojis();
        assertTrue(emojis.stream().anyMatch(e -> e.getEmoji().equals("👍")), "The set of emojis should contain the '👍' emoji.");
    }

    @Test
    public void testGetAllEmojisImmutable() {
        Set<Emoji> emojis = EmojiManager.getAllEmojis();
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> emojis.add(null), "The set of emojis should be immutable.");
    }

    @Test
    public void testGetAllEmojisSubGrouped() {
        Map<EmojiSubGroup, Set<Emoji>> subGroupedEmojis = EmojiManager.getAllEmojisSubGrouped();

        assertNotNull(subGroupedEmojis, "The grouping result should not be null.");

        // Ensure all EmojiSubGroups are present in the map
        for (EmojiSubGroup emojiSubGroup : EmojiSubGroup.values()) {
            assertTrue(subGroupedEmojis.containsKey(emojiSubGroup), "Missing sub group: " + emojiSubGroup);
        }

        // Ensure none of the groups are empty
        for (Map.Entry<EmojiSubGroup, Set<Emoji>> entry : subGroupedEmojis.entrySet()) {
            EmojiSubGroup group = entry.getKey();
            Set<Emoji> emojis = entry.getValue();

            assertNotNull(emojis, "The sub group " + group + " should not have a null emoji set.");
            assertFalse(emojis.isEmpty(), "The sub group " + group + " should not be empty.");
        }

        // Verify emojis belong to the correct group
        for (Map.Entry<EmojiSubGroup, Set<Emoji>> entry : subGroupedEmojis.entrySet()) {
            EmojiSubGroup group = entry.getKey();
            Set<Emoji> emojis = entry.getValue();

            for (Emoji emoji : emojis) {
                assertEquals(group, emoji.getSubgroup(), "Emoji " + emoji.getEmoji() + " does not belong to sub group " + group);
            }
        }
    }

    @Test
    public void testGetAllEmojisGrouped() {
        Map<EmojiGroup, Set<Emoji>> groupedEmojis = EmojiManager.getAllEmojisGrouped();

        assertNotNull(groupedEmojis, "The grouping result should not be null.");

        // Ensure all EmojiGroups are present in the map
        for (EmojiGroup group : EmojiGroup.values()) {
            assertTrue(groupedEmojis.containsKey(group), "Missing group: " + group);
        }

        // Ensure none of the groups are empty
        for (Map.Entry<EmojiGroup, Set<Emoji>> entry : groupedEmojis.entrySet()) {
            EmojiGroup group = entry.getKey();
            Set<Emoji> emojis = entry.getValue();

            assertNotNull(emojis, "The group " + group + " should not have a null emoji set.");
            assertFalse(emojis.isEmpty(), "The group " + group + " should not be empty.");
        }

        // Verify emojis belong to the correct group
        for (Map.Entry<EmojiGroup, Set<Emoji>> entry : groupedEmojis.entrySet()) {
            EmojiGroup group = entry.getKey();
            Set<Emoji> emojis = entry.getValue();

            for (Emoji emoji : emojis) {
                assertEquals(group, emoji.getGroup(), "Emoji " + emoji.getEmoji() + " does not belong to group " + group);
            }
        }
    }

    @Test
    public void testGetAllEmojisByGroup() {
        // Verify all groups are present and contain emojis
        for (EmojiGroup group : EmojiGroup.values()) {
            Set<Emoji> emojis = EmojiManager.getAllEmojisByGroup(group);
            assertNotNull(emojis, "The result for group " + group + " should not be null.");
            assertFalse(emojis.isEmpty(), "The group " + group + " should not be empty.");
            for (Emoji emoji : emojis) {
                assertEquals(group, emoji.getGroup(), "Emoji " + emoji.getEmoji() + " does not belong to group " + group);
            }
        }

        // Check for invalid group input (this should return an empty set)
        Set<Emoji> invalidGroupResult = EmojiManager.getAllEmojisByGroup(null);
        assertNotNull(invalidGroupResult, "The result for null group should not be null.");
        assertTrue(invalidGroupResult.isEmpty(), "The result for null group should be an empty set.");
    }

    // ===== Platform alias lookups =====

    @Test
    public void getByGithubAlias() {
        String alias = Emojis.THUMBS_UP.getGithubAliases().get(0);
        Optional<Emoji> emoji = EmojiManager.getByGithubAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals(Emojis.THUMBS_UP.getEmoji(), emoji.get().getEmoji());
    }

    @Test
    public void getByGithubAliasWithColons() {
        String alias = Emojis.THUMBS_UP.getGithubAliases().get(0);
        String aliasWithColons = alias.startsWith(":") ? alias : ":" + alias + ":";
        Optional<Emoji> emoji = EmojiManager.getByGithubAlias(aliasWithColons);
        assertTrue(emoji.isPresent());
    }

    @Test
    public void getByGithubAliasNullAndEmpty() {
        assertFalse(EmojiManager.getByGithubAlias(null).isPresent());
        assertFalse(EmojiManager.getByGithubAlias("").isPresent());
    }

    @Test
    public void getByGithubAliasNonExistent() {
        assertFalse(EmojiManager.getByGithubAlias("thisAliasDefinitelyDoesNotExist99999").isPresent());
    }

    @Test
    public void getBySlackAlias() {
        String alias = Emojis.THUMBS_UP.getSlackAliases().get(0);
        Optional<Emoji> emoji = EmojiManager.getBySlackAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals(Emojis.THUMBS_UP.getEmoji(), emoji.get().getEmoji());
    }

    @Test
    public void getBySlackAliasWithColons() {
        String alias = Emojis.THUMBS_UP.getSlackAliases().get(0);
        String aliasWithColons = alias.startsWith(":") ? alias : ":" + alias + ":";
        Optional<Emoji> emoji = EmojiManager.getBySlackAlias(aliasWithColons);
        assertTrue(emoji.isPresent());
    }

    @Test
    public void getBySlackAliasNullAndEmpty() {
        assertFalse(EmojiManager.getBySlackAlias(null).isPresent());
        assertFalse(EmojiManager.getBySlackAlias("").isPresent());
    }

    @Test
    public void getBySlackAliasNonExistent() {
        assertFalse(EmojiManager.getBySlackAlias("thisAliasDefinitelyDoesNotExist99999").isPresent());
    }

    @Test
    public void getByAliasNullAndEmpty() {
        assertFalse(EmojiManager.getByAlias(null).isPresent());
        assertFalse(EmojiManager.getByAlias("").isPresent());
    }

    // ===== Encoding-based direct lookups =====

    @Test
    public void getByHtmlDecimal() {
        Optional<Emoji> emoji = EmojiManager.getByHtmlDecimal(Emojis.THUMBS_UP.getHtmlDecimalCode());
        assertTrue(emoji.isPresent());
        assertEquals(Emojis.THUMBS_UP.getEmoji(), emoji.get().getEmoji());
    }

    @Test
    public void getByHtmlDecimalNullEmptyGarbage() {
        assertFalse(EmojiManager.getByHtmlDecimal(null).isPresent());
        assertFalse(EmojiManager.getByHtmlDecimal("").isPresent());
        assertFalse(EmojiManager.getByHtmlDecimal("&#notAnEmoji;").isPresent());
    }

    @Test
    public void getByHtmlHexadecimal() {
        // Map stores keys in uppercase, so input must be uppercase to match
        Optional<Emoji> emoji = EmojiManager.getByHtmlHexadecimal(Emojis.THUMBS_UP.getHtmlHexadecimalCode().toUpperCase());
        assertTrue(emoji.isPresent());
        assertEquals(Emojis.THUMBS_UP.getEmoji(), emoji.get().getEmoji());
    }

    @Test
    public void getByHtmlHexadecimalNullEmptyGarbage() {
        assertFalse(EmojiManager.getByHtmlHexadecimal(null).isPresent());
        assertFalse(EmojiManager.getByHtmlHexadecimal("").isPresent());
        assertFalse(EmojiManager.getByHtmlHexadecimal("&#xNOTANEMOJI;").isPresent());
    }

    @Test
    public void getByUrlEncoded() {
        Optional<Emoji> emoji = EmojiManager.getByUrlEncoded(Emojis.THUMBS_UP.getURLEncoded());
        assertTrue(emoji.isPresent());
        assertEquals(Emojis.THUMBS_UP.getEmoji(), emoji.get().getEmoji());
    }

    @Test
    public void getByUrlEncodedNullEmptyGarbage() {
        assertFalse(EmojiManager.getByUrlEncoded(null).isPresent());
        assertFalse(EmojiManager.getByUrlEncoded("").isPresent());
        assertFalse(EmojiManager.getByUrlEncoded("%NOTANEMOJI").isPresent());
    }

    // ===== containsAnyEmoji with EmojiType =====

    @Test
    public void containsAnyEmojiWithHtmlDecimalType() {
        assertTrue(EmojiManager.containsAnyEmoji(Emojis.THUMBS_UP.getHtmlDecimalCode(), EnumSet.of(EmojiType.HTML_DECIMAL)));
        assertFalse(EmojiManager.containsAnyEmoji("Hello World", EnumSet.of(EmojiType.HTML_DECIMAL)));
        assertFalse(EmojiManager.containsAnyEmoji(null, EnumSet.of(EmojiType.HTML_DECIMAL)));
        assertFalse(EmojiManager.containsAnyEmoji("", EnumSet.of(EmojiType.HTML_DECIMAL)));
    }

    @Test
    public void containsAnyEmojiWithHtmlHexadecimalType() {
        assertTrue(EmojiManager.containsAnyEmoji(Emojis.THUMBS_UP.getHtmlHexadecimalCode(), EnumSet.of(EmojiType.HTML_HEXADECIMAL)));
        assertFalse(EmojiManager.containsAnyEmoji("Hello World", EnumSet.of(EmojiType.HTML_HEXADECIMAL)));
    }

    @Test
    public void containsAnyEmojiWithUrlEncodedType() {
        assertTrue(EmojiManager.containsAnyEmoji(Emojis.THUMBS_UP.getURLEncoded(), EnumSet.of(EmojiType.URL_ENCODED)));
        assertFalse(EmojiManager.containsAnyEmoji("Hello World", EnumSet.of(EmojiType.URL_ENCODED)));
    }

    @Test
    public void containsAnyEmojiNullOrEmpty() {
        assertFalse(EmojiManager.containsAnyEmoji(null));
        assertFalse(EmojiManager.containsAnyEmoji(""));
    }

    // ===== extractEmojis/extractEmojisInOrder with EnumSet =====

    @Test
    public void extractEmojisInOrderWithHtmlDecimalType() {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrder(Emojis.THUMBS_UP.getHtmlDecimalCode(), EnumSet.of(EmojiType.HTML_DECIMAL));
        assertEquals(1, emojis.size());
        assertEquals(Emojis.THUMBS_UP, emojis.get(0));
    }

    @Test
    public void extractEmojisInOrderWithHtmlHexadecimalType() {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrder(Emojis.THUMBS_UP.getHtmlHexadecimalCode(), EnumSet.of(EmojiType.HTML_HEXADECIMAL));
        assertEquals(1, emojis.size());
        assertEquals(Emojis.THUMBS_UP, emojis.get(0));
    }

    @Test
    public void extractEmojisInOrderWithUrlEncodedType() {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrder(Emojis.THUMBS_UP.getURLEncoded(), EnumSet.of(EmojiType.URL_ENCODED));
        assertEquals(1, emojis.size());
        assertEquals(Emojis.THUMBS_UP, emojis.get(0));
    }

    @Test
    public void extractEmojisInOrderWithEmptyEnumSet() {
        assertTrue(EmojiManager.extractEmojisInOrder("👍", EnumSet.noneOf(EmojiType.class)).isEmpty());
    }

    @Test
    public void extractEmojisInOrderWithNullOrEmptyText() {
        assertTrue(EmojiManager.extractEmojisInOrder(null, EnumSet.of(EmojiType.UNICODE)).isEmpty());
        assertTrue(EmojiManager.extractEmojisInOrder("", EnumSet.of(EmojiType.UNICODE)).isEmpty());
    }

    @Test
    public void extractEmojisSetWithHtmlDecimalType() {
        Set<Emoji> emojis = EmojiManager.extractEmojis(Emojis.THUMBS_UP.getHtmlDecimalCode() + Emojis.THUMBS_UP.getHtmlDecimalCode(), EnumSet.of(EmojiType.HTML_DECIMAL));
        assertEquals(1, emojis.size());
        assertTrue(emojis.contains(Emojis.THUMBS_UP));
    }

    // ===== removeAllEmojis with EnumSet =====

    @Test
    public void removeAllEmojisWithHtmlDecimalType() {
        assertEquals("", EmojiManager.removeAllEmojis(Emojis.THUMBS_UP.getHtmlDecimalCode(), EnumSet.of(EmojiType.HTML_DECIMAL)));
        assertEquals("Hello  World", EmojiManager.removeAllEmojis("Hello " + Emojis.THUMBS_UP.getHtmlDecimalCode() + " World", EnumSet.of(EmojiType.HTML_DECIMAL)));
    }

    @Test
    public void removeAllEmojisWithUrlEncodedType() {
        assertEquals("", EmojiManager.removeAllEmojis(Emojis.THUMBS_UP.getURLEncoded(), EnumSet.of(EmojiType.URL_ENCODED)));
    }

    @Test
    public void removeAllEmojisWithEmptyEnumSetLeavesTextUnchanged() {
        String text = "Hello 👍 World";
        assertEquals(text, EmojiManager.removeAllEmojis(text, EnumSet.noneOf(EmojiType.class)));
    }

    @Test
    public void removeAllEmojisNullOrEmpty() {
        assertEquals("", EmojiManager.removeAllEmojis(null));
        assertEquals("", EmojiManager.removeAllEmojis(""));
    }

    // ===== removeEmojis varargs and Collection variants =====

    @Test
    public void removeEmojisVarargs() {
        assertEquals("Hello  World", EmojiManager.removeEmojis("Hello 👍 World", Emojis.THUMBS_UP));
        assertEquals("Hello  👎 World", EmojiManager.removeEmojis("Hello 👍 👎 World", Emojis.THUMBS_UP));
    }

    @Test
    public void removeEmojisCollection() {
        assertEquals("Hello  World", EmojiManager.removeEmojis("Hello 👍 World", singletonList(Emojis.THUMBS_UP)));
        assertEquals("Hello  👎 World", EmojiManager.removeEmojis("Hello 👍 👎 World", singletonList(Emojis.THUMBS_UP)));
    }

    @Test
    public void removeEmojisCollectionEquivalentToVarargs() {
        String text = "Hello 👍 👎 World";
        assertEquals(
                EmojiManager.removeEmojis(text, Emojis.THUMBS_UP),
                EmojiManager.removeEmojis(text, singletonList(Emojis.THUMBS_UP)));
    }

    @Test
    public void removeEmojisCollectionWithType() {
        String htmlText = "Hello " + Emojis.THUMBS_UP.getHtmlDecimalCode() + " World";
        assertEquals("Hello  World", EmojiManager.removeEmojis(htmlText, singletonList(Emojis.THUMBS_UP), EnumSet.of(EmojiType.HTML_DECIMAL)));
        // Unicode type should NOT remove HTML decimal representation
        assertEquals(htmlText, EmojiManager.removeEmojis(htmlText, singletonList(Emojis.THUMBS_UP), EnumSet.of(EmojiType.UNICODE)));
    }

    // ===== removeAllEmojisExcept Collection variant =====

    @Test
    public void removeAllEmojisExceptCollection() {
        String result = EmojiManager.removeAllEmojisExcept("Hello 👍 👎 World", singletonList(Emojis.THUMBS_UP));
        assertEquals("Hello 👍  World", result);
    }

    @Test
    public void removeAllEmojisExceptCollectionEquivalentToArray() {
        String text = "Hello 👍 👎 World" + SIMPLE_EMOJI_STRING;
        assertEquals(
                EmojiManager.removeAllEmojisExcept(text, Emojis.THUMBS_UP),
                EmojiManager.removeAllEmojisExcept(text, singletonList(Emojis.THUMBS_UP)));
    }

    @Test
    public void removeAllEmojisExceptCollectionWithType() {
        String htmlText = Emojis.THUMBS_UP.getHtmlDecimalCode() + Emojis.THUMBS_DOWN.getHtmlDecimalCode();
        // Keep thumbs up (HTML_DECIMAL), remove others
        String result = EmojiManager.removeAllEmojisExcept(htmlText, singletonList(Emojis.THUMBS_UP), EnumSet.of(EmojiType.HTML_DECIMAL));
        assertEquals(Emojis.THUMBS_UP.getHtmlDecimalCode(), result);
    }

    // ===== replaceAllEmojis with Function and EnumSet =====

    @Test
    public void replaceAllEmojisFunctionWithHtmlDecimalType() {
        String result = EmojiManager.replaceAllEmojis(Emojis.THUMBS_UP.getHtmlDecimalCode(), emoji -> "REPLACED", EnumSet.of(EmojiType.HTML_DECIMAL));
        assertEquals("REPLACED", result);
    }

    @Test
    public void replaceAllEmojisFunctionNullOrEmpty() {
        assertEquals("", EmojiManager.replaceAllEmojis(null, "x"));
        assertEquals("", EmojiManager.replaceAllEmojis("", "x"));
    }

    // ===== replaceEmojis varargs =====

    @Test
    public void replaceEmojisVarargs() {
        assertEquals("Hello REPLACED World", EmojiManager.replaceEmojis("Hello 👍 World", "REPLACED", Emojis.THUMBS_UP));
        // Emoji not in list stays unchanged
        assertEquals("Hello 👍 World", EmojiManager.replaceEmojis("Hello 👍 World", "REPLACED", Emojis.THUMBS_DOWN));
    }

    // ===== replaceEmojis Function + Collection =====

    @Test
    public void replaceEmojisFunctionCollection() {
        String result = EmojiManager.replaceEmojis("Hello 👍 World", emoji -> "[" + emoji.getDescription() + "]", singletonList(Emojis.THUMBS_UP));
        assertTrue(result.startsWith("Hello ["));
        assertTrue(result.endsWith("] World"));
    }

    @Test
    public void replaceEmojisFunctionCollectionWithType() {
        String htmlText = "Hello " + Emojis.THUMBS_UP.getHtmlDecimalCode() + " World";
        String result = EmojiManager.replaceEmojis(htmlText, emoji -> "REPLACED", singletonList(Emojis.THUMBS_UP), EnumSet.of(EmojiType.HTML_DECIMAL));
        assertEquals("Hello REPLACED World", result);
    }

    @Test
    public void replaceEmojisStringCollectionWithType() {
        String htmlText = "Hello " + Emojis.THUMBS_UP.getHtmlDecimalCode() + " World";
        String result = EmojiManager.replaceEmojis(htmlText, "REPLACED", singletonList(Emojis.THUMBS_UP), EnumSet.of(EmojiType.HTML_DECIMAL));
        assertEquals("Hello REPLACED World", result);
    }

    // ===== replaceAliases null/empty =====

    @Test
    public void replaceAliasesNullOrEmpty() {
        assertEquals("", EmojiManager.replaceAliases(null, (alias, emojis) -> alias));
        assertEquals("", EmojiManager.replaceAliases("", (alias, emojis) -> alias));
    }

    // ===== extractAliasesInOrderWithIndex =====

    @Test
    public void extractAliasesInOrderWithIndexPositions() {
        String alias1 = Emojis.THUMBS_UP.getDiscordAliases().get(0);
        String alias2 = Emojis.THUMBS_DOWN.getDiscordAliases().get(0);
        String sep = " test ";
        String text = alias1 + sep + alias2;

        List<IndexedAlias> aliases = EmojiManager.extractAliasesInOrderWithIndex(text);
        assertEquals(2, aliases.size());

        assertEquals(alias1, aliases.get(0).getAlias());
        assertEquals(0, aliases.get(0).getCharIndex());
        assertEquals(0, aliases.get(0).getCodePointIndex());
        assertEquals(alias1.length(), aliases.get(0).getEndCharIndex());

        assertEquals(alias2, aliases.get(1).getAlias());
        assertEquals(alias1.length() + sep.length(), aliases.get(1).getCharIndex());
    }

    @Test
    public void extractAliasesInOrderWithIndexEmptyAndNull() {
        assertTrue(EmojiManager.extractAliasesInOrderWithIndex(null).isEmpty());
        assertTrue(EmojiManager.extractAliasesInOrderWithIndex("").isEmpty());
    }

    // ===== getAllEmojisLengthDescending ordering =====

    @Test
    public void getAllEmojisLengthDescendingOrdering() {
        List<Emoji> emojis = EmojiManager.getAllEmojisLengthDescending();
        assertFalse(emojis.isEmpty());

        int firstCodePoints = emojis.get(0).getEmoji().codePointCount(0, emojis.get(0).getEmoji().length());
        int lastCodePoints = emojis.get(emojis.size() - 1).getEmoji().codePointCount(0, emojis.get(emojis.size() - 1).getEmoji().length());
        assertTrue(firstCodePoints >= lastCodePoints);
    }

    @Test
    public void testGetAllEmojisBySubGroup() {
        // Verify all subgroups are present and contain emojis
        for (EmojiSubGroup subGroup : EmojiSubGroup.values()) {
            Set<Emoji> emojis = EmojiManager.getAllEmojisBySubGroup(subGroup);
            assertNotNull(emojis, "The result for subgroup " + subGroup + " should not be null.");
            assertFalse(emojis.isEmpty(), "The subgroup " + subGroup + " should not be empty.");
            for (Emoji emoji : emojis) {
                assertEquals(subGroup, emoji.getSubgroup(), "Emoji " + emoji.getEmoji() + " does not belong to subgroup " + subGroup);
            }
        }

        // Check for invalid subgroup input (this should return an empty set)
        Set<Emoji> invalidSubGroupResult = EmojiManager.getAllEmojisBySubGroup(null);
        assertNotNull(invalidSubGroupResult, "The result for null subgroup should not be null.");
        assertTrue(invalidSubGroupResult.isEmpty(), "The result for null subgroup should be an empty set.");
    }
}
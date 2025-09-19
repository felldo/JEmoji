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
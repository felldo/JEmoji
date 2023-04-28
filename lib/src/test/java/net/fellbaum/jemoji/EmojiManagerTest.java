package net.fellbaum.jemoji;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmojiManagerTest {

    private static final String ALL_EMOJIS = EmojiManager.getAllEmojisLengthDescending().stream().map(Emoji::getEmoji).collect(Collectors.joining());
    private static final String SIMPLE_EMOJI_STRING = "Hello ❤️ World";
    /*@Test
    public void testContainsEmoji() {
        String allEmojis = EmojiManager.EMOJIS_LENGTH_DESCENDING.stream().map(Emoji::getEmoji).collect(Collectors.joining());

        long startTime = System.currentTimeMillis();
        List<Emoji> str = EmojiManager.extractEmojisInOrderEmojiRegex(allEmojis);
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");

        Assert.assertEquals(EmojiManager.EMOJIS_LENGTH_DESCENDING.size(), str.size());

        System.out.println(" ".chars().mapToObj(value -> "\\u" + Integer.toHexString(value).toUpperCase())
                .collect(Collectors.joining("")));
    }



        /*Pattern pat = Pattern.compile("(🙅‍♀️)|(🙅‍♀)", Pattern.UNICODE_CASE);
        Matcher match = pat.matcher(emoji);

        System.out.println(emoji.chars().mapToObj(it -> "\\u" + Integer.toHexString(it).toUpperCase()).collect(Collectors.joining("")));
        while (match.find()) {
            System.out.println(match.group().chars().mapToObj(it -> "\\u" + Integer.toHexString(it).toUpperCase()).collect(Collectors.joining("")));
        }
    //  \uD83D\uDE45\u200D\u2640\uFE0F
    //0 \uD83D\uDE45\u200D\u2640\uFE0F
    //1 \uD83D\uDE45\u200D\u2640\uFE0F

    //System.out.println("\uD83D\uDE45\u200D\u2640\uFE0F".replaceAll("(🙅‍♀)", "").chars().mapToObj(it -> "\\u" + Integer.toHexString(it).toUpperCase()).collect(Collectors.joining("")));


    */
    /*@Test
    public void extractEmojis() {
        List<Emoji> emojis = EmojiManager.testEmojiPattern(allEmojis);

        Assert.assertEquals(EmojiManager.getAllEmojisLengthDescending().size(), emojis.size());
    }*/


    @Test
    public void extractEmojisInOrder() {
        List<Emoji> str = EmojiManager.extractEmojisInOrder(ALL_EMOJIS);

        Assert.assertEquals(EmojiManager.getAllEmojisLengthDescending().size(), str.size());
    }

    @Test
    public void getEmoji() {
        String emojiString = "👍";

        Optional<Emoji> emoji = EmojiManager.getEmoji(emojiString);
        Assert.assertTrue(emoji.isPresent());
        Assert.assertEquals(emojiString, emoji.get().getEmoji());
    }

    @Test
    public void isEmoji() {
        String emojiString = "\uD83D\uDC4D";

        Assert.assertTrue(EmojiManager.isEmoji(emojiString));
    }

    @Test
    public void getByAlias() {
        String alias = "smile";

        Optional<Emoji> emoji = EmojiManager.getByAlias(alias);
        Assert.assertTrue(emoji.isPresent());
        Assert.assertEquals("😄", emoji.get().getEmoji());
    }

    @Test
    public void getByAliasWithColon() {
        String alias = ":smile:";

        Optional<Emoji> emoji = EmojiManager.getByAlias(alias);
        Assert.assertTrue(emoji.isPresent());
        Assert.assertEquals("😄", emoji.get().getEmoji());
    }

    @Test
    public void containsEmoji() {
        Assert.assertTrue(EmojiManager.containsEmoji(SIMPLE_EMOJI_STRING));
    }

    @Test
    public void removeEmojis() {
        Assert.assertEquals("Hello  World", EmojiManager.removeAllEmojis(SIMPLE_EMOJI_STRING));
    }

    @Test
    public void removeAllEmojisExcept() {
        Assert.assertEquals("Hello ❤️ World", EmojiManager.removeAllEmojisExcept(SIMPLE_EMOJI_STRING + "👍", Collections.singletonList(EmojiManager.getEmoji("❤️").get())));
    }

    @Test
    public void replaceEmojis() {
        Assert.assertEquals("Hello :heart: World", EmojiManager.replaceEmojis(SIMPLE_EMOJI_STRING, ":heart:", Collections.singletonList(EmojiManager.getEmoji("❤️").get())));
    }

    @Test
    public void replaceAllEmojis() {
        Assert.assertEquals("Hello something World something something", EmojiManager.replaceAllEmojis(SIMPLE_EMOJI_STRING + " 👍 😊", "something"));
    }
}
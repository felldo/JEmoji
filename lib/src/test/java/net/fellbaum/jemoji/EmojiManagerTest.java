package net.fellbaum.jemoji;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EmojiManagerTest {

    private static final String ALL_EMOJIS = EmojiManager.getAllEmojisLengthDescending().stream().map(Emoji::getEmoji).collect(Collectors.joining());
    private static final String SIMPLE_EMOJI_STRING = "Hello ❤️ World";

    @Test
    public void extractEmojisInOrder() {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrder(ALL_EMOJIS + ALL_EMOJIS);

        Assert.assertEquals(EmojiManager.getAllEmojisLengthDescending().size() * 2, emojis.size());
    }

    @Test
    public void extractEmojis() {
        Set<Emoji> emojis = EmojiManager.extractEmojis(ALL_EMOJIS + ALL_EMOJIS);

        Assert.assertEquals(EmojiManager.getAllEmojisLengthDescending().size(), emojis.size());
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
package net.fellbaum.jemoji;

import javax.annotation.Nonnull;

/**
 * Represents an emoji with character and codepoint indexes.
 *
 * @see EmojiManager#extractEmojisInOrderWithIndex(String)
 */
public class IndexedEmoji {
    private final Emoji emoji;
    private final int charIndex;
    private final int codePointIndex;

    IndexedEmoji(Emoji emoji, int charIndex, int codePointIndex) {
        this.emoji = emoji;
        this.charIndex = charIndex;
        this.codePointIndex = codePointIndex;
    }

    /**
     * Gets the captured {@link Emoji emoji}.
     *
     * @return The captured emoji
     */
    @Nonnull
    public Emoji getEmoji() {
        return emoji;
    }

    /**
     * Gets the character index at which the emoji starts.
     *
     * <p>The index is included in {@code 0 < input <= input.length() - 1}
     *
     * @return The character index at which the emoji starts
     */
    public int getCharIndex() {
        return charIndex;
    }

    /**
     * Gets the codepoint index at which the emoji starts.
     *
     * <p>This must not be confused with {@link #getCharIndex()},
     * a codepoint can contain one or two characters,
     * which means the codepoint index will likely be lower than the character index.
     *
     * @return The character index at which the emoji starts
     */
    public int getCodePointIndex() {
        return codePointIndex;
    }
}

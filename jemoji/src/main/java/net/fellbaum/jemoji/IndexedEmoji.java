package net.fellbaum.jemoji;

/**
 * Represents an emoji with character and codepoint indexes.
 */
public final class IndexedEmoji {
    private final Emoji emoji;
    private final int charIndex;
    private final int codePointIndex;
    private final int endCharIndex;
    private final int endCodePointIndex;

    IndexedEmoji(final Emoji emoji, final int charIndex, final int codePointIndex, int endCharIndex, int endCodePointIndex) {
        this.emoji = emoji;
        this.charIndex = charIndex;
        this.codePointIndex = codePointIndex;
        this.endCharIndex = endCharIndex;
        this.endCodePointIndex = endCodePointIndex;
    }

    /**
     * Gets the captured {@link Emoji emoji}.
     *
     * @return The captured emoji.
     */
    public Emoji getEmoji() {
        return emoji;
    }

    /**
     * Gets the character index at which the emoji starts.
     *
     * <p>The index is included in {@code 0 < input <= input.length() - 1}
     *
     * @return The character index at which the emoji starts.
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
     * @return The character index at which the emoji starts.
     */
    public int getCodePointIndex() {
        return codePointIndex;
    }

    /**
     * Gets the character index at which the emoji ends.
     *
     * <p>The index is included in {@code 0 < input <= input.length() - 1}
     *
     * @return The character index at which the emoji ends.
     */
    public int getEndCharIndex() {
        return endCharIndex;
    }

    /**
     * Gets the codepoint index at which the emoji ends.
     *
     * <p>This must not be confused with {@link #getEndCharIndex()},
     * a codepoint can contain one or two characters,
     * which means the codepoint index will likely be lower than the character index.
     *
     * @return The character index at which the emoji ends.
     */
    public int getEndCodePointIndex() {
        return endCodePointIndex;
    }

    @Override
    public String toString() {
        return "IndexedEmoji{" +
                "emoji=" + emoji +
                ", charIndex=" + charIndex +
                ", codePointIndex=" + codePointIndex +
                ", endCharIndex=" + endCharIndex +
                ", endCodePointIndex=" + endCodePointIndex +
                '}';
    }
}

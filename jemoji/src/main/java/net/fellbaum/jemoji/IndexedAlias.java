package net.fellbaum.jemoji;

import java.util.List;

/**
 * Represents an alias with character and codepoint indexes.
 */
public final class IndexedAlias {
    private final List<Emoji> emojis;
    private final String alias;
    private final int charIndex;
    private final int codePointIndex;
    private final int endCharIndex;
    private final int endCodePointIndex;

    IndexedAlias(final String alias, final List<Emoji> emojis, final int charIndex, final int codePointIndex, int endCharIndex, int endCodePointIndex) {
        this.emojis = emojis;
        this.alias = alias;
        this.charIndex = charIndex;
        this.codePointIndex = codePointIndex;
        this.endCharIndex = endCharIndex;
        this.endCodePointIndex = endCodePointIndex;
    }

    /**
     * Gets the {@link Emoji}s.
     *
     * @return The emojis.
     */
    public List<Emoji> getEmojis() {
        return emojis;
    }

    /**
     * Gets the captured alias.
     *
     * @return The captured alias.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Gets the character index at which the alias starts.
     *
     * <p>The index is included in {@code 0 < input <= input.length() - 1}
     *
     * @return The character index at which the alias starts.
     */
    public int getCharIndex() {
        return charIndex;
    }

    /**
     * Gets the codepoint index at which the alias starts.
     *
     * <p>This must not be confused with {@link #getCharIndex()},
     * a codepoint can contain one or two characters,
     * which means the codepoint index will likely be lower than the character index.
     *
     * @return The character index at which the alias starts.
     */
    public int getCodePointIndex() {
        return codePointIndex;
    }

    /**
     * Gets the character index at which the alias ends.
     *
     * <p>The index is included in {@code 0 < input <= input.length() - 1}
     *
     * @return The character index at which the alias ends.
     */
    public int getEndCharIndex() {
        return endCharIndex;
    }

    /**
     * Gets the codepoint index at which the alias ends.
     *
     * <p>This must not be confused with {@link #getEndCharIndex()},
     * a codepoint can contain one or two characters,
     * which means the codepoint index will likely be lower than the character index.
     *
     * @return The character index at which the alias ends.
     */
    public int getEndCodePointIndex() {
        return endCodePointIndex;
    }

    @Override
    public String toString() {
        return "IndexedAlias{" +
                "emojis=" + emojis +
                ", alias='" + alias + '\'' +
                ", charIndex=" + charIndex +
                ", codePointIndex=" + codePointIndex +
                ", endCharIndex=" + endCharIndex +
                ", endCodePointIndex=" + endCodePointIndex +
                '}';
    }
}

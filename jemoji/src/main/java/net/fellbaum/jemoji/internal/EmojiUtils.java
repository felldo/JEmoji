package net.fellbaum.jemoji.internal;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.fellbaum.jemoji.internal.EmojiData.*;

public final class EmojiUtils {

    private EmojiUtils() {
    }

    public static final char TEXT_VARIATION_CHARACTER = '\uFE0E';
    public static final char EMOJI_VARIATION_CHARACTER = '\uFE0F';
    private static final char ZERO_WIDTH_JOINER = '\u200D';
    private static final int MAX_LENGTH_HTML_DECIMAL_NUMBER_COUNT = 6;

    /**
     * Removes every occurrence of the given modifier, together with a Zero Width Joiner
     * directly preceding it, from the text.
     * <p>
     * Equivalent to {@code text.replaceAll("\u200D?" + modifier, "")} but without compiling
     * a regular expression on every call.
     *
     * @param text     The text to remove the modifier from.
     * @param modifier The modifier to remove.
     * @return The text without the modifier.
     */
    public static String removeModifier(final String text, final String modifier) {
        int modifierIndex = text.indexOf(modifier);
        if (modifierIndex == -1) return text;

        final StringBuilder sb = new StringBuilder(text.length());
        int copiedUpTo = 0;
        while (modifierIndex != -1) {
            final boolean precededByJoiner = modifierIndex > 0 && text.charAt(modifierIndex - 1) == ZERO_WIDTH_JOINER;
            sb.append(text, copiedUpTo, precededByJoiner ? modifierIndex - 1 : modifierIndex);
            copiedUpTo = modifierIndex + modifier.length();
            modifierIndex = text.indexOf(modifier, copiedUpTo);
        }
        sb.append(text, copiedUpTo, text.length());
        return sb.toString();
    }

    public static int getCodePointCount(final String string) {
        return string.codePointCount(0, string.length());
    }

    public static boolean isStringNullOrEmpty(@Nullable final String string) {
        return null == string || string.isEmpty();
    }

    public static String removeColonFromAlias(final String alias) {
        return alias.startsWith(":") && alias.endsWith(":") ? alias.substring(1, alias.length() - 1) : alias;
    }

    public static String addColonToAlias(final String alias) {
        return alias.startsWith(":") && alias.endsWith(":") ? alias : ":" + alias + ":";
    }

    public static Optional<List<Emoji>> findEmojiByEitherAlias(final Map<CodepointSequence, List<Emoji>> map, final String alias) {
        final List<Emoji> firstValue = map.get(new CodepointSequence(addColonToAlias(alias)));
        if (firstValue != null) return Optional.of(firstValue);
        final List<Emoji> secondValue = map.get(new CodepointSequence(removeColonFromAlias(alias)));
        if (secondValue != null) return Optional.of(secondValue);
        return Optional.empty();
    }

    public static int[] stringToCodePoints(final String text) {
        final int[] codePoints = new int[getCodePointCount(text)];
        int j = 0;
        for (int i = 0; i < text.length(); ) {
            final int codePoint = text.codePointAt(i);
            codePoints[j++] = codePoint;
            i += Character.charCount(codePoint);
        }
        return codePoints;
    }

    /**
     * Finds the longest unicode emoji starting at {@code charIndex}.
     *
     * @param text      The text.
     * @param charIndex The char index of the codepoint to check.
     * @param codepoint The codepoint at {@code charIndex}.
     * @return The found emoji with its end char index, otherwise {@code null}.
     */
    @Nullable
    static UniqueEmojiFoundResult findUnicodeEmoji(final String text, final int charIndex, final int codepoint) {
        // Walk the emoji trie, remembering the longest emoji that ended along the way.
        // Only follows the path of the text instead of testing every emoji with the same first codepoint
        // (up to ~500 for e.g. 👩).
        CodepointTrie node = UNICODE_TRIE.child(codepoint);
        if (node == null) return null;

        int currentIndex = charIndex + Character.charCount(codepoint);
        List<Emoji> longestMatchEmojis = node.getEmojis();
        int longestMatchEndIndex = currentIndex;

        final int textLength = text.length();
        while (currentIndex < textLength) {
            final int currentCodepoint = text.codePointAt(currentIndex);
            node = node.child(currentCodepoint);
            if (node == null) break;
            currentIndex += Character.charCount(currentCodepoint);

            final List<Emoji> emojis = node.getEmojis();
            if (emojis != null) {
                longestMatchEmojis = emojis;
                longestMatchEndIndex = currentIndex;
            }
        }

        if (longestMatchEmojis == null) return null;
        return new UniqueEmojiFoundResult(longestMatchEmojis.get(0), longestMatchEndIndex);
    }

    @Nullable
    static UniqueEmojiFoundResult findHtmlDecimalEmoji(final String text, final int textIndex, final boolean isHex) {
        final int textLength = text.length();
        if (isHex ? ((textIndex >= textLength - 3) || isInvalidHtmlHexadecimalSequence(text, textIndex)) : textIndex >= textLength - 2 || isInvalidHtmlDecimalSequence(text, textIndex)) {
            return null; // Sequence does not start with "&#x"
        }

        // Value which checks how many numbers have been after &# so it break out,
        // when something like this appears &#123456789123456789;
        int numberSequenceCount = 0;
        int currentIndex = textIndex;
        int lastValidSemicolonIndex = -1;

        int leadingZeros = 0;
        while (numberSequenceCount < PreComputedConstants.MAX_HTML_DECIMAL_SINGLE_EMOJIS_CONCATENATED_LENGTH && currentIndex < (textLength - (isHex ? 3 : 2))) {
            // Ensure each sequence starts with "&#"
            if (isHex ? isInvalidHtmlHexadecimalSequence(text, currentIndex) : isInvalidHtmlDecimalSequence(text, currentIndex)) {
                break;
            }

            currentIndex += (isHex ? 3 : 2); // Skip "&#x"

            int digitCount = 0;
            boolean isLeadingZero = true;
            while (digitCount < (MAX_LENGTH_HTML_DECIMAL_NUMBER_COUNT + leadingZeros) && currentIndex < textLength
                    && (isHex ? isValidHexadecimalCharacter(text.charAt(currentIndex)) : isValidDecimalCharacter(text.charAt(currentIndex)))) {
                if (isLeadingZero && text.charAt(currentIndex + leadingZeros) == '0') {
                    leadingZeros++;
                    continue;
                } else {
                    isLeadingZero = false;
                }
                digitCount++;
                currentIndex++;
            }

            // Validates the sequence ends with a semicolon
            if (digitCount == 0 || currentIndex >= textLength || text.charAt(currentIndex) != ';') {
                break;
            }

            currentIndex++; // Move past the semicolon
            lastValidSemicolonIndex = currentIndex;
            numberSequenceCount++;
        }

        // No valid HTML character entity found
        if (lastValidSemicolonIndex == -1) {
            return null;
        }

        final StringBuilder htmlEmoji = new StringBuilder(text.substring(textIndex, lastValidSemicolonIndex).toUpperCase());
        while (!htmlEmoji.isEmpty()) {
            final String htmlEmojiString = htmlEmoji.toString();
            String formattedHtmlCharacterEntity = leadingZeros != 0 ? removeLeadingZerosFromHtmlCharacterEntity(htmlEmojiString, isHex) : htmlEmojiString;

            final Emoji emoji = isHex ? EMOJI_HTML_HEXADECIMAL_REPRESENTATION_TO_EMOJI.get(formattedHtmlCharacterEntity) : EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI.get(formattedHtmlCharacterEntity);
            if (emoji != null) {
                return new UniqueEmojiFoundResult(emoji, textIndex + htmlEmojiString.length());
            }
            htmlEmoji.delete(htmlEmoji.lastIndexOf("&"), htmlEmojiString.length());
        }

        return null;
    }

    @Nullable
    static UniqueEmojiFoundResult findUrlEncodedEmoji(final String text, final int textIndex) {
        final int textLength = text.length();
        if ((textIndex + PreComputedConstants.MINIMUM_EMOJI_URL_ENCODED_LENGTH >= textLength) || !PreComputedConstants.POSSIBLE_EMOJI_URL_ENCODED_STARTER_CODEPOINTS.contains((int) text.charAt(textIndex))) {
            return null;
        }

//      %
//      2%EF%B8%8F%E2%83%A3
//      Add 1 if the character is no a % as it then proceeds to repeat %12%34 ...
        int currentIndex = textIndex;
        if (text.charAt(textIndex) != '%') {
            currentIndex++;
        }

        while ((currentIndex - textIndex) < PreComputedConstants.MAXIMUM_EMOJI_URL_ENCODED_LENGTH && (currentIndex + 1) <= textLength) {
            // Break out when it does not start with a '%', should always be true on the first run
            if (text.charAt(currentIndex) != '%') {
                break;
            }
            // Skip the %
            currentIndex++;

            if (currentIndex + 2 <= textLength) {
                if (PreComputedConstants.ALLOWED_EMOJI_URL_ENCODED_SEQUENCES.contains(text.substring(currentIndex, currentIndex + 2))) {
                    currentIndex = currentIndex + 2;
                } else if (PreComputedConstants.ALLOWED_EMOJI_URL_ENCODED_SEQUENCES.contains(text.substring(currentIndex, currentIndex + 1))) {
                    currentIndex = currentIndex + 1;
                }
            } else if (currentIndex + 1 <= textLength) {
                if (PreComputedConstants.ALLOWED_EMOJI_URL_ENCODED_SEQUENCES.contains(text.substring(currentIndex, currentIndex + 1))) {
                    currentIndex = currentIndex + 1;
                }
            } else {
                break;
            }
        }

        final StringBuilder urlEncodedEmoji = new StringBuilder(text.substring(textIndex, currentIndex).toUpperCase());
        while (urlEncodedEmoji.indexOf("%") != -1) {
            final Emoji emoji = EMOJI_URL_ENCODED_REPRESENTATION_TO_EMOJI.get(urlEncodedEmoji.toString());
            if (emoji != null) {
                return new UniqueEmojiFoundResult(emoji, textIndex + urlEncodedEmoji.length());
            }
            urlEncodedEmoji.delete(urlEncodedEmoji.lastIndexOf("%"), urlEncodedEmoji.length());
        }

        return null;
    }

    private static String removeLeadingZerosFromHtmlCharacterEntity(final String str, final boolean isHex) {
        final StringBuilder sb = new StringBuilder(str);
        int i = 0;
        while (i < sb.length()) {
            if (sb.charAt(i) == '&' && i + 1 < sb.length() && sb.charAt(i + 1) == '#') {
                final int start = i + (isHex ? 3 : 2);
                int end = start;

                // Skip leading zeros
                while (end < sb.length() && sb.charAt(end) == '0') {
                    end++;
                }

                if (end < sb.length() &&
                        ((isHex && isValidHexadecimalCharacter(sb.charAt(end))) || (!isHex && isValidDecimalCharacter(sb.charAt(end))))) {
                    // Entferne führende Nullen
                    sb.delete(start, end);
                }
            }

            i++;
        }
        return sb.toString();
    }

    /**
     * Checks whether this is a valid decimal emoji starting sequence.
     *
     * @param textCodePointsArray The textCodePointsArray to check if it starts with a decimal emoji sequence.
     * @param currentIndex        The current index.
     * @return Whether the textCodePointsArray at textIndex start if a valid decimal emoji sequence.
     */
    private static boolean isInvalidHtmlHexadecimalSequence(final String text, final int currentIndex) {
        return isInvalidHtmlDecimalSequence(text, currentIndex) || (text.charAt(currentIndex + 2) != 'x' && text.charAt(currentIndex + 2) != 'X');
    }

    /**
     * Checks whether this is a valid hexadecimal emoji starting sequence.
     *
     * @param textCodePointsArray The textCodePointsArray to check if it starts with a hexadecimal emoji sequence.
     * @param currentIndex        The current index.
     * @return Whether the textCodePointsArray at textIndex start if a valid hexadecimal emoji sequence.
     */
    private static boolean isInvalidHtmlDecimalSequence(final String text, final int currentIndex) {
        return text.charAt(currentIndex) != '&' || text.charAt(currentIndex + 1) != '#';
    }

    /**
     * Checks whether the character is a valid hexadecimal number of the base of 16.
     *
     * @param character The character to check.
     * @return Whether the character is a valid base 16 number.
     */
    private static boolean isValidHexadecimalCharacter(final int character) {
        return Character.digit(character, 16) != -1;
    }

    /**
     * Checks whether the character is a valid decimal number of the base of 10.
     *
     * @param character The character to check.
     * @return Whether the character is a valid base 10 number.
     */
    private static boolean isValidDecimalCharacter(final int character) {
        return Character.digit(character, 10) != -1;
    }

    /**
     * Finds a unique emoji starting at the char index {@code charIndex} of the text.
     *
     * @param text      The text to check if it contains an emoji.
     * @param charIndex The char index to check, must be at the start of a codepoint.
     * @param codepoint The codepoint at {@code charIndex}.
     * @param type      The {@link EmojiType} to check the codepoint against.
     * @return The found emoji with its end char index (exclusive), otherwise {@code null}.
     */
    @Nullable
    public static UniqueEmojiFoundResult findUniqueEmoji(final String text, final int charIndex, final int codepoint, final EmojiType type) {
        return switch (type) {
            case UNICODE -> findUnicodeEmoji(text, charIndex, codepoint);
            case HTML_DECIMAL -> codepoint == '&' ? findHtmlDecimalEmoji(text, charIndex, false) : null;
            case HTML_HEXADECIMAL -> codepoint == '&' ? findHtmlDecimalEmoji(text, charIndex, true) : null;
            case URL_ENCODED -> findUrlEncodedEmoji(text, charIndex);
        };
    }

    /**
     * Finds the longest alias starting at the char index {@code charIndex} of the text.
     *
     * @param text      The text to check if it contains an alias.
     * @param charIndex The char index to check, must be at the start of a codepoint.
     * @return The found emojis with the end char index (exclusive) of the alias, otherwise {@code null}.
     */
    @Nullable
    public static NonUniqueEmojiFoundResult findNonUniqueEmoji(final String text, final int charIndex) {
        // Walk the alias trie, remembering the longest alias that ended along the way.
        // The walk stops as soon as no alias shares the current prefix.
        CodepointTrie node = ALIAS_TRIE;
        List<Emoji> longestMatchEmojis = null;
        int longestMatchEndIndex = -1;

        final int textLength = text.length();
        for (int currentIndex = charIndex; currentIndex < textLength; ) {
            final int codepoint = text.codePointAt(currentIndex);
            node = node.child(codepoint);
            if (node == null) break;
            currentIndex += Character.charCount(codepoint);

            final List<Emoji> emojis = node.getEmojis();
            if (emojis != null) {
                longestMatchEmojis = emojis;
                longestMatchEndIndex = currentIndex;
            }
        }

        if (longestMatchEmojis == null) return null;
        return new NonUniqueEmojiFoundResult(longestMatchEmojis, longestMatchEndIndex);
    }

    /**
     * Checks if the codepoint is a valid starter character for any {@link EmojiType}.
     * This avoids running unnecessary for loops which take ~1ms longer.
     *
     * @param currentCodepoint The codepoint to check.
     * @return Whether the codepoint is a valid starter.
     */
    public static boolean checkIfCodepointIsInvalidEmojiStarter(final int currentCodepoint) {
        return isNotInBitset(EMOJI_STARTER_BITS, currentCodepoint);
    }

    /**
     * Checks if the codepoint cannot be the start of an alias.
     *
     * @param currentCodepoint The codepoint to check.
     * @return Whether no alias starts with the codepoint.
     */
    public static boolean checkIfCodepointIsInvalidAliasStarter(final int currentCodepoint) {
        return isNotInBitset(ALIAS_STARTER_BITS, currentCodepoint);
    }

    private static boolean isNotInBitset(final long[] bits, final int codepoint) {
        final int word = codepoint >>> 6;
        return word >= bits.length || (bits[word] & (1L << codepoint)) == 0;
    }
}

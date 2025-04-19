package net.fellbaum.jemoji;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.fellbaum.jemoji.EmojiManager.*;

final class InternalEmojiUtils {

    private InternalEmojiUtils() {
    }

    public static final char TEXT_VARIATION_CHARACTER = '\uFE0E';
    public static final char EMOJI_VARIATION_CHARACTER = '\uFE0F';
    private static final int MAX_LENGTH_HTML_DECIMAL_NUMBER_COUNT = 6;

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

    public static Optional<List<Emoji>> findEmojiByEitherAlias(final Map<InternalCodepointSequence, List<Emoji>> map, final String alias) {
        final List<Emoji> firstValue = map.get(new InternalCodepointSequence(addColonToAlias(alias)));
        if (firstValue != null) return Optional.of(firstValue);
        final List<Emoji> secondValue = map.get(new InternalCodepointSequence(removeColonFromAlias(alias)));
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

    @Nullable
    static UniqueEmojiFoundResult findUnicodeEmoji(final int[] textCodePointsArray, final long textCodePointsLength, final int textIndex) {
        //noinspection DataFlowIssue
        final List<Emoji> emojisByCodePoint = EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.get(textCodePointsArray[textIndex]);
        if (emojisByCodePoint == null) return null;
        for (final Emoji emoji : emojisByCodePoint) {
            final int[] emojiCodePointsArray = stringToCodePoints(emoji.getEmoji());
            final int emojiCodePointsLength = emojiCodePointsArray.length;
            // Check if Emoji code points are in bounds of the text code points
            if (!((textIndex + emojiCodePointsLength) <= textCodePointsLength)) {
                continue;
            }

            for (int emojiCodePointIndex = 0; emojiCodePointIndex < emojiCodePointsLength; emojiCodePointIndex++) {
                //break out because the emoji is not the same
                if (textCodePointsArray[textIndex + emojiCodePointIndex] != emojiCodePointsArray[emojiCodePointIndex]) {
                    break;
                }

                if (emojiCodePointIndex == (emojiCodePointsLength - 1)) {
                    return new UniqueEmojiFoundResult(emoji, textIndex + emojiCodePointsLength);
                }
            }
        }

        return null;
    }

    @Nullable
    static UniqueEmojiFoundResult findHtmlDecimalEmoji(final int[] textCodePointsArray, final long textCodePointsLength, final int textIndex, final boolean isHex) {
        if (isHex ? ((textIndex >= textCodePointsLength - 3) || isInvalidHtmlHexadecimalSequence(textCodePointsArray, textIndex)) : textIndex >= textCodePointsLength - 2 || isInvalidHtmlDecimalSequence(textCodePointsArray, textIndex)) {
            return null; // Sequence does not start with "&#x"
        }

        // Value which checks how many numbers have been after &# so it break out,
        // when something like this appears &#123456789123456789;
        int numberSequenceCount = 0;
        int currentIndex = textIndex;
        int lastValidSemicolonIndex = -1;
        final StringBuilder sequenceBuilder = new StringBuilder();

        int leadingZeros = 0;
        while (numberSequenceCount < PreComputedConstants.MAX_HTML_DECIMAL_SINGLE_EMOJIS_CONCATENATED_LENGTH && currentIndex < (textCodePointsLength - (isHex ? 3 : 2))) {
            // Ensure each sequence starts with "&#"
            if (isHex ? isInvalidHtmlHexadecimalSequence(textCodePointsArray, currentIndex) : isInvalidHtmlDecimalSequence(textCodePointsArray, currentIndex)) {
                break;
            }

            currentIndex += (isHex ? 3 : 2); // Skip "&#x"

            int digitCount = 0;
            boolean isLeadingZero = true;
            while (digitCount < (MAX_LENGTH_HTML_DECIMAL_NUMBER_COUNT + leadingZeros) && currentIndex < textCodePointsLength
                    && (isHex ? isValidHexadecimalCharacter(textCodePointsArray[currentIndex]) : isValidDecimalCharacter(textCodePointsArray[currentIndex]))) {
                if (isLeadingZero && textCodePointsArray[currentIndex + leadingZeros] == '0') {
                    leadingZeros++;
                    continue;
                } else {
                    isLeadingZero = false;
                }
                digitCount++;
                currentIndex++;
            }

            // Validate the sequence ends with a semicolon
            if (digitCount == 0 || currentIndex >= textCodePointsLength || textCodePointsArray[currentIndex] != ';') {
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

        final StringBuilder htmlEmoji = new StringBuilder(new String(textCodePointsArray, textIndex, lastValidSemicolonIndex - textIndex).toUpperCase());
        while (htmlEmoji.length() != 0) {
            final String htmlEmojiString = htmlEmoji.toString();
            String formattedHtmlCharacterEntity = leadingZeros != 0 ? removeLeadingZerosFromHtmlCharacterEntity(htmlEmojiString, isHex) : htmlEmojiString;

            //noinspection DataFlowIssue
            final Emoji emoji = isHex ? EMOJI_HTML_HEXADECIMAL_REPRESENTATION_TO_EMOJI.get(formattedHtmlCharacterEntity) : EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI.get(formattedHtmlCharacterEntity);
            if (emoji != null) {
                return new UniqueEmojiFoundResult(emoji, textIndex + htmlEmojiString.length());
            }
            htmlEmoji.delete(htmlEmoji.lastIndexOf("&"), htmlEmojiString.length());
        }

        return null;
    }

    @Nullable
    static UniqueEmojiFoundResult findUrlEncodedEmoji(final int[] textCodePointsArray, final long textCodePointsLength, final int textIndex) {
        if ((textIndex + PreComputedConstants.MINIMUM_EMOJI_URL_ENCODED_LENGTH >= textCodePointsLength) || !PreComputedConstants.POSSIBLE_EMOJI_URL_ENCODED_STARTER_CODEPOINTS.contains(textCodePointsArray[textIndex])) {
            return null;
        }

//      %
//      2%EF%B8%8F%E2%83%A3
//      Add 1 if the character is no a % as it then proceeds to repeat %12%34 ...
        int currentIndex = textIndex;
        if (textCodePointsArray[textIndex] != '%') {
            currentIndex++;
        }

        while ((currentIndex - textIndex) < PreComputedConstants.MAXIMUM_EMOJI_URL_ENCODED_LENGTH && (currentIndex + 1) <= textCodePointsLength) {
            // Break out when it does not start with a '%', should always be true on the first run
            if (textCodePointsArray[currentIndex] != '%') {
                break;
            }
            // Skip the %
            currentIndex++;

            if (currentIndex + 2 <= textCodePointsLength) {
                if (PreComputedConstants.ALLOWED_EMOJI_URL_ENCODED_SEQUENCES.contains(new String(textCodePointsArray, currentIndex, 2))) {
                    currentIndex = currentIndex + 2;
                } else if (PreComputedConstants.ALLOWED_EMOJI_URL_ENCODED_SEQUENCES.contains(new String(textCodePointsArray, currentIndex, 1))) {
                    currentIndex = currentIndex + 1;
                }
            } else if (currentIndex + 1 <= textCodePointsLength) {
                if (PreComputedConstants.ALLOWED_EMOJI_URL_ENCODED_SEQUENCES.contains(new String(textCodePointsArray, currentIndex, 1))) {
                    currentIndex = currentIndex + 1;
                }
            } else {
                break;
            }
        }

        final StringBuilder urlEncodedEmoji = new StringBuilder(new String(textCodePointsArray, textIndex, currentIndex - textIndex).toUpperCase());
        while (urlEncodedEmoji.toString().contains("%")) {
            //noinspection DataFlowIssue
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
    private static boolean isInvalidHtmlHexadecimalSequence(final int[] textCodePointsArray, final int currentIndex) {
        return isInvalidHtmlDecimalSequence(textCodePointsArray, currentIndex) || (textCodePointsArray[currentIndex + 2] != 'x' && textCodePointsArray[currentIndex + 2] != 'X');
    }

    /**
     * Checks whether this is a valid hexadecimal emoji starting sequence.
     *
     * @param textCodePointsArray The textCodePointsArray to check if it starts with a hexadecimal emoji sequence.
     * @param currentIndex        The current index.
     * @return Whether the textCodePointsArray at textIndex start if a valid hexadecimal emoji sequence.
     */
    private static boolean isInvalidHtmlDecimalSequence(final int[] textCodePointsArray, final int currentIndex) {
        return textCodePointsArray[currentIndex] != '&' || textCodePointsArray[currentIndex + 1] != '#';
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
     * Finds a unique emoji starting at the position textIndex in the textCodePointsArray.
     *
     * @param textCodePointsArray  The textCodePointsArray to check if it contains an emoji.
     * @param textIndex            The current text index.
     * @param textCodePointsLength The length of the textCodePointsArray.
     * @param type                 The {@link EmojiType} to check the codepoint against.
     * @return The found emoji, otherwise {@code null}.
     */
    @Nullable
    public static UniqueEmojiFoundResult findUniqueEmoji(final int[] textCodePointsArray, final int textIndex, final long textCodePointsLength, final EmojiType type) {
        switch (type) {
            case UNICODE: {
                return findUnicodeEmoji(textCodePointsArray, textCodePointsLength, textIndex);
            }
            case HTML_DECIMAL: {
                return findHtmlDecimalEmoji(textCodePointsArray, textCodePointsLength, textIndex, false);
            }
            case HTML_HEXADECIMAL: {
                return findHtmlDecimalEmoji(textCodePointsArray, textCodePointsLength, textIndex, true);
            }
            case URL_ENCODED: {
                return findUrlEncodedEmoji(textCodePointsArray, textCodePointsLength, textIndex);
            }
            default: {
                throw new IllegalArgumentException("Unknown EmojiType: " + type);
            }
        }
    }

    /**
     * Finds a unique emoji starting at the position textIndex in the textCodePointsArray.
     *
     * @param textCodePointsArray  The textCodePointsArray to check if it contains an emoji.
     * @param textIndex            The current text index.
     * @param textCodePointsLength The length of the textCodePointsArray.
     * @return The found emoji, otherwise {@code null}.
     */
    @Nullable
    public static NonUniqueEmojiFoundResult findNonUniqueEmoji(final int[] textCodePointsArray, final int textIndex, final long textCodePointsLength) {
        return findAliasEmoji(textCodePointsArray, textCodePointsLength, textIndex);
    }

    @Nullable
    static NonUniqueEmojiFoundResult findAliasEmoji(final int[] textCodePointsArray, final long textCodePointsLength, final int textIndex) {
        if (!PreComputedConstants.POSSIBLE_EMOJI_ALIAS_STARTER_CODEPOINTS.contains(textCodePointsArray[textIndex])) return null;

        InternalCodepointSequence lastKnownCodepointSequence = null;
        for (int aliasCodePointIndex = 0; aliasCodePointIndex < PreComputedConstants.ALIAS_EMOJI_MAX_LENGTH && (aliasCodePointIndex + textIndex) <= textCodePointsLength; aliasCodePointIndex++) {
            final InternalCodepointSequence tempCodepointSequence = new InternalCodepointSequence(Arrays.copyOfRange(textCodePointsArray, textIndex, textIndex + aliasCodePointIndex));
            //noinspection DataFlowIssue
            if (ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.containsKey(tempCodepointSequence)) {
                lastKnownCodepointSequence = tempCodepointSequence;
            }
        }

        if (lastKnownCodepointSequence != null) {
            return new NonUniqueEmojiFoundResult(ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.get(lastKnownCodepointSequence), textIndex + lastKnownCodepointSequence.getCodepoints().length, lastKnownCodepointSequence);
        }

        return null;
    }

    /**
     * Checks if the codepoint is a valid starter character for any {@link EmojiType}.
     * This avoids running unnecessary for loops which take ~1ms longer.
     *
     * @param currentCodepoint The codepoint to check.
     * @return Whether the codepoint is a valid starter.
     */
    public static boolean checkIfCodepointIsInvalidEmojiStarter(final int currentCodepoint) {
        //noinspection DataFlowIssue
        return EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.get(currentCodepoint) == null && currentCodepoint != '&' && currentCodepoint != '%';
    }
}

final class UniqueEmojiFoundResult {

    private final Emoji emoji;
    private final int endIndex;

    public UniqueEmojiFoundResult(Emoji emoji, int endIndex) {
        this.emoji = emoji;
        this.endIndex = endIndex;
    }

    public Emoji getEmoji() {
        return emoji;
    }

    public int getEndIndex() {
        return endIndex;
    }

    @Override
    public String toString() {
        return "UniqueEmojiFoundResult{" +
                "emoji=" + emoji +
                ", endIndex=" + endIndex +
                '}';
    }
}

final class NonUniqueEmojiFoundResult {

    private final List<Emoji> emojis;
    private final int endIndex;
    private final InternalCodepointSequence codepointSequence;

    public NonUniqueEmojiFoundResult(List<Emoji> emojis, int endIndex, InternalCodepointSequence codepointSequence) {
        this.emojis = emojis;
        this.endIndex = endIndex;
        this.codepointSequence = codepointSequence;
    }

    public List<Emoji> getEmojis() {
        return emojis;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public InternalCodepointSequence getCodepointSequence() {
        return codepointSequence;
    }
}

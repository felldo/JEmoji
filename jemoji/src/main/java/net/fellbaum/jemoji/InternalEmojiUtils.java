package net.fellbaum.jemoji;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.fellbaum.jemoji.EmojiManager.*;

final class InternalEmojiUtils {

    private InternalEmojiUtils() {
    }

    public static final char TEXT_VARIATION_CHARACTER = '\uFE0E';
    public static final char EMOJI_VARIATION_CHARACTER = '\uFE0F';

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

    public static <K, V> Optional<V> findEmojiByEitherAlias(final Map<K, V> map, final K aliasWithColon, final K aliasWithoutColon) {
        final V firstValue = map.get(aliasWithColon);
        if (firstValue != null) return Optional.of(firstValue);
        final V secondValue = map.get(aliasWithoutColon);
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
    static EmojiFindResult findUnicodeEmoji(final int[] textCodePointsArray, final long textCodePointsLength, final int textIndex) {
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
                    return new EmojiFindResult(emoji, textIndex + emojiCodePointsLength);
                }
            }
        }

        return null;
    }

    private static final int MAX_LENGTH_HTML_DECIMAL_NUMBER_COUNT = 6;

    @Nullable
    static EmojiFindResult findHtmlDecimalEmoji(final int[] textCodePointsArray, final long textCodePointsLength, final int textIndex, final boolean isHex) {
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
        while (numberSequenceCount < MAX_HTML_DECIMAL_SINGLE_EMOJIS_CONCATENATED_LENGTH && currentIndex < (textCodePointsLength - (isHex ? 3 : 2))) {
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

            final Emoji emoji = isHex ? EMOJI_HTML_HEXADECIMAL_REPRESENTATION_TO_EMOJI.get(formattedHtmlCharacterEntity) : EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI.get(formattedHtmlCharacterEntity);
            if (emoji != null) {
                return new EmojiFindResult(emoji, textIndex + htmlEmojiString.length());
            }
            htmlEmoji.delete(htmlEmoji.lastIndexOf("&"), htmlEmojiString.length());
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

    private static boolean isInvalidHtmlHexadecimalSequence(final int[] textCodePointsArray, final int currentIndex) {
        return isInvalidHtmlDecimalSequence(textCodePointsArray, currentIndex) || (textCodePointsArray[currentIndex + 2] != 'x' && textCodePointsArray[currentIndex + 2] != 'X');
    }

    private static boolean isInvalidHtmlDecimalSequence(final int[] textCodePointsArray, final int currentIndex) {
        return textCodePointsArray[currentIndex] != '&' || textCodePointsArray[currentIndex + 1] != '#';
    }

    private static boolean isValidHexadecimalCharacter(final int character) {
        return Character.digit(character, 16) != -1;
    }

    private static boolean isValidDecimalCharacter(final int character) {
        return Character.digit(character, 10) != -1;
    }
}

class EmojiFindResult {

    private final Emoji emoji;
    private final int endIndex;

    public EmojiFindResult(Emoji emoji, int endIndex) {
        this.emoji = emoji;
        this.endIndex = endIndex;
    }

    public Emoji getEmoji() {
        return emoji;
    }

    public int getEndIndex() {
        return endIndex;
    }

}

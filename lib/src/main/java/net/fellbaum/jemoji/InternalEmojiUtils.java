package net.fellbaum.jemoji;

import java.util.Map;
import java.util.Optional;

class InternalEmojiUtils {

    private InternalEmojiUtils() {
    }

    public static final char TEXT_VARIATION_CHARACTER = '\uFE0E';
    public static final char EMOJI_VARIATION_CHARACTER = '\uFE0F';

    public static int getCodePointCount(final String string) {
        return string.codePointCount(0, string.length());
    }

    public static boolean isStringNullOrEmpty(final String string) {
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
}

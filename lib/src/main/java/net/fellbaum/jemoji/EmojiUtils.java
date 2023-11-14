package net.fellbaum.jemoji;

import java.util.Map;
import java.util.Optional;

class EmojiUtils {

    public static int getCodePointCount(String string) {
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
}

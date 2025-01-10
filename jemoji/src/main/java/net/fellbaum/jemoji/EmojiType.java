package net.fellbaum.jemoji;

import java.util.EnumSet;

public enum EmojiType {

    HTML_DECIMAL,
    HTML_HEXADECIMAL,
    UNICODE,
    ALIAS;

    public static EnumSet<EmojiType> allExceptAliases() {
        return EnumSet.complementOf(EnumSet.of(ALIAS));
    }

}


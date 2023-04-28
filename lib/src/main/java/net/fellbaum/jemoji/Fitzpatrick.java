package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Fitzpatrick {

    LIGHT_SKIN("🏻"),
    MEDIUM_LIGHT_SKIN("🏼"),
    MEDIUM_SKIN("🏽"),
    MEDIUM_DARK_SKIN("🏾"),
    DARK_SKIN("🏿");

    private static final List<Fitzpatrick> FITZPATRICK_LIST = Arrays.asList(values());
    private final String unicode;

    Fitzpatrick(String unicode) {
        this.unicode = unicode;
    }

    public String getUnicode() {
        return unicode;
    }

    public static boolean isFitzpatrickEmoji(String unicode) {
        return Arrays.stream(values()).anyMatch(fitzpatrick -> unicode.contains(fitzpatrick.unicode) && !unicode.equals(fitzpatrick.unicode));
    }

    public static String removeFitzpatrick(String unicode) {
        for (Fitzpatrick value : values()) {
            unicode = unicode.replaceAll(value.getUnicode(), "");
        }
        return unicode;
    }
}

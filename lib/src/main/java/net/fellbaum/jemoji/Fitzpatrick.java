package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.List;

public enum Fitzpatrick {

    LIGHT_SKIN("\uD83C\uDFFB"),
    MEDIUM_LIGHT_SKIN("\uD83C\uDFFC"),
    MEDIUM_SKIN("\uD83C\uDFFD"),
    MEDIUM_DARK_SKIN("\uD83C\uDFFE"),
    DARK_SKIN("\uD83C\uDFFF");

    private static final List<Fitzpatrick> FITZPATRICK_LIST = Arrays.asList(values());
    private final String unicode;

    Fitzpatrick(String unicode) {
        this.unicode = unicode;
    }

    public String getUnicode() {
        return unicode;
    }

    public static boolean isFitzpatrickEmoji(String unicode) {
        return FITZPATRICK_LIST.stream().anyMatch(fitzpatrick -> unicode.contains(fitzpatrick.unicode) && !unicode.equals(fitzpatrick.unicode));
    }

    public static String removeFitzpatrick(String unicode) {
        for (Fitzpatrick value : FITZPATRICK_LIST) {
            unicode = unicode.replaceAll(value.getUnicode(), "");
        }
        return unicode;
    }
}

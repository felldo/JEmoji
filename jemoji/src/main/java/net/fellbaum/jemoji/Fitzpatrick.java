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

    Fitzpatrick(final String unicode) {
        this.unicode = unicode;
    }

    /**
     * Gets the Unicode of the fitzpatrick modifier.
     *
     * @return The Unicode of the fitzpatrick modifier.
     */
    public String getUnicode() {
        return unicode;
    }

    /**
     * Check if the given emoji contains a fitzpatrick modifier.
     *
     * @param unicode The Unicode of the emoji.
     * @return True if the emoji contains a fitzpatrick modifier.
     */
    public static boolean isFitzpatrickEmoji(final String unicode) {
        return FITZPATRICK_LIST.stream().anyMatch(fitzpatrick -> unicode.contains(fitzpatrick.unicode) && !unicode.equals(fitzpatrick.unicode));
    }

    /**
     * Removes the fitzpatrick modifier from the given emoji.
     *
     * @param unicode The Unicode of the emoji.
     * @return The Unicode of the emoji without the fitzpatrick modifier.
     */
    public static String removeFitzpatrick(String unicode) {
        for (Fitzpatrick value : FITZPATRICK_LIST) {
            unicode = unicode.replaceAll("\u200D?" + value.getUnicode(), "");
        }
        return unicode;
    }
}

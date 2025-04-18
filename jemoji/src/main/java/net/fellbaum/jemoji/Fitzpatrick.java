package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.List;

/**
 * The Fitzpatrick enum represents skin tone modifiers in the Fitzpatrick scale,
 * often used in conjunction with emojis to provide a range of skin tone
 * variations.
 */
public enum Fitzpatrick {

    /**
     * Represents the light skin tone modifier in the Fitzpatrick scale.
     */
    LIGHT_SKIN("\uD83C\uDFFB"),
    /**
     * Represents the medium light skin tone modifier in the Fitzpatrick scale.
     */
    MEDIUM_LIGHT_SKIN("\uD83C\uDFFC"),
    /**
     * Represents the medium skin tone modifier in the Fitzpatrick scale.
     */
    MEDIUM_SKIN("\uD83C\uDFFD"),
    /**
     * Represents the medium dark skin tone modifier in the Fitzpatrick scale.
     */
    MEDIUM_DARK_SKIN("\uD83C\uDFFE"),
    /**
     * Represents the dark skin tone modifier in the Fitzpatrick scale.
     */
    DARK_SKIN("\uD83C\uDFFF");

    private static final List<Fitzpatrick> FITZPATRICK_LIST = Arrays.asList(values());
    private final String unicode;

    /**
     * Constructs a Fitzpatrick modifier with the specified Unicode value.
     *
     * @param unicode The Unicode string representing the Fitzpatrick modifier.
     */
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

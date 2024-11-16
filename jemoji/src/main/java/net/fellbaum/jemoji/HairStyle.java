package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.List;

public enum HairStyle {

    RED_HAIR("🦰"),
    CURLY_HAIR("🦱"),
    WHITE_HAIR("🦳"),
    BALD("🦲");

    private static final List<HairStyle> HAIR_STYLE_LIST = Arrays.asList(values());
    private final String unicode;

    HairStyle(final String unicode) {
        this.unicode = unicode;
    }

    /**
     * Gets the Unicode of the hairstyle.
     *
     * @return The Unicode of the hairstyle.
     */
    public String getUnicode() {
        return unicode;
    }

    /**
     * Check if the given emoji contains a hairstyle element.
     *
     * @param unicode The Unicode of the emoji.
     * @return True if the emoji contains a hairstyle element.
     */
    public static boolean isHairStyleEmoji(final String unicode) {
        return HAIR_STYLE_LIST.stream().anyMatch(hairStyle -> unicode.contains(hairStyle.unicode) && !unicode.equals(hairStyle.unicode));
    }

    /**
     * Removes the hairstyle element from the given emoji.
     *
     * @param unicode The Unicode of the emoji.
     * @return The Unicode of the emoji without the hairstyle element.
     */
    public static String removeHairStyle(String unicode) {
        for (HairStyle value : HAIR_STYLE_LIST) {
            unicode = unicode.replaceAll("\u200D?" + value.getUnicode(), "");
        }
        return unicode;
    }

}

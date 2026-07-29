package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.List;

import static net.fellbaum.jemoji.internal.EmojiUtils.removeModifier;

/**
 * An enumeration representing different types of hairstyles, each associated with a Unicode character.
 */
public enum HairStyle {

    /**
     * Represents red hair as a hairstyle, associated with the Unicode character "🦰".
     */
    RED_HAIR("🦰"),
    /**
     * Represents a curly hairstyle, associated with the Unicode character '🦱'.
     */
    CURLY_HAIR("🦱"),
    /**
     * Represents red hair as a hairstyle, associated with the Unicode character "🦳".
     */
    WHITE_HAIR("🦳"),
    /**
     * Represents the "Bald" hairstyle, associated with the Unicode character 🦲.
     */
    BALD("🦲");

    private static final List<HairStyle> HAIR_STYLE_LIST = Arrays.asList(values());
    private final String unicode;

    /**
     * Constructor for the HairStyle enum, which associates each hairstyle type with its corresponding Unicode.
     *
     * @param unicode The Unicode representation of the hairstyle.
     */
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
            unicode = removeModifier(unicode, value.unicode);
        }
        return unicode;
    }

}

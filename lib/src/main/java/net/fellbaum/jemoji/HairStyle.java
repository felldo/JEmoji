package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum HairStyle {

    RED_HAIR("🦰"),
    CURLY_HAIR("🦱"),
    WHITE_HAIR("🦳"),
    BALD("🦲");

    private static final List<HairStyle> HAIR_STYLE_LIST = Arrays.asList(values());
    private final String unicode;

    HairStyle(String unicode) {
        this.unicode = unicode;
    }

    public String getUnicode() {
        return unicode;
    }

    public static boolean isHairStyleEmoji(String unicode) {
        return Arrays.stream(values()).anyMatch(hairStyle -> unicode.contains(hairStyle.unicode) && !unicode.equals(hairStyle.unicode));
    }

    public static String removeHairStyle(String unicode) {
        for (HairStyle value : values()) {
            unicode = unicode.replaceAll(value.getUnicode(), "");
        }
        return unicode;
    }

}

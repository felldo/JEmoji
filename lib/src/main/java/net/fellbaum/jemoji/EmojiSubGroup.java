package net.fellbaum.jemoji;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.List;

/**
 * Represents an emoji sub group.
 */
public enum EmojiSubGroup {

    ALPHANUM("alphanum"),
    ANIMAL_AMPHIBIAN("animal-amphibian"),
    ANIMAL_BIRD("animal-bird"),
    ANIMAL_BUG("animal-bug"),
    ANIMAL_MAMMAL("animal-mammal"),
    ANIMAL_MARINE("animal-marine"),
    ANIMAL_REPTILE("animal-reptile"),
    ARROW("arrow"),
    ARTS_CRAFTS("arts & crafts"),
    AV_SYMBOL("av-symbol"),
    AWARD_MEDAL("award-medal"),
    BODY_PARTS("body-parts"),
    BOOK_PAPER("book-paper"),
    CAT_FACE("cat-face"),
    CLOTHING("clothing"),
    COMPUTER("computer"),
    COUNTRY_FLAG("country-flag"),
    CURRENCY("currency"),
    DISHWARE("dishware"),
    DRINK("drink"),
    EMOTION("emotion"),
    EVENT("event"),
    FACE_AFFECTION("face-affection"),
    FACE_CONCERNED("face-concerned"),
    FACE_COSTUME("face-costume"),
    FACE_GLASSES("face-glasses"),
    FACE_HAND("face-hand"),
    FACE_HAT("face-hat"),
    FACE_NEGATIVE("face-negative"),
    FACE_NEUTRAL_SKEPTICAL("face-neutral-skeptical"),
    FACE_SLEEPY("face-sleepy"),
    FACE_SMILING("face-smiling"),
    FACE_TONGUE("face-tongue"),
    FACE_UNWELL("face-unwell"),
    FAMILY("family"),
    FLAG("flag"),
    FOOD_ASIAN("food-asian"),
    FOOD_FRUIT("food-fruit"),
    FOOD_MARINE("food-marine"),
    FOOD_PREPARED("food-prepared"),
    FOOD_SWEET("food-sweet"),
    FOOD_VEGETABLE("food-vegetable"),
    GAME("game"),
    GENDER("gender"),
    GEOMETRIC("geometric"),
    HAIR_STYLE("hair-style"),
    HANDS("hands"),
    HAND_FINGERS_CLOSED("hand-fingers-closed"),
    HAND_FINGERS_OPEN("hand-fingers-open"),
    HAND_FINGERS_PARTIAL("hand-fingers-partial"),
    HAND_PROP("hand-prop"),
    HAND_SINGLE_FINGER("hand-single-finger"),
    HEART("heart"),
    HOTEL("hotel"),
    HOUSEHOLD("household"),
    KEYCAP("keycap"),
    LIGHT_VIDEO("light & video"),
    LOCK("lock"),
    MAIL("mail"),
    MATH("math"),
    MEDICAL("medical"),
    MONEY("money"),
    MONKEY_FACE("monkey-face"),
    MUSIC("music"),
    MUSICAL_INSTRUMENT("musical-instrument"),
    OFFICE("office"),
    OTHER_OBJECT("other-object"),
    OTHER_SYMBOL("other-symbol"),
    PERSON("person"),
    PERSON_ACTIVITY("person-activity"),
    PERSON_FANTASY("person-fantasy"),
    PERSON_GESTURE("person-gesture"),
    PERSON_RESTING("person-resting"),
    PERSON_ROLE("person-role"),
    PERSON_SPORT("person-sport"),
    PERSON_SYMBOL("person-symbol"),
    PHONE("phone"),
    PLACE_BUILDING("place-building"),
    PLACE_GEOGRAPHIC("place-geographic"),
    PLACE_MAP("place-map"),
    PLACE_OTHER("place-other"),
    PLACE_RELIGIOUS("place-religious"),
    PLANT_FLOWER("plant-flower"),
    PLANT_OTHER("plant-other"),
    PUNCTUATION("punctuation"),
    RELIGION("religion"),
    SCIENCE("science"),
    SKIN_TONE("skin-tone"),
    SKY_WEATHER("sky & weather"),
    SOUND("sound"),
    SPORT("sport"),
    SUBDIVISION_FLAG("subdivision-flag"),
    TIME("time"),
    TOOL("tool"),
    TRANSPORT_AIR("transport-air"),
    TRANSPORT_GROUND("transport-ground"),
    TRANSPORT_SIGN("transport-sign"),
    TRANSPORT_WATER("transport-water"),
    WARNING("warning"),
    WRITING("writing"),
    ZODIAC("zodiac");

    private static final List<EmojiSubGroup> EMOJI_SUBGROUPS = Arrays.asList(values());
    private final String name;

    EmojiSubGroup(final String name) {
        this.name = name;
    }

    /**
     * Gets the name of the emoji subgroup.
     *
     * @return The name of the emoji subgroup
     */
    public String getName() {
        return name;
    }

    /**
     * Gets all emoji subgroups.
     *
     * @return All emoji subgroups
     */
    public static List<EmojiSubGroup> getSubGroups() {
        return EMOJI_SUBGROUPS;
    }

    /**
     * Gets the emoji subgroup for the given name.
     *
     * @param name The name of the emoji subgroup.
     * @return The emoji subgroup.
     */
    @JsonCreator
    public static EmojiSubGroup fromString(final String name) {
        for (final EmojiSubGroup emojiSubGroup : EMOJI_SUBGROUPS) {
            if (emojiSubGroup.getName().equals(name)) {
                return emojiSubGroup;
            }
        }
        throw new IllegalArgumentException("No EmojiSubGroup found for name: " + name);
    }
}

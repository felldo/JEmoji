package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.List;

/**
 * Represents an emoji sub group.
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public enum EmojiSubGroup {

    ALPHANUM("alphanum", EmojiGroup.SYMBOLS),
    ANIMAL_AMPHIBIAN("animal-amphibian", EmojiGroup.ANIMALS_AND_NATURE),
    ANIMAL_BIRD("animal-bird", EmojiGroup.ANIMALS_AND_NATURE),
    ANIMAL_BUG("animal-bug", EmojiGroup.ANIMALS_AND_NATURE),
    ANIMAL_MAMMAL("animal-mammal", EmojiGroup.ANIMALS_AND_NATURE),
    ANIMAL_MARINE("animal-marine", EmojiGroup.ANIMALS_AND_NATURE),
    ANIMAL_REPTILE("animal-reptile", EmojiGroup.ANIMALS_AND_NATURE),
    ARROW("arrow", EmojiGroup.SYMBOLS),
    ARTS_AND_CRAFTS("arts & crafts", EmojiGroup.ACTIVITIES),
    AV_SYMBOL("av-symbol", EmojiGroup.SYMBOLS),
    AWARD_MEDAL("award-medal", EmojiGroup.ACTIVITIES),
    BODY_PARTS("body-parts", EmojiGroup.PEOPLE_AND_BODY),
    BOOK_PAPER("book-paper", EmojiGroup.OBJECTS),
    CAT_FACE("cat-face", EmojiGroup.SMILEYS_AND_EMOTION),
    CLOTHING("clothing", EmojiGroup.OBJECTS),
    COMPUTER("computer", EmojiGroup.OBJECTS),
    COUNTRY_FLAG("country-flag", EmojiGroup.FLAGS),
    CURRENCY("currency", EmojiGroup.SYMBOLS),
    DISHWARE("dishware", EmojiGroup.FOOD_AND_DRINK),
    DRINK("drink", EmojiGroup.FOOD_AND_DRINK),
    EMOTION("emotion", EmojiGroup.SMILEYS_AND_EMOTION),
    EVENT("event", EmojiGroup.ACTIVITIES),
    FACE_AFFECTION("face-affection", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_CONCERNED("face-concerned", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_COSTUME("face-costume", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_GLASSES("face-glasses", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_HAND("face-hand", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_HAT("face-hat", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_NEGATIVE("face-negative", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_NEUTRAL_SKEPTICAL("face-neutral-skeptical", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_SLEEPY("face-sleepy", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_SMILING("face-smiling", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_TONGUE("face-tongue", EmojiGroup.SMILEYS_AND_EMOTION),
    FACE_UNWELL("face-unwell", EmojiGroup.SMILEYS_AND_EMOTION),
    FAMILY("family", EmojiGroup.PEOPLE_AND_BODY),
    FLAG("flag", EmojiGroup.FLAGS),
    FOOD_ASIAN("food-asian", EmojiGroup.FOOD_AND_DRINK),
    FOOD_FRUIT("food-fruit", EmojiGroup.FOOD_AND_DRINK),
    FOOD_MARINE("food-marine", EmojiGroup.FOOD_AND_DRINK),
    FOOD_PREPARED("food-prepared", EmojiGroup.FOOD_AND_DRINK),
    FOOD_SWEET("food-sweet", EmojiGroup.FOOD_AND_DRINK),
    FOOD_VEGETABLE("food-vegetable", EmojiGroup.FOOD_AND_DRINK),
    GAME("game", EmojiGroup.ACTIVITIES),
    GENDER("gender", EmojiGroup.SYMBOLS),
    GEOMETRIC("geometric", EmojiGroup.SYMBOLS),
    HAIR_STYLE("hair-style", EmojiGroup.COMPONENT),
    HANDS("hands", EmojiGroup.PEOPLE_AND_BODY),
    HAND_FINGERS_CLOSED("hand-fingers-closed", EmojiGroup.PEOPLE_AND_BODY),
    HAND_FINGERS_OPEN("hand-fingers-open", EmojiGroup.PEOPLE_AND_BODY),
    HAND_FINGERS_PARTIAL("hand-fingers-partial", EmojiGroup.PEOPLE_AND_BODY),
    HAND_PROP("hand-prop", EmojiGroup.PEOPLE_AND_BODY),
    HAND_SINGLE_FINGER("hand-single-finger", EmojiGroup.PEOPLE_AND_BODY),
    HEART("heart", EmojiGroup.SMILEYS_AND_EMOTION),
    HOTEL("hotel", EmojiGroup.TRAVEL_AND_PLACES),
    HOUSEHOLD("household", EmojiGroup.OBJECTS),
    KEYCAP("keycap", EmojiGroup.SYMBOLS),
    LIGHT_AND_VIDEO("light & video", EmojiGroup.OBJECTS),
    LOCK("lock", EmojiGroup.OBJECTS),
    MAIL("mail", EmojiGroup.OBJECTS),
    MATH("math", EmojiGroup.SYMBOLS),
    MEDICAL("medical", EmojiGroup.OBJECTS),
    MONEY("money", EmojiGroup.OBJECTS),
    MONKEY_FACE("monkey-face", EmojiGroup.SMILEYS_AND_EMOTION),
    MUSIC("music", EmojiGroup.OBJECTS),
    MUSICAL_INSTRUMENT("musical-instrument", EmojiGroup.OBJECTS),
    OFFICE("office", EmojiGroup.OBJECTS),
    OTHER_OBJECT("other-object", EmojiGroup.OBJECTS),
    OTHER_SYMBOL("other-symbol", EmojiGroup.SYMBOLS),
    PERSON("person", EmojiGroup.PEOPLE_AND_BODY),
    PERSON_ACTIVITY("person-activity", EmojiGroup.PEOPLE_AND_BODY),
    PERSON_FANTASY("person-fantasy", EmojiGroup.PEOPLE_AND_BODY),
    PERSON_GESTURE("person-gesture", EmojiGroup.PEOPLE_AND_BODY),
    PERSON_RESTING("person-resting", EmojiGroup.PEOPLE_AND_BODY),
    PERSON_ROLE("person-role", EmojiGroup.PEOPLE_AND_BODY),
    PERSON_SPORT("person-sport", EmojiGroup.PEOPLE_AND_BODY),
    PERSON_SYMBOL("person-symbol", EmojiGroup.PEOPLE_AND_BODY),
    PHONE("phone", EmojiGroup.OBJECTS),
    PLACE_BUILDING("place-building", EmojiGroup.TRAVEL_AND_PLACES),
    PLACE_GEOGRAPHIC("place-geographic", EmojiGroup.TRAVEL_AND_PLACES),
    PLACE_MAP("place-map", EmojiGroup.TRAVEL_AND_PLACES),
    PLACE_OTHER("place-other", EmojiGroup.TRAVEL_AND_PLACES),
    PLACE_RELIGIOUS("place-religious", EmojiGroup.TRAVEL_AND_PLACES),
    PLANT_FLOWER("plant-flower", EmojiGroup.ANIMALS_AND_NATURE),
    PLANT_OTHER("plant-other", EmojiGroup.ANIMALS_AND_NATURE),
    PUNCTUATION("punctuation", EmojiGroup.SYMBOLS),
    RELIGION("religion", EmojiGroup.SYMBOLS),
    SCIENCE("science", EmojiGroup.OBJECTS),
    SKIN_TONE("skin-tone", EmojiGroup.COMPONENT),
    SKY_AND_WEATHER("sky & weather", EmojiGroup.TRAVEL_AND_PLACES),
    SOUND("sound", EmojiGroup.OBJECTS),
    SPORT("sport", EmojiGroup.ACTIVITIES),
    SUBDIVISION_FLAG("subdivision-flag", EmojiGroup.FLAGS),
    TIME("time", EmojiGroup.TRAVEL_AND_PLACES),
    TOOL("tool", EmojiGroup.OBJECTS),
    TRANSPORT_AIR("transport-air", EmojiGroup.TRAVEL_AND_PLACES),
    TRANSPORT_GROUND("transport-ground", EmojiGroup.TRAVEL_AND_PLACES),
    TRANSPORT_SIGN("transport-sign", EmojiGroup.SYMBOLS),
    TRANSPORT_WATER("transport-water", EmojiGroup.TRAVEL_AND_PLACES),
    WARNING("warning", EmojiGroup.SYMBOLS),
    WRITING("writing", EmojiGroup.OBJECTS),
    ZODIAC("zodiac", EmojiGroup.SYMBOLS);

    private static final List<EmojiSubGroup> EMOJI_SUBGROUPS = Arrays.asList(values());
    private final String name;
    private final EmojiGroup group;

    EmojiSubGroup(final String name, final EmojiGroup group) {
        this.name = name;
        this.group = group;
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
    public static EmojiSubGroup fromString(final String name) {
        for (final EmojiSubGroup emojiSubGroup : EMOJI_SUBGROUPS) {
            if (emojiSubGroup.getName().equals(name)) {
                return emojiSubGroup;
            }
        }
        throw new IllegalArgumentException("No EmojiSubGroup found for name: " + name);
    }

    public EmojiGroup getGroup() {
        return group;
    }
}

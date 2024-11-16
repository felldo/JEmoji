package net.fellbaum.jemoji;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an emoji group.
 */
@SuppressWarnings("unused")
public enum EmojiGroup {

    ACTIVITIES("Activities"),
    ANIMALS_AND_NATURE("Animals & Nature"),
    COMPONENT("Component"),
    FLAGS("Flags"),
    FOOD_AND_DRINK("Food & Drink"),
    OBJECTS("Objects"),
    PEOPLE_AND_BODY("People & Body"),
    SMILEYS_AND_EMOTION("Smileys & Emotion"),
    SYMBOLS("Symbols"),
    TRAVEL_AND_PLACES("Travel & Places");

    private static final List<EmojiGroup> EMOJI_GROUPS = Arrays.asList(values());
    private final String name;

    EmojiGroup(final String name) {
        this.name = name;
    }

    /**
     * Gets the name of the group.
     *
     * @return The name of the group
     */
    public String getName() {
        return name;
    }

    /**
     * Gets all emoji groups.
     *
     * @return All emoji groups
     */
    public static List<EmojiGroup> getGroups() {
        return EMOJI_GROUPS;
    }

    /**
     * Gets all emoji subgroups of this group.
     *
     * @return All emoji subgroups of this group
     */
    public EnumSet<EmojiSubGroup> getEmojiSubGroups() {
        return EnumSet.copyOf(EmojiSubGroup.getSubGroups().stream().filter(subgroup -> subgroup.getGroup() == this).collect(Collectors.toList()));
    }

    /**
     * Gets the emoji group for the given name.
     *
     * @param name The name of the group.
     * @return The emoji group.
     */
    public static EmojiGroup fromString(String name) {
        for (EmojiGroup emojiGroup : EMOJI_GROUPS) {
            if (emojiGroup.getName().equals(name)) {
                return emojiGroup;
            }
        }
        throw new IllegalArgumentException("No EmojiGroup found for name " + name);
    }

}
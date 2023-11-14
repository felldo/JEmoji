package net.fellbaum.jemoji;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.fellbaum.jemoji.EmojiUtils.getCodePointCount;

/**
 * Represents an emoji.
 */
public class Emoji implements Comparable<Emoji> {

    private final String emoji;
    private final String unicode;
    private final List<String> discordAliases;
    private final List<String> githubAliases;
    private final List<String> slackAliases;
    private final boolean hasFitzpatrick;
    private final boolean hasHairStyle;
    private final double version;
    private final Qualification qualification;
    private final String description;
    private final EmojiGroup group;
    private final EmojiSubGroup subgroup;

    private final List<String> allAliases;

    Emoji(
            @JsonProperty("emoji") String emoji,
            @JsonProperty("unicode") String unicode,
            @JsonProperty("discordAliases") List<String> discordAliases,
            @JsonProperty("githubAliases") List<String> githubAliases,
            @JsonProperty("slackAliases") List<String> slackAliases,
            @JsonProperty("hasFitzpatrick") boolean hasFitzpatrick,
            @JsonProperty("hasHairStyle") boolean hasHairStyle,
            @JsonProperty("version") double version,
            @JsonProperty("qualification") Qualification qualification,
            @JsonProperty("description") String description,
            @JsonProperty("group") EmojiGroup group,
            @JsonProperty("subgroup") EmojiSubGroup subgroup) {
        this.emoji = emoji;
        this.unicode = unicode;
        this.discordAliases = discordAliases;
        this.githubAliases = githubAliases;
        this.slackAliases = slackAliases;
        this.hasFitzpatrick = hasFitzpatrick;
        this.hasHairStyle = hasHairStyle;
        this.version = version;
        this.qualification = qualification;
        this.description = description;
        this.group = group;
        this.subgroup = subgroup;
        Set<String> aliases = new HashSet<>();
        aliases.addAll(getDiscordAliases());
        aliases.addAll(getGithubAliases());
        aliases.addAll(getSlackAliases());
        allAliases = Collections.unmodifiableList(new ArrayList<>(aliases));
    }

    /**
     * Gets the emoji.
     *
     * @return The emoji
     */
    public String getEmoji() {
        return emoji;
    }

    /**
     * Gets the unicode representation of the emoji as a string i.e. \uD83D\uDC4D.
     *
     * @return The unicode representation of the emoji
     */
    public String getUnicode() {
        return unicode;
    }

    /**
     * Gets the HTML decimal code for this emoji.
     *
     * @return The HTML decimal code for this emoji.
     */
    public String getHtmlDecimalCode() {
        return getEmoji().codePoints().mapToObj(operand -> "&#" + operand).collect(Collectors.joining(";")) + ";";
    }

    /**
     * Gets the HTML hexadecimal code for this emoji.
     *
     * @return The HTML hexadecimal code for this emoji.
     */
    public String getHtmlHexadecimalCode() {
        return getEmoji().codePoints().mapToObj(operand -> "&#x" + Integer.toHexString(operand).toUpperCase()).collect(Collectors.joining(";")) + ";";
    }

    /**
     * Gets variations of this emoji with different Fitzpatrick or HairStyle modifiers, if there are any.
     * The returned list does not include this emoji itself.
     *
     * @return Variations of this emoji with different Fitzpatrick or HairStyle modifiers, if there are any.
     */
    public List<Emoji> getVariations() {
        final String baseEmoji = HairStyle.removeHairStyle(Fitzpatrick.removeFitzpatrick(emoji));
        return EmojiManager.getAllEmojis()
                .parallelStream()
                .filter(emoji -> HairStyle.removeHairStyle(Fitzpatrick.removeFitzpatrick(emoji.getEmoji())).equals(baseEmoji))
                .filter(emoji -> !emoji.equals(this))
                .collect(Collectors.toList());
    }

    /**
     * Gets the URL encoded emoji.
     *
     * @return The URL encoded emoji
     */
    public String getURLEncoded() {
        try {
            return URLEncoder.encode(getEmoji(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the Discord aliases for this emoji.
     *
     * @return The Discord aliases for this emoji.
     */
    public List<String> getDiscordAliases() {
        return discordAliases;
    }

    /**
     * Gets the GitHub aliases for this emoji.
     *
     * @return The GitHub aliases for this emoji.
     */
    public List<String> getGithubAliases() {
        return githubAliases;
    }

    /**
     * Gets the Slack aliases for this emoji.
     *
     * @return The Slack aliases for this emoji.
     */
    public List<String> getSlackAliases() {
        return slackAliases;
    }

    /**
     * Gets all the aliases for this emoji.
     *
     * @return All the aliases for this emoji.
     */
    public List<String> getAllAliases() {
        return allAliases;
    }

    /**
     * Checks if this emoji has a fitzpatrick modifier.
     *
     * @return True if this emoji has a fitzpatrick modifier, false otherwise.
     */
    public boolean hasFitzpatrickComponent() {
        return hasFitzpatrick;
    }

    /**
     * Checks if this emoji has a hairstyle modifier.
     *
     * @return True if this emoji has a hairstyle modifier, false otherwise.
     */
    public boolean hasHairStyleComponent() {
        return hasHairStyle;
    }

    /**
     * Gets the version this emoji was added to the unicode consortium.
     *
     * @return The version this emoji was added to the unicode consortium.
     */
    public double getVersion() {
        return version;
    }

    /**
     * Gets the qualification of this emoji.
     *
     * @return The qualification of this emoji.
     */
    public Qualification getQualification() {
        return qualification;
    }

    /**
     * Gets the description of this emoji.
     *
     * @return The description of this emoji.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the group or "category" this emoji belongs to.
     *
     * @return The group this emoji belongs to.
     */
    public EmojiGroup getGroup() {
        return group;
    }

    /**
     * Gets the subgroup of this emoji.
     *
     * @return The subgroup of this emoji.
     */
    public EmojiSubGroup getSubgroup() {
        return subgroup;
    }

    @Override
    public String toString() {
        return "Emoji{" +
                "emoji='" + emoji + '\'' +
                ", unicode='" + unicode + '\'' +
                ", discordAliases=" + discordAliases +
                ", githubAliases=" + githubAliases +
                ", slackAliases=" + slackAliases +
                ", hasFitzpatrick=" + hasFitzpatrick +
                ", hasHairStyle=" + hasHairStyle +
                ", version=" + version +
                ", qualification=" + qualification +
                ", description='" + description + '\'' +
                ", group=" + group +
                ", subgroup=" + subgroup +
                '}';
    }

    /**
     * Compares the emojis based on their codepoint length,
     * and if they are equal, compare them lexicographically based on the emoji.
     *
     * @param o the object to be compared.
     * @return the value 0 if they are fully equal, 1 if the emoji has more codepoints
     * and has a higher unicode value, otherwise return -1
     */
    @Override
    public int compareTo(final Emoji o) {
        final int comparedValue = Integer.compare(getCodePointCount(this.getEmoji()), getCodePointCount(o.getEmoji()));
        if (comparedValue != 0) {
            return comparedValue;
        }

        return this.getEmoji().compareTo(o.getEmoji());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Emoji emoji1 = (Emoji) o;

        if (hasFitzpatrick != emoji1.hasFitzpatrick) return false;
        if (hasHairStyle != emoji1.hasHairStyle) return false;
        if (Double.compare(emoji1.version, version) != 0) return false;
        if (!emoji.equals(emoji1.emoji)) return false;
        if (!unicode.equals(emoji1.unicode)) return false;
        if (!discordAliases.equals(emoji1.discordAliases)) return false;
        if (!githubAliases.equals(emoji1.githubAliases)) return false;
        if (!slackAliases.equals(emoji1.slackAliases)) return false;
        if (qualification != emoji1.qualification) return false;
        if (!description.equals(emoji1.description)) return false;
        if (group != emoji1.group) return false;
        return subgroup == emoji1.subgroup;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = emoji.hashCode();
        result = 31 * result + unicode.hashCode();
        result = 31 * result + discordAliases.hashCode();
        result = 31 * result + githubAliases.hashCode();
        result = 31 * result + slackAliases.hashCode();
        result = 31 * result + (hasFitzpatrick ? 1 : 0);
        result = 31 * result + (hasHairStyle ? 1 : 0);
        temp = Double.doubleToLongBits(version);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + qualification.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + group.hashCode();
        result = 31 * result + subgroup.hashCode();
        return result;
    }
}

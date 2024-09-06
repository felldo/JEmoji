package net.fellbaum.jemoji;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static net.fellbaum.jemoji.InternalEmojiUtils.*;

/**
 * Represents an emoji.
 */
@SuppressWarnings("unused")
public final class Emoji implements Comparable<Emoji> {

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
    private final boolean hasVariationSelectors;

    private final List<String> allAliases;

    Emoji(
            final String emoji,
            final String unicode,
            final List<String> discordAliases,
            final List<String> slackAliases,
            final List<String> githubAliases,
            final boolean hasFitzpatrick,
            final boolean hasHairStyle,
            final double version,
            final Qualification qualification,
            final String description,
            final EmojiGroup group,
            final EmojiSubGroup subgroup,
            final boolean hasVariationSelectors) {
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
        this.hasVariationSelectors = hasVariationSelectors;
        final Set<String> aliases = new HashSet<>();
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
     * @return The Unicode representation of the emoji
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
     * Gets variations of this emoji with different Fitzpatrick or HairStyle modifiers if there are any.
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
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the Discord aliases for this emoji.
     * If possible, do not use the :name::skin-tone-1: variants as they might not produce the emoji you want.
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
     * Gets the version this emoji was added to the Unicode consortium.
     *
     * @return The version this emoji was added to the Unicode consortium.
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
     * Gets the description of this emoji in the specified language.
     * May not be present for all languages.
     *
     * @param emojiLanguage The language type the description should be searched for.
     * @return The description of this emoji in the specified language.
     */
    public Optional<String> getDescription(final EmojiLanguage emojiLanguage) {
        return EmojiManager.getEmojiDescriptionForLanguageAndEmoji(emojiLanguage, emoji);
    }

    /**
     * Gets the keywords of this emoji in the specified language.
     * May not be present for all languages.
     *
     * @param emojiLanguage The language type the keywords should be searched for.
     * @return The keywords of this emoji in the specified language.
     */
    public Optional<Set<String>> getKeywords(final EmojiLanguage emojiLanguage) {
        return EmojiManager.getEmojiKeywordsForLanguageAndEmoji(emojiLanguage, emoji);
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

    /**
     * Returns whether the emoji has text or emoji variations.
     * This means the emoji is allowed to be appended with a FE0E or FE0F character to control how the emoji should be displayed.
     *
     * @return Whether the emoji is a standardized emoji that can be appended with a Variation Selector.
     * @see <a href="https://www.unicode.org/faq/vs.html">Read more here about Emoji Variation Sequences</a>
     */
    public boolean hasVariationSelectors() {
        return hasVariationSelectors;
    }

    /**
     * Gets the text representation of this emoji if the emoji is a standardized emoji that allows variations.
     *
     * @return The text variation of this emoji.
     */
    public Optional<String> getTextVariation() {
        return hasVariationSelectors() ? Optional.of(emoji + TEXT_VARIATION_CHARACTER) : Optional.empty();
    }

    /**
     * Gets the emoji representation of this emoji if the emoji is a standardized emoji that allows variations.
     *
     * @return The emoji variation of this emoji.
     */
    public Optional<String> getEmojiVariation() {
        return hasVariationSelectors() ? Optional.of(emoji + EMOJI_VARIATION_CHARACTER) : Optional.empty();
    }

    /**
     * Compares the emojis based on their codepoint length,
     * and if they are equal, compare them lexicographically based on the emoji.
     *
     * @param o the object to be compared.
     * @return Zero if they are fully equal, 1 if the emoji has more codepoints
     * and has a higher Unicode value, otherwise return -1
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
                ", hasVariationSelectors=" + hasVariationSelectors +
                ", allAliases=" + allAliases +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Emoji emoji1 = (Emoji) o;
        return emoji.equals(emoji1.emoji);
    }

    @Override
    public int hashCode() {
        return emoji.hashCode();
    }
}

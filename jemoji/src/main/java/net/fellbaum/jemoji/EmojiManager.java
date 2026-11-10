package net.fellbaum.jemoji;

import net.fellbaum.jemoji.internal.NonUniqueEmojiFoundResult;
import net.fellbaum.jemoji.internal.UniqueEmojiFoundResult;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.fellbaum.jemoji.EmojiLoader.readFromAllLanguageResourceFiles;
import static net.fellbaum.jemoji.internal.EmojiData.*;
import static net.fellbaum.jemoji.internal.EmojiUtils.*;

@SuppressWarnings("unused")
public final class EmojiManager {

    /**
     * The default emoji type, shared to avoid allocating an {@link EnumSet} on every call.
     * Never hand this instance out to callers, as {@link EnumSet} is mutable.
     */
    private static final EnumSet<EmojiType> UNICODE_ONLY = EnumSet.of(EmojiType.UNICODE);
    private static final EmojiType[] UNICODE_ONLY_ARRAY = {EmojiType.UNICODE};
    private static final EmojiType[] NO_TYPES = new EmojiType[0];

    /**
     * Iterating an {@link EnumSet} allocates an iterator, which the JIT does not reliably eliminate in the
     * scan loops (once per emoji starter codepoint). Converting it once per call avoids that.
     */
    private static EmojiType[] toTypeArray(final EnumSet<EmojiType> emojiType) {
        if (emojiType.size() == 1 && emojiType.contains(EmojiType.UNICODE)) return UNICODE_ONLY_ARRAY;
        return emojiType.toArray(NO_TYPES);
    }

    /** Content of {@code /emoji_sources/description/<language>.json}: emoji -> description. */
    private static final TypeReference<Map<String, String>> DESCRIPTION_FILE_TYPE = new TypeReference<Map<String, String>>() {
    };

    /** Content of {@code /emoji_sources/keyword/<language>.json}: emoji -> keywords. */
    private static final TypeReference<Map<String, List<String>>> KEYWORD_FILE_TYPE = new TypeReference<Map<String, List<String>>>() {
    };

    static Optional<String> getEmojiDescriptionForLanguageAndEmoji(final EmojiLanguage language, final String emoji) {
        Map<String, String> descriptions = EMOJI_DESCRIPTION_LANGUAGE_MAP.get(language);
        if (descriptions == null) {
            // Only the first access of a language takes the (locking) computeIfAbsent path
            descriptions = EMOJI_DESCRIPTION_LANGUAGE_MAP.computeIfAbsent(language, emojiLanguage -> readFromAllLanguageResourceFiles("/emoji_sources/description/", emojiLanguage, DESCRIPTION_FILE_TYPE));
        }
        return Optional.ofNullable(descriptions.get(emoji));
    }

    static Optional<List<String>> getEmojiKeywordsForLanguageAndEmoji(final EmojiLanguage language, final String emoji) {
        Map<String, List<String>> keywords = EMOJI_KEYWORD_LANGUAGE_MAP.get(language);
        if (keywords == null) {
            keywords = EMOJI_KEYWORD_LANGUAGE_MAP.computeIfAbsent(language, emojiLanguage -> readFromAllLanguageResourceFiles("/emoji_sources/keyword/", emojiLanguage, KEYWORD_FILE_TYPE));
        }
        return Optional.ofNullable(keywords.get(emoji));
    }

    private EmojiManager() {
    }

    /**
     * Returns the emoji for the given Unicode.
     *
     * @param emoji The Unicode of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getEmoji(final String emoji) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(emoji)) return Optional.empty();
        return Optional.ofNullable(EMOJI_UNICODE_TO_EMOJI.get(emoji));
    }

    /**
     * Check if the given string is an emoji.
     *
     * @param emoji The emoji to check.
     * @return True if the given string is an emoji.
     */
    public static boolean isEmoji(final String emoji) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(emoji)) return false;
        return EMOJI_UNICODE_TO_EMOJI.containsKey(emoji);
    }

    /**
     * Gets all emojis.
     *
     * @return A set of all emojis.
     */
    public static Set<Emoji> getAllEmojis() {
        ensureUnicodeInitialized();
        return EMOJIS_AS_SET;
    }

    /**
     * Gets all emojis mapped to their group.
     *
     * @return A map of all emojis mapped to their group.
     */
    public static Map<EmojiGroup, Set<Emoji>> getAllEmojisGrouped() {
        ensureUnicodeInitialized();
        return EMOJIS_GROUPED;
    }

    /**
     * Gets all emojis mapped to their subgroup.
     *
     * @return A map of all emojis mapped to their subgroup.
     */
    public static Map<EmojiSubGroup, Set<Emoji>> getAllEmojisSubGrouped() {
        ensureUnicodeInitialized();
        return EMOJIS_SUB_GROUPED;
    }

    /**
     * Gets all emojis that are part of the given group.
     *
     * @param group The group to get the emojis for.
     * @return A set of all emojis that are part of the given group.
     */
    public static Set<Emoji> getAllEmojisByGroup(final EmojiGroup group) {
        ensureUnicodeInitialized();
        return EMOJIS_GROUPED.getOrDefault(group, Set.of());
    }

    /**
     * Gets all emojis that are part of the given subgroup.
     *
     * @param subgroup The subgroup to get the emojis for.
     * @return A set of all emojis that are part of the given subgroup.
     */
    public static Set<Emoji> getAllEmojisBySubGroup(final EmojiSubGroup subgroup) {
        ensureUnicodeInitialized();
        return EMOJIS_SUB_GROUPED.getOrDefault(subgroup, Set.of());
    }

    /**
     * Gets all emojis in descending order by their char length.
     *
     * @return A list of all emojis.
     */
    public static List<Emoji> getAllEmojisLengthDescending() {
        ensureUnicodeInitialized();
        return EMOJIS_LENGTH_DESCENDING;
    }

    /**
     * Gets an emoji for the given HTML decimal i.e. {@literal &#128077;} (thumbsup emoji) if present.
     *
     * @param htmlDecimal The HTML decimal of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByHtmlDecimal(final String htmlDecimal) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(htmlDecimal)) return Optional.empty();
        return Optional.ofNullable(EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI.get(htmlDecimal));
    }

    /**
     * Gets an emoji for the given HTML hexadecimal i.e. {@literal &#x1F44D;} (thumbsup emoji) if present.
     *
     * @param htmlHexadecimal The HTML hexadecimal of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByHtmlHexadecimal(final String htmlHexadecimal) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(htmlHexadecimal)) return Optional.empty();
        return Optional.ofNullable(EMOJI_HTML_HEXADECIMAL_REPRESENTATION_TO_EMOJI.get(htmlHexadecimal));
    }

    /**
     * Gets an emoji for the given url encoded emoji i.e. {@literal %F0%9F%91%8D} (thumbsup emoji) if present.
     *
     * @param urlEncodedEmoji The HTML hexadecimal of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByUrlEncoded(final String urlEncodedEmoji) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(urlEncodedEmoji)) return Optional.empty();
        return Optional.ofNullable(EMOJI_URL_ENCODED_REPRESENTATION_TO_EMOJI.get(urlEncodedEmoji));
    }

    /**
     * Gets an emoji for the given alias i.e. :thumbsup: if present.
     *
     * @param alias The alias of the emoji.
     * @return The emoji.
     */
    public static Optional<List<Emoji>> getByAlias(final String alias) {
        ensureAliasInitialized();
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        return findEmojiByEitherAlias(ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING, alias);
    }

    /**
     * Gets an emoji for the given Discord alias i.e. :thumbsup: if present.
     *
     * @param alias The Discord alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByDiscordAlias(final String alias) {
        ensureAliasInitialized();
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        return findEmojiByEitherAlias(ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING, alias).flatMap(emojis -> {
            for (Emoji emoji : emojis) {
                if (!emoji.getDiscordAliases().isEmpty()) {
                    return Optional.of(emoji);
                }
            }
            return Optional.empty();
        });
    }

    /**
     * Gets an emoji for the given GitHub alias i.e. :thumbsup: if present.
     *
     * @param alias The GitHub alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByGithubAlias(final String alias) {
        ensureAliasInitialized();
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        return findEmojiByEitherAlias(ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING, alias).flatMap(emojis -> {
            for (Emoji emoji : emojis) {
                if (!emoji.getGithubAliases().isEmpty()) {
                    return Optional.of(emoji);
                }
            }
            return Optional.empty();
        });
    }

    /**
     * Gets an emoji for the given Slack alias i.e. :thumbsup: if present.
     *
     * @param alias The Slack alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getBySlackAlias(final String alias) {
        ensureAliasInitialized();
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        return findEmojiByEitherAlias(ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING, alias).flatMap(emojis -> {
            for (Emoji emoji : emojis) {
                if (!emoji.getSlackAliases().isEmpty()) {
                    return Optional.of(emoji);
                }
            }
            return Optional.empty();
        });
    }

    /**
     * Gets the pattern checking for all emojis.
     *
     * @return The pattern for all emojis.
     */
    public static Pattern getEmojiPattern() {
        ensureUnicodeInitialized();
        if (EMOJI_PATTERN == null) {
            EMOJI_PATTERN = Pattern.compile(EMOJIS_LENGTH_DESCENDING.stream()
                    .map(s -> "(" + Pattern.quote(s.getEmoji()) + ")").collect(Collectors.joining("|")));
        }

        return EMOJI_PATTERN;
    }

    /**
     * Checks if the given text contains emojis.
     *
     * @param text The text to check.
     * @return True if the given text contains emojis.
     */
    public static boolean containsAnyEmoji(final String text) {
        return containsAnyEmoji(text, UNICODE_ONLY);
    }

    /**
     * Checks if the given text contains emojis.
     *
     * @param text      The text to check.
     * @param emojiType The type of the emoji appearance in the string which should be checked against.
     * @return True if the given text contains emojis.
     */
    public static boolean containsAnyEmoji(final String text, EnumSet<EmojiType> emojiType) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(text)) return false;

        final EmojiType[] types = toTypeArray(emojiType);
        final int textLength = text.length();
        for (int charIndex = 0; charIndex < textLength; ) {
            final int currentCodepoint = text.codePointAt(charIndex);
            if (!checkIfCodepointIsInvalidEmojiStarter(currentCodepoint)) {
                for (final EmojiType type : types) {
                    if (findUniqueEmoji(text, charIndex, currentCodepoint, type) != null) {
                        return true;
                    }
                }
            }
            charIndex += Character.charCount(currentCodepoint);
        }
        return false;
    }

    // =======================================================
    // ======================== EXTRACT ======================
    // =======================================================

    /**
     * Extracts all emojis from the given text.
     *
     * @param text The text to extract emojis from.
     * @return A list of emojis.
     */
    public static Set<Emoji> extractEmojis(final String text) {
        return Set.copyOf(extractEmojisInOrder(text));
    }

    /**
     * Extracts all emojis from the given text.
     *
     * @param text      The text to extract emojis from.
     * @param emojiType The type of the emoji appearance in the string which should be extracted.
     * @return A list of emojis.
     */
    public static Set<Emoji> extractEmojis(final String text, EnumSet<EmojiType> emojiType) {
        return Set.copyOf(extractEmojisInOrder(text, emojiType));
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text The text to extract emojis from.
     * @return A list of emojis.
     */
    public static List<Emoji> extractEmojisInOrder(final String text) {
        return extractEmojisInOrderWithIndex(text).stream().map(IndexedEmoji::getEmoji).toList();
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text      The text to extract emojis from.
     * @param emojiType The type of the emoji appearance in the string which should be extracted.
     * @return A list of emojis.
     */
    public static List<Emoji> extractEmojisInOrder(final String text, EnumSet<EmojiType> emojiType) {
        return extractEmojisInOrderWithIndex(text, emojiType).stream().map(IndexedEmoji::getEmoji).toList();
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text The text to extract emojis from.
     * @return A list of indexed emojis.
     */
    public static List<IndexedEmoji> extractEmojisInOrderWithIndex(final String text) {
        return extractEmojisInOrderWithIndex(text, UNICODE_ONLY);
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text      The text to extract emojis from.
     * @param emojiType The type of the emoji appearance in the string which should be extracted.
     * @return A list of indexed emojis.
     */
    public static List<IndexedEmoji> extractEmojisInOrderWithIndex(final String text, EnumSet<EmojiType> emojiType) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(text)) return List.of();

        final List<IndexedEmoji> emojis = new ArrayList<>();

        final EmojiType[] types = toTypeArray(emojiType);
        final int textLength = text.length();
        int codePointIndex = 0;
        nextTextIteration:
        for (int charIndex = 0; charIndex < textLength; ) {
            final int currentCodepoint = text.codePointAt(charIndex);
            if (!checkIfCodepointIsInvalidEmojiStarter(currentCodepoint)) {
                for (final EmojiType type : types) {
                    final UniqueEmojiFoundResult uniqueEmojiFoundResult = findUniqueEmoji(text, charIndex, currentCodepoint, type);
                    if (uniqueEmojiFoundResult == null) {
                        continue;
                    }

                    final int endCharIndex = uniqueEmojiFoundResult.endIndex();
                    final int endCodePointIndex = codePointIndex + text.codePointCount(charIndex, endCharIndex);
                    emojis.add(new IndexedEmoji(uniqueEmojiFoundResult.emoji(), charIndex, codePointIndex, endCharIndex, endCodePointIndex));
                    charIndex = endCharIndex;
                    codePointIndex = endCodePointIndex;
                    continue nextTextIteration;
                }
            }
            charIndex += Character.charCount(currentCodepoint);
            codePointIndex++;
        }

        return List.copyOf(emojis);
    }

    // ======================================================
    // ======================== REMOVE ======================
    // ======================================================

    /**
     * Removes all emojis from the given text.
     *
     * @param text The text to remove emojis from.
     * @return The text without emojis.
     */
    public static String removeAllEmojis(final String text) {
        return removeAllEmojisExcept(text, List.of(), UNICODE_ONLY);
    }

    /**
     * Removes all emojis from the given text.
     *
     * @param text      The text to remove emojis from.
     * @param emojiType The type of the emoji appearance in the string which should be removed.
     * @return The text without emojis.
     */
    public static String removeAllEmojis(final String text, EnumSet<EmojiType> emojiType) {
        return removeAllEmojisExcept(text, List.of(), emojiType);
    }

    /**
     * Removes the given emojis from the given text.
     *
     * @param text           The text to remove emojis from.
     * @param emojisToRemove The emojis to remove.
     * @return The text without the given emojis.
     */
    public static String removeEmojis(final String text, final Emoji... emojisToRemove) {
        return removeEmojis(text, Arrays.asList(emojisToRemove));
    }

    /**
     * Removes the given emojis from the given text.
     *
     * @param text           The text to remove emojis from.
     * @param emojisToRemove The emojis to remove.
     * @return The text without the given emojis.
     */
    public static String removeEmojis(final String text, final Collection<Emoji> emojisToRemove) {
        return removeEmojis(text, emojisToRemove, UNICODE_ONLY);
    }

    /**
     * Removes the given emojis from the given text.
     *
     * @param text           The text to remove emojis from.
     * @param emojisToRemove The emojis to remove.
     * @param emojiType      The type of the emoji appearance in the string which should be removed.
     * @return The text without the given emojis.
     */
    public static String removeEmojis(final String text, final Collection<Emoji> emojisToRemove, EnumSet<EmojiType> emojiType) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(text)) return "";
        final Set<Emoji> toRemove = emojisToRemove instanceof Set ? (Set<Emoji>) emojisToRemove : new HashSet<>(emojisToRemove);
        return replaceEmojisInternal(text, toRemove, true, null, emojiType);
    }

    /**
     * Shared core of all remove and replace methods.
     * <p>
     * Unchanged text is copied in runs with {@link StringBuilder#append(CharSequence, int, int)} instead of
     * codepoint by codepoint. If no emoji is changed, the given text itself is returned without any copy.
     * Emojis which are found but not changed keep their original representation in the text.
     *
     * @param text                The text, not null or empty.
     * @param emojis              The emojis to change (if {@code changeContained}) or to keep (otherwise).
     * @param changeContained     Whether the emojis in {@code emojis} are changed, or all others.
     * @param replacementFunction The replacement of a changed emoji, {@code null} to remove it.
     * @param emojiType           The type of the emoji appearance in the string.
     * @return The resulting text.
     */
    private static String replaceEmojisInternal(final String text, final Set<Emoji> emojis, final boolean changeContained, final @Nullable Function<Emoji, String> replacementFunction, final EnumSet<EmojiType> emojiType) {
        final EmojiType[] types = toTypeArray(emojiType);
        final int textLength = text.length();
        StringBuilder sb = null;
        int copiedUpToCharIndex = 0;

        nextTextIteration:
        for (int charIndex = 0; charIndex < textLength; ) {
            final int currentCodepoint = text.codePointAt(charIndex);
            if (!checkIfCodepointIsInvalidEmojiStarter(currentCodepoint)) {
                for (final EmojiType type : types) {
                    final UniqueEmojiFoundResult uniqueEmojiFoundResult = findUniqueEmoji(text, charIndex, currentCodepoint, type);
                    if (uniqueEmojiFoundResult == null) {
                        continue;
                    }

                    final int endCharIndex = uniqueEmojiFoundResult.endIndex();
                    final Emoji emoji = uniqueEmojiFoundResult.emoji();
                    if (emojis.contains(emoji) == changeContained) {
                        if (sb == null) sb = new StringBuilder(textLength);
                        sb.append(text, copiedUpToCharIndex, charIndex);
                        if (replacementFunction != null) sb.append(replacementFunction.apply(emoji));
                        copiedUpToCharIndex = endCharIndex;
                    }
                    charIndex = endCharIndex;
                    continue nextTextIteration;
                }
            }
            charIndex += Character.charCount(currentCodepoint);
        }

        if (sb == null) return text;
        return sb.append(text, copiedUpToCharIndex, text.length()).toString();
    }

    /**
     * Removes all emojis except the given emojis from the given text.
     *
     * @param text         The text to remove emojis from.
     * @param emojisToKeep The emojis to keep.
     * @return The text with only the given emojis.
     */
    public static String removeAllEmojisExcept(final String text, final Emoji... emojisToKeep) {
        return removeAllEmojisExcept(text, Arrays.asList(emojisToKeep));
    }

    /**
     * Removes all emojis except the given emojis from the given text.
     *
     * @param text         The text to remove emojis from.
     * @param emojisToKeep The emojis to keep.
     * @return The text with only the given emojis.
     */
    public static String removeAllEmojisExcept(final String text, final Collection<Emoji> emojisToKeep) {
        return removeAllEmojisExcept(text, emojisToKeep, UNICODE_ONLY);
    }

    /**
     * Removes all emojis except the given emojis from the given text.
     *
     * @param text         The text to remove emojis from.
     * @param emojisToKeep The emojis to keep.
     * @param emojiType    The type of the emoji appearance in the string which should be removed.
     * @return The text with only the given emojis.
     */
    public static String removeAllEmojisExcept(final String text, final Collection<Emoji> emojisToKeep, EnumSet<EmojiType> emojiType) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(text)) return "";

        final Set<Emoji> keepSet = emojisToKeep instanceof Set ? (Set<Emoji>) emojisToKeep : new HashSet<>(emojisToKeep);
        return replaceEmojisInternal(text, keepSet, false, null, emojiType);
    }

    // =======================================================
    // ======================== REPLACE ======================
    // =======================================================

    /**
     * Replaces all emojis in the text with the given replacement string.
     *
     * @param text              The text to replace emojis from.
     * @param replacementString The replacement string.
     * @return The text with all emojis replaced.
     */
    public static String replaceAllEmojis(final String text, final String replacementString) {
        ensureUnicodeInitialized();
        return replaceEmojis(text, replacementString, EMOJIS_AS_SET);
    }

    /**
     * Replaces all emojis in the text with the given replacement string.
     *
     * @param text              The text to replace emojis from.
     * @param replacementString The replacement string.
     * @param emojiType         The type of the emoji appearance in the string which should be replaced.
     * @return The text with all emojis replaced.
     */
    public static String replaceAllEmojis(final String text, final String replacementString, final EnumSet<EmojiType> emojiType) {
        ensureUnicodeInitialized();
        return replaceEmojis(text, replacementString, EMOJIS_AS_SET, emojiType);
    }

    /**
     * Replaces all emojis in the text with the given replacement function.
     *
     * @param text                The text to replace emojis from.
     * @param replacementFunction The replacement function.
     * @return The text with all emojis replaced.
     */
    public static String replaceAllEmojis(final String text, final Function<Emoji, String> replacementFunction) {
        ensureUnicodeInitialized();
        return replaceEmojis(text, replacementFunction, EMOJIS_AS_SET);
    }

    /**
     * Replaces all emojis in the text with the given replacement function.
     *
     * @param text                The text to replace emojis from.
     * @param replacementFunction The replacement function.
     * @param emojiType           The type of the emoji appearance in the string which should be replaced.
     * @return The text with all emojis replaced.
     */
    public static String replaceAllEmojis(final String text, final Function<Emoji, String> replacementFunction, final EnumSet<EmojiType> emojiType) {
        ensureUnicodeInitialized();
        return replaceEmojis(text, replacementFunction, EMOJIS_AS_SET, emojiType);
    }

    /**
     * Replaces the given emojis with the given replacement string.
     *
     * @param text              The text to replace emojis from.
     * @param replacementString The replacement string.
     * @param emojisToReplace   The emojis to replace.
     * @return The text with the given emojis replaced.
     */
    public static String replaceEmojis(final String text, final String replacementString, final Collection<Emoji> emojisToReplace) {
        return replaceEmojis(text, emoji -> replacementString, emojisToReplace);
    }

    /**
     * Replaces the given emojis with the given replacement string.
     *
     * @param text              The text to replace emojis from.
     * @param replacementString The replacement string.
     * @param emojisToReplace   The emojis to replace.
     * @param emojiType         The type of the emoji appearance in the string which should be replaced.
     * @return The text with the given emojis replaced.
     */
    public static String replaceEmojis(final String text, final String replacementString, final Collection<Emoji> emojisToReplace, final EnumSet<EmojiType> emojiType) {
        return replaceEmojis(text, emoji -> replacementString, emojisToReplace, emojiType);
    }

    /**
     * Replaces the given emojis with the given replacement string.
     *
     * @param text              The text to replace emojis from.
     * @param replacementString The replacement string.
     * @param emojisToReplace   The emojis to replace.
     * @return The text with the given emojis replaced.
     */
    public static String replaceEmojis(final String text, final String replacementString, final Emoji... emojisToReplace) {
        return replaceEmojis(text, emoji -> replacementString, Arrays.asList(emojisToReplace));
    }

    /**
     * Replaces all emojis in the text with the given replacement function.
     *
     * @param text                The text to replace emojis from.
     * @param replacementFunction The replacement function.
     * @param emojisToReplace     The emojis to replace.
     * @return The text with all emojis replaced.
     */
    public static String replaceEmojis(final String text, final Function<Emoji, String> replacementFunction, final Collection<Emoji> emojisToReplace) {
        return replaceEmojis(text, replacementFunction, emojisToReplace, UNICODE_ONLY);
    }

    /**
     * Replaces all emojis in the text with the given replacement function.
     *
     * @param text                The text to replace emojis from.
     * @param replacementFunction The replacement function.
     * @param emojisToReplace     The emojis to replace.
     * @param emojiType           The type of the emoji appearance in the string which should be replaced.
     * @return The text with all emojis replaced.
     */
    public static String replaceEmojis(final String text, final Function<Emoji, String> replacementFunction, final Collection<Emoji> emojisToReplace, final EnumSet<EmojiType> emojiType) {
        ensureUnicodeInitialized();
        if (isStringNullOrEmpty(text)) return "";

        final Set<Emoji> replaceSet = emojisToReplace instanceof Set ? (Set<Emoji>) emojisToReplace : new HashSet<>(emojisToReplace);
        return replaceEmojisInternal(text, replaceSet, true, replacementFunction, emojiType);
    }

    /**
     * Replaces all aliases in the text with the given replacement function.
     *
     * @param text                The text to replace aliases from.
     * @param replacementFunction A non-empty list.
     *                            Always contains at least one emoji, but there may be more than 1 available for one alias.
     *                            Therefore, it's up to you to decide which one you take.
     *                            <p>
     *                            For example, when replacing the alias ":beach_umbrella:", these two emojis will be available.
     *                            {emoji='🏖️', unicode='\uD83C\uDFD6\uFE0F', discordAliases=[:beach:, :beach_with_umbrella:], githubAliases=[:beach_umbrella:], slackAliases=[:beach_with_umbrella:], hasFitzpatrick=false, hasHairStyle=false, version=0.7, qualification=FULLY_QUALIFIED, description='beach with umbrella', group=TRAVEL_AND_PLACES, subgroup=PLACE_GEOGRAPHIC, hasVariationSelectors=false, allAliases=[:beach:, :beach_umbrella:, :beach_with_umbrella:]},
     *                            {emoji='⛱️', unicode='\u26F1\uFE0F', discordAliases=[:beach_umbrella:, :umbrella_on_ground:], githubAliases=[:parasol_on_ground:], slackAliases=[:umbrella_on_ground:], hasFitzpatrick=false, hasHairStyle=false, version=0.7, qualification=FULLY_QUALIFIED, description='umbrella on ground', group=TRAVEL_AND_PLACES, subgroup=SKY_AND_WEATHER, hasVariationSelectors=false, allAliases=[:beach_umbrella:, :umbrella_on_ground:, :parasol_on_ground:]}
     *                            </p>
     * @return The text with the aliases replaced.
     */
    public static String replaceAliases(final String text, final BiFunction<String, List<Emoji>, String> replacementFunction) {
        ensureAliasInitialized();
        if (isStringNullOrEmpty(text)) return "";

        final int textLength = text.length();
        StringBuilder sb = null;
        int copiedUpToCharIndex = 0;

        for (int charIndex = 0; charIndex < textLength; ) {
            final int currentCodepoint = text.codePointAt(charIndex);
            final NonUniqueEmojiFoundResult nonUniqueEmojiFoundResult = checkIfCodepointIsInvalidAliasStarter(currentCodepoint) ? null : findNonUniqueEmoji(text, charIndex);
            if (nonUniqueEmojiFoundResult == null) {
                charIndex += Character.charCount(currentCodepoint);
                continue;
            }

            final int endCharIndex = nonUniqueEmojiFoundResult.endIndex();
            if (sb == null) sb = new StringBuilder(textLength);
            sb.append(text, copiedUpToCharIndex, charIndex);
            sb.append(replacementFunction.apply(text.substring(charIndex, endCharIndex), nonUniqueEmojiFoundResult.emojis()));
            copiedUpToCharIndex = endCharIndex;
            charIndex = endCharIndex;
        }

        if (sb == null) return text;
        return sb.append(text, copiedUpToCharIndex, textLength).toString();
    }

    /**
     * Extracts all aliases from the given text.
     *
     * @param text The text to extract aliases from.
     * @return A set of aliases.
     */
    public static Set<String> extractAliases(final String text) {
        return Set.copyOf(extractAliasesInOrder(text));
    }

    /**
     * Extracts all aliases from the given text in the order they appear.
     *
     * @param text The text to extract aliases from.
     * @return A list of aliases.
     */
    public static List<String> extractAliasesInOrder(final String text) {
        return extractAliasesInOrderWithIndex(text).stream().map(IndexedAlias::getAlias).toList();
    }

    /**
     * Extracts all aliases from the given text in the order they appear.
     *
     * @param text The text to extract aliases from.
     * @return A list of indexed aliases.
     */
    public static List<IndexedAlias> extractAliasesInOrderWithIndex(final String text) {
        ensureAliasInitialized();
        if (isStringNullOrEmpty(text)) return List.of();

        final int textLength = text.length();
        final List<IndexedAlias> indexedAliases = new ArrayList<>();
        int codePointIndex = 0;
        for (int charIndex = 0; charIndex < textLength; ) {
            final int currentCodepoint = text.codePointAt(charIndex);
            final NonUniqueEmojiFoundResult nonUniqueEmojiFoundResult = checkIfCodepointIsInvalidAliasStarter(currentCodepoint) ? null : findNonUniqueEmoji(text, charIndex);
            if (nonUniqueEmojiFoundResult == null) {
                charIndex += Character.charCount(currentCodepoint);
                codePointIndex++;
                continue;
            }

            final int endCharIndex = nonUniqueEmojiFoundResult.endIndex();
            final int endCodePointIndex = codePointIndex + text.codePointCount(charIndex, endCharIndex);
            indexedAliases.add(new IndexedAlias(
                    text.substring(charIndex, endCharIndex),
                    nonUniqueEmojiFoundResult.emojis(),
                    charIndex,
                    codePointIndex,
                    endCharIndex,
                    endCodePointIndex));
            charIndex = endCharIndex;
            codePointIndex = endCodePointIndex;
        }
        return List.copyOf(indexedAliases);
    }
}

package net.fellbaum.jemoji;

import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.fellbaum.jemoji.EmojiLoader.readFromAllLanguageResourceFiles;
import static net.fellbaum.jemoji.InternalEmojiUtils.*;

@SuppressWarnings("unused")
public final class EmojiManager {

    private static final Map<String, Emoji> EMOJI_UNICODE_TO_EMOJI;
    static final Map<Integer, List<Emoji>> EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING;

    static final Map<InternalCodepointSequence, List<Emoji>> ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING;
    static final Set<Integer> POSSIBLE_EMOJI_ALIAS_STARTER_CODEPOINTS;
    static final int ALIAS_EMOJI_MAX_LENGTH;

    static final Set<Integer> POSSIBLE_EMOJI_URL_ENCODED_STARTER_CODEPOINTS;
    static final int MINIMUM_EMOJI_URL_ENCODED_LENGTH;
    static final int MAXIMUM_EMOJI_URL_ENCODED_LENGTH;
    static final Set<String> ALLOWED_EMOJI_URL_ENCODED_SEQUENCES;


    static final Map<String, Emoji> EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI;
    static final Map<String, Emoji> EMOJI_HTML_HEXADECIMAL_REPRESENTATION_TO_EMOJI;
    static final Map<String, Emoji> EMOJI_URL_ENCODED_REPRESENTATION_TO_EMOJI;
    static final Map<String, List<Emoji>> EMOJI_ALIAS_TO_EMOJIS;
    static final int MIN_HTML_DECIMAL_CODEPOINT_LENGTH;
    // Emojis, which consist of multiple single emojis
    static final int MAX_HTML_DECIMAL_SINGLE_EMOJIS_CONCATENATED_LENGTH;
    private static final List<Emoji> EMOJIS_LENGTH_DESCENDING;

    private static @Nullable Pattern EMOJI_PATTERN;
    private static final Pattern NOT_WANTED_EMOJI_CHARACTERS = Pattern.compile("[\\p{Alpha}\\p{Z}]");

    private static final Map<EmojiLanguage, Map<String, String>> EMOJI_DESCRIPTION_LANGUAGE_MAP = new HashMap<>();
    private static final Map<EmojiLanguage, Map<String, List<String>>> EMOJI_KEYWORD_LANGUAGE_MAP = new HashMap<>();

    static {
        //TODO: Automate somehow loading the emoji loader files?
        final Set<Emoji> emojis = new HashSet<>();
        emojis.addAll(EmojiLoaderA.EMOJI_LIST);
        emojis.addAll(EmojiLoaderB.EMOJI_LIST);

        EMOJI_UNICODE_TO_EMOJI = Collections.unmodifiableMap(prepareEmojisStreamForInitialization(emojis).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (existing, replacement) -> existing)));

        EMOJIS_LENGTH_DESCENDING = Collections.unmodifiableList(emojis.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()));

        EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING = Collections.unmodifiableMap(prepareEmojisStreamForInitialization(emojis).collect(getEmojiLinkedHashMapCollector()));

        EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI = Collections.unmodifiableMap(
                emojis.stream().collect(Collectors.toMap(
                        o -> o.getHtmlDecimalCode().toUpperCase(),
                        emoji -> emoji,
                        (existing, replacement) -> existing)
                ));

        EMOJI_HTML_HEXADECIMAL_REPRESENTATION_TO_EMOJI = Collections.unmodifiableMap(
                emojis.stream().collect(Collectors.toMap(
                        o -> o.getHtmlHexadecimalCode().toUpperCase(),
                        emoji -> emoji,
                        (existing, replacement) -> existing)
                ));

        EMOJI_URL_ENCODED_REPRESENTATION_TO_EMOJI = Collections.unmodifiableMap(
                emojis.stream().collect(Collectors.toMap(
                        o -> o.getURLEncoded().toUpperCase(),
                        emoji -> emoji,
                        (existing, replacement) -> existing)
                ));

        MAX_HTML_DECIMAL_SINGLE_EMOJIS_CONCATENATED_LENGTH = (int) EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI.keySet().stream()
                .mapToLong(value -> value.chars().filter(ch -> ch == ';').count())
                .max()
                .orElseThrow(IllegalStateException::new);

        MIN_HTML_DECIMAL_CODEPOINT_LENGTH = EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI.keySet().stream()
                .map(InternalEmojiUtils::stringToCodePoints)
                .map(ints -> ints.length)
                .min(Comparator.comparingInt(Integer::intValue))
                .orElseThrow(IllegalStateException::new);

        EMOJI_ALIAS_TO_EMOJIS = EmojiManager.getAllEmojis().stream()
                .flatMap(emoji -> emoji.getAllAliases().stream().map(alias -> new AbstractMap.SimpleEntry<>(alias, emoji)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        POSSIBLE_EMOJI_ALIAS_STARTER_CODEPOINTS = emojis.stream().flatMap(emoji -> emoji.getAllAliases().stream()).distinct().map(s -> s.codePointAt(0)).collect(Collectors.toSet());
        ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING = mapAliasesToEmojis(EMOJIS_LENGTH_DESCENDING);
        ALIAS_EMOJI_MAX_LENGTH = EMOJIS_LENGTH_DESCENDING.stream().flatMap(emoji -> emoji.getAllAliases().stream()).distinct().map(InternalEmojiUtils::getCodePointCount).max(Comparator.naturalOrder()).orElse(0);

        POSSIBLE_EMOJI_URL_ENCODED_STARTER_CODEPOINTS = emojis.stream().map(Emoji::getURLEncoded).distinct().map(s -> s.codePointAt(0)).collect(Collectors.toSet());
        MINIMUM_EMOJI_URL_ENCODED_LENGTH = emojis.stream().map(Emoji::getURLEncoded).distinct().map(InternalEmojiUtils::getCodePointCount).min(Comparator.naturalOrder()).orElse(0);
        MAXIMUM_EMOJI_URL_ENCODED_LENGTH = emojis.stream().map(Emoji::getURLEncoded).distinct().map(InternalEmojiUtils::getCodePointCount).max(Comparator.naturalOrder()).orElse(0);
        ALLOWED_EMOJI_URL_ENCODED_SEQUENCES = emojis.stream().map(Emoji::getURLEncoded).distinct().flatMap(s -> Arrays.stream(s.split("%"))).collect(Collectors.toSet());
    }

    private static Map<InternalCodepointSequence, List<Emoji>> mapAliasesToEmojis(final List<Emoji> emojis) {
        return emojis.stream()
                .flatMap(emoji -> emoji.getAllAliases().stream()
                        .map(alias -> new AbstractMap.SimpleEntry<>(new InternalCodepointSequence(stringToCodePoints(alias)), emoji)))
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleEntry::getKey,
                        HashMap::new,
                        Collectors.mapping(AbstractMap.SimpleEntry::getValue, Collectors.toList())
                ));
    }

    private static Collector<AbstractMap.SimpleEntry<String, Emoji>, ?, LinkedHashMap<Integer, List<Emoji>>> getEmojiLinkedHashMapCollector() {
        return Collectors.groupingBy(
                entry -> entry.getValue().getEmoji().codePointAt(0),
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList()),
                        list -> {
                            list.sort((e1, e2) -> Integer.compare(e2.getEmoji().length(), e1.getEmoji().length()));
                            return list;
                        }
                )
        );
    }

    private static Stream<AbstractMap.SimpleEntry<String, Emoji>> prepareEmojisStreamForInitialization(final Set<Emoji> emojis) {
        return emojis.stream()
                .flatMap(emoji -> {
                    final Stream.Builder<AbstractMap.SimpleEntry<String, Emoji>> streamBuilder = Stream.builder();
                    streamBuilder.add(new AbstractMap.SimpleEntry<>(emoji.getEmoji(), emoji));
                    if (emoji.hasVariationSelectors()) {
                        emoji.getTextVariation().ifPresent(variation -> streamBuilder.add(new AbstractMap.SimpleEntry<>(variation, emoji)));
                        emoji.getEmojiVariation().ifPresent(variation -> streamBuilder.add(new AbstractMap.SimpleEntry<>(variation, emoji)));
                    }
                    return streamBuilder.build();
                });
    }

    @SuppressWarnings("unchecked")
    static Optional<String> getEmojiDescriptionForLanguageAndEmoji(final EmojiLanguage language, final String emoji) {
        return Optional.ofNullable(EMOJI_DESCRIPTION_LANGUAGE_MAP.computeIfAbsent(language, emojiLanguage -> (Map<String, String>) readFromAllLanguageResourceFiles("/emoji_sources/description/", emojiLanguage)).get(emoji));
    }

    @SuppressWarnings("unchecked")
    static Optional<List<String>> getEmojiKeywordsForLanguageAndEmoji(final EmojiLanguage language, final String emoji) {
        return Optional.ofNullable(EMOJI_KEYWORD_LANGUAGE_MAP.computeIfAbsent(language, emojiLanguage -> (Map<String, List<String>>) readFromAllLanguageResourceFiles("/emoji_sources/keyword/", emojiLanguage)).get(emoji));
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
        if (isStringNullOrEmpty(emoji)) return false;
        return EMOJI_UNICODE_TO_EMOJI.containsKey(emoji);
    }

    /**
     * Gets all emojis.
     *
     * @return A set of all emojis.
     */
    public static Set<Emoji> getAllEmojis() {
        return new HashSet<>(EMOJIS_LENGTH_DESCENDING);
    }

    /**
     * Gets all emojis mapped to their group.
     *
     * @return A map of all emojis mapped to their group.
     */
    public static Map<EmojiGroup, Set<Emoji>> getAllEmojisGrouped() {
        return EMOJIS_LENGTH_DESCENDING.stream().collect(Collectors.groupingBy(Emoji::getGroup, Collectors.toSet()));
    }

    /**
     * Gets all emojis mapped to their subgroup.
     *
     * @return A map of all emojis mapped to their subgroup.
     */
    public static Map<EmojiSubGroup, Set<Emoji>> getAllEmojisSubGrouped() {
        return EMOJIS_LENGTH_DESCENDING.stream().collect(Collectors.groupingBy(Emoji::getSubgroup, Collectors.toSet()));
    }

    /**
     * Gets all emojis that are part of the given group.
     *
     * @param group The group to get the emojis for.
     * @return A set of all emojis that are part of the given group.
     */
    public static Set<Emoji> getAllEmojisByGroup(final EmojiGroup group) {
        return EMOJIS_LENGTH_DESCENDING.stream().filter(emoji -> emoji.getGroup() == group).collect(Collectors.toSet());
    }

    /**
     * Gets all emojis that are part of the given subgroup.
     *
     * @param subgroup The subgroup to get the emojis for.
     * @return A set of all emojis that are part of the given subgroup.
     */
    public static Set<Emoji> getAllEmojisBySubGroup(final EmojiSubGroup subgroup) {
        return EMOJIS_LENGTH_DESCENDING.stream().filter(emoji -> emoji.getSubgroup() == subgroup).collect(Collectors.toSet());
    }

    /**
     * Gets all emojis in descending order by their char length.
     *
     * @return A list of all emojis.
     */
    public static List<Emoji> getAllEmojisLengthDescending() {
        return EMOJIS_LENGTH_DESCENDING;
    }

    /**
     * Gets an emoji for the given alias i.e. :thumbsup: if present.
     *
     * @param alias The alias of the emoji.
     * @return The emoji.
     */
    public static Optional<List<Emoji>> getByAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        return findEmojiByEitherAlias(EMOJI_ALIAS_TO_EMOJIS, alias);
    }

    /**
     * Gets an emoji for the given Discord alias i.e. :thumbsup: if present.
     *
     * @param alias The Discord alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByDiscordAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        return findEmojiByEitherAlias(EMOJI_ALIAS_TO_EMOJIS, alias).flatMap(emojis -> {
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
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        return findEmojiByEitherAlias(EMOJI_ALIAS_TO_EMOJIS, alias).flatMap(emojis -> {
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
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        return findEmojiByEitherAlias(EMOJI_ALIAS_TO_EMOJIS, alias).flatMap(emojis -> {
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
        return containsAnyEmoji(text, EnumSet.of(EmojiType.UNICODE));
    }

    /**
     * Checks if the given text contains emojis.
     *
     * @param text      The text to check.
     * @param emojiType The type of the emoji appearance in the string which should be checked against.
     * @return True if the given text contains emojis.
     */
    public static boolean containsAnyEmoji(final String text, EnumSet<EmojiType> emojiType) {
        if (isStringNullOrEmpty(text)) return false;

        final int[] textCodePointsArray = stringToCodePoints(text);
        final long textCodePointsLength = textCodePointsArray.length;

        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            if (checkIfCodepointIsInvalidEmojiStarter(textCodePointsArray[textIndex])) {
                continue;
            }
            for (final EmojiType type : emojiType) {
                final UniqueEmojiFoundResult uniqueEmojiFoundResult = findUniqueEmoji(textCodePointsArray, textIndex, textCodePointsLength, type);
                if (uniqueEmojiFoundResult == null) {
                    continue;
                }
                return true;
            }
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
        return Collections.unmodifiableSet(new HashSet<>(extractEmojisInOrder(text)));
    }

    /**
     * Extracts all emojis from the given text.
     *
     * @param text      The text to extract emojis from.
     * @param emojiType The type of the emoji appearance in the string which should be extracted.
     * @return A list of emojis.
     */
    public static Set<Emoji> extractEmojis(final String text, EnumSet<EmojiType> emojiType) {
        return Collections.unmodifiableSet(new HashSet<>(extractEmojisInOrder(text, emojiType)));
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text The text to extract emojis from.
     * @return A list of emojis.
     */
    public static List<Emoji> extractEmojisInOrder(final String text) {
        return extractEmojisInOrderWithIndex(text).stream().map(IndexedEmoji::getEmoji).collect(Collectors.toList());
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text      The text to extract emojis from.
     * @param emojiType The type of the emoji appearance in the string which should be extracted.
     * @return A list of emojis.
     */
    public static List<Emoji> extractEmojisInOrder(final String text, EnumSet<EmojiType> emojiType) {
        return extractEmojisInOrderWithIndex(text, emojiType).stream().map(IndexedEmoji::getEmoji).collect(Collectors.toList());
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text The text to extract emojis from.
     * @return A list of indexed emojis.
     */
    public static List<IndexedEmoji> extractEmojisInOrderWithIndex(final String text) {
        return extractEmojisInOrderWithIndex(text, EnumSet.of(EmojiType.UNICODE));
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text      The text to extract emojis from.
     * @param emojiType The type of the emoji appearance in the string which should be extracted.
     * @return A list of indexed emojis.
     */
    public static List<IndexedEmoji> extractEmojisInOrderWithIndex(final String text, EnumSet<EmojiType> emojiType) {
        if (isStringNullOrEmpty(text)) return Collections.emptyList();

        final List<IndexedEmoji> emojis = new ArrayList<>();

        final int[] textCodePointsArray = stringToCodePoints(text);
        final long textCodePointsLength = textCodePointsArray.length;

        int charIndex = 0;
        nextTextIteration:
        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];
            if (checkIfCodepointIsInvalidEmojiStarter(currentCodepoint)) {
                charIndex += Character.charCount(currentCodepoint);
                continue;
            }
            for (final EmojiType type : emojiType) {
                final UniqueEmojiFoundResult uniqueEmojiFoundResult = findUniqueEmoji(textCodePointsArray, textIndex, textCodePointsLength, type);
                if (uniqueEmojiFoundResult == null) {
                    continue;
                }

                final int startCharIndex = charIndex;
                final int startTextIndex = textIndex;

                for (int i = textIndex; i < uniqueEmojiFoundResult.getEndIndex(); i++) {
                    charIndex += Character.charCount(textCodePointsArray[i]);
                }
                emojis.add(new IndexedEmoji(uniqueEmojiFoundResult.getEmoji(), startCharIndex, startTextIndex, charIndex, textIndex));
                //-1 because loop adds +1
                textIndex = uniqueEmojiFoundResult.getEndIndex() - 1;

                continue nextTextIteration;
            }
            charIndex += Character.charCount(currentCodepoint);
        }

        return Collections.unmodifiableList(emojis);
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
        return removeAllEmojisExcept(text, Collections.emptyList(), EnumSet.of(EmojiType.UNICODE));
    }

    /**
     * Removes all emojis from the given text.
     *
     * @param text      The text to remove emojis from.
     * @param emojiType The type of the emoji appearance in the string which should be removed.
     * @return The text without emojis.
     */
    public static String removeAllEmojis(final String text, EnumSet<EmojiType> emojiType) {
        return removeAllEmojisExcept(text, Collections.emptyList(), emojiType);
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
        return removeEmojis(text, emojisToRemove, EnumSet.of(EmojiType.UNICODE));
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
        final Set<Emoji> emojis = new HashSet<>(EMOJIS_LENGTH_DESCENDING);
        emojis.removeAll(emojisToRemove);
        return removeAllEmojisExcept(text, emojis, emojiType);
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
        return removeAllEmojisExcept(text, emojisToKeep, EnumSet.of(EmojiType.UNICODE));
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
        // TODO: Could be replaced by #replaceEmojis?
        if (isStringNullOrEmpty(text)) return "";

        final int[] textCodePointsArray = stringToCodePoints(text);
        final long textCodePointsLength = textCodePointsArray.length;

        final StringBuilder sb = new StringBuilder();

        nextTextIteration:
        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];
            sb.appendCodePoint(currentCodepoint);
            if (checkIfCodepointIsInvalidEmojiStarter(currentCodepoint)) {
                continue;
            }
            for (final EmojiType type : emojiType) {
                final UniqueEmojiFoundResult uniqueEmojiFoundResult = findUniqueEmoji(textCodePointsArray, textIndex, textCodePointsLength, type);
                if (uniqueEmojiFoundResult == null) {
                    continue;
                }

                if (emojisToKeep.contains(uniqueEmojiFoundResult.getEmoji())) {
                    for (int i = textIndex + 1; i < uniqueEmojiFoundResult.getEndIndex(); i++) {
                        sb.appendCodePoint(textCodePointsArray[i]);
                    }
                } else {
                    sb.delete(sb.length() - Character.charCount(currentCodepoint), sb.length());
                }
                //-1 because loop adds +1
                textIndex = uniqueEmojiFoundResult.getEndIndex() - 1;
                continue nextTextIteration;
            }
        }
        return sb.toString();
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
        return replaceEmojis(text, replacementString, EMOJIS_LENGTH_DESCENDING);
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
        return replaceEmojis(text, replacementString, EMOJIS_LENGTH_DESCENDING, emojiType);
    }

    /**
     * Replaces all emojis in the text with the given replacement function.
     *
     * @param text                The text to replace emojis from.
     * @param replacementFunction The replacement function.
     * @return The text with all emojis replaced.
     */
    public static String replaceAllEmojis(final String text, final Function<Emoji, String> replacementFunction) {
        return replaceEmojis(text, replacementFunction, EMOJIS_LENGTH_DESCENDING);
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
        return replaceEmojis(text, replacementFunction, EMOJIS_LENGTH_DESCENDING, emojiType);
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
        return replaceEmojis(text, replacementFunction, emojisToReplace, EnumSet.of(EmojiType.UNICODE));
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
        if (isStringNullOrEmpty(text)) return "";

        final int[] textCodePointsArray = stringToCodePoints(text);
        final long textCodePointsLength = textCodePointsArray.length;

        final StringBuilder sb = new StringBuilder();

        nextTextIteration:
        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];
            sb.appendCodePoint(currentCodepoint);
            if (checkIfCodepointIsInvalidEmojiStarter(currentCodepoint)) {
                continue;
            }
            for (final EmojiType type : emojiType) {
                final UniqueEmojiFoundResult uniqueEmojiFoundResult = findUniqueEmoji(textCodePointsArray, textIndex, textCodePointsLength, type);
                if (uniqueEmojiFoundResult == null) {
                    continue;
                }

                //-1 because loop adds +1
                textIndex = uniqueEmojiFoundResult.getEndIndex() - 1;
                sb.delete(sb.length() - Character.charCount(currentCodepoint), sb.length());
                if (emojisToReplace.contains(uniqueEmojiFoundResult.getEmoji())) {
                    sb.append(replacementFunction.apply(uniqueEmojiFoundResult.getEmoji()));
                } else {
                    sb.append(uniqueEmojiFoundResult.getEmoji().getEmoji());
                }
                continue nextTextIteration;
            }
        }
        return sb.toString();
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
        if (isStringNullOrEmpty(text)) return "";

        final int[] textCodePointsArray = stringToCodePoints(text);
        final long textCodePointsLength = textCodePointsArray.length;

        final StringBuilder sb = new StringBuilder();

        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];
            sb.appendCodePoint(currentCodepoint);

            if (!POSSIBLE_EMOJI_ALIAS_STARTER_CODEPOINTS.contains(currentCodepoint)) {
                continue;
            }

            final NonUniqueEmojiFoundResult nonUniqueEmojiFoundResult = findNonUniqueEmoji(textCodePointsArray, textIndex, textCodePointsLength);
            if (nonUniqueEmojiFoundResult == null) {
                continue;
            }

            //-1 because loop adds +1
            textIndex = nonUniqueEmojiFoundResult.getEndIndex() - 1;
            sb.delete(sb.length() - Character.charCount(currentCodepoint), sb.length());

            sb.append(replacementFunction.apply(new String(nonUniqueEmojiFoundResult.getCodepointSequence().getCodepoints(), 0, nonUniqueEmojiFoundResult.getCodepointSequence().getCodepoints().length), nonUniqueEmojiFoundResult.getEmojis()));
        }

        return sb.toString();
    }

    /**
     * Extracts all aliases from the given text.
     *
     * @param text The text to extract aliases from.
     * @return A set of aliases.
     */
    public static Set<String> extractAliases(final String text) {
        return Collections.unmodifiableSet(new HashSet<>(extractAliasesInOrder(text)));
    }

    /**
     * Extracts all aliases from the given text in the order they appear.
     *
     * @param text The text to extract aliases from.
     * @return A list of aliases.
     */
    public static List<String> extractAliasesInOrder(final String text) {
        return extractAliasesInOrderWithIndex(text).stream().map(IndexedAlias::getAlias).collect(Collectors.toList());
    }

    /**
     * Extracts all aliases from the given text in the order they appear.
     *
     * @param text The text to extract aliases from.
     * @return A list of indexed aliases.
     */
    public static List<IndexedAlias> extractAliasesInOrderWithIndex(final String text) {
        if (isStringNullOrEmpty(text)) return Collections.emptyList();

        final int[] textCodePointsArray = stringToCodePoints(text);
        final long textCodePointsLength = textCodePointsArray.length;
        final List<IndexedAlias> indexedAliases = new ArrayList<>();
        int charIndex = 0;
        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];

            if (!POSSIBLE_EMOJI_ALIAS_STARTER_CODEPOINTS.contains(currentCodepoint)) {
                charIndex += Character.charCount(currentCodepoint);
                continue;
            }

            final NonUniqueEmojiFoundResult nonUniqueEmojiFoundResult = findNonUniqueEmoji(textCodePointsArray, textIndex, textCodePointsLength);
            if (nonUniqueEmojiFoundResult == null) {
                continue;
            }

            final int startCharIndex = charIndex;
            final int startTextIndex = textIndex;
            for (int i = textIndex; i < nonUniqueEmojiFoundResult.getEndIndex(); i++) {
                charIndex += Character.charCount(textCodePointsArray[i]);
            }

            indexedAliases.add(new IndexedAlias(new String(
                    nonUniqueEmojiFoundResult.getCodepointSequence().getCodepoints(), 0, nonUniqueEmojiFoundResult.getCodepointSequence().getCodepoints().length),
                    nonUniqueEmojiFoundResult.getEmojis(),
                    startCharIndex,
                    startTextIndex,
                    charIndex,
                    textIndex));

            //-1 because loop adds +1
            textIndex = nonUniqueEmojiFoundResult.getEndIndex() - 1;
        }
        return indexedAliases;
    }
}

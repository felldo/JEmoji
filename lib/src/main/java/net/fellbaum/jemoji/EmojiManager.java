package net.fellbaum.jemoji;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class EmojiManager {

    private static class FunctionResult<T> {
        private final T result;
        private final boolean keepLooking;

        private FunctionResult(T result, boolean keepLooking) {
            this.result = result;
            this.keepLooking = keepLooking;
        }

        private static <T> FunctionResult<T> keepLooking() {
            return new FunctionResult<>(null, true);
        }

        private static <T> FunctionResult<T> returnValue(T result) {
            return new FunctionResult<>(result, false);
        }
    }

    private interface EmojiProcessor<T> {
        T getDefaultValue();

        default void onCodepoint(int codepoint) {}

        FunctionResult<T> onEmoji(int textIndex, Emoji emoji);
    }

    private interface VoidEmojiProcessor extends EmojiProcessor<Void> {
        @Override
        default Void getDefaultValue() { return null; }
    }

    private static final String PATH = "emoji_sources/emojis.json";

    private static final Map<String, Emoji> EMOJI_UNICODE_TO_EMOJI;
    private static final Map<Integer, List<Emoji>> EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING;
    private static final List<Emoji> EMOJIS_LENGTH_DESCENDING;

    // Get emoji by alias
    private static final Map<AliasGroup, Map<String, Emoji>> ALIAS_GROUP_TO_EMOJI_ALIAS_TO_EMOJI = new EnumMap<>(AliasGroup.class);

    private static Pattern EMOJI_PATTERN;
    private static final Pattern NOT_WANTED_EMOJI_CHARACTERS = Pattern.compile("[\\p{Alpha}\\p{Z}]");

    private static final Comparator<Emoji> EMOJI_CODEPOINT_COMPARATOR = (final Emoji o1, final Emoji o2) -> {
        final int codePointCount1 = getCodePointCount(o1.getEmoji());
        final int codePointCount2 = getCodePointCount(o2.getEmoji());
        if (codePointCount1 == codePointCount2) return 0;
        return codePointCount1 > codePointCount2 ? -1 : 1;
    };

    static {
        final String fileContent = readFileAsString();
        try {
            final List<Emoji> emojis = new ObjectMapper().readValue(fileContent, new TypeReference<List<Emoji>>() {
            });

            EMOJI_UNICODE_TO_EMOJI = Collections.unmodifiableMap(
                    emojis.stream().collect(Collectors.toMap(Emoji::getEmoji, Function.identity()))
            );

            EMOJIS_LENGTH_DESCENDING = Collections.unmodifiableList(emojis.stream().sorted(EMOJI_CODEPOINT_COMPARATOR).collect(Collectors.toList()));

            EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING = emojis.stream().collect(getEmojiLinkedHashMapCollector());
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getCodePointCount(String string) {
        return string.codePointCount(0, string.length());
    }

    private static Collector<Emoji, ?, LinkedHashMap<Integer, List<Emoji>>> getEmojiLinkedHashMapCollector() {
        return Collectors.groupingBy(
                emoji -> emoji.getEmoji().codePointAt(0),
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            list.sort(EMOJI_CODEPOINT_COMPARATOR);
                            return list;
                        }
                )
        );
    }

    private static String readFileAsString() {
        try {
            final ClassLoader classLoader = EmojiManager.class.getClassLoader();
            try (final InputStream is = classLoader.getResourceAsStream(PATH)) {
                if (is == null) return null;
                try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                     final BufferedReader reader = new BufferedReader(isr)) {
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Emoji> getEmojiAliasToEmoji(AliasGroup aliasGroup) {
        return ALIAS_GROUP_TO_EMOJI_ALIAS_TO_EMOJI.computeIfAbsent(aliasGroup, group -> {
            final Map<String, Emoji> emojiAliasToEmoji = new HashMap<>();
            for (Emoji emoji : EMOJIS_LENGTH_DESCENDING) {
                for (String alias : group.getAliasCollectionSupplier().apply(emoji)) {
                    emojiAliasToEmoji.put(alias, emoji);
                }
            }
            return Collections.unmodifiableMap(emojiAliasToEmoji);
        });
    }

    private EmojiManager() {
    }

    /**
     * Returns the emoji for the given unicode.
     *
     * @param emoji The unicode of the emoji.
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
    public static Optional<Emoji> getByAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        final String aliasWithoutColon = removeColonFromAlias(alias);
        final String aliasWithColon = addColonToAlias(alias);
        return Arrays.stream(AliasGroup.values())
                .map(EmojiManager::getEmojiAliasToEmoji)
                .filter(m -> m.containsKey(aliasWithColon) || m.containsKey(aliasWithoutColon))
                .map(m -> getEither(m, aliasWithColon, aliasWithoutColon))
                .findAny();
    }

    /**
     * Gets an emoji for the given Discord alias i.e. :thumbsup: if present.
     *
     * @param alias The Discord alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByDiscordAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        final String aliasWithoutColon = removeColonFromAlias(alias);
        final String aliasWithColon = addColonToAlias(alias);
        final Map<String, Emoji> emojiAliasToEmoji = getEmojiAliasToEmoji(AliasGroup.DISCORD);
        return Optional.of(getEither(emojiAliasToEmoji, aliasWithColon, aliasWithoutColon));
    }

    /**
     * Gets an emoji for the given GitHub alias i.e. :thumbsup: if present.
     *
     * @param alias The GitHub alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByGithubAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        final String aliasWithoutColon = removeColonFromAlias(alias);
        final String aliasWithColon = addColonToAlias(alias);
        final Map<String, Emoji> emojiAliasToEmoji = getEmojiAliasToEmoji(AliasGroup.GITHUB);
        return Optional.of(getEither(emojiAliasToEmoji, aliasWithColon, aliasWithoutColon));
    }

    /**
     * Gets an emoji for the given Slack alias i.e. :thumbsup: if present.
     *
     * @param alias The Slack alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getBySlackAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        final String aliasWithoutColon = removeColonFromAlias(alias);
        final String aliasWithColon = addColonToAlias(alias);
        final Map<String, Emoji> emojiAliasToEmoji = getEmojiAliasToEmoji(AliasGroup.SLACK);
        return Optional.of(getEither(emojiAliasToEmoji, aliasWithColon, aliasWithoutColon));
    }

    private static String removeColonFromAlias(final String alias) {
        return alias.startsWith(":") && alias.endsWith(":") ? alias.substring(1, alias.length() - 1) : alias;
    }

    private static String addColonToAlias(final String alias) {
        return alias.startsWith(":") && alias.endsWith(":") ? alias : ":" + alias + ":";
    }

    private static <K, V> V getEither(Map<K, V> map, K first, K second) {
        final V firstValue = map.get(first);
        if (firstValue != null) return firstValue;
        final V secondValue = map.get(second);
        if (secondValue != null) return secondValue;
        throw new NoSuchElementException("No element for " + first + " or " + second);
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
    public static boolean containsEmoji(final String text) {
        if (isStringNullOrEmpty(text)) return false;

        return forEachEmoji(text, new EmojiProcessor<Boolean>() {
            @Override
            public Boolean getDefaultValue() { return Boolean.FALSE; }

            @Override
            public FunctionResult<Boolean> onEmoji(int textIndex, Emoji emoji) {
                return FunctionResult.returnValue(Boolean.TRUE);
            }
        });
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text The text to extract emojis from.
     * @return A list of emojis.
     */
    public static List<Emoji> extractEmojisInOrder(final String text) {
        if (isStringNullOrEmpty(text)) return Collections.emptyList();

        final List<Emoji> emojis = new ArrayList<>();
        forEachEmoji(text, (VoidEmojiProcessor) (textIndex, emoji) -> {
            emojis.add(emoji);
            return FunctionResult.keepLooking();
        });
        return Collections.unmodifiableList(emojis);
    }

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
     * Removes all emojis from the given text.
     *
     * @param text The text to remove emojis from.
     * @return The text without emojis.
     */
    public static String removeAllEmojis(final String text) {
        return removeAllEmojisExcept(text, Collections.emptyList());
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
        final Set<Emoji> emojis = new HashSet<>(EMOJIS_LENGTH_DESCENDING);
        emojis.removeAll(emojisToRemove);
        return removeAllEmojisExcept(text, emojis);
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
        if (isStringNullOrEmpty(text)) return "";

        final StringBuilder sb = new StringBuilder();

        forEachEmoji(text, new VoidEmojiProcessor() {
            private int currentCodepoint;

            @Override
            public void onCodepoint(int codepoint) {
                currentCodepoint = codepoint;

                sb.appendCodePoint(currentCodepoint);
            }

            @Override
            public FunctionResult<Void> onEmoji(int textIndex, Emoji emoji) {
                sb.delete(sb.length() - Character.charCount(currentCodepoint), sb.length());

                if (emojisToKeep.contains(emoji)) {
                    // if the emoji should be kept, add it again
                    sb.append(emoji.getEmoji());
                }

                return FunctionResult.keepLooking();
            }
        });

        return sb.toString();
    }

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
     * Replaces all emojis in the text with the given replacement function.
     *
     * @param text                The text to replace emojis from.
     * @param replacementFunction The replacement function.
     * @return The text with all emojis replaced.
     */
    public static String replaceAllEmojis(final String text, Function<Emoji, String> replacementFunction) {
        return replaceEmojis(text, replacementFunction, EMOJIS_LENGTH_DESCENDING);
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
    public static String replaceEmojis(final String text, Function<Emoji, String> replacementFunction, final Collection<Emoji> emojisToReplace) {
        if (isStringNullOrEmpty(text)) return "";

        final StringBuilder sb = new StringBuilder(text.length());

        forEachEmoji(text, new VoidEmojiProcessor() {
            private int currentCodepoint;

            @Override
            public void onCodepoint(int codepoint) {
                currentCodepoint = codepoint;

                sb.appendCodePoint(codepoint);
            }

            @Override
            public FunctionResult<Void> onEmoji(int textIndex, Emoji emoji) {
                sb.delete(sb.length() - Character.charCount(currentCodepoint), sb.length());

                if (emojisToReplace.contains(emoji)) {
                    sb.append(replacementFunction.apply(emoji));
                } else {
                    sb.append(emoji.getEmoji());
                }

                return FunctionResult.keepLooking();
            }
        });

        return sb.toString();
    }

    /**
     * Replaces all emojis in the text with the given replacement function.
     *
     * @param text                The text to replace emojis from.
     * @param replacementFunction The replacement function.
     * @param emojisToReplace     The emojis to replace.
     * @return The text with all emojis replaced.
     */
    public static String replaceEmojis(final String text, Function<Emoji, String> replacementFunction, final Emoji... emojisToReplace) {
        return replaceEmojis(text, replacementFunction, Arrays.asList(emojisToReplace));
    }

    private static <T> T forEachEmoji(final String text,
                                      final EmojiProcessor<T> processor) {
        final int[] textCodePointsArray = stringToCodePoints(text);
        final long textCodePointsLength = textCodePointsArray.length;

        nextTextIteration:
        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];
            processor.onCodepoint(currentCodepoint);

            final List<Emoji> emojisByCodePoint = EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.get(currentCodepoint);
            if (emojisByCodePoint == null) continue;
            for (final Emoji emoji : emojisByCodePoint) {
                final int[] emojiCodePointsArray = stringToCodePoints(emoji.getEmoji());
                final int emojiCodePointsLength = emojiCodePointsArray.length;
                // Emoji code points are in bounds of the text code points
                if (!((textIndex + emojiCodePointsLength) <= textCodePointsLength)) {
                    continue;
                }

                for (int emojiCodePointIndex = 0; emojiCodePointIndex < emojiCodePointsLength; emojiCodePointIndex++) {
                    //break out because the emoji is not the same
                    if (textCodePointsArray[textIndex + emojiCodePointIndex] != emojiCodePointsArray[emojiCodePointIndex]) {
                        break;
                    }

                    if (emojiCodePointIndex == emojiCodePointsLength - 1) {
                        final FunctionResult<T> functionResult = processor.onEmoji(textIndex, emoji);
                        if (functionResult.keepLooking) {
                            textIndex += emojiCodePointsLength - 1;
                            continue nextTextIteration;
                        } else {
                            return functionResult.result;
                        }
                    }
                }
            }
        }

        return processor.getDefaultValue();
    }

    private static int[] stringToCodePoints(String text) {
        int[] codePoints = new int[getCodePointCount(text)];
        int j = 0;
        for (int i = 0; i < text.length();) {
            final int codePoint = text.codePointAt(i);
            codePoints[j++] = codePoint;
            i += Character.charCount(codePoint);
        }
        return codePoints;
    }

    private static boolean isStringNullOrEmpty(final String string) {
        return null == string || string.isEmpty();
    }


    /*public static List<Emoji> testEmojiPattern(final String text) {
        if (isStringNullOrEmpty(text)) return Collections.emptyList();

        final Matcher matcher = EMOJI_PATTERN.matcher(text);

        final List<Emoji> emojis = new ArrayList<>();
        while (matcher.find()) {
            emojis.add(EMOJIS_LENGTH_DESCENDING.stream().filter(emoji -> emoji.getEmoji().equals(matcher.group())).findFirst().get());
        }
        return Collections.unmodifiableList(emojis);
    }*/

    /*public static List<Emoji> extractEmojisInOrderEmojiRegex(String text) {
        if (isStringNullOrEmpty(text)) return Collections.emptyList();

        final List<Emoji> emojis = new ArrayList<>();
        System.out.println(EMOJI_PATTERN.pattern());
        System.out.println(EMOJI_PATTERN.toString());

        Matcher matcher = EMOJI_PATTERN.matcher(text);
        while (matcher.find()) {
            String emoji = matcher.group();

            emojis.add(EMOJI_CHAR_TO_EMOJI.get(emoji));
        }

        return emojis;
    }*/
}



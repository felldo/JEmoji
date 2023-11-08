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

    private static final String PATH = "emoji_sources/emojis.json";

    private static final Map<String, Emoji> EMOJI_UNICODE_TO_EMOJI;
    private static final Map<Integer, List<Emoji>> EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING;
    private static final List<Emoji> EMOJIS_LENGTH_DESCENDING;

    private static Pattern EMOJI_PATTERN;
    private static final Pattern NOT_WANTED_EMOJI_CHARACTERS = Pattern.compile("[\\p{Alpha}\\p{Z}]");

    private static final Comparator<Emoji> EMOJI_CODEPOINT_COMPARATOR = (final Emoji o1, final Emoji o2) -> {
        if (o1.getEmoji().codePoints().toArray().length == o2.getEmoji().codePoints().toArray().length) return 0;
        return o1.getEmoji().codePoints().toArray().length > o2.getEmoji().codePoints().toArray().length ? -1 : 1;
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

    private static Collector<Emoji, ?, LinkedHashMap<Integer, List<Emoji>>> getEmojiLinkedHashMapCollector() {
        return Collectors.groupingBy(
                emoji -> emoji.getEmoji().codePoints().toArray()[0],
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
        return EMOJI_UNICODE_TO_EMOJI.values()
                .stream()
                .filter(emoji -> emoji.getAllAliases().contains(aliasWithoutColon) || emoji.getAllAliases().contains(aliasWithColon))
                .findFirst();
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
        return EMOJI_UNICODE_TO_EMOJI.values()
                .stream()
                .filter(emoji -> emoji.getDiscordAliases().contains(aliasWithoutColon) || emoji.getDiscordAliases().contains(aliasWithColon))
                .findFirst();
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
        return EMOJI_UNICODE_TO_EMOJI.values()
                .stream()
                .filter(emoji -> emoji.getGithubAliases().contains(aliasWithoutColon) || emoji.getGithubAliases().contains(aliasWithColon))
                .findFirst();
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
        return EMOJI_UNICODE_TO_EMOJI.values()
                .stream()
                .filter(emoji -> emoji.getSlackAliases().contains(aliasWithoutColon) || emoji.getSlackAliases().contains(aliasWithColon))
                .findFirst();
    }

    private static String removeColonFromAlias(final String alias) {
        return alias.startsWith(":") && alias.endsWith(":") ? alias.substring(1, alias.length() - 1) : alias;
    }

    private static String addColonToAlias(final String alias) {
        return alias.startsWith(":") && alias.endsWith(":") ? alias : ":" + alias + ":";
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

        final List<Emoji> emojis = new ArrayList<>();

        final int[] textCodePointsArray = text.codePoints().toArray();
        final long textCodePointsLength = textCodePointsArray.length;

        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final List<Emoji> emojisByCodePoint = EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.get(textCodePointsArray[textIndex]);
            if (emojisByCodePoint == null) continue;
            for (final Emoji emoji : emojisByCodePoint) {
                final int[] emojiCodePointsArray = emoji.getEmoji().codePoints().toArray();
                final int emojiCodePointsLength = emojiCodePointsArray.length;
                // Emoji code points are in bounds of the text code points
                if (!((textIndex + emojiCodePointsLength) <= textCodePointsLength)) {
                    continue;
                }

                for (int i = 0; i < emojiCodePointsLength; i++) {
                    if (textCodePointsArray[textIndex + i] != emojiCodePointsArray[i]) {
                        break;
                    }
                    if (i == emojiCodePointsLength - 1) {
                        return true;
                    }
                }
            }
        }
        return false;
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

        final int[] textCodePointsArray = text.codePoints().toArray();
        final long textCodePointsLength = textCodePointsArray.length;

        // JDK 21 Characters.isEmoji

        nextTextIteration:
        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];
            final List<Emoji> emojisByCodePoint = EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.get(currentCodepoint);
            if (emojisByCodePoint == null) continue;
            for (final Emoji emoji : emojisByCodePoint) {
                final int[] emojiCodePointsArray = emoji.getEmoji().codePoints().toArray();
                final int emojiCodePointsLength = emojiCodePointsArray.length;
                // Emoji code points are in bounds of the text code points
                if (!((textIndex + emojiCodePointsLength) <= textCodePointsLength)) {
                    continue;
                }

                for (int emojiCodePointIndex = 0; emojiCodePointIndex < emojiCodePointsLength; emojiCodePointIndex++) {
                    if (textCodePointsArray[textIndex + emojiCodePointIndex] != emojiCodePointsArray[emojiCodePointIndex]) {
                        break;
                    }
                    if (emojiCodePointIndex == (emojiCodePointsLength - 1)) {
                        emojis.add(emoji);
                        textIndex += emojiCodePointsLength - 1;
                        continue nextTextIteration;
                    }
                }
            }
        }
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
        final int[] textCodePointsArray = text.codePoints().toArray();
        final long textCodePointsLength = textCodePointsArray.length;

        final StringBuilder sb = new StringBuilder();

        nextTextIteration:
        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];
            sb.appendCodePoint(currentCodepoint);

            final List<Emoji> emojisByCodePoint = EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.get(currentCodepoint);
            if (emojisByCodePoint == null) continue;
            for (final Emoji emoji : emojisByCodePoint) {
                final int[] emojiCodePointsArray = emoji.getEmoji().codePoints().toArray();
                final int emojiCodePointsLength = emojiCodePointsArray.length;
                // Check if Emoji code points are in bounds of the text code points
                if (!((textIndex + emojiCodePointsLength) <= textCodePointsLength)) {
                    continue;
                }

                for (int emojiCodePointIndex = 0; emojiCodePointIndex < emojiCodePointsLength; emojiCodePointIndex++) {
                    //break out because the emoji is not the same
                    if (textCodePointsArray[textIndex + emojiCodePointIndex] != emojiCodePointsArray[emojiCodePointIndex]) {
                        break;
                    }

                    if (emojiCodePointIndex == (emojiCodePointsLength - 1)) {
                        textIndex += emojiCodePointsLength - 1;
                        sb.delete(sb.length() - Character.charCount(currentCodepoint), sb.length());

                        if (emojisToKeep.contains(emoji)) {
                            // if the emoji should be kept, add it again
                            sb.append(emoji.getEmoji());
                        }
                        continue nextTextIteration;
                    }
                }
            }
        }

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

        final int[] textCodePointsArray = text.codePoints().toArray();
        final long textCodePointsLength = textCodePointsArray.length;

        final StringBuilder sb = new StringBuilder();

        nextTextIteration:
        for (int textIndex = 0; textIndex < textCodePointsLength; textIndex++) {
            final int currentCodepoint = textCodePointsArray[textIndex];
            sb.appendCodePoint(currentCodepoint);

            final List<Emoji> emojisByCodePoint = EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING.get(currentCodepoint);
            if (emojisByCodePoint == null) continue;
            for (final Emoji emoji : emojisByCodePoint) {
                final int[] emojiCodePointsArray = emoji.getEmoji().codePoints().toArray();
                final int emojiCodePointsLength = emojiCodePointsArray.length;
                // Check if Emoji code points are in bounds of the text code points
                if (!((textIndex + emojiCodePointsLength) <= textCodePointsLength)) {
                    continue;
                }

                for (int emojiCodePointIndex = 0; emojiCodePointIndex < emojiCodePointsLength; emojiCodePointIndex++) {
                    //break out because the emoji is not the same
                    if (textCodePointsArray[textIndex + emojiCodePointIndex] != emojiCodePointsArray[emojiCodePointIndex]) {
                        break;
                    }

                    if (emojiCodePointIndex == (emojiCodePointsLength - 1)) {
                        textIndex += emojiCodePointsLength - 1;
                        sb.delete(sb.length() - Character.charCount(currentCodepoint), sb.length());

                        if (emojisToReplace.contains(emoji)) {
                            sb.append(replacementFunction.apply(emoji));
                        } else {
                            sb.append(emoji.getEmoji());
                        }

                        continue nextTextIteration;
                    }
                }
            }
        }

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



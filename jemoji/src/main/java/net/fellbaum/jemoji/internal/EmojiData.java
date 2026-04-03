package net.fellbaum.jemoji.internal;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiLanguage;
import net.fellbaum.jemoji.EmojiManager;
import net.fellbaum.jemoji.Qualification;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.fellbaum.jemoji.internal.EmojiUtils.stringToCodePoints;

public class EmojiData {

    public static List<Emoji> EMOJIS_LENGTH_DESCENDING = null;
    public static Map<String, Emoji> EMOJI_UNICODE_TO_EMOJI = null;
    public static Map<Integer, List<Emoji>> EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING = null;
    public static Map<String, Emoji> EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI = null;
    public static Map<String, Emoji> EMOJI_HTML_HEXADECIMAL_REPRESENTATION_TO_EMOJI = null;
    public static Map<String, Emoji> EMOJI_URL_ENCODED_REPRESENTATION_TO_EMOJI = null;

    public static Map<CodepointSequence, List<Emoji>> ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING = null;

    @Nullable
    public static Pattern EMOJI_PATTERN;

    public static final Map<EmojiLanguage, Map<String, String>> EMOJI_DESCRIPTION_LANGUAGE_MAP = new HashMap<>();
    public static final Map<EmojiLanguage, Map<String, List<String>>> EMOJI_KEYWORD_LANGUAGE_MAP = new HashMap<>();

    public static volatile boolean unicodeInitialized = false;
    public static final Object UNICODE_INIT_LOCK = new Object();

    public static void ensureUnicodeInitialized() {
        if (!unicodeInitialized) {
            synchronized (UNICODE_INIT_LOCK) {
                if (!unicodeInitialized) {
                    initUnicode();
                    unicodeInitialized = true;
                }
            }
        }
    }

    private static void initUnicode() {
        EMOJIS_LENGTH_DESCENDING = InitHelper.emojisLengthDescending();
        EMOJI_UNICODE_TO_EMOJI = InitHelper.emojiUnicodeToEmoji();
        EMOJI_FIRST_CODEPOINT_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING = InitHelper.emojiFirstCodepointToEmojisOrderCodepointLengthDescending();
        EMOJI_HTML_DECIMAL_REPRESENTATION_TO_EMOJI = InitHelper.emojiHtmlDecimalRepresentationToEmoji();
        EMOJI_HTML_HEXADECIMAL_REPRESENTATION_TO_EMOJI = InitHelper.emojiHtmlHexadecimalRepresentationToEmoji();
        EMOJI_URL_ENCODED_REPRESENTATION_TO_EMOJI = InitHelper.emojiUrlEncodedRepresentationToEmoji();
        InitHelper.initFinished();
    }

    private static volatile boolean aliasInitialized = false;
    private static final Object ALIAS_INIT_LOCK = new Object();

    public static void ensureAliasInitialized() {
        if (!aliasInitialized) {
            synchronized (ALIAS_INIT_LOCK) {
                if (!aliasInitialized) {
                    initAlias();
                    aliasInitialized = true;
                }
            }
        }
    }

    private static void initAlias() {
        ALIAS_EMOJI_TO_EMOJIS_ORDER_CODEPOINT_LENGTH_DESCENDING = InitHelper.aliasEmojiToEmojisOrderCodepointLengthDescending();
    }

    //https://pangin.pro/posts/computation-in-static-initializer
    private static final class InitHelper {

        @Nullable
        private static Set<Emoji> EMOJIS = null;
        static ObjectMapper objectMapper = new ObjectMapper();

        static Object readFileAsObject(final String filePathName) {
            try {
                try (final InputStream is = EmojiManager.class.getResourceAsStream(filePathName);
                     final BufferedInputStream bis = new BufferedInputStream(Objects.requireNonNull(is));
                     final ObjectInputStream ois = new ObjectInputStream(bis);) {
                    return ois.readObject();
                } catch (final ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        static Set<Emoji> getEmojis() {
            if (EMOJIS == null) {
                EMOJIS = objectMapper.readValue(EmojiManager.class.getResourceAsStream("/jemoji/emojis.json"), new TypeReference<>() {
                });
            }
            return EMOJIS;
        }

        static List<Emoji> emojisLengthDescending() {
            return Collections.unmodifiableList(getEmojis().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()));
        }

        static Map<String, Emoji> emojiUnicodeToEmoji() {
            final Map<String, Emoji> resultMap = new HashMap<>();
            prepareEmojisStreamForInitialization(getEmojis()).forEach(entry -> {
                final String key = entry.getKey();
                final Emoji emoji = entry.getValue();

                resultMap.merge(key, emoji, (existing, replacement) -> {
                    // Prefer exact match on the base emoji string (without variation selectors)
                    if (replacement.getEmoji().equals(key)) {
                        return replacement;
                    }
                    if (existing.getEmoji().equals(key)) {
                        return existing;
                    }

                    // If both don't match exactly, prefer FULLY_QUALIFIED
                    if (replacement.getQualification() == Qualification.FULLY_QUALIFIED &&
                            existing.getQualification() != Qualification.FULLY_QUALIFIED) {
                        return replacement;
                    }

                    return existing;
                });
            });
            return Map.copyOf(resultMap);
        }

        static Map<Integer, List<Emoji>> emojiFirstCodepointToEmojisOrderCodepointLengthDescending() {
            return Map.copyOf(prepareEmojisStreamForInitialization(getEmojis()).collect(getEmojiLinkedHashMapCollector()));
        }

        static Map<CodepointSequence, List<Emoji>> aliasEmojiToEmojisOrderCodepointLengthDescending() {
            return mapAliasesToEmojis(getEmojis());
        }

        static Map<String, Emoji> emojiHtmlDecimalRepresentationToEmoji() {
            return getEmojis().stream().collect(Collectors.toUnmodifiableMap(
                    o -> o.getHtmlDecimalCode().toUpperCase(),
                    Function.identity(),
                    (existing, replacement) -> existing)
            );
        }

        static Map<String, Emoji> emojiHtmlHexadecimalRepresentationToEmoji() {
            return getEmojis().stream().collect(Collectors.toUnmodifiableMap(
                    o -> o.getHtmlHexadecimalCode().toUpperCase(),
                    Function.identity(),
                    (existing, replacement) -> existing)
            );
        }

        static Map<String, Emoji> emojiUrlEncodedRepresentationToEmoji() {
            return getEmojis().stream().collect(Collectors.toUnmodifiableMap(
                    o -> o.getURLEncoded().toUpperCase(),
                    Function.identity(),
                    (existing, replacement) -> existing)
            );
        }

        static boolean initFinished() {
            EMOJIS = null;
            return true;
        }

        private static Map<CodepointSequence, List<Emoji>> mapAliasesToEmojis(final Collection<Emoji> emojis) {
            return emojis.stream()
                    .flatMap(emoji -> emoji.getAllAliases().stream()
                            .map(alias -> new AbstractMap.SimpleEntry<>(new CodepointSequence(stringToCodePoints(alias)), emoji)))
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

        private static Stream<AbstractMap.SimpleEntry<String, Emoji>> prepareEmojisStreamForInitialization(final Collection<Emoji> emojis) {
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
    }


}

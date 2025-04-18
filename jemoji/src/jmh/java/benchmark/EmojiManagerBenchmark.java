package benchmark;

import net.fellbaum.jemoji.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class EmojiManagerBenchmark {

    private static final String TEXT = new BufferedReader(new InputStreamReader(Objects.requireNonNull(EmojiManagerBenchmark.class.getClassLoader().getResourceAsStream("ExampleTextFileWithEmojis.txt"))))
            .lines().collect(Collectors.joining("\n"));

    private static final String CONTAINS_EMOJI_TEXT = new BufferedReader(new InputStreamReader(Objects.requireNonNull(EmojiManagerBenchmark.class.getClassLoader().getResourceAsStream("ContainsBenchmarkTextFileWithEmojis.txt"))))
            .lines().collect(Collectors.joining("\n"));

    private static final String ALIAS_TEXT = EmojiManager.replaceAllEmojis(new BufferedReader(new InputStreamReader(Objects.requireNonNull(EmojiManagerBenchmark.class.getClassLoader().getResourceAsStream("ExampleTextFileWithEmojis.txt"))))
            .lines().collect(Collectors.joining("\n")), emoji -> emoji.getAllAliases().stream().findFirst().orElse("null"));

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    private static final Collector<?, ?, ?> SHUFFLER = Collectors.collectingAndThen(
            Collectors.toCollection(ArrayList::new),
            list -> {
                Collections.shuffle(list);
                return list;
            }
    );

    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, List<T>> toShuffledList() {
        return (Collector<T, ?, List<T>>) SHUFFLER;
    }

    public static final String EMOJI_STARTER_STRING = IntStream.range(0, 1000000)
            .mapToObj(i -> "#")
            .collect(Collectors.joining());

    private static final String EMOJIS_RANDOM_ORDER = String.join("", EmojiManager.getAllEmojisLengthDescending().stream().map(Emoji::getEmoji).collect(toShuffledList()));

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Optional<Emoji> getByDiscordAlias() {
        return EmojiManager.getByDiscordAlias(":merman::skin-tone-5:");
    }

    @Benchmark
    //@BenchmarkMode(Mode.AverageTime)
    //@Warmup(iterations = 1)
    public String replaceAllEmojis() {
        return EmojiManager.replaceAllEmojis(TEXT, "<replaced emoji>");
    }

    @Benchmark
    public String replaceAllEmojisManyStarter() {
        return EmojiManager.replaceAllEmojis(EMOJI_STARTER_STRING, "<replaced emoji>");
    }

    @Benchmark
    public String replaceAllEmojisFunction() {
        return EmojiManager.replaceAllEmojis(TEXT, emoji -> emoji.getGroup().toString());
    }

    @Benchmark
    public String removeAllEmojis() {
        return EmojiManager.removeAllEmojis(TEXT);
    }

    @Benchmark
    public List<Emoji> extractEmojisInOrder() {
        return EmojiManager.extractEmojisInOrder(TEXT);
    }

    @Benchmark
    public List<IndexedEmoji> extractEmojisInOrderWithIndex() {
        return EmojiManager.extractEmojisInOrderWithIndex(TEXT);
    }

    @Benchmark
    public List<Emoji> extractEmojisInOrderOnlyEmojisLengthDescending() {
        return EmojiManager.extractEmojisInOrder(EmojiManagerTest.ALL_EMOJIS_STRING);
    }

    @Benchmark
    public List<Emoji> extractEmojisInOrderOnlyEmojisRandomOrder() {
        return EmojiManager.extractEmojisInOrder(EMOJIS_RANDOM_ORDER);
    }

    @Benchmark
    public boolean containsEmoji() {
        return EmojiManager.containsAnyEmoji(CONTAINS_EMOJI_TEXT);
    }

    @Benchmark
    public String replaceAliasesFunction() {
        return EmojiManager.replaceAliases(ALIAS_TEXT, (s, emojis) -> "<replaced alias>");
    }

    @Benchmark
    public List<IndexedAlias> extractAliasesInOrder() {
        return EmojiManager.extractAliasesInOrderWithIndex(ALIAS_TEXT);
    }

}

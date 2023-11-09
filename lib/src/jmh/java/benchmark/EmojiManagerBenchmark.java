package benchmark;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import net.fellbaum.jemoji.EmojiManagerTest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
public class EmojiManagerBenchmark {

    private static final String TEXT = new BufferedReader(new InputStreamReader(Objects.requireNonNull(EmojiManagerBenchmark.class.getClassLoader().getResourceAsStream("ExampleTextFileWithEmojis.txt"))))
            .lines().collect(Collectors.joining("\n"));

    private static final String CONTAINS_EMOJI_TEXT = new BufferedReader(new InputStreamReader(Objects.requireNonNull(EmojiManagerBenchmark.class.getClassLoader().getResourceAsStream("ContainsBenchmarkTextFileWithEmojis.txt"))))
            .lines().collect(Collectors.joining("\n"));

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

    private static final String EMOJIS_RANDOM_ORDER = String.join("", EmojiManager.getAllEmojisLengthDescending().stream().map(Emoji::getEmoji).collect(toShuffledList()));

    @Param({":+1:", "nope"})
    private String alias;

    @Benchmark
    //@BenchmarkMode(Mode.AverageTime)
    //@Warmup(iterations = 1)
    public Optional<Emoji> getByAlias() {
        return EmojiManager.getByAlias(alias);
    }

    @Benchmark
    //@BenchmarkMode(Mode.AverageTime)
    //@Warmup(iterations = 1)
    public String replaceAllEmojis() {
        return EmojiManager.replaceAllEmojis(TEXT, "<replaced emoji>");
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
    public List<Emoji> extractEmojisInOrderOnlyEmojisLengthDescending() {
        return EmojiManager.extractEmojisInOrder(EmojiManagerTest.ALL_EMOJIS_STRING);
    }

    @Benchmark
    public List<Emoji> extractEmojisInOrderOnlyEmojisRandomOrder() {
        return EmojiManager.extractEmojisInOrder(EMOJIS_RANDOM_ORDER);
    }

    @Benchmark
    public boolean containsEmoji() {
        return EmojiManager.containsEmoji(CONTAINS_EMOJI_TEXT);
    }

}

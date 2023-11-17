package benchmark;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import net.fellbaum.jemoji.EmojiManagerTest;
import org.openjdk.jmh.annotations.Benchmark;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class EmojiManagerBenchmark {


    /*
    <tem
    Benchmark                                                             Mode  Cnt   Score   Error  Units
EmojiManagerBenchmark.extractEmojisInOrder                            avgt   10   2,206 ± 0,013  ms/op
EmojiManagerBenchmark.extractEmojisInOrderOnlyEmojisLengthDescending  avgt   10   9,773 ± 0,083  ms/op
EmojiManagerBenchmark.extractEmojisInOrderOnlyEmojisRandomOrder       avgt   10  10,469 ± 0,107  ms/op
EmojiManagerBenchmark.removeAllEmojis                                 avgt   10   2,822 ± 0,026  ms/op
EmojiManagerBenchmark.replaceAllEmojis                                avgt   10   2,836 ± 0,070  ms/op
EmojiManagerBenchmark.replaceAllEmojisFunction                        avgt   10   2,834 ± 0,015  ms/op

Benchmark                                                             Mode  Cnt   Score   Error  Units
EmojiManagerBenchmark.extractEmojisInOrder                            avgt   10   2,186 ± 0,057  ms/op
EmojiManagerBenchmark.extractEmojisInOrderOnlyEmojisLengthDescending  avgt   10  10,044 ± 0,117  ms/op
EmojiManagerBenchmark.extractEmojisInOrderOnlyEmojisRandomOrder       avgt   10  10,584 ± 0,196  ms/op
EmojiManagerBenchmark.removeAllEmojis                                 avgt   10   3,062 ± 0,203  ms/op
EmojiManagerBenchmark.replaceAllEmojis                                avgt   10   3,034 ± 0,042  ms/op
EmojiManagerBenchmark.replaceAllEmojisFunction                        avgt   10   3,319 ± 0,281  ms/op
     */
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

    @Benchmark
    //@BenchmarkMode(Mode.AverageTime)
    //@Warmup(iterations = 1)
    public String replaceAllEmojis() {

        EmojiManager.replaceAllEmojis(TEXT, "<replaced emoji>");
        EmojiManager.replaceAllEmojis(TEXT, "<replaced emoji>");
        EmojiManager.replaceAllEmojis(TEXT, "<replaced emoji>");
        EmojiManager.replaceAllEmojis(TEXT, "<replaced emoji>");
        EmojiManager.replaceAllEmojis(TEXT, "<replaced emoji>");
        return EmojiManager.replaceAllEmojis(TEXT, "<replaced emoji>");
    }
/*
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
*/
}

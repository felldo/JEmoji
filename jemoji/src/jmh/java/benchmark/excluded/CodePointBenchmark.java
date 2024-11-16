package benchmark.excluded;

import benchmark.EmojiManagerBenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CodePointBenchmark {

    private static final String TEXT = new BufferedReader(new InputStreamReader(Objects.requireNonNull(EmojiManagerBenchmark.class.getClassLoader().getResourceAsStream("ExampleTextFileWithEmojis.txt"))))
            .lines().collect(Collectors.joining("\n"));

    @Param("\uD83D\uDC4D\uD83C\uDFFC")
    private String alias;

    /*
    Benchmark                                            (alias)  Mode  Cnt   Score   Error  Units
    CodePointBenchmark.codePointArrayLength                👍🏼    avgt   10  20,336 ± 0,062  ns/op
    CodePointBenchmark.codePointCount                      👍🏼    avgt   10   2,838 ± 0,011  ns/op
    CodePointBenchmark.codePointStreamCount                👍🏼    avgt   10  22,974 ± 0,236  ns/op
    CodePointBenchmark.codepointsStreamToArray             👍🏼    avgt   10  1707,719 ±  11,200  us/op
    CodePointBenchmark.codepointsArrayForIndex             👍🏼    avgt   10   610,829 ± 112,007  us/op
    CodePointBenchmark.codepointsStreamToArrayIterator     👍🏼    avgt   10  1604,416 ±   7,238  us/op
     */

    @Benchmark
    public int codePointArrayLength() {
        return alias.codePoints().toArray().length;
    }

    @Benchmark
    public long codePointStreamCount() {
        return alias.codePoints().count();
    }

    @Benchmark
    public int codePointCount() {
        return alias.codePointCount(0, alias.length());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public int[] codepointsStreamToArray() {
        return TEXT.codePoints().toArray();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public int[] codepointsStreamToArrayIterator() {
        int[] codePoints = new int[getCodePointCount(TEXT)];
        int j = 0;
        for (final int codepoint : (Iterable<Integer>) TEXT.codePoints()::iterator) {
            codePoints[j++] = codepoint;
        }
        return codePoints;
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public int[] codepointsArrayForIndex() {
        int[] codePoints = new int[getCodePointCount(TEXT)];
        int j = 0;
        for (int i = 0; i < TEXT.length(); ) {
            final int codePoint = TEXT.codePointAt(i);
            codePoints[j++] = codePoint;
            i += Character.charCount(codePoint);
        }
        return codePoints;
    }

    private static int getCodePointCount(final String text) {
        return text.codePointCount(0, text.length());
    }
}

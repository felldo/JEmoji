package benchmark.excluded;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class CodePointBenchmark {
    @Param("\uD83D\uDC4D\uD83C\uDFFC")
    private String alias;

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
}
